package org.ie;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
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
 
    
  public static Multi<Client> findAll(MySQLPool client, SecretKey key) throws InvalidKeyException,
        IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
    return client.query("SELECT id, name, balance FROM Client ORDER BY name ASC").execute()
            .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
            .onItem().transform(row -> {
                Long id = row.getLong("id");
                String encryptedName = row.getString("name");
                String encryptedBalance = row.getString("balance");
                String name = "";
                Integer balance = 0;
                try {
                    balance = Integer.parseInt(Crypto.decodeAndDecrypt(encryptedBalance, key));
                    name = Crypto.decodeAndDecrypt(encryptedName, key);
                } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                        | IllegalBlockSizeException | BadPaddingException e) {
                    e.printStackTrace();
                }
                return new Client(id, name, balance);
            });
    }


   public static Multi<Client> findClientState(MySQLPool client, Long id, SecretKey key) throws InvalidKeyException,
    IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
    String query = "SELECT id, name, balance FROM Client WHERE id = ?";
    return client.preparedQuery(query)
        .execute(Tuple.of(id))
        .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
        .onItem().transform(row -> {
            Long clientId = row.getLong("id");
            String encryptedName = row.getString("name");
            String encryptedBalance = row.getString("balance");
            String name = "";
            Integer balance = 0;
            try {
                balance = Integer.parseInt(Crypto.decodeAndDecrypt(encryptedBalance, key));
                name = Crypto.decodeAndDecrypt(encryptedName, key);
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                    | IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
            return new Client(clientId, name, balance);
        });
}



    public static Uni<Boolean> subscribe(MySQLPool client,String name, Integer balance, SecretKey key) {
        try {
            return client.preparedQuery("INSERT INTO Client(name,balance) VALUES (?,?)").execute(Tuple.of(Crypto.encryptAndEncode(name, key),Crypto.encryptAndEncode(Integer.toString(balance), key)))
            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null; 
    }

    public static Uni<Boolean> unsubscribe(MySQLPool client, Long id) {
        return client.preparedQuery("DELETE FROM Client WHERE id = ?").execute(Tuple.of(id))
        .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); 
    }


    public static Uni<Boolean> updateBalance(MySQLPool client, Long id, Integer carPrice, SecretKey key) {
        return client.preparedQuery("SELECT balance FROM Client WHERE id = ?").execute(Tuple.of(id))
                .onItem().transformToUni(result -> {
                    Row row = result.iterator().next();
                    String encryptedBalance = row.getString("balance");
                    Integer balance;
                    try {
                        balance = Integer.parseInt(Crypto.decodeAndDecrypt(encryptedBalance, key));
                        balance -= carPrice;
                        encryptedBalance = Crypto.encryptAndEncode(Integer.toString(balance), key);
                    } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException |
                            BadPaddingException | NoSuchPaddingException e) {
                        throw new RuntimeException("Error decrypting/encrypting balance", e);
                    }
                    return client.preparedQuery("UPDATE Client SET balance = ? WHERE id = ?")
                            .execute(Tuple.of(encryptedBalance, id))
                            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
                });
    }

   public static Uni<Boolean> incrementBalance(MySQLPool client, Long id, Integer balanceIncrement, SecretKey key) {
        return client.preparedQuery("SELECT balance FROM Client WHERE id = ?").execute(Tuple.of(id))
                .onItem().transformToUni(result -> {
                    Row row = result.iterator().next();
                    String encryptedBalance = row.getString("balance");
                    Integer balance;
                    try {
                        balance = Integer.parseInt(Crypto.decodeAndDecrypt(encryptedBalance, key));
                        balance += balanceIncrement;
                        encryptedBalance = Crypto.encryptAndEncode(Integer.toString(balance), key);
                    } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException |
                            BadPaddingException | NoSuchPaddingException e) {
                        throw new RuntimeException("Error decrypting/encrypting balance", e);
                    }
                    return client.preparedQuery("UPDATE Client SET balance = ? WHERE id = ?")
                            .execute(Tuple.of(encryptedBalance, id))
                            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
                });  
    }

    public String getName(){
        return this.name;
    }

    public Integer getBalance(){
        return this.balance;
    }


    public void decryptParameters() throws InvalidKeyException, NoSuchAlgorithmException{
        
    }
    
}