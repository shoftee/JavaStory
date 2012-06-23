package javastory.server.handling;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javastory.channel.ChannelClient;
import javastory.client.GameClient;
import javastory.cryptography.AesTransform;
import javastory.cryptography.VersionType;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.Randomizer;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.google.common.collect.Maps;

public abstract class PacketHandler extends IoHandlerAdapter {

	private final List<String> blockedIPs = new ArrayList<String>();
	private final Map<String, SessionFloodEntry> floodTracker = Maps.newConcurrentMap();

	public PacketHandler() {
	}

	@Override
	public final void messageSent(final IoSession session, final Object message) throws Exception {
		super.messageSent(session, message);
	}

	@Override
	public final void exceptionCaught(final IoSession session, final Throwable cause) throws Exception {
		// Empty statement
	}

	@Override
	public void sessionOpened(final IoSession session) throws Exception {
		final InetSocketAddress address = (InetSocketAddress) session.getRemoteAddress();
		final String ip = address.getAddress().getHostAddress();
		if (this.blockedIPs.contains(address)) {
			session.close(true);
			return;
		}
		final SessionFloodEntry floodEntry = this.floodTracker.get(ip);
		int count;
		if (floodEntry == null) {
			count = 1;
		} else {
			count = floodEntry.Count;
			final long difference = System.currentTimeMillis() - floodEntry.Timestamp;
			if (difference < 2000) { // Less than 2 sec
				count++;
			} else if (difference > 20000) { // Over 20 sec
				count = 1;
			}
			if (count >= 10) {
				this.blockedIPs.add(ip);
				this.floodTracker.remove(ip);
				session.close(true);
				return;
			}
		}
		
		this.floodTracker.put(ip, new SessionFloodEntry(System.currentTimeMillis(), count));

		final byte clientIv[] = { 70, 114, 122, (byte) Randomizer.nextInt(255) };
		final byte serverIv[] = { 82, 48, 120, (byte) Randomizer.nextInt(255) };

		final AesTransform serverCrypto = new AesTransform(serverIv, ServerConstants.GAME_VERSION, VersionType.COMPLEMENT);
		final AesTransform clientCrypto = new AesTransform(clientIv, ServerConstants.GAME_VERSION, VersionType.REGULAR);

		final GameClient client = this.createClient(clientCrypto, serverCrypto, session);

		final PacketDecoder.DecoderState decoderState = new PacketDecoder.DecoderState();
		session.setAttribute(PacketDecoder.DECODER_STATE_KEY, decoderState);
		final GamePacket helloPacket = getHello(ServerConstants.GAME_VERSION, clientIv, serverIv);
		session.write(helloPacket);
		session.setAttribute(GameClient.CLIENT_KEY, client);
		session.getConfig().setBothIdleTime(30);
		System.out.println(":: IoSession opened " + ip + " ::");
	}

	protected abstract GameClient createClient(final AesTransform clientCrypto, final AesTransform serverCrypto, final IoSession session);

	@Override
	public void sessionClosed(final IoSession session) throws Exception {
		final GameClient client = (GameClient) session.getAttribute(GameClient.CLIENT_KEY);
		if (client != null) {
			client.disconnect();
			session.removeAttribute(GameClient.CLIENT_KEY);
		}
		super.sessionClosed(session);
	}

	@Override
	public void messageReceived(final IoSession session, final Object message) throws Exception {
		final PacketReader reader = new PacketReader((byte[]) message);
		final short opCode = reader.readShort();
		for (final ClientPacketOpcode code : ClientPacketOpcode.values()) {
			if (code.getValue() == opCode) {
				final GameClient client = (GameClient) session.getAttribute(GameClient.CLIENT_KEY);
				if (code.NeedsChecking()) {
					if (!client.isLoggedIn()) {
						return;
					}
				}
				try {
					this.handlePacket(code, reader, client);
				} catch (final PacketFormatException ex) {
					client.disconnect();
				}
				return;
			}
		}
	}

	@Override
	public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
		final ChannelClient client = (ChannelClient) session.getAttribute(GameClient.CLIENT_KEY);
		if (client != null) {
			client.sendPing();
		}
		super.sessionIdle(session, status);
	}

	protected abstract void handlePacket(final ClientPacketOpcode header, final PacketReader reader, GameClient client) throws PacketFormatException;

	private static GamePacket getHello(final short version, final byte[] clientIv, final byte[] serverIv) {
		final PacketBuilder builder = new PacketBuilder(16);

		builder.writeAsShort(14);
		builder.writeAsShort(version);
		builder.writeLengthPrefixedString("1");
		builder.writeBytes(clientIv);
		builder.writeBytes(serverIv);
		builder.writeAsByte(7);

		return builder.getPacket();
	}
}
