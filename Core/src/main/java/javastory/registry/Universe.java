package javastory.registry;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javastory.rmi.Sockets;

public class Universe {

	private static WorldRegistry worldRegistry;

	public static final WorldRegistry getOrBindWorldRegistry() throws RemoteException, AccessException, NotBoundException {
		if (worldRegistry == null) {
			final String registryIP = System.getProperty("org.javastory.world.ip");
			final Registry registry = LocateRegistry.getRegistry(registryIP, Registry.REGISTRY_PORT, Sockets.getClientFactory());
			worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
		}
		return worldRegistry;
	}

	public static WorldRegistry getWorldRegistry() {
		return worldRegistry;
	}
}
