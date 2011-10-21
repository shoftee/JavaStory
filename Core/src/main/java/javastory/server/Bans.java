/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.db.Database;

/**
 *
 * @author shoftee
 */
public final class Bans {

    private Bans() {
    }

    public static boolean banByCharacterName(String name, String reason) {
        boolean success = false;
        try {
            Connection con = Database.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
                psb.setString(1, reason);
                psb.setInt(2, rs.getInt(1));
                psb.execute();
                psb.close();
                success = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        }
        return success;
    }

    public static boolean banBySessionIP(String ip, String reason) {
        Connection con = Database.getConnection();
        if (!ip.matches("/[0-9]{1,3}\\..*")) {
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)")) {
            ps.setString(1, ip);
            ps.execute();
            return true;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        }
        
        return false;
    }
    
    public static byte unban(String charname) {
        try {
            Connection con = Database.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid from characters where name = ?");
            ps.setString(1, charname);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return -1;
            }
            final int accid = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET banned = 0 and banreason = '' WHERE id = ?");
            ps.setInt(1, accid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
        }
        return 0;
    }
}
