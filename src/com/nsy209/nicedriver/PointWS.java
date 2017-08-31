package com.nsy209.nicedriver;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.plugins.providers.jaxb.json.JsonParsing;

@Path("/points")
public class PointWS {

	public Connection connectionDB() throws InstantiationException, IllegalAccessException, SQLException {
		String url = "jdbc:mysql://91.134.139.64:32768/NiceDriver_Data?rewriteBatchedStatements=true";
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Connection con = DriverManager.getConnection(url, "root", "projetCNAM");
		return con;
	}

	@GET
	@Path("/locations")
	@Produces(MediaType.APPLICATION_JSON)
	public String locations() throws IOException {
		InputStream is = JsonParsing.class.getResourceAsStream("/com/nsy209/nicedriver/data/locations.json");
		String jsonTxt = IOUtils.toString(is);
		return jsonTxt;
	}

	@GET
	@Path("/trips")
	@Produces(MediaType.APPLICATION_JSON)
	public String trips() throws IOException {
		InputStream is = JsonParsing.class.getResourceAsStream("/com/nsy209/nicedriver/data/trips.json");
		String jsonTxt = IOUtils.toString(is);
		return jsonTxt;
	}

	@GET
	@Path("/signals")
	@Produces(MediaType.APPLICATION_JSON)
	public String signals() throws IOException {
		InputStream is = JsonParsing.class.getResourceAsStream("/com/nsy209/nicedriver/data/signals.json");
		String jsonTxt = IOUtils.toString(is);
		return jsonTxt;
	}

	@GET
	@Path("{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PointCalcul> getListePoint(@PathParam("type") String type, @QueryParam("longitude1") double longitude1,
			@QueryParam("latitude1") double latitude1, @QueryParam("longitude2") double longitude2,
			@QueryParam("latitude2") double latitude2)
			throws IOException, InstantiationException, IllegalAccessException, SQLException, ParseException {

		List<PointCalcul> pointsCalcul = new ArrayList<PointCalcul>();

		Connection con = this.connectionDB();

		Statement stmt = con.createStatement();
		ResultSet resultats = stmt.executeQuery("SELECT * FROM PointCalcul WHERE type=\"" + type + "\"");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		while (resultats.next()) {
			PointCalcul pointCalcul = new PointCalcul();
			pointCalcul.setCouleur(resultats.getString("couleur"));
			pointCalcul.setLatitude(resultats.getDouble("latitude"));
			pointCalcul.setLongitude(resultats.getDouble("longitude"));
			pointCalcul.setDate(sdf.format(resultats.getString("date")));
			pointCalcul.setValeur(resultats.getDouble("valeur"));
			pointCalcul.setType(resultats.getString("type"));
			pointsCalcul.add(pointCalcul);
		}
		resultats.close();

		return pointsCalcul;
	}

	@POST
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PointCalcul> getListePointTrajet(String data)
			throws JSONException, JsonParseException, JsonMappingException, IOException, InstantiationException,
			IllegalAccessException, SQLException, ParseException {

		Connection con = this.connectionDB();

		Statement stmt = con.createStatement();

		List<PointCalcul> pointsCalcul = new ArrayList<PointCalcul>();

		JSONObject jsonObject = new JSONObject(data);

		ObjectMapper mapper = new ObjectMapper();
		String jsonPointsTrajet = jsonObject.getJSONArray("locations").toString();
		String jsonSignal = jsonObject.getJSONArray("signals").toString();

		List<PointTrajet> pointsTrajet = mapper.readValue(jsonPointsTrajet,
				mapper.getTypeFactory().constructParametricType(List.class, PointTrajet.class));

		List<Signal> signaux = mapper.readValue(jsonSignal,
				mapper.getTypeFactory().constructParametricType(List.class, Signal.class));

		PointTrajet A = new PointTrajet();
		PointTrajet B = new PointTrajet();
		for (Signal signal : signaux) {
			long dateSignal = signal.getDate().getTime();
			long min = 0;
			for (PointTrajet pointTrajet : pointsTrajet) {

				long datePoint = pointTrajet.getDate().getTime();
				// System.out.println("datePoint : " + datePoint);

				if (datePoint < dateSignal) {
					final long diff = Math.abs(signal.getDate().getTime() - pointTrajet.getDate().getTime());
					if (min == 0) {
						min = diff;
						A = pointTrajet;
					}
					if (diff < min) {
						min = diff;
						A = pointTrajet;
					}
				} else {
					final long diff = Math.abs(pointTrajet.getDate().getTime() - signal.getDate().getTime());
					if (min == 0) {
						min = diff;
						B = pointTrajet;
					}
					if (diff < min) {
						min = diff;
						B = pointTrajet;
					}

				}
			}
			PointCalcul pointCalcul = this.midPoint(A, B);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
			pointCalcul.setDate(sdf.format(signal.getDate()));
			pointCalcul.setValeur(signal.getValue());
			pointCalcul.setType(signal.getName());
			stmt.addBatch(
					"INSERT INTO `PointCalcul` (`id`, `latitude`, `longitude`, `date`, `valeur`, `couleur`, `type`) VALUES (NULL, '"
							+ pointCalcul.getLatitude() + "', '" + pointCalcul.getLongitude() + "', '"
							+ sdf.format(signal.getDate()) + "', '"
							+ pointCalcul.getValeur() + "', '" + pointCalcul.getCouleur() + "', '"
							+ pointCalcul.getType() + "')");
			pointsCalcul.add(pointCalcul);
		}
		
		stmt.executeBatch();
		con.close();
		return pointsCalcul;
	}

	public PointCalcul midPoint(PointTrajet A, PointTrajet B) {
		PointCalcul pointCalcul = new PointCalcul();

		double lon1 = A.getLongitude();
		double lon2 = B.getLongitude();

		double lat1 = A.getLatitude();
		double lat2 = B.getLatitude();

		double dLon = Math.toRadians(lon2 - lon1);

		// convert to radians
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		lon1 = Math.toRadians(lon1);

		double Bx = Math.cos(lat2) * Math.cos(dLon);
		double By = Math.cos(lat2) * Math.sin(dLon);
		double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2),
				Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
		double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

		pointCalcul.setLatitude(Math.toDegrees(lat3));
		pointCalcul.setLongitude(Math.toDegrees(lon3));
		return pointCalcul;
	}
}
