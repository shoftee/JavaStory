package javastory.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javastory.rmi.Sockets;
import javastory.server.handling.GameCodecFactory;
import javastory.server.handling.PacketHandler;
import javastory.world.core.ServerStatus;
import javastory.world.core.WorldRegistry;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * 
 * @author shoftee
 */
public abstract class GameService {

	private final static IoFilter packetFilter = new ProtocolCodecFilter(new GameCodecFactory());

	protected static WorldRegistry worldRegistry;

	private IoAcceptor acceptor;
	private volatile ServerStatus status;

	protected EndpointInfo endpointInfo;
	protected Properties settings = new Properties();
	protected AtomicBoolean isWorldReady;

	private GameService() {
		this.isWorldReady = new AtomicBoolean(false);
	}

	protected GameService(final EndpointInfo endpointInfo) {
		this();
		this.endpointInfo = endpointInfo;
	}

	protected GameService(final int port) {
		this();
		this.endpointInfo = new EndpointInfo("127.0.0.1", port);
	}

	private static IoFilter getPacketFilter() {
		return packetFilter;
	}

	private void getNewAcceptor(final PacketHandler handler) {
		if (this.acceptor != null) {
			this.acceptor.unbind();
			this.acceptor.dispose(false);
		}
		this.acceptor = new NioSocketAcceptor();
		this.acceptor.getSessionConfig().setWriterIdleTime(0);
		this.acceptor.getFilterChain().addLast("codec", getPacketFilter());
		this.acceptor.setHandler(handler);
	}

	protected final WorldRegistry getRegistry() throws RemoteException, AccessException, NotBoundException {
		final String registryIP = System.getProperty("org.javastory.world.ip");
		final Registry registry = LocateRegistry.getRegistry(registryIP, Registry.REGISTRY_PORT, Sockets.getClientFactory());
		return (WorldRegistry) registry.lookup("WorldRegistry");
	}

	protected final void bind(final PacketHandler handler) {
		this.getNewAcceptor(handler);

		final SocketAddress address = this.endpointInfo.asSocketAddress();

		try {
			this.acceptor.bind(address);
			System.out.println(":: Successfully bound to " + address + " ::");
		} catch (final IOException e) {
			System.err.println("Binding to port " + address + " failed" + e);
		}
	}

	protected final void unbind() {
		this.acceptor.unbind();
	}

	public final String getIP() {
		return this.endpointInfo.getHost();
	}

	public final ServerStatus getStatus() {
		return this.status;
	}

	protected final void setStatus(final ServerStatus status) {
		this.status = status;
	}

	protected final void reconnectWorld() {
		if (this.isWorldReady.compareAndSet(false, true)) {
			this.connectToWorld();
		}
	}

	protected abstract void connectToWorld();

	protected abstract void loadSettings();

	public abstract void shutdown();

	public static WorldRegistry getWorldRegistry() {
		return worldRegistry;
	}
}
