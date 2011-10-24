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

public final class ChannelConfig {

	private ChannelConfig() {
	}

	public static ImmutableMap<Integer, ChannelInfo> loadForWorld(final int worldId) {
		checkArgument(worldId >= 0, "'worldId' must be non-negative.");

		try (final PreparedStatement ps = getSelectByWorldId(worldId)) {
			return loadWithPreparedStatement(ps);
		} catch (SQLException ex) {
			Logger.getLogger(ChannelConfig.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public static ChannelInfo load(final int worldId, final int channelId) {
		try (	final PreparedStatement ps = getSelectByIds(worldId, channelId);
				final ResultSet rs = ps.executeQuery()) {
			if (!rs.next()) {
				return null;
			}
			final String name = rs.getString("name");
			final String host = rs.getString("host");
			final int port = rs.getInt("port");
			return new ChannelInfo(worldId, channelId, name, host, port);
		} catch (SQLException ex) {
			Logger.getLogger(ChannelConfig.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private static ImmutableMap<Integer, ChannelInfo> loadWithPreparedStatement(final PreparedStatement ps) throws SQLException {
		ImmutableMap.Builder<Integer, ChannelInfo> builder = ImmutableMap.builder();

		try (final ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				final int worldId = rs.getInt("world_id");
				final int channelId = rs.getInt("channel_id");
				final String channelName = rs.getString("name");
				final String channelHost = rs.getString("host");
				final int channelPort = rs.getInt("port");
				final ChannelInfo channelInfo = new ChannelInfo(worldId, channelId, channelName, channelHost, channelPort);
				builder.put(channelId, channelInfo);
			}
		}

		return builder.build();
	}

	private static PreparedStatement getSelectByWorldId(final int worldId) throws SQLException {
		final Connection connection = Database.getConnection();
		final String sql = "SELECT * FROM `channel_config` WHERE `world_id` = ?";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, worldId);
		return ps;
	}

	private static PreparedStatement getSelectByIds(final int worldId, final int channelId) throws SQLException {
		final Connection connection = Database.getConnection();
		final String sql = "SELECT * FROM `channel_config` WHERE `world_id` = ? AND `channel_id` = ?";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, worldId);
		ps.setInt(2, channelId);
		return ps;
	}
}
