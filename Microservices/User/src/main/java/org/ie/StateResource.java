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
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.ie.Dtos.CarEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;



@Path("State")
public class StateResource {
    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;
    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true") 
    boolean schemaCreate;
    @Channel("vehicle-txns")
    Emitter<String> vehicleEmitter; // TODO change to carevent
    @Channel("apilot-txns")
    Emitter<String> apilotEmitter;  // TODO change to apilot event
    @PostConstruct
    void config() {
        if (schemaCreate) {
            initdb();
        }
    }
    
    private void initdb() {

        client.query("DROP TABLE IF EXISTS State").execute()
            .flatMap(r -> client.query("CREATE TABLE State (client_id BIGINT, apilot_id BIGINT NULL, car_name varchar(100) NOT NULL, manufactor varchar(100) NOT NULL, PRIMARY KEY (client_id, car_name, manufactor))").execute())
            .flatMap(r -> client.query("INSERT INTO State (client_id,apilot_id, car_name, manufactor) VALUES (1,NULL,'Bentley EXP 100 GT','Bentley')").execute())
            .flatMap(r -> client.query("INSERT INTO State (client_id,apilot_id, car_name, manufactor) VALUES (2,NULL,'Ford Boss 302 Mustang','Ford')").execute())
            .flatMap(r -> client.query("INSERT INTO State (client_id,apilot_id, car_name, manufactor) VALUES (3,NULL,'Lamborghini Reventon','Lamborghini')").execute())
            .flatMap(r -> client.query("INSERT INTO State (client_id,apilot_id, car_name, manufactor) VALUES (4,NULL,'Bugatti La Voiture Noire','Bugatti')").execute())
            .await().indefinitely();

        client.query("DROP TABLE IF EXISTS Client").execute()
            .flatMap(r -> client.query("CREATE TABLE Client (id SERIAL PRIMARY KEY, name varchar(100), balance Integer)").execute())
            .flatMap(r -> client.query("INSERT INTO Client (name,  balance) VALUES ('Laura',10000000)").execute())
            .flatMap(r -> client.query("INSERT INTO Client (name,  balance) VALUES ('David',10000000)").execute())
            .flatMap(r -> client.query("INSERT INTO Client (name,  balance) VALUES ('Charles',10000000)").execute())
            .flatMap(r -> client.query("INSERT INTO Client (name,  balance) VALUES ('Rita',10000000)").execute())
            .await().indefinitely();
            
    }

    
    @GET
    @Path("/getAllClients")
    public Multi<Client> getAll() {
        return Client.findAll(client);
    }

    @GET
    @Path("/getState")
    public Multi<State> getState() {
        return State.findAll(client);
    }


    @POST
    @Path("/buyCar/{balance}/{car_price}")
    public Uni<Response> buyCar(State state,@PathParam Integer balance,@PathParam Integer car_price) throws InterruptedException {
        Integer new_balance = balance-car_price;
        if(new_balance>0 ){
            vehicleEmitter.send((new CarEvent("BUY CAR", state.getClient_id(),state.getCar_name(), state.getManufactor(), car_price)).toString());
            state.buyCar(client)
            .onItem().transform(id -> URI.create("/cars/" + id))
            .onItem().transform(uri -> Response.created(uri).build()).await().indefinitely();            
            return Client.updateBalance(client,state.getClient_id(),car_price)
            .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());

        }
        return Uni.createFrom().nullItem();
    }

    @DELETE
    @Path("/sellCar/{id}/{car_name}/{manufactor}/{car_price}")
    public Uni<Response> sellCar(@PathParam Long id,@PathParam String car_name,@PathParam String manufactor,@PathParam Integer car_price) throws InterruptedException {
            vehicleEmitter.send((new CarEvent("SELL CAR", id, car_name, manufactor,car_price)).toString());
            Client.incrementBalance(client,id,car_price).onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build()).await().indefinitely();
            System.out.println("Ola1");
            System.out.println("Ola3");
            return State.sellCar(client,id,car_name,manufactor)
            .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());

    }


    @POST
    @Path("/subscribe/{name}/{balance}")
    public Uni<Response> subscribe(@PathParam String name,@PathParam Integer balance) {
        return Client.subscribe(client,name,balance)
        .onItem().transform(id -> URI.create("/cars/" + id))
        .onItem().transform(uri -> Response.created(uri).build());
    }


    @DELETE
    @Path("/unsubscribe/{id}")
    public Uni<Response> unsubscribe(@PathParam Long id) {
            State.unsubscribe(client, id).onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build()).await().indefinitely();
            return Client.unsubscribe(client,id)
            .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());

    }
    
    @PUT
    @Path("/selectPilot")
        public Uni<Response> selectapilot(State state) {
        apilotEmitter.send("SELECTED " + state.getApilot_id() + " from " + state.getManufactor());
        return State.selectApilot(client, state.getClient_id(),state.getApilot_id(),state.getCar_name(),state.getManufactor())
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/unselectPilot")
    public Uni<Response> unselectapilot(State state) {
        apilotEmitter.send("UNSELECTED " + state.getApilot_id() + " from " + state.getManufactor());
        return State.unselectApilot(client, state.getClient_id(),state.getCar_name(),state.getManufactor())
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

}