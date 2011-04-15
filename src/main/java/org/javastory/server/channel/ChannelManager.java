/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server.channel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javastory.server.ChannelInfo;
import client.OdinSEA;
import database.DatabaseConnection;
import handling.ServerConstants;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.google.common.collect.Maps;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Collections;
import org.javastory.tools.PropertyUtil;

/**
 *
 * @author Tosho
 */
public class ChannelManager {

    private static final ChannelManager instance = new ChannelManager();
    private final Map<Integer, ChannelInfo> configs = Maps.newHashMap();
    private final Map<Integer, ChannelServer> channels = Maps.newHashMap();
    private int channelCount;

    private ChannelManager() {
        DatabaseConnection.initialize();
        this.channelCount = 0;
    }

    private void startChannelsInternal() {
        Connection connection = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM `channel_config`");
            ResultSet rs = ps.executeQuery();
            while (rs.getRow() != 0) {
                int channelId = rs.getInt("channel_id");
                String channelName = rs.getString("channel_name");
                String channelHost = rs.getString("channel_host");
                int channelPort = rs.getInt("channel_port");
                final ChannelInfo channelInfo = new ChannelInfo(
                        channelId, channelName, channelHost, channelPort);
                configs.put(channelId, channelInfo);
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
    
    public static Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instance.channels.values());
    }

    public static ChannelServer getInstance(final int channel) {
        return instance.channels.get(channel);
    }

    public static void startChannels() {
        instance.startChannelsInternal();
    }
}
