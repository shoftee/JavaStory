/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.channel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javastory.db.DatabaseConnection;
import javastory.server.ChannelInfo;
import javastory.server.ChannelServer;

import com.google.common.collect.Maps;

/**
 *
 * @author shoftee
 */
public class ChannelManager {

    private static final ChannelManager instance = new ChannelManager();
    private final Map<Integer, ChannelInfo> configs = Maps.newHashMap();
    private final Map<Integer, ChannelServer> channels = Maps.newHashMap();

    private ChannelManager() {
        DatabaseConnection.initialize();
    }

    private void startChannelsInternal() {
        Connection connection = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM `channel_config`"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int channelId = rs.getInt("channel_id");
                    String channelName = rs.getString("channel_name");
                    String channelHost = rs.getString("channel_host");
                    int channelPort = rs.getInt("channel_port");
                    final ChannelInfo channelInfo = new ChannelInfo(
                            channelId, channelName, channelHost, channelPort);
                    configs.put(channelId, channelInfo);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(ChannelManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (ChannelInfo info : configs.values()) {
            startChannel(info);
        }
        OdinSEA.start();
        DatabaseConnection.initialize();
    }

    public static void startChannel(ChannelInfo info) {
        final ChannelServer channel = new ChannelServer(info);
        channel.initialize();
    }

    public static Iterable<ChannelServer> getAllInstances() {
        return instance.channels.values();
    }

    public static ChannelServer getInstance(final int channel) {
        return instance.channels.get(channel);
    }

    public static void startChannels() {
        instance.startChannelsInternal();
    }
}
