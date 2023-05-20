package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class APILOT {
    public static final String TABLE_NAME = "apilots";
    public static final String MODEL_COLUMN = "model";
    public static final String DEVELOPER_COLUMN = "developer";
    public static final String ID_COLUMN = "id";

    public Integer id;
    public String model;
    public String developer;

    public APILOT(String model, String developer) {
        this.model = model;
        this.developer = developer;
    }

    private static APILOT from(Row row) {
        return new APILOT(row.getInteger(ID_COLUMN), row.getString(MODEL_COLUMN), row.getString(DEVELOPER_COLUMN));
    }

    public static Uni<APILOT> findByModelAndDeveloper(MySQLPool client, String model, String developer) {
        String preparedQuery = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?",
                 TABLE_NAME, MODEL_COLUMN, DEVELOPER_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(model, developer))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ?  from(iterator.next()) : null);
    }

    public static Uni<APILOT> findById(MySQLPool client, Integer id) {
        String preparedQuery = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, ID_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ?  from(iterator.next()) : null);
    }

    public Uni<String> save(MySQLPool client) {
        String preparedQuery = String.format("INSERT INTO %s(%s, %s) values (?, ?)",
                TABLE_NAME, MODEL_COLUMN, DEVELOPER_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(model, developer))
                .onItem().transform(pgRowSet -> (pgRowSet.rowCount() == 1) ? model+"-"+developer : "");
    }

    public static Multi<APILOT> findAll(MySQLPool client) {
        return client.query(String.format("SELECT * FROM %s;", TABLE_NAME)).execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(APILOT::from);
    }

    public static Uni<Boolean> delete(MySQLPool client, String model, String developer) {
        String preparedQuery = String.format("DELETE FROM %s WHERE %s = ? AND %s = ?",
                TABLE_NAME, MODEL_COLUMN, DEVELOPER_COLUMN);
        return client.preparedQuery(preparedQuery).execute(Tuple.of(model, developer))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

}
