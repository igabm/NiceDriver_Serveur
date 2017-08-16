import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public class PointWS extends Application{
	   @GET 
	   @Path("/points") 
	   @Produces(MediaType.APPLICATION_JSON) 
	   public String getListePoint(){ 
	      return "Toto"; 
	   }  
}
