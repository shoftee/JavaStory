/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import org.javastory.client.ChannelCharacter.FameStatus;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author Tosho
 */
public final class FameLog {

    private FameLog() {
    }

    public static long getLastTimestamp(int famerId) {
        long timestamp = 0;
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT `timestamp` " +
                    "FROM `famelog` " +
                    "WHERE `famer_id` = ? " +
                    "ORDER BY `timestamp` DESC " +
                    "LIMIT 1");
            ps.setInt(1, famerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                timestamp = rs.getTimestamp("timestamp").getTime();
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("ERROR reading famelog.");
        }
        return timestamp;
    }

    public static boolean hasFamedRecently(int famerId, int receiverId) {
        boolean success = true;
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT `timestamp` " +
                    "FROM `famelog` " +
                    "WHERE `famer_id` = ? AND `receiver_id` = ? AND DATEDIFF(NOW(),`timestamp`) < 30" +
                    "ORDER BY `timestamp` DESC " +
                    "LIMIT 1");
            ps.setInt(1, famerId);
            ps.setInt(2, receiverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                success = false;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("ERROR reading famelog.");
        }
        return success;
    }

    public static long addEntry(int famerId, int receiverId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO famelog (famer_id, receiver_id) VALUES (?, ?)");
            ps.setInt(1, famerId);
            ps.setInt(2, receiverId);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog entry (" + famerId +
                    " -> " + receiverId + ")");
        }
        return System.currentTimeMillis();
    }
}
