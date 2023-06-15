package org.ie;
import java.net.URI;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
@Path("Cars")
public class CarResource {
    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;
    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true") 
    boolean schemaCreate;
    @PostConstruct
    void config() {
        if (schemaCreate) {
            initdb();
        }
    }
    
    private void initdb() {
        client.query("DROP TABLE IF EXISTS AVaas").execute()
            .flatMap(r -> client.query("CREATE TABLE AVaas (name varchar(100) NOT NULL, manufactor varchar(100) NOT NULL, cost integer, stock integer, PRIMARY KEY (name, manufactor))").execute())
            .flatMap(r -> client.query("INSERT INTO AVaas (name, manufactor, cost, stock) VALUES ('Focus', 'Ford',1900000, 10)").execute())

            .await().indefinitely();

    }
    
    
    @GET
    @Path("/getAllCars")
    public Multi<Car> getAllCars() {
        return Car.findAll(client);
    }


    @POST
    @Path("/createCar")
    public Uni<Response> createCar(Car car) {
        return car.save(client)
        .onItem().transform(id -> URI.create("/cars/" + id))
        .onItem().transform(uri -> Response.created(uri).build());
    }


    @DELETE
    @Path("/delete/{name}/{manufactor}")
    public Uni<Response> deleteCar(@PathParam String name,@PathParam String manufactor) {
        return Car.delete(client,name,manufactor)
        .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }


    @PUT
    @Path("/addOne/{name}/{manufactor}")
        public Uni<Response> addOneCar( @PathParam String name,@PathParam String manufactor) {
        return Car.add(client, name, manufactor)
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/removeOne/{name}/{manufactor}")
        public Uni<Response> removeOneCar( @PathParam String name,@PathParam String manufactor) {
        return Car.remove(client, name,manufactor)
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/update_cost")
    public Uni<Response> updateCost(Car car) {
        return Car.updateStock(client, car.getName(), car.getManufactor(),car.getCost())
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/update_stock")
        public Uni<Response> updateStock(Car car) {
        return Car.updateStock(client, car.getName(), car.getManufactor(),car.getStock())
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }
    
}