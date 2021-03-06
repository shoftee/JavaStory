package javastory.rmi;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

/**
 * 
 * @author shoftee
 */
public final class Sockets {

	private final static RMIServerSocketFactory serverFactory = new SslRMIServerSocketFactory();
	private final static RMIClientSocketFactory clientFactory = new SslRMIClientSocketFactory();

	public static RMIServerSocketFactory getServerFactory() {
		return serverFactory;
	}

	public static RMIClientSocketFactory getClientFactory() {
		return clientFactory;
	}
}
