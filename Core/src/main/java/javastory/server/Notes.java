/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javastory.db.DatabaseConnection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 * @author Tosho
 */
public final class Notes {

    public static class Note {

        private int id;
        private String sender;
        private String recepient;
        private String message;
        private long timestamp;

        private Note(int id, String sender, String recepient, String message, long timestamp) {
            this.id = id;
            this.sender = sender;
            this.recepient = recepient;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        private Note(ResultSet rs) throws SQLException {
            this.id = rs.getInt("id");
            this.sender = rs.getString("sender");
            this.recepient = rs.getString("recepient");
            this.message = rs.getString("message");
            this.timestamp = rs.getTimestamp("timestamp").getTime();
        }

        public int getId() {
            return id;
        }

        public String getSender() {
            return sender;
        }

        public String getRecepient() {
            return recepient;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private Notes() {
    }

    public static ImmutableList<Note> loadReceived(String recepient) {
        List<Note> list = Lists.newArrayList();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM notes WHERE `recepient` = ?",
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            ps.setString(1, recepient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Note(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Unable to load notes: " + ex);
        }
        return ImmutableList.copyOf(list);
    }

    public static void send(String from, String to, String message) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`sender`, `recepient`, `message`) VALUES (?, ?, ?)");
            ps.setString(1, from);
            ps.setString(2, to);
            ps.setString(3, message);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Unable to send note: " + ex);
        }
    }

    public static void delete(int noteId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM `notes` WHERE `id` = ?");
            ps.setInt(1, noteId);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Unable to delete note: " + ex);
        }
    }
}
