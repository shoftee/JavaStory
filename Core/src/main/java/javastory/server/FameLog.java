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
public final class FameLog {

	public static final String CanFamePlayerSql = "SELECT `timestamp` FROM `famelog` "
		+ "WHERE `famer_id` = ? AND `receiver_id` = ? AND DATEDIFF(NOW(),`timestamp`) < 30 " + "ORDER BY `timestamp` DESC LIMIT 1";
	private static final String SelectLastTimestampSql = "SELECT `timestamp` FROM `famelog` WHERE `famer_id` = ? ORDER BY `timestamp` DESC LIMIT 1";

	private FameLog() {
	}

	// TODO: Extract prepare statement call into method.
	public static long getLastTimestamp(final int famerId) {

		final Connection connection = Database.getConnection();
		try (PreparedStatement ps = connection.prepareStatement(SelectLastTimestampSql)) {
			long timestamp = 0;
			ps.setInt(1, famerId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					timestamp = rs.getTimestamp("timestamp").getTime();
				}
			}
			return timestamp;
		} catch (final SQLException ex) {
			System.err.println("ERROR reading famelog.");
		}
		return 0;
	}

	// TODO: Extract prepare statement call into method.
	public static boolean hasFamedRecently(final int famerId, final int receiverId) {
		boolean success = true;
		final Connection connection = Database.getConnection();
		try (PreparedStatement ps = connection.prepareStatement(CanFamePlayerSql)) {
			ps.setInt(1, famerId);
			ps.setInt(2, receiverId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					success = false;
				}
			}
		} catch (final SQLException ex) {
			System.err.println("ERROR reading famelog.");
		}
		return success;
	}

	public static long addEntry(final int famerId, final int receiverId) {
		final Connection connection = Database.getConnection();
		try {
			try (PreparedStatement ps = connection.prepareStatement("INSERT INTO famelog (famer_id, receiver_id) VALUES (?, ?)")) {
				ps.setInt(1, famerId);
				ps.setInt(2, receiverId);
				ps.execute();
			}
		} catch (final SQLException e) {
			System.err.println("ERROR writing famelog entry (" + famerId + " -> " + receiverId + ")");
		}
		return System.currentTimeMillis();
	}
}
