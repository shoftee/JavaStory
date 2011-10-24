package javastory.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javastory.server.Location;

public interface LocationRegistry extends Remote {
	
	public Location locate(int characterId) throws RemoteException;

	public Location locate(String characterName) throws RemoteException;

	public void notifyLocationChanged(int characterId, Location newLocation) throws RemoteException;

	public void notifyLocationChanged(String characterName, Location newLocation) throws RemoteException;
	
}
