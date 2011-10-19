/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import javastory.db.DatabaseConnection;
import javastory.world.core.ServerStatus;
import javastory.world.core.WorldRegistry;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import javastory.server.handling.PacketHandler;
import java.io.IOException;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javastory.rmi.Sockets;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import javastory.server.handling.GameCodecFactory;

/**
 *
 * @author Tosho
 */
public abstract class GameService {

    private final static IoFilter packetFilter =
            new ProtocolCodecFilter(new GameCodecFactory());

    protected static WorldRegistry worldRegistry;
    
    private IoAcceptor acceptor;
    private volatile ServerStatus status;
    
    protected EndpointInfo endpointInfo;
    protected Properties settings = new Properties();
    protected AtomicBoolean isWorldReady;

    private GameService() {
        isWorldReady = new AtomicBoolean(false);
        DatabaseConnection.initialize();
    }

    protected GameService(EndpointInfo endpointInfo) {
        this();
        this.endpointInfo = endpointInfo;
    }

    protected GameService(int port) {
        this();
        this.endpointInfo = new EndpointInfo("127.0.0.1", port);
    }

    private static IoFilter getPacketFilter() {
        return packetFilter;
    }

    private void getNewAcceptor(final PacketHandler handler) {
        if (acceptor != null) {
            acceptor.unbind();
            acceptor.dispose(false);
        }
        acceptor = new NioSocketAcceptor();
        acceptor.getSessionConfig().setWriterIdleTime(0);
        acceptor.getFilterChain().addLast("codec", getPacketFilter());
        acceptor.setHandler(handler);
    }

    protected final WorldRegistry getRegistry()
            throws RemoteException, AccessException, NotBoundException {
        final String registryIP =
                System.getProperty("org.javastory.world.ip");
        Registry registry = LocateRegistry.getRegistry(registryIP,
                                                       Registry.REGISTRY_PORT,
                                                       Sockets.getClientFactory());
        return (WorldRegistry) registry.lookup("WorldRegistry");
    }

    protected final void bind(final PacketHandler handler) {
        getNewAcceptor(handler);

        SocketAddress address = endpointInfo.asSocketAddress();
        
        try {
            acceptor.bind(address);
            System.out.println(":: Successfully bound to " + address + " ::");
        } catch (final IOException e) {
            System.err.println("Binding to port " + address + " failed" + e);
        }
    }

    protected final void unbind() {
        this.acceptor.unbind();
    }

    public final String getIP() {
        return endpointInfo.getHost();
    }

    public final ServerStatus getStatus() {
        return status;
    }

    protected final void setStatus(ServerStatus status) {
        this.status = status;
    }

    protected final void reconnectWorld() {
        if (isWorldReady.compareAndSet(false, true)) {
            connectToWorld();
        }
    }

    protected abstract void connectToWorld();

    protected abstract void loadSettings();

    public abstract void shutdown();

    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }
}
