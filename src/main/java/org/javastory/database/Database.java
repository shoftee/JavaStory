/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.database;

import com.mysql.jdbc.Driver;
import java.io.FileNotFoundException;
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

/**
 *
 * @author Tosho
 */
public class Database implements ConnectionPool {

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

        this.connections = new LinkedList<Connection>();

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
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
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

    public static Connection getConnection() throws SQLException {
        return instance.getConnectionInternal();
    }

    private synchronized Connection getConnectionInternal() throws SQLException {
        if (connections.isEmpty()) {
             return new PooledConnection(
                    DriverManager.getConnection(url, username, password),
                    this);
        }
        return connections.remove();
    }

    public synchronized void release(PooledConnection connection) throws SQLException {
        if (this.connections.size() >= POOLING_CAPACITY) {
            connection.getInnerConnection().close();
            return;
        }
        this.connections.add(connection);
    }
}
