package org.ie;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

public class State {

    /* Attributes */ 
    public Long client_id;
    public Long apilot_id;
    public String car_name;
    public String manufactor;
    //public Integer car_price;
  
   
    /* Constructors */ 
    public State() {}


    public State(Long client_id,Long apilot_id,String car_name,String manufactor) { 
        this.client_id = client_id;
        this.apilot_id = apilot_id;
        this.car_name= car_name;
        this.manufactor = manufactor;
    }


    public static Multi<State> findAll(MySQLPool client,SecretKey key) {
        String query = "SELECT client_id, apilot_id, car_name, manufactor FROM State ORDER BY client_id ASC";
            return client
            .preparedQuery(query)
            .execute()
            .onItem()
            .transformToMulti(result -> {
                List<State> states = new ArrayList<>();
                for (Row row : result) {
                    String encryptedCarName = row.getString("car_name");
                    String encryptedManufacturer = row.getString("manufactor");
                    Long client_id = row.getLong("client_id");
                    Long apilot_id = row.getLong("apilot_id");
                    String carName = "";
                    String manufacturer = "";
                    try {
                        System.out.println("Entrei");
                        carName = Crypto.decodeAndDecrypt(encryptedCarName, key);
                        System.out.println(carName);
                        manufacturer = Crypto.decodeAndDecrypt(encryptedManufacturer, key);
                        System.out.println(manufacturer);
                    } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException
                            | BadPaddingException | NoSuchPaddingException e) {
                        throw new RuntimeException("Error decrypting values", e);
                    }
                    State state = new State(client_id, apilot_id, carName, manufacturer);
                    states.add(state);
                }
                return Multi.createFrom().iterable(states);
            });
    }

  public static Multi<State> findAllCarsFromUser(MySQLPool client, Long client_id, SecretKey key) {
    String query = "SELECT car_name, apilot_id, manufactor FROM State WHERE client_id = ?";
    return client
        .preparedQuery(query)
        .execute(Tuple.of(client_id))
        .onItem()
        .transformToMulti(result -> {
            List<State> states = new ArrayList<>();
            for (Row row : result) {
                String encryptedCarName = row.getString("car_name");
                String encryptedManufacturer = row.getString("manufactor");
                Long apilot_id = row.getLong("apilot_id");
                String carName = "";
                String manufacturer = "";
                try {
                    carName = Crypto.decodeAndDecrypt(encryptedCarName, key);
                    manufacturer = Crypto.decodeAndDecrypt(encryptedManufacturer, key);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException
                        | BadPaddingException | NoSuchPaddingException e) {
                    throw new RuntimeException("Error decrypting values", e);
                }
                State state = new State(client_id,apilot_id, carName, manufacturer);
                states.add(state);
            }
            return Multi.createFrom().iterable(states);
        });
    }


    public static Uni<Boolean> buyCar(MySQLPool client,Long client_id,String carName,String manufactor,SecretKey key) {
        try {
            return client.preparedQuery("INSERT INTO State(client_id, apilot_id, car_name, manufactor) VALUES (?, ?, ?, ?)")
            .execute(Tuple.of(client_id, null, Crypto.encryptAndEncode(carName, key), Crypto.encryptAndEncode(manufactor, key)))
            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);

        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Uni<Boolean> sellCar(MySQLPool client, Long client_id, String car_name, String manufactor,SecretKey key) {
        try {
            return client.preparedQuery("DELETE FROM State WHERE client_id = ?  AND car_name = ? AND manufactor = ?").execute(Tuple.of(client_id,Crypto.encryptAndEncode(car_name, key),Crypto.encryptAndEncode(manufactor, key)))
            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); 
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Uni<Boolean> selectApilot(MySQLPool client, Long client_id, Long apilot_id, String car_name, String manufactor,SecretKey key) {
        try {
            return client.preparedQuery("UPDATE State SET apilot_id = ? WHERE client_id = ? AND car_name = ? AND manufactor = ?").
            execute(Tuple.of(apilot_id,client_id,Crypto.encryptAndEncode(car_name, key),Crypto.encryptAndEncode(manufactor, key)))
            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        } 
        return null;
    }

    public static Uni<Boolean> unselectApilot(MySQLPool client, Long client_id, String car_name, String manufactor,SecretKey key) {
        try {
            return client.preparedQuery("UPDATE State SET apilot_id = NULL WHERE client_id = ? AND car_name = ? AND manufactor = ?").execute(Tuple.of(client_id,Crypto.encryptAndEncode(car_name, key),Crypto.encryptAndEncode(manufactor, key)))
            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null; 
    }


    public static Uni<Boolean> unsubscribe(MySQLPool client, Long client_id) {
        return client.preparedQuery("DELETE FROM State WHERE client_id = ?").execute(Tuple.of(client_id))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); 
    }


    public Long getClient_id(){
        return this.client_id;
    }

    public Long getApilot_id(){
        return this.apilot_id;
    }

    public String getCar_name(){
        return this.car_name;
    }

    public String getManufactor(){
        return this.manufactor;
    }

    /*public Integer getCarPrice(){
        return this.car_price;
    }*/


    
}

