package javastory.db;

import java.sql.SQLException;

/**
 * 
 * @author shoftee
 */
interface ConnectionPool {
	public void release(PooledConnection connection) throws SQLException;
}
