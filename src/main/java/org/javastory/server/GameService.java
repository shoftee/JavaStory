/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import handling.world.remote.ServerStatus;
import handling.world.remote.WorldRegistry;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import org.javastory.server.mina.PacketHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Properties;
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

    private static IoFilter getPacketFilter() {
        return packetFilter;
    }

    private static RMIClientSocketFactory getSocketFactory() {
        return socketFactory;
    }
    
    private IoAcceptor acceptor;
    protected EndpointInfo endpointInfo;
    protected Properties settings = new Properties();
    private volatile ServerStatus status;

    public abstract void shutdown();

    public final void unbind() {
        this.acceptor.unbind();
    }
    
    protected GameService(EndpointInfo endpointInfo) {
        this.endpointInfo = endpointInfo;
    }
    
    protected GameService(int port) {
        this.endpointInfo = new EndpointInfo("127.0.0.1", port);
    }
    
    protected GameService(String host, int port) {
        this.endpointInfo = new EndpointInfo(host, port);
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

    protected void bind(final PacketHandler handler) {
        getNewAcceptor(handler);
        
        int port = endpointInfo.getPort();

        try {
            acceptor.bind(new InetSocketAddress(port));
            System.out.println(":: Listening on port " + port + " ::");
        } catch (final IOException e) {
            System.err.println("Binding to port " + port + " failed" + e);
        }
    }

    private void getNewAcceptor(final PacketHandler handler) {
        acceptor = new NioSocketAcceptor();
        acceptor.getSessionConfig().setWriterIdleTime(0);
        acceptor.getFilterChain().addLast("codec", getPacketFilter());
        acceptor.setHandler(handler);
    }

    public final String getIP() {
        return endpointInfo.getHost();
    }

    public ServerStatus getStatus() {
        return status;
    }

    protected void setStatus(ServerStatus status) {
        this.status = status;
    }
}
