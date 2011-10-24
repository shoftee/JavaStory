package javastory.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javastory.db.Database;

public final class GameCharacterUtil {

	private GameCharacterUtil() {
	}

	private static final Pattern namePattern = Pattern.compile("[a-zA-Z0-9_-]{4,12}");
	private static final Pattern petPattern = Pattern.compile("[a-zA-Z0-9_-]{4,12}");

	public static boolean validateCharacterName(final String name) {
		return namePattern.matcher(name).matches();
	}

	public static boolean isAvailableName(final String name) {
		return getIdByName(name) == -1;
	}

	public static boolean validatePetName(final String name) {
		if (petPattern.matcher(name).matches()) {
			return true;
		}
		return false;
	}

	public static int getIdByName(final String name) {
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `name` = ?")) {
			ps.setString(1, name);
			final int id;
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return -1;
				}
				id = rs.getInt("id");
			}
			return id;
		} catch (SQLException e) {
			System.err.println("error 'getIdByName' " + e);
		}
		return -1;
	}
}