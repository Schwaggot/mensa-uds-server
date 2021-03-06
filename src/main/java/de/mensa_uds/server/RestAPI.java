package de.mensa_uds.server;

import com.google.gson.Gson;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("mensaar/api/v1")
public class RestAPI {

    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "test";
    }

    @GET
    @Path("menu")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMenuStatus() {
        Gson gson = new Gson();
        return gson.toJson(DataProvider.getInstance().getMenuStatus());
    }

    @GET
    @Path("menu/{location}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMenu(@PathParam("location") String location) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("location can't be empty");
        }

        switch (location.toLowerCase()) {
            case "sb": {
                Gson gson = new Gson();
                return gson.toJson(DataProvider.getInstance().getMenuSB());
            }
            case "hom": {
                Gson gson = new Gson();
                return gson.toJson(DataProvider.getInstance().getMenuHOM());
            }
            default:
                throw new IllegalArgumentException("location must be sb or hom");
        }
    }

    @GET
    @Path("openingtimes")
    @Produces(MediaType.APPLICATION_JSON)
    public String getOpeningTimesStatus() {
        Gson gson = new Gson();
        return gson.toJson(DataProvider.getInstance().getOpeningTimesStatus());
    }

    @GET
    @Path("openingtimes/{location}")
    @Produces(MediaType.TEXT_HTML)
    public String getOpeningTimes(@PathParam("location") String location) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("location can't be empty");
        }

        switch (location.toLowerCase()) {
            case "sb": {
                return DataProvider.getInstance().getOpeningTimesSB();
            }
            case "hom": {
                return DataProvider.getInstance().getOpeningTimesHOM();
            }
            default:
                throw new IllegalArgumentException("location must be sb or hom");
        }
    }
}
