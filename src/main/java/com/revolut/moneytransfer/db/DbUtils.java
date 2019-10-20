package com.revolut.moneytransfer.db;

import com.revolut.moneytransfer.exceptions.ImpossibleOperationExecution;
import com.revolut.moneytransfer.model.ModelHasId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.function.BiConsumer;

/**
 * Utilities class contains a number of methods to manipulate with the data base
 */
public class DbUtils {
    private static final Logger log = LoggerFactory.getLogger(DbUtils.class);

    private static final DbUtils dbUtils = new DbUtils();

    private DbUtils() {
    }

    /**
     * @return the singleton object of DbUtils class
     */
    public static DbUtils getInstance() {
        return dbUtils;
    }

    /**
     * The method executes the query passed into the method with the execute method provided
     * This method responds to handle work with the connection, transaction and prepared statement life cycles
     * <p>
     * Example:
     * <PRE>
     * DbUtils.executeQuery("select * from table", ps -> {
     * ResultSet rs = ps.executeQuery();
     * if (rs != null) {
     * while (rs.next()) {
     * System.out.println(rs.getString(1));
     * }
     * }
     * });
     * </PRE>
     *
     * @param query         the query string which will be passed into <code>Connection.preparedStatement</code> method
     * @param queryExecutor the executor with only one method accepting <code>PreparedStatement</code> instance created
     * @return query result object with the only method <code>getResult</code> returns the result of queryExecutor
     */
    public <E> QueryResult<E> executeQuery(String query, QueryExecutor<E> queryExecutor) {
        Connection con = null;
        PreparedStatement preparedStatement = null;

        try {
            con = H2DataSource.getConnection();
            preparedStatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            QueryResult<E> qr = new QueryResult<>(queryExecutor.execute(preparedStatement));

            con.commit();

            return qr;
        } catch (Throwable th) {
            safeRollback(con);
            log.error("Unexpected exception", th);
            throw new ImpossibleOperationExecution(th);
        } finally {
            quietlyClose(preparedStatement);

            quietlyClose(con);
        }
    }

    /**
     * The same logic as for the <code>executeQuery</code> method without connection parameter.
     * The connection will be not committed.
     * The difference is that this method is not responsible to correctly close and rollback provided connection.
     *
     * @param con           the connection which will be used to create a prepared statement
     * @param query         the query string which will be passed into <code>Connection.preparedStatement</code> method
     * @param queryExecutor the executor with only one method accepting <code>PreparedStatement</code> instance created
     * @return query result object with the only method <code>getResult</code> returns the result of queryExecutor
     */
    public <E> QueryResult<E> executeQueryInConnection(Connection con, String query, QueryExecutor<E> queryExecutor) {
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            return new QueryResult<>(queryExecutor.execute(preparedStatement));
        } catch (Throwable th) {
            log.error("Unexpected exception", th);
            throw new ImpossibleOperationExecution(th);
        } finally {
            quietlyClose(preparedStatement);
        }
    }

    private static void quietlyClose(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                log.error("Unexpected exception", e);
            }
        }

    }

    public static void quietlyClose(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log.error("Unexpected exception", e);
            }
        }
    }

    public static void safeRollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException e) {
                log.error("Unexpected exception", e);
            }
        }
    }

    /**
     * The Interface used to implement the logic which will be applied for the provided <code>preparedStatement</code>
     * Used only to work with methods from <code>DbUtils</code> class.
     *
     * @param <T> the type of object which will be returned by the query
     */
    public interface QueryExecutor<T> {
        T execute(PreparedStatement preparedStatement) throws SQLException;
    }

    /**
     * The class used to wrap and generify result provided by <code>QueryExecutor</code>
     *
     * @param <T> the type of object which will be returned by the query
     */
    public static class QueryResult<T> {
        private T result;

        public QueryResult(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }
    }

    /**
     * The SQL query executor which ease the process of object creation with ability to update ID
     * of the just created object.
     * To create this executor you need to provide lambda function which will fill in the created prepared
     * statement with the object parameter. This function should not return anything and accept two parameters:
     * <code>PreparedStatement</code> and object itself
     * <p>
     * The result will be updated object which has been created using this executor
     *
     * @param <T> - the object which should be inserted. Used to fill in prepared statement
     */
    public static class CreationQueryExecutor<T extends ModelHasId> implements QueryExecutor<T> {
        private T object;
        private BiConsumer<PreparedStatement, T> fillInPreparedStatement;

        public CreationQueryExecutor(T object, BiConsumer<PreparedStatement, T> fillInPreparedStatement) {
            this.object = object;
            this.fillInPreparedStatement = fillInPreparedStatement;
        }

        @Override
        public T execute(PreparedStatement preparedStatement) throws SQLException {
            fillInPreparedStatement.accept(preparedStatement, object);

            int res = preparedStatement.executeUpdate();

            Long obtainedId = null;

            if (res != 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        obtainedId = generatedKeys.getLong(1);
                    }
                }
            }

            if (obtainedId == null) {
                return null;
            }

            object.setId(obtainedId);

            return object;

        }
    }
}
