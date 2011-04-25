/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Tosho
 */
public final class Bans {

    private Bans() {
    }

    public static boolean banByCharacterName(String name, String reason) {
        boolean success = false;
        try {
            Connection con = DatabaseConnection.getConnection();
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
        boolean success = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (ip.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, ip);
                ps.execute();
                ps.close();
                success = true;
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        }
        return success;
    }
}
