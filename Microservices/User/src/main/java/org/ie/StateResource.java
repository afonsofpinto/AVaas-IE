package org.ie;
import io.smallrye.mutiny.Uni;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
import org.eclipse.microprofile.openapi.annotations.media.Schema.True;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import io.smallrye.mutiny.Multi;
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
    Integer done;
    static SecretKey key;
    public static void generateKey(){
        try {
            key = Crypto.generateAESKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String object) throws InvalidKeyException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException{
            return Crypto.encryptAndEncode(object, key);
    }

    public Object decrypt(String object) throws InvalidKeyException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException{
            return Crypto.decodeAndDecrypt(object, key);
    }

    
    private void initdb() {

        generateKey();
        String carName = "";
        String manufactor = "";
        try {
            manufactor = encrypt("Ford");
            carName = encrypt("Focus");
            System.out.println(carName);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        
        client.query("DROP TABLE IF EXISTS State").execute().await().indefinitely();

        client.query("CREATE TABLE State (client_id BIGINT, apilot_id BIGINT NULL, car_name varchar(100) NOT NULL, manufactor varchar(100) NOT NULL, PRIMARY KEY (client_id, car_name, manufactor))").execute().await().indefinitely();

        client.preparedQuery("INSERT INTO State (client_id, apilot_id, car_name, manufactor) VALUES (1, NULL, ?, ?)").execute(Tuple.of(carName, manufactor)).await().indefinitely();
        
        String name = "";
        String balance = "";
        try {
            name = encrypt("Carlos");
            balance = encrypt("7500000"); 
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        client.query("DROP TABLE IF EXISTS Client").execute().await().indefinitely();
        client.query("CREATE TABLE Client (id SERIAL PRIMARY KEY, name varchar(100) NOT NULL, balance varchar(100) NOT NULL)").execute().await().indefinitely();
        client.query("ALTER TABLE Client AUTO_INCREMENT = 1").execute().await().indefinitely();
        client.preparedQuery("INSERT INTO Client (name, balance) VALUES (?, ?)").execute(Tuple.of(name, balance)).await().indefinitely();


            
    }
    
    @GET
    @Path("/getAllClients")
    public Multi<Client> getAll() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        return Client.findAll(client,key);
    }


    @GET
    @Path("/getClientState/{client_id}")
    public Multi<Client> getClientState(@PathParam Long client_id) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        return Client.findClientState(client,client_id,key);
    }
    

    @GET
    @Path("/getAllCarsFromUser/{client_id}")
    public Multi<State> getAllCarsFromUser(@PathParam Long client_id) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException{
        System.out.println(client_id);
        return State.findAllCarsFromUser(client,client_id,key);
    }

    @GET
    @Path("/getState")
    public Multi<State> getState() {
        return State.findAll(client,key);
    }


    @POST
    @Path("/buyCar/{balance}/{car_price}/{clientId}/{carName}/{manufactor}")
    public Uni<Response> buyCar(@PathParam Integer balance,@PathParam Integer car_price,@PathParam Long clientId,
    @PathParam String carName,@PathParam String manufactor,@PathParam Integer price) {
        Integer new_balance = balance-car_price;
        if(new_balance>0 ){
            vehicleEmitter.send((new CarEvent("BUY CAR", clientId,carName, manufactor, car_price)).toString());
            State.buyCar(client,clientId,carName,manufactor,key)
            .onItem().transform(id -> URI.create("/cars/" + id))
            .onItem().transform(uri -> Response.created(uri).build()).await().indefinitely();            
            return Client.updateBalance(client,clientId,car_price,key)
            .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());
        }
        return Uni.createFrom().nullItem();
    }

    @DELETE
    @Path("/sellCar/{id}/{car_name}/{manufactor}/{car_price}")
    public Uni<Response> sellCar(@PathParam Long id,@PathParam String car_name,@PathParam String manufactor,@PathParam Integer car_price) throws InterruptedException {
            vehicleEmitter.send((new CarEvent("SELL CAR", id, car_name, manufactor,car_price)).toString());
            Client.incrementBalance(client,id,car_price,key).onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build()).await().indefinitely();
            return State.sellCar(client,id,car_name,manufactor,key)
            .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());

    }


    @POST
    @Path("/subscribe/{name}/{balance}")
    public Uni<Response> subscribe(@PathParam String name,@PathParam Integer balance) {
        return Client.subscribe(client,name,balance,key)
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
    @Path("/selectPilot/{id}/{apilot_Id}/{car_name}/{manufactor}")
        public Uni<Response> selectapilot(@PathParam Long id,@PathParam Long apilot_Id,@PathParam String car_name,@PathParam String manufactor) {
        apilotEmitter.send("SELECTED " + apilot_Id + " from " + manufactor);
        return State.selectApilot(client, id,apilot_Id,car_name,manufactor,key)
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/unselectPilot/{id}/{apilot_Id}/{car_name}/{manufactor}")
    public Uni<Response> unselectapilot(@PathParam Long id,@PathParam Long apilot_Id,@PathParam String car_name,@PathParam String manufactor) {
        apilotEmitter.send("UNSELECTED " + apilot_Id + " from " + manufactor);
        return State.unselectApilot(client, id,car_name,manufactor,key)
        .onItem().transform(updated -> updated ? Status.NO_CONTENT : Status.NOT_FOUND)
        .onItem().transform(status -> Response.status(status).build());
    }

}