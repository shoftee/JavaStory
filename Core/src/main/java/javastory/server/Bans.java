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

	public static boolean banByCharacterName(final String name, final String reason) {
		boolean success = false;
		final Connection con = Database.getConnection();
		try (	final PreparedStatement ps = getSelectAccountId(name, con);
				final ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				updateBan(reason, con, rs);
				success = true;
			}
		} catch (final SQLException ex) {
			System.err.println("Error while banning" + ex);
		}
		return success;
	}

	private static void updateBan(final String reason, final Connection con, final ResultSet rs) throws SQLException {
		try (final PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
			psb.setString(1, reason);
			psb.setInt(2, rs.getInt(1));
			psb.execute();
		}
	}

	public static boolean banBySessionIP(final String ip, final String reason) {
		if (!ip.matches("/[0-9]{1,3}\\..*")) {
			return false;
		}

		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)")) {
			ps.setString(1, ip);
			ps.execute();
			return true;
		} catch (final SQLException ex) {
			System.err.println("Error while banning" + ex);
		}

		return false;
	}

	public static byte unban(final String characterName) {
		final Connection con = Database.getConnection();
		try (	final PreparedStatement ps = getSelectAccountId(characterName, con);
				final ResultSet rs = ps.executeQuery()) {
			if (!rs.next()) {
				return -1;
			}

			final int accountId = rs.getInt(1);
			updateUnban(con, accountId);
		} catch (final SQLException e) {
			System.err.println("Error while unbanning" + e);
			return -2;
		}
		return 0;
	}

	private static void updateUnban(final Connection con, final int accountId) throws SQLException {
		try (final PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 0 and banreason = '' WHERE id = ?")) {
			ps.setInt(1, accountId);
			ps.executeUpdate();
		}
	}

	private static PreparedStatement getSelectAccountId(final String characterName, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT accountid from characters where name = ?");
		ps.setString(1, characterName);
		return ps;
	}
}
