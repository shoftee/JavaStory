package org.javastory.client;

import database.DatabaseConnection;
import handling.GamePacket;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import org.apache.mina.core.session.IoSession;
import org.javastory.cryptography.AesTransform;
import server.TimerManager;
import tools.packet.LoginPacket;

/**
 *
 * @author Tosho
 */
public abstract class GameClient {

    public static final String CLIENT_KEY = "CLIENT";
    private static final long serialVersionUID = 9179541993413738569L;
    private int accountId = -1;
    private String accountName;
    private transient long lastPong;
    private transient ScheduledFuture<?> idleTask = null;
    private int channelId, worldId;
    protected boolean loggedIn;
    //
    private final transient AesTransform clientCrypto;
    private final transient AesTransform serverCrypto;
    private final transient IoSession session;
    private final transient String ip;

    public GameClient(AesTransform clientCrypto, AesTransform serverCrypto,
            IoSession session) {
        this.clientCrypto = clientCrypto;
        this.serverCrypto = serverCrypto;
        this.session = session;
        ip = session.getRemoteAddress().toString().split(":")[0];
    }

    public void disconnect() {
        this.disconnect(false);
    }

    public abstract void disconnect(boolean immediately);

    public int getAccountId() {
        return this.accountId;
    }

    public final String getAccountName() {
        return accountName;
    }

    public final AesTransform getClientCrypto() {
        return clientCrypto;
    }

    public final AesTransform getServerCrypto() {
        return serverCrypto;
    }

    public final String getSessionIP() {
        return ip;
    }

    public void setAccountId(int id) {
        this.accountId = id;
    }

    public final void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public final void write(GamePacket packet) {
        this.session.write(packet);
    }

    protected IoSession getSession() {
        return session;
    }

    public final long getLastPong() {
        return lastPong;
    }

    public final void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public final void sendPing() {
        final long then = System.currentTimeMillis();
        session.write(LoginPacket.getPing());
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    if (lastPong - then < 0) {
                        if (session.isConnected()) {
                            session.close(false);
                        }
                    }
                } catch (final NullPointerException e) {
                    // client already gone
                }
            }
        }, 150000000);
        // note: idletime gets added to this too

    }

    public static String getLogMessage(final ChannelClient cfor, final String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static String getLogMessage(final ChannelClient cfor, final String message, final Object... parms) {
        final StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append("<");
                builder.append(GameCharacterUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append(" (cid: ");
                builder.append(cfor.getPlayer().getId());
                builder.append(")> ");
            }
            if (cfor.getAccountName() != null) {
                builder.append("(Account: ");
                builder.append(cfor.getAccountName());
                builder.append(") ");
            }
        }
        builder.append(message);
        int start;
        for (final Object parm : parms) {
            start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static String getLogMessage(final ChannelCharacter cfor, final String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static String getLogMessage(final ChannelCharacter cfor, final String message, final Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public void unban() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 0 and banreason = '' WHERE id = ?");
            ps.setInt(1, getAccountId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error while unbanning " + e);
        }
    }

    public final ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public final void setIdleTask(final ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public final int getChannelId() {
        return channelId;
    }

    public int getWorldId() {
        return worldId;
    }

    public final void setChannelId(final int id) {
        this.channelId = id;
    }

    public final void setWorldId(final int id) {
        this.worldId = id;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    protected void logOff() {
        if (loggedIn) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `loggedin` = ? WHERE `id` = ?");
                ps.setBoolean(1, false);
                ps.setInt(2, getAccountId());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                System.err.println("error updating login state" + e);
            }
        }
    }
}
