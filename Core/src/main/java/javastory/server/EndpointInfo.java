/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 *
 * @author Tosho
 */
public class EndpointInfo implements Serializable {

    private String host;
    private int port;
    private SocketAddress address;

    public EndpointInfo(int port) {
        Preconditions.checkArgument(0 <= port && port <= 65535);

        this.host = "127.0.0.1";
        this.port = port;

        this.address = new InetSocketAddress(port);
    }

    public EndpointInfo(String host, int port) {
        Preconditions.checkNotNull(host);

        this.host = host;
        this.port = port;

        this.address = new InetSocketAddress(host, port);
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public final SocketAddress asSocketAddress() {
        return this.address;
    }
}
