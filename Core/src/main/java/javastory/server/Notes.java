package javastory.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javastory.db.Database;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * 
 * @author shoftee
 */
public final class Notes {

	public static class Note {

		private final int id;
		private final String sender;
		private final String recepient;
		private final String message;
		private final long timestamp;

		private Note(final int id, final String sender, final String recepient, final String message, final long timestamp) {
			this.id = id;
			this.sender = sender;
			this.recepient = recepient;
			this.message = message;
			this.timestamp = timestamp;
		}

		private Note(final ResultSet rs) throws SQLException {
			this.id = rs.getInt("id");
			this.sender = rs.getString("sender");
			this.recepient = rs.getString("recepient");
			this.message = rs.getString("message");
			this.timestamp = rs.getTimestamp("timestamp").getTime();
		}

		public int getId() {
			return this.id;
		}

		public String getSender() {
			return this.sender;
		}

		public String getRecepient() {
			return this.recepient;
		}

		public String getMessage() {
			return this.message;
		}

		public long getTimestamp() {
			return this.timestamp;
		}
	}

	private Notes() {
	}

	public static ImmutableList<Note> loadReceived(final String recepient) {
		final List<Note> list = Lists.newArrayList();

		final Connection con = Database.getConnection();
		try (	final PreparedStatement ps = getSelectNotes(recepient, con);
				final ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new Note(rs));
			}
		} catch (final SQLException ex) {
			System.err.println("Unable to load notes: " + ex);
		}
		return ImmutableList.copyOf(list);
	}

	private static PreparedStatement getSelectNotes(final String recepient, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `recepient` = ?", ResultSet.TYPE_SCROLL_SENSITIVE,
			ResultSet.CONCUR_UPDATABLE);
		ps.setString(1, recepient);
		return ps;
	}

	public static void send(final String from, final String to, final String message) {
		try {
			final Connection con = Database.getConnection();
			final PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`sender`, `recepient`, `message`) VALUES (?, ?, ?)");
			ps.setString(1, from);
			ps.setString(2, to);
			ps.setString(3, message);
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException ex) {
			System.err.println("Unable to send note: " + ex);
		}
	}

	public static void delete(final int noteId) {
		try {
			final Connection connection = Database.getConnection();
			final PreparedStatement ps = connection.prepareStatement("DELETE FROM `notes` WHERE `id` = ?");
			ps.setInt(1, noteId);
			ps.execute();
			ps.close();
		} catch (final SQLException ex) {
			System.err.println("Unable to delete note: " + ex);
		}
	}
}
