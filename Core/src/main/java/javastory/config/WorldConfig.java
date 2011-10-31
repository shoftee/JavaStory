package javastory.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javastory.db.Database;

import com.google.common.collect.ImmutableMap;

public final class WorldConfig {

	private WorldConfig() {
	}

	public static ImmutableMap<Integer, ChannelInfo> loadChannelInfo(final int worldId) {
		checkArgument(worldId >= 0, "'worldId' must be non-negative.");

		return ChannelConfig.loadForWorld(worldId);
	}

	public static WorldInfo load(final int worldId) {
		try (	final PreparedStatement ps = getSelectById(worldId);
				final ResultSet rs = ps.executeQuery()) {
			if (!rs.next()) {
				return null;
			}
			final String name = rs.getString("name");
			final int expRate = rs.getInt("exp_rate");
			final int mesoRate = rs.getInt("meso_rate");
			final int itemRate = rs.getInt("item_rate");
			final String host = rs.getString("host");
			final int port = rs.getInt("port");
			return new WorldInfo(worldId, name, expRate, mesoRate, itemRate, host, port);
		} catch (final SQLException ex) {
			Logger.getLogger(WorldConfig.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private static PreparedStatement getSelectById(final int worldId) throws SQLException {
		final Connection connection = Database.getConnection();
		final String sql = "SELECT * FROM `world_config` WHERE `world_id` = ?";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, worldId);
		return ps;
	}
}
