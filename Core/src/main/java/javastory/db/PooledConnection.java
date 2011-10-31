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

	private final Connection connection;
	private final ConnectionPool pool;
	private boolean isInUse;

	public PooledConnection(final Connection inner, final ConnectionPool pool) {
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
		return this.connection.createStatement();
	}

	@Override
	public PreparedStatement prepareStatement(final String string) throws SQLException {
		return this.connection.prepareStatement(string);
	}

	@Override
	public CallableStatement prepareCall(final String string) throws SQLException {
		return this.connection.prepareCall(string);
	}

	@Override
	public String nativeSQL(final String string) throws SQLException {
		return this.connection.nativeSQL(string);
	}

	@Override
	public void setAutoCommit(final boolean bln) throws SQLException {
		this.connection.setAutoCommit(bln);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return this.connection.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		this.connection.commit();
	}

	@Override
	public void rollback() throws SQLException {
		this.connection.rollback();
	}

	@Override
	public void close() throws SQLException {
		this.isInUse = false;
		this.connection.clearWarnings();
		this.pool.reclaim(this);
	}

	@Override
	public boolean isClosed() throws SQLException {
		return this.isInUse;
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return this.connection.getMetaData();
	}

	@Override
	public void setReadOnly(final boolean bln) throws SQLException {
		this.connection.setReadOnly(bln);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return this.connection.isReadOnly();
	}

	@Override
	public void setCatalog(final String string) throws SQLException {
		this.connection.setCatalog(string);
	}

	@Override
	public String getCatalog() throws SQLException {
		return this.connection.getCatalog();
	}

	@Override
	public void setTransactionIsolation(final int i) throws SQLException {
		this.connection.setTransactionIsolation(i);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return this.connection.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return this.connection.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		this.connection.clearWarnings();
	}

	@Override
	public Statement createStatement(final int i, final int i1) throws SQLException {
		return this.connection.createStatement(i, i1);
	}

	@Override
	public PreparedStatement prepareStatement(final String string, final int i, final int i1) throws SQLException {
		return this.connection.prepareStatement(string, i, i1);
	}

	@Override
	public CallableStatement prepareCall(final String string, final int i, final int i1) throws SQLException {
		return this.connection.prepareCall(string, i, i1);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return this.connection.getTypeMap();
	}

	@Override
	public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
		this.connection.setTypeMap(map);
	}

	@Override
	public void setHoldability(final int i) throws SQLException {
		this.connection.setHoldability(i);
	}

	@Override
	public int getHoldability() throws SQLException {
		return this.connection.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return this.connection.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(final String string) throws SQLException {
		return this.connection.setSavepoint(string);
	}

	@Override
	public void rollback(final Savepoint svpnt) throws SQLException {
		this.connection.rollback(svpnt);
	}

	@Override
	public void releaseSavepoint(final Savepoint svpnt) throws SQLException {
		this.connection.releaseSavepoint(svpnt);
	}

	@Override
	public Statement createStatement(final int i, final int i1, final int i2) throws SQLException {
		return this.connection.createStatement(i, i1, i2);
	}

	@Override
	public PreparedStatement prepareStatement(final String string, final int i, final int i1, final int i2) throws SQLException {
		return this.connection.prepareStatement(string, i, i1, i2);
	}

	@Override
	public CallableStatement prepareCall(final String string, final int i, final int i1, final int i2) throws SQLException {
		return this.connection.prepareCall(string, i, i1, i2);
	}

	@Override
	public PreparedStatement prepareStatement(final String string, final int i) throws SQLException {
		return this.connection.prepareStatement(string, i);
	}

	@Override
	public PreparedStatement prepareStatement(final String string, final int[] ints) throws SQLException {
		return this.connection.prepareStatement(string, ints);
	}

	@Override
	public PreparedStatement prepareStatement(final String string, final String[] strings) throws SQLException {
		return this.connection.prepareStatement(string, strings);
	}

	@Override
	public Clob createClob() throws SQLException {
		return this.connection.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return this.connection.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return this.connection.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return this.connection.createSQLXML();
	}

	@Override
	public boolean isValid(final int i) throws SQLException {
		return this.connection.isValid(i);
	}

	@Override
	public void setClientInfo(final String string, final String string1) throws SQLClientInfoException {
		this.connection.setClientInfo(string, string1);
	}

	@Override
	public void setClientInfo(final Properties prprts) throws SQLClientInfoException {
		this.connection.setClientInfo(prprts);
	}

	@Override
	public String getClientInfo(final String string) throws SQLException {
		return this.connection.getClientInfo(string);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return this.connection.getClientInfo();
	}

	@Override
	public Array createArrayOf(final String string, final Object[] os) throws SQLException {
		return this.connection.createArrayOf(string, os);
	}

	@Override
	public Struct createStruct(final String string, final Object[] os) throws SQLException {
		return this.connection.createStruct(string, os);
	}

	@Override
	public <T> T unwrap(final Class<T> type) throws SQLException {
		return this.connection.unwrap(type);
	}

	@Override
	public boolean isWrapperFor(final Class<?> type) throws SQLException {
		return this.connection.isWrapperFor(type);
	}

	@Override
	public void setSchema(final String schema) throws SQLException {
		this.connection.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return this.connection.getSchema();
	}

	@Override
	public void abort(final Executor executor) throws SQLException {
		this.connection.abort(executor);
	}

	@Override
	public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
		this.connection.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return this.connection.getNetworkTimeout();
	}
}
