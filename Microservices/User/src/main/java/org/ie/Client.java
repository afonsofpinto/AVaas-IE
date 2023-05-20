package org.ie;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

public class Client {
    /* Attributes */ 
    public Long id;
    public String name;
    public Integer balance;
   

    /* Constructors */ 

    public Client() {}

    public Client(Long id ,String name, Integer balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }
 
    /* Methods*/
    
    private static Client from(Row row) {
        return new Client(row.getLong("id"),row.getString("name"),row.getInteger("balance"));
    }

    
    public static Multi<Client> findAll(MySQLPool client) {
        return client.query("SELECT id, name, balance FROM Client ORDER BY name ASC").execute()
        .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
        .onItem().transform(Client::from);
    }


    public static Uni<Boolean> subscribe(MySQLPool client,String name, Integer balance) {
        return client.preparedQuery("INSERT INTO Client(name,balance) VALUES (?,?)").execute(Tuple.of(name,balance))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public static Uni<Boolean> unsubscribe(MySQLPool client, Long id) {
        return client.preparedQuery("DELETE FROM Client WHERE id = ?").execute(Tuple.of(id))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); 
    }


    public static Uni<Boolean> updateBalance(MySQLPool client,Long id, Integer car_price) {
        return client.preparedQuery("UPDATE Client SET balance = balance - ? WHERE id = ?").execute(Tuple.of(car_price,id))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public static Uni<Boolean> incrementBalance(MySQLPool client,Long id, Integer balance) {
        return client.preparedQuery("UPDATE Client SET balance = balance + ? WHERE id = ?").execute(Tuple.of(balance,id))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public static Uni<Boolean> updateName(MySQLPool client,Long id, String name) {
        return client.preparedQuery("UPDATE Client SET name = ? WHERE id = ?").execute(Tuple.of(name,id))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }


    public String getName(){
        return this.name;
    }

    public Integer getBalance(){
        return this.balance;
    }
    
}