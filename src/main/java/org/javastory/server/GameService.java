/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import database.DatabaseConnection;
import handling.world.remote.ServerStatus;
import handling.world.remote.WorldRegistry;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import org.javastory.server.channel.ChannelServer;
import org.javastory.server.channel.ChannelWorldInterfaceImpl;
import org.javastory.server.mina.PacketHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.javastory.server.mina.GameCodecFactory;

/**
 *
 * @author Tosho
 */
public abstract class GameService {

    private final static IoFilter packetFilter =
            new ProtocolCodecFilter(new GameCodecFactory());
    private static final RMIClientSocketFactory socketFactory =
            new SslRMIClientSocketFactory();
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

    private static RMIClientSocketFactory getSocketFactory() {
        return socketFactory;
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
                                                       getSocketFactory());
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