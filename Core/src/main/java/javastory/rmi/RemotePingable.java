package javastory.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemotePingable extends Remote {
	public boolean ping() throws RemoteException;
}
