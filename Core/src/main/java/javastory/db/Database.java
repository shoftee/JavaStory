package javastory.db;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.mysql.jdbc.Driver;

public final class Database implements ConnectionPool {

	private static final Database instance = new Database();
	private final ConcurrentLinkedQueue<Connection> connections;
	private String url;
	private String username;
	private String password;
	private static final int POOLING_CAPACITY = 20;

	private Database() {
		try {
			DriverManager.registerDriver(new Driver());
		} catch (SQLException ex) {
			Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
		}

		this.connections = new ConcurrentLinkedQueue<>();

		Map<String, String> properties = this.loadDbProperties();

		this.url = properties.get("url");
		this.username = properties.get("username");
		this.password = properties.get("password");
	}

	private Map<String, String> loadDbProperties() {
		String dbConfigFilename = System.getProperty("dbConfigFilename", "database.properties");
		Properties properties = new Properties();

		InputStreamReader reader = null;
		try {
			reader = new FileReader(dbConfigFilename);
			properties.load(reader);
		} catch (IOException ex) {
			Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				reader.close();
			} catch (IOException ex) {
				Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		return Maps.fromProperties(properties);
	}

	public static Connection getConnection() {
		return instance.getConnectionInternal();
	}

	private synchronized Connection getConnectionInternal() {
		Connection fromQueue = connections.poll();
		if (fromQueue == null) {
			try {
				final Connection newConnection = DriverManager.getConnection(url, username, password);
				return new PooledConnection(newConnection, this);
			} catch (SQLException exception) {
				Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, exception);
			}
		}
		return fromQueue;
	}

	@Override
	public synchronized void reclaim(PooledConnection connection) throws SQLException {
		if (this.connections.size() >= POOLING_CAPACITY) {
			connection.getInnerConnection().close();
			return;
		}
		this.connections.add(connection);
	}
}