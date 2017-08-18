package com.nsy209.nicedriver;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
	@GET
	@Path("/locations")
	@Produces(MediaType.APPLICATION_JSON)
	public String locations() throws IOException {
		InputStream is = 
                JsonParsing.class.getResourceAsStream("/com/nsy209/nicedriver/data/locations.json");
        String jsonTxt = IOUtils.toString(is);
		return jsonTxt;
	}
	
	@GET
	@Path("/trips")
	@Produces(MediaType.APPLICATION_JSON)
	public String trips() throws IOException {
		InputStream is = 
                JsonParsing.class.getResourceAsStream("/com/nsy209/nicedriver/data/trips.json");
        String jsonTxt = IOUtils.toString(is);
		return jsonTxt;
	}
	
	@GET
	@Path("/signals")
	@Produces(MediaType.APPLICATION_JSON)
	public String signals() throws IOException {
		InputStream is = 
                JsonParsing.class.getResourceAsStream("/com/nsy209/nicedriver/data/signals.json");
        String jsonTxt = IOUtils.toString(is);
		return jsonTxt;
	}

	@POST
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PointCalcul> getListePoint(String data)
			throws JSONException, JsonParseException, JsonMappingException, IOException {

		System.out.println(data);
		
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

			System.out.println("dateSignal : " + dateSignal);

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
				System.out.println("date du signal : " + signal.getDate());
				System.out.println("date de A : " + A.getDate());
				System.out.println("date de B : " + B.getDate());
				System.out.println("----------------------");
				System.out.println("lon de A : " + A.getLongitude());
				System.out.println("lat de A : " + A.getLatitude());
				System.out.println("----------------------");
				System.out.println("lon de B : " + B.getLongitude());
				System.out.println("lat de B : " + B.getLatitude());
				System.out.println("----------------------");

				PointCalcul pointCalcul = this.midPoint(A, B);

				System.out.println("lon de Final : " + pointCalcul.getLongitude());
				System.out.println("lat de Final : " + pointCalcul.getLatitude());
				System.out.println("_______________________________________________");

				pointCalcul.setDate(signal.getDate());
				pointCalcul.setValeur(signal.getValue());
				pointsCalcul.add(pointCalcul);
			}
		}

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

		// print out in degrees
		System.out.println(Math.toDegrees(lat3) + " " + Math.toDegrees(lon3));

		pointCalcul.setLatitude(Math.toDegrees(lat3));
		pointCalcul.setLongitude(Math.toDegrees(lon3));
		return pointCalcul;
	}
}
