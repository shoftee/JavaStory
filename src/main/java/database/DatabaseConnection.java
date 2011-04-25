package database;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.mysql.jdbc.Driver;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final ThreadLocal<Connection> localConnection =
            new ThreadLocalConnection();
    private static boolean isInitialized = false;
    private static Properties settings = null;

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        settings = new Properties();
        InputStreamReader reader = null;
        try {
            reader = new FileReader("database.properties");
            settings.load(reader);
            isInitialized = true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(DatabaseConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        DatabaseConnection.getConnection();
    }

    public static Connection getConnection() {
        if (!isInitialized) {
            throw new RuntimeException("DatabaseConnection not initialized");
        }
        return localConnection.get();
    }

    public static void closeAll() throws SQLException {
        for (final Connection connection : ThreadLocalConnection.allConnections) {
            connection.close();
        }
    }

    private static final class ThreadLocalConnection
            extends ThreadLocal<Connection> {

        public static final Collection<Connection> allConnections =
                new LinkedList<Connection>();

        @Override
        protected final Connection initialValue() {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (final ClassNotFoundException e) {
                System.err.println("ERROR" + e);
            }
            try {
                final Connection con = DriverManager.getConnection(
                        settings.getProperty("url"),
                        settings.getProperty("username"),
                        settings.getProperty("password"));
                allConnections.add(con);
                return con;
            } catch (SQLException e) {
                System.err.println("ERROR" + e);
                return null;
            }
        }
    }
}