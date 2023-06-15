package org.acme;

import java.net.URI;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("")
public class APILOTResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean createSchema;

    @PostConstruct
    void config() {
        if (createSchema) {
            initdb();
        }
    }

    private void initdb() {
        // clear tables
        client.query("DROP TABLE IF EXISTS " + APILOT.TABLE_NAME).execute()
                .flatMap(r -> client.query("DROP TABLE IF EXISTS " + APILOTDeveloper.TABLE_NAME).execute())
                // apilot_manufacturer table
                .flatMap(r -> client.query(
                        String.format("CREATE TABLE %s ( %s VARCHAR(50) PRIMARY KEY)",
                        APILOTDeveloper.TABLE_NAME, APILOTDeveloper.NAME_COLUMN)).execute())
                .flatMap(r -> client.query(String.format("INSERT INTO %s (%s) VALUES ('Aurora')",
                        APILOTDeveloper.TABLE_NAME, APILOTDeveloper.NAME_COLUMN)).execute())
                // apilots table
                .flatMap(r -> client.query(
                        String.format("CREATE TABLE %s (", APILOT.TABLE_NAME) +
                        String.format("%s SERIAL PRIMARY KEY, %s VARCHAR(50)  NOT NULL, %s VARCHAR(50),",
                                APILOT.ID_COLUMN, APILOT.MODEL_COLUMN, APILOT.DEVELOPER_COLUMN) +
                        String.format("FOREIGN KEY (%s) references %s(%s) )",
                                APILOT.DEVELOPER_COLUMN, APILOTDeveloper.TABLE_NAME, APILOTDeveloper.NAME_COLUMN)
                ).execute())
                .flatMap(r -> client.query(String.format("INSERT INTO %s (%s, %s) VALUES ('fastAI', 'Aurora')",
                        APILOT.TABLE_NAME, APILOT.MODEL_COLUMN, APILOT.DEVELOPER_COLUMN)).execute())
                .await().indefinitely();
    }


    //                      ----------- Developers -----------
    @POST
    @Path("/developers")
    public Uni<Response> createDeveloper(APILOTDeveloper developer) {
        System.out.println("Received developer with name: " + developer.name);
        return developer.save(client)
                .onItem().transform(name -> URI.create("/manufacturer/" + name))
                .onItem().transform(uri -> Response.created(uri).build());
    }

    @GET
    @Path("/developers")
    public Multi<APILOTDeveloper> getDevelopers() {
        return APILOTDeveloper.findAll(client);
    }

    @GET
    @Path("/developers/{developer}")
    public Uni<APILOTDeveloper> getDevelopers(@PathParam String developer) {
        return APILOTDeveloper.findByName(client, developer);
    }


    @DELETE
    @Path("/developers/{devName}")
    public Uni<Response> removeDeveloper(@PathParam String devName) {
        return APILOTDeveloper.delete(client, devName)
                .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    //                      ----------- APILOT -----------

    @POST
    @Path("/apilot")
    public Uni<Response> createAPILOT(APILOT apilot) {
        return apilot.save(client)
                .onItem().transform(name -> URI.create("/apilot/" + name))
                .onItem().transform(uri -> Response.created(uri).build());
    }

    @GET
    @Path("/apilots")
    public Multi<APILOT> getAPILOTS() {
        return APILOT.findAll(client);
    }

    @GET
    @Path("/apilot/{model}/{developer}")
    public Uni<APILOT> getAPILOTbyId(@PathParam String model, @PathParam String developer) {
        return APILOT.findByModelAndDeveloper(client, model, developer);
    }

    @GET
    @Path("/apilots/{id}")
    public Uni<APILOT> getAPILOTbyId(@PathParam Integer id) {
        return APILOT.findById(client, id);
    }


    @DELETE
    @Path("/apilot/{model}/{developer}")
    public Uni<Response> removeAPILOT(@PathParam String model, @PathParam String developer) {
        return APILOT.delete(client, model, developer)
                .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }


}
