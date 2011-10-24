package javastory.db;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mysql.jdbc.Driver;

public final class Database implements ConnectionPool {

	private static final Database instance = new Database();
	private final Queue<Connection> connections;
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

		this.connections = new LinkedList<>();

		this.loadDbProperties();
	}

	private void loadDbProperties() {
		String dbConfigFilename =
				System.getProperty("dbConfigFilename", "database.properties");
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

		this.url = properties.getProperty("url");
		this.username = properties.getProperty("username");
		this.password = properties.getProperty("password");
	}

	public static Connection getConnection() {
		return instance.getConnectionInternal();
	}

	private synchronized Connection getConnectionInternal() {
		if (connections.isEmpty()) {
			Connection result = null; 
			try {
				final Connection connection = DriverManager.getConnection(url, username, password);
				result = new PooledConnection(connection, this);
			} catch (SQLException exception) {
				Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, exception);
			}
			return result;
		}	else {
		return connections.remove();
	}
	}

	@Override
	public synchronized void release(PooledConnection connection) throws SQLException {
		if (this.connections.size() >= POOLING_CAPACITY) {
			connection.getInnerConnection().close();
			return;
		}
		this.connections.add(connection);
	}
}