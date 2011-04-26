package client;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.script.ScriptEngine;
import database.DatabaseConnection;
import org.javastory.server.login.LoginServer;
import handling.world.MessengerMember;
import handling.world.PartyMember;
import handling.world.PartyOperation;
import scripting.NpcScriptManager;
import server.Trade;
import server.shops.PlayerShop;
import tools.LogUtil;
import tools.IPAddressTool;
import org.javastory.cryptography.AesTransform;

import org.apache.mina.core.session.IoSession;
import org.javastory.client.GameClient;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;

public final class ChannelClient extends GameClient {

    private ChannelCharacter player;
    private boolean serverTransition = false;
    private transient Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();

    public ChannelClient(AesTransform clientCrypto, AesTransform serverCrypto,
            IoSession session) {
        super(clientCrypto, serverCrypto, session);
    }

    public ChannelCharacter getPlayer() {
        return player;
    }

    public void setPlayer(ChannelCharacter player) {
        this.player = player;
    }

    /**
     * Gets the special server IP if the client matches a certain subnet.
     *
     * @param subnetInfo A <code>Properties</code> instance containing all the subnet info.
     * @param clientIPAddress The IP address of the client as a dotted quad.
     * @param channel The requested channel to match with the subnet.
     * @return <code>0.0.0.0</code> if no subnet matched, or the IP if the subnet matched.
     */
    public static String getChannelServerIPFromSubnet(String clientIPAddress, int channel) {
        long ipAddress = IPAddressTool.dottedQuadToLong(clientIPAddress);
        Properties subnetInfo = LoginServer.getInstance().getSubnetInfo();

        if (subnetInfo.contains("net.sf.odinms.net.login.subnetcount")) {
            int subnetCount = Integer.parseInt(subnetInfo.getProperty("net.sf.odinms.net.login.subnetcount"));
            for (int i = 0; i < subnetCount; i++) {
                String[] connectionInfo = subnetInfo.getProperty("net.sf.odinms.net.login.subnet." +
                        i).split(":");
                long subnet = IPAddressTool.dottedQuadToLong(connectionInfo[0]);
                long channelIP = IPAddressTool.dottedQuadToLong(connectionInfo[1]);
                int channelNumber = Integer.parseInt(connectionInfo[2]);

                if (((ipAddress & subnet) == (channelIP & subnet)) && (channel ==
                        channelNumber)) {
                    return connectionInfo[1];
                }
            }
        }
        return "0.0.0.0";
    }

    public byte unban(String charname) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid from characters where name = ?");
            ps.setString(1, charname);

            ResultSet rs = ps.executeQuery();
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
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
        }
        return 0;
    }

    public final void removalTask() {
        try {
            if (!player.getAllBuffs().isEmpty()) {
                player.clearAllBuffEffects();
            }
            if (!player.getAllDiseases().isEmpty()) {
                player.cancelAllDebuffs();
            }
            if (player.getTrade() != null) {
                Trade.cancelTrade(player.getTrade());
            }
            NpcScriptManager.getInstance().dispose(this);

            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player);
            }
            player.getCheatTracker().dispose();
            if (player.getMap() != null) {
                player.getMap().removePlayer(player);
            }

            final PlayerShop shop = player.getPlayerShop();
            if (shop != null) {
                shop.removeVisitor(player);
                if (shop.isOwner(player)) {
                    shop.setOpen(true);
                }
            }
        } catch (final Throwable e) {
            LogUtil.outputFileError(LogUtil.Acc_Stuck, e);
        }
    }

    public final void DebugMessage(final StringBuilder sb) {
        sb.append(getSession().getRemoteAddress());
        sb.append(" Connected: ");
        sb.append(getSession().isConnected());
        sb.append(" Closing: ");
        sb.append(getSession().isClosing());
        sb.append(" ClientKeySet: ");
        sb.append(getSession().getAttribute(ChannelClient.CLIENT_KEY) != null);
        sb.append(" has char: ");
        sb.append(getPlayer() != null);
    }

    public final ChannelServer getChannelServer() {
        return ChannelManager.getInstance(super.getChannelId());
    }

    public final void setScriptEngine(final String name, final ScriptEngine e) {
        engines.put(name, e);
    }

    public final ScriptEngine getScriptEngine(final String name) {
        return engines.get(name);
    }

    public final void removeScriptEngine(final String name) {
        engines.remove(name);
    }

    public final void disconnect(boolean immediately) {
        if (!immediately) {
            this.removalTask();
            this.player.saveToDb(true);
            final ChannelServer channel = getChannelServer();
            try {
                if (this.player.getMessenger() != null) {
                    channel.getWorldInterface().leaveMessenger(this.player.getMessenger().getId(), new MessengerMember(this.player));
                    this.player.setMessenger(null);
                }
                if (this.player.getParty() != null) {
                    final PartyMember chrp = new PartyMember(this.player);
                    chrp.setOnline(false);
                    channel.getWorldInterface().updateParty(this.player.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
                }
                if (!this.serverTransition) {
                    channel.getWorldInterface().loggedOff(this.player.getName(), this.player.getId(), super.getChannelId(), this.player.getBuddylist().getBuddyIds());
                } else {
                    // Change channel
                    channel.getWorldInterface().loggedOn(this.player.getName(), this.player.getId(), super.getChannelId(), this.player.getBuddylist().getBuddyIds());
                }
                if (this.player.getGuildId() > 0) {
                    channel.getWorldInterface().setGuildMemberOnline(this.player.getGuildMembership(), false, -1);
                }
            } catch (final RemoteException e) {
                channel.pingWorld();
                this.player.setMessenger(null);
            } catch (final Exception e) {
                System.err.println(getLogMessage(this, "ERROR") + e);
            } finally {
                if (channel != null) {
                    channel.removePlayer(this.player);
                }
                this.player = null;
            }
        }
        super.getSession().close(immediately);
    }
}
