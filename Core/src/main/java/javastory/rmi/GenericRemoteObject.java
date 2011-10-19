/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author shoftee
 */
public abstract class GenericRemoteObject extends UnicastRemoteObject implements RemotePingable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6606025263720430182L;

	protected GenericRemoteObject() throws RemoteException {
        super(0, Sockets.getClientFactory(), Sockets.getServerFactory());
    }
    
    @Override
	public boolean ping() throws RemoteException {
        return true;
    }
}
