package org.ie;
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



 
    /* Methods*/
    
    private static State from(Row row) {
        return new State(row.getLong("client_id"),row.getLong("apilot_id"),row.getString("car_name"),row.getString("manufactor"));
    }
  
  
    public static Multi<State> findAll(MySQLPool client) {
        return client.query("SELECT client_id, apilot_id, car_name, manufactor FROM State ORDER BY client_id ASC").execute()
        .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
        .onItem().transform(State::from);
    }
    public Uni<Boolean> buyCar(MySQLPool client) {
        return client.preparedQuery("INSERT INTO State(client_id, apilot_id, car_name, manufactor) VALUES (?, ?, ?, ?)")
        .execute(Tuple.of(client_id, null, car_name, manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }
    
    public static Uni<Boolean> sellCar(MySQLPool client, Long client_id, String car_name, String manufactor) {
        return client.preparedQuery("DELETE FROM State WHERE client_id = ?  AND car_name = ? AND manufactor = ?").execute(Tuple.of(client_id,car_name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); 
    }


    public static Uni<Boolean> selectApilot(MySQLPool client, Long client_id, Long apilot_id, String car_name, String manufactor) {
        return client.preparedQuery("UPDATE State SET apilot_id = ? WHERE client_id = ? AND car_name = ? AND manufactor = ?").execute(Tuple.of(apilot_id,client_id,car_name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public static Uni<Boolean> unselectApilot(MySQLPool client, Long client_id, String car_name, String manufactor) {
        return client.preparedQuery("UPDATE State SET apilot_id = NULL WHERE client_id = ? AND car_name = ? AND manufactor = ?").execute(Tuple.of(client_id,car_name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
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

