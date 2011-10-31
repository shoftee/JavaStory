package javastory.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import javastory.cryptography.AesTransform;
import javastory.db.Database;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.TimerManager;
import javastory.server.handling.ServerPacketOpcode;

import org.apache.mina.core.session.IoSession;

/**
 * 
 * @author shoftee
 */
public abstract class GameClient {

	public static final String CLIENT_KEY = "CLIENT";
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

	public GameClient(final AesTransform clientCrypto, final AesTransform serverCrypto, final IoSession session) {
		this.clientCrypto = clientCrypto;
		this.serverCrypto = serverCrypto;
		this.session = session;
		this.ip = session.getRemoteAddress().toString().split(":")[0];
	}

	public void disconnect() {
		this.disconnect(false);
	}

	public abstract void disconnect(boolean immediately);

	public int getAccountId() {
		return this.accountId;
	}

	public final String getAccountName() {
		return this.accountName;
	}

	public final AesTransform getClientCrypto() {
		return this.clientCrypto;
	}

	public final AesTransform getServerCrypto() {
		return this.serverCrypto;
	}

	public final String getSessionIP() {
		return this.ip;
	}

	public void setAccountId(final int id) {
		this.accountId = id;
	}

	public final void setAccountName(final String accountName) {
		this.accountName = accountName;
	}

	public final void write(final GamePacket packet) {
		this.session.write(packet);
	}

	protected IoSession getSession() {
		return this.session;
	}

	public final long getLastPong() {
		return this.lastPong;
	}

	public final void pongReceived() {
		this.lastPong = System.currentTimeMillis();
	}

	public final void sendPing() {
		final long then = System.currentTimeMillis();
		this.session.write(getPingPacket());
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				try {
					if (GameClient.this.lastPong - then < 0) {
						if (GameClient.this.session.isConnected()) {
							GameClient.this.session.close(false);
						}
					}
				} catch (final NullPointerException e) {
					// client already gone
				}
			}
		}, 150000000);
		// note: idletime gets added to this too

	}

	private static GamePacket getPingPacket() {
		final PacketBuilder builder = new PacketBuilder(16);

		builder.writeAsShort(ServerPacketOpcode.PING.getValue());

		return builder.getPacket();
	}

	public void unban() {
		try {
			final Connection con = Database.getConnection();
			final PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 0 and banreason = '' WHERE id = ?");
			ps.setInt(1, this.getAccountId());
			ps.executeUpdate();
			ps.close();
		} catch (final SQLException e) {
			System.err.println("Error while unbanning " + e);
		}
	}

	public final ScheduledFuture<?> getIdleTask() {
		return this.idleTask;
	}

	public final void setIdleTask(final ScheduledFuture<?> idleTask) {
		this.idleTask = idleTask;
	}

	public final int getChannelId() {
		return this.channelId;
	}

	public int getWorldId() {
		return this.worldId;
	}

	public final void setChannelId(final int id) {
		this.channelId = id;
	}

	public final void setWorldId(final int id) {
		this.worldId = id;
	}

	public boolean isLoggedIn() {
		return this.loggedIn;
	}

	protected void logOff() {
		if (this.loggedIn) {
			try {
				final Connection con = Database.getConnection();
				final PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `loggedin` = ? WHERE `id` = ?");
				ps.setBoolean(1, false);
				ps.setInt(2, this.getAccountId());
				ps.executeUpdate();
				ps.close();
			} catch (final SQLException e) {
				System.err.println("error updating login state" + e);
			}
		}
	}
}
