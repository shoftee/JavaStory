/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.db;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 *
 * @author shoftee
 */
class PooledConnection implements Connection {

    private Connection connection;
    private ConnectionPool pool;
    private boolean isInUse;
    
    public PooledConnection(Connection inner, ConnectionPool pool) {
        this.connection = inner;
        this.pool = pool;
    }
    
    public Connection getInnerConnection() {
        return this.connection;
    }
    
    public void open() {
        this.isInUse = true;
    }
    
    @Override
	public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    @Override
	public PreparedStatement prepareStatement(String string) throws SQLException {
        return connection.prepareStatement(string);
    }

    @Override
	public CallableStatement prepareCall(String string) throws SQLException {
        return connection.prepareCall(string);
    }

    @Override
	public String nativeSQL(String string) throws SQLException {
        return connection.nativeSQL(string);
    }

    @Override
	public void setAutoCommit(boolean bln) throws SQLException {
        connection.setAutoCommit(bln);
    }

    @Override
	public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    @Override
	public void commit() throws SQLException {
        connection.commit();
    }

    @Override
	public void rollback() throws SQLException {
        connection.rollback();
    }

    @Override
	public void close() throws SQLException {
        isInUse = false;
        connection.clearWarnings();
        pool.release(this);
    }

    @Override
	public boolean isClosed() throws SQLException {
        return isInUse;
    }

    @Override
	public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    @Override
	public void setReadOnly(boolean bln) throws SQLException {
        connection.setReadOnly(bln);
    }

    @Override
	public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    @Override
	public void setCatalog(String string) throws SQLException {
        connection.setCatalog(string);
    }

    @Override
	public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    @Override
	public void setTransactionIsolation(int i) throws SQLException {
        connection.setTransactionIsolation(i);
    }

    @Override
	public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    @Override
	public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    @Override
	public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }

    @Override
	public Statement createStatement(int i, int i1) throws SQLException {
        return connection.createStatement(i, i1);
    }

    @Override
	public PreparedStatement prepareStatement(String string, int i, int i1) throws SQLException {
        return connection.prepareStatement(string, i, i1);
    }

    @Override
	public CallableStatement prepareCall(String string, int i, int i1) throws SQLException {
        return connection.prepareCall(string, i, i1);
    }

    @Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    @Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        connection.setTypeMap(map);
    }

    @Override
	public void setHoldability(int i) throws SQLException {
        connection.setHoldability(i);
    }

    @Override
	public int getHoldability() throws SQLException {
        return connection.getHoldability();
    }

    @Override
	public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
    }

    @Override
	public Savepoint setSavepoint(String string) throws SQLException {
        return connection.setSavepoint(string);
    }

    @Override
	public void rollback(Savepoint svpnt) throws SQLException {
        connection.rollback(svpnt);
    }

    @Override
	public void releaseSavepoint(Savepoint svpnt) throws SQLException {
        connection.releaseSavepoint(svpnt);
    }

    @Override
	public Statement createStatement(int i, int i1, int i2) throws SQLException {
        return connection.createStatement(i, i1, i2);
    }

    @Override
	public PreparedStatement prepareStatement(String string, int i, int i1, int i2) throws SQLException {
        return connection.prepareStatement(string, i, i1, i2);
    }

    @Override
	public CallableStatement prepareCall(String string, int i, int i1, int i2) throws SQLException {
        return connection.prepareCall(string, i, i1, i2);
    }

    @Override
	public PreparedStatement prepareStatement(String string, int i) throws SQLException {
        return connection.prepareStatement(string, i);
    }

    @Override
	public PreparedStatement prepareStatement(String string, int[] ints) throws SQLException {
        return connection.prepareStatement(string, ints);
    }

    @Override
	public PreparedStatement prepareStatement(String string, String[] strings) throws SQLException {
        return connection.prepareStatement(string, strings);
    }

    @Override
	public Clob createClob() throws SQLException {
        return connection.createClob();
    }

    @Override
	public Blob createBlob() throws SQLException {
        return connection.createBlob();
    }

    @Override
	public NClob createNClob() throws SQLException {
        return connection.createNClob();
    }

    @Override
	public SQLXML createSQLXML() throws SQLException {
        return connection.createSQLXML();
    }

    @Override
	public boolean isValid(int i) throws SQLException {
        return connection.isValid(i);
    }

    @Override
	public void setClientInfo(String string, String string1) throws SQLClientInfoException {
        connection.setClientInfo(string, string1);
    }

    @Override
	public void setClientInfo(Properties prprts) throws SQLClientInfoException {
        connection.setClientInfo(prprts);
    }

    @Override
	public String getClientInfo(String string) throws SQLException {
        return connection.getClientInfo(string);
    }

    @Override
	public Properties getClientInfo() throws SQLException {
        return connection.getClientInfo();
    }

    @Override
	public Array createArrayOf(String string, Object[] os) throws SQLException {
        return connection.createArrayOf(string, os);
    }

    @Override
	public Struct createStruct(String string, Object[] os) throws SQLException {
        return connection.createStruct(string, os);
    }

    @Override
	public <T> T unwrap(Class<T> type) throws SQLException {
        return connection.unwrap(type);
    }

    @Override
	public boolean isWrapperFor(Class<?> type) throws SQLException {
        return connection.isWrapperFor(type);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        connection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        connection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return connection.getNetworkTimeout();
    }
}
