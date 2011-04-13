/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package handling.world;

import client.OdinSEA;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import database.DatabaseConnection;

public class WorldServer {

    private final static WorldServer instance = new WorldServer();
    private int worldId;
    private Properties dbProp = new Properties();
    private Properties worldProp = new Properties();

    private WorldServer() {
	try {
	    InputStreamReader is = new FileReader("db.properties");
	    dbProp.load(is);
	    is.close();
	    DatabaseConnection.setProps(dbProp);
	    DatabaseConnection.getConnection();
	    is = new FileReader("world.properties");
	    worldProp.load(is);
	    is.close();
	} catch (final Exception e) {
	    System.err.println("Could not configuration" + e);
	}
    }

    public static final WorldServer getInstance() {
	return instance;
    }

    public final int getWorldId() {
	return worldId;
    }

    public final Properties getDbProp() {
	return dbProp;
    }

    public final Properties getWorldProp() {
	return worldProp;
    }

    public static final void startWorld_Main() {
	try {
	    final Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
	    registry.rebind("WorldRegistry", WorldRegistryImpl.getInstance());
            	} catch (final RemoteException ex) {
	    System.err.println("Could not initialize RMI system" + ex);
	}
    }
} 
 /*   public static void startWorld_Main(String[] args) {
		try {
			Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT,
				new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
			registry.rebind("WorldRegistry", WorldRegistryImpl.getInstance());
		} catch (RemoteException ex) {
			System.err.println("Could not initialize RMI system", ex);
		}
    }
}*/
