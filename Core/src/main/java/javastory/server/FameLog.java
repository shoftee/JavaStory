/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.db.DatabaseConnection;

/**
 *
 * @author Tosho
 */
public final class FameLog {

    public static final String CanFamePlayerSql =
            "SELECT `timestamp` FROM `famelog` "
            + "WHERE `famer_id` = ? AND `receiver_id` = ? AND DATEDIFF(NOW(),`timestamp`) < 30 "
            + "ORDER BY `timestamp` DESC LIMIT 1";
    private static final String SelectLastTimestampSql =
            "SELECT `timestamp` FROM `famelog` WHERE `famer_id` = ? ORDER BY `timestamp` DESC LIMIT 1";

    private FameLog() {
    }

    public static long getLastTimestamp(int famerId) {

        Connection connection = DatabaseConnection.getConnection();
        try (PreparedStatement ps = connection.prepareStatement(SelectLastTimestampSql)) {
            long timestamp = 0;
            ps.setInt(1, famerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    timestamp = rs.getTimestamp("timestamp").getTime();
                }
            }
            return timestamp;
        } catch (SQLException ex) {
            System.err.println("ERROR reading famelog.");
        }
        return 0;
    }

    public static boolean hasFamedRecently(int famerId, int receiverId) {
        boolean success = true;
        Connection connection = DatabaseConnection.getConnection();
        try (PreparedStatement ps = connection.prepareStatement(CanFamePlayerSql)) {
            ps.setInt(1, famerId);
            ps.setInt(2, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    success = false;
                }
            }
        } catch (SQLException ex) {
            System.err.println("ERROR reading famelog.");
        }
        return success;
    }

    public static long addEntry(int famerId, int receiverId) {
            Connection connection = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO famelog (famer_id, receiver_id) VALUES (?, ?)")) {
                ps.setInt(1, famerId);
                ps.setInt(2, receiverId);
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog entry (" + famerId
                    + " -> " + receiverId + ")");
        }
        return System.currentTimeMillis();
    }
}
