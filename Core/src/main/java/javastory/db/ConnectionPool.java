package javastory.db;

import java.sql.SQLException;

/**
 * 
 * @author shoftee
 */
interface ConnectionPool {
	public void reclaim(PooledConnection connection) throws SQLException;
}