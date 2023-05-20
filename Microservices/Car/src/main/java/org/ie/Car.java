package org.ie;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

public class Car {
    /* Attributes */ 
    public String name;
    public String manufactor;
    public Integer cost;
    public Integer stock;
   

    /* Constructors */ 

    public Car() {}



    public Car(String name, String manufactor,Integer cost, Integer stock) {
        this.name = name;
        this.manufactor = manufactor;
        this.cost = cost;
        this.stock = stock;
    }



 
    /* Methods*/
    
    private static Car from(Row row) {
        return new Car(row.getString("name"),row.getString("manufactor"),row.getInteger("cost"),row.getInteger("stock"));
    }

    
    public static Multi<Car> findAll(MySQLPool client) {
        return client.query("SELECT name, manufactor,cost, stock FROM AVaas ORDER BY name ASC").execute()
        .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
        .onItem().transform(Car::from);
    }

    public Uni<Boolean> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO AVaas(name,manufactor,cost,stock) VALUES (?,?,?,?)").execute(Tuple.of(name,manufactor,cost,stock))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public static Uni<Boolean> delete(MySQLPool client, String name, String manufactor) {
        return client.preparedQuery("DELETE FROM AVaas WHERE name = ? AND manufactor = ?").execute(Tuple.of(name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); 
    }

    public static Uni<Boolean> add(MySQLPool client,String name, String manufactor) {
        return client.preparedQuery("UPDATE AVaas SET stock=stock+1  WHERE name = ? AND manufactor = ?").execute(Tuple.of(name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }


    public static Uni<Boolean> remove(MySQLPool client,String name, String manufactor) {
        return client.preparedQuery("UPDATE AVaas SET stock=CASE WHEN stock > 0 THEN stock -1 ELSE 0 END WHERE name = ? AND manufactor = ?").execute(Tuple.of(name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }


    public static Uni<Boolean> updateCost(MySQLPool client,String name, String manufactor, Integer cost) {
        return client.preparedQuery("UPDATE AVaas SET cost = ? WHERE name = ? AND manufactor = ?").execute(Tuple.of(cost,name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public static Uni<Boolean> updateStock(MySQLPool client,String name, String manufactor, Integer stock) {
        return client.preparedQuery("UPDATE AVaas SET stock = ? WHERE name = ? AND manufactor = ?").execute(Tuple.of(stock,name,manufactor))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
    }

    public String getName(){
        return this.name;
    }

    public String getManufactor(){
        return this.manufactor;
    }

    public Integer getStock(){
        return this.stock;
    }

    public Integer getCost(){
        return this.cost;
    }



    
}