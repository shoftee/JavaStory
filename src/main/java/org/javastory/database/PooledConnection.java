/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.database;

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

/**
 *
 * @author Tosho
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
    
    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    public PreparedStatement prepareStatement(String string) throws SQLException {
        return connection.prepareStatement(string);
    }

    public CallableStatement prepareCall(String string) throws SQLException {
        return connection.prepareCall(string);
    }

    public String nativeSQL(String string) throws SQLException {
        return connection.nativeSQL(string);
    }

    public void setAutoCommit(boolean bln) throws SQLException {
        connection.setAutoCommit(bln);
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }

    public void close() throws SQLException {
        isInUse = false;
        connection.clearWarnings();
        pool.release(this);
    }

    public boolean isClosed() throws SQLException {
        return isInUse;
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    public void setReadOnly(boolean bln) throws SQLException {
        connection.setReadOnly(bln);
    }

    public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    public void setCatalog(String string) throws SQLException {
        connection.setCatalog(string);
    }

    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    public void setTransactionIsolation(int i) throws SQLException {
        connection.setTransactionIsolation(i);
    }

    public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }

    public Statement createStatement(int i, int i1) throws SQLException {
        return connection.createStatement(i, i1);
    }

    public PreparedStatement prepareStatement(String string, int i, int i1) throws SQLException {
        return connection.prepareStatement(string, i, i1);
    }

    public CallableStatement prepareCall(String string, int i, int i1) throws SQLException {
        return connection.prepareCall(string, i, i1);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        connection.setTypeMap(map);
    }

    public void setHoldability(int i) throws SQLException {
        connection.setHoldability(i);
    }

    public int getHoldability() throws SQLException {
        return connection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
    }

    public Savepoint setSavepoint(String string) throws SQLException {
        return connection.setSavepoint(string);
    }

    public void rollback(Savepoint svpnt) throws SQLException {
        connection.rollback(svpnt);
    }

    public void releaseSavepoint(Savepoint svpnt) throws SQLException {
        connection.releaseSavepoint(svpnt);
    }

    public Statement createStatement(int i, int i1, int i2) throws SQLException {
        return connection.createStatement(i, i1, i2);
    }

    public PreparedStatement prepareStatement(String string, int i, int i1, int i2) throws SQLException {
        return connection.prepareStatement(string, i, i1, i2);
    }

    public CallableStatement prepareCall(String string, int i, int i1, int i2) throws SQLException {
        return connection.prepareCall(string, i, i1, i2);
    }

    public PreparedStatement prepareStatement(String string, int i) throws SQLException {
        return connection.prepareStatement(string, i);
    }

    public PreparedStatement prepareStatement(String string, int[] ints) throws SQLException {
        return connection.prepareStatement(string, ints);
    }

    public PreparedStatement prepareStatement(String string, String[] strings) throws SQLException {
        return connection.prepareStatement(string, strings);
    }

    public Clob createClob() throws SQLException {
        return connection.createClob();
    }

    public Blob createBlob() throws SQLException {
        return connection.createBlob();
    }

    public NClob createNClob() throws SQLException {
        return connection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return connection.createSQLXML();
    }

    public boolean isValid(int i) throws SQLException {
        return connection.isValid(i);
    }

    public void setClientInfo(String string, String string1) throws SQLClientInfoException {
        connection.setClientInfo(string, string1);
    }

    public void setClientInfo(Properties prprts) throws SQLClientInfoException {
        connection.setClientInfo(prprts);
    }

    public String getClientInfo(String string) throws SQLException {
        return connection.getClientInfo(string);
    }

    public Properties getClientInfo() throws SQLException {
        return connection.getClientInfo();
    }

    public Array createArrayOf(String string, Object[] os) throws SQLException {
        return connection.createArrayOf(string, os);
    }

    public Struct createStruct(String string, Object[] os) throws SQLException {
        return connection.createStruct(string, os);
    }

    public <T> T unwrap(Class<T> type) throws SQLException {
        return connection.unwrap(type);
    }

    public boolean isWrapperFor(Class<?> type) throws SQLException {
        return connection.isWrapperFor(type);
    }
}
