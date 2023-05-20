package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class APILOTDeveloper {
    public static final String TABLE_NAME = "apilot_developers";
    public static final String NAME_COLUMN = "name";

    public String name;

    private static APILOTDeveloper from(Row row) {
        return new APILOTDeveloper(row.getString(NAME_COLUMN));
    }

    public static Uni<APILOTDeveloper> findByName(MySQLPool client, String name) {
        String preparedQuery = String.format("SELECT * FROM %s WHERE %s = ?",
                 TABLE_NAME, NAME_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(name))
            .onItem().transform(RowSet::iterator)
            .onItem().transform(iterator -> iterator.hasNext() ?  from(iterator.next()) : null);
    }

    public Uni<String> save(MySQLPool client) {
        String preparedQuery = String.format("INSERT INTO %s(%s) values (?)", TABLE_NAME, NAME_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(name))
            .onItem().transform(pgRowSet -> (pgRowSet.rowCount() == 1) ? name : "");
    }

    public static Multi<APILOTDeveloper> findAll(MySQLPool client) {
        return client.query(String.format("SELECT * FROM %s;", TABLE_NAME)).execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(APILOTDeveloper::from);
    }

    public static Uni<Boolean> delete(MySQLPool client, String name) {
        String preparedQuery = String.format("DELETE FROM %s WHERE %s = ?",
                TABLE_NAME, NAME_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(name))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }
 
}
