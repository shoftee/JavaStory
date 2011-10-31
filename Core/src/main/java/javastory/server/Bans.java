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

	// TODO: Extract prepare statement calls into methods.
	public static boolean banByCharacterName(final String name, final String reason) {
		boolean success = false;
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps;
			ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
			ps.setString(1, name);
			final ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				final PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
				psb.setString(1, reason);
				psb.setInt(2, rs.getInt(1));
				psb.execute();
				psb.close();
				success = true;
			}
			rs.close();
			ps.close();
		} catch (final SQLException ex) {
			System.err.println("Error while banning" + ex);
		}
		return success;
	}

	public static boolean banBySessionIP(final String ip, final String reason) {
		final Connection con = Database.getConnection();
		if (!ip.matches("/[0-9]{1,3}\\..*")) {
			return false;
		}

		try (PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)")) {
			ps.setString(1, ip);
			ps.execute();
			return true;
		} catch (final SQLException ex) {
			System.err.println("Error while banning" + ex);
		}

		return false;
	}

	// TODO: Extract prepare statement calls into methods.
	public static byte unban(final String charname) {
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT accountid from characters where name = ?");
			ps.setString(1, charname);

			final ResultSet rs = ps.executeQuery();
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
		} catch (final SQLException e) {
			System.err.println("Error while unbanning" + e);
			return -2;
		}
		return 0;
	}
}
