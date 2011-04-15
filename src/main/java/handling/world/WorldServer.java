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

import java.io.FileReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import database.DatabaseConnection;
import java.io.IOException;
import java.io.Reader;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;

public class WorldServer {

    private final static WorldServer instance = new WorldServer();
    private int worldId;
    private final Properties worldProperties;

    private WorldServer() {
        worldProperties = new Properties();
        DatabaseConnection.initialize();

        try {
            Reader reader = new FileReader("world.properties");
            worldProperties.load(reader);
            reader.close();
        } catch (final IOException e) {
            System.err.println("Could not configuration" + e);
        }
    }

    public static WorldServer getInstance() {
        return instance;
    }

    public final int getWorldId() {
        return worldId;
    }

    public final Properties getWorldProperties() {
        return worldProperties;
    }

    public static void startWorld_Main() {
        try {
            final RMIClientSocketFactory clientFactory =
                    new SslRMIClientSocketFactory();
            final RMIServerSocketFactory serverFactory =
                    new SslRMIServerSocketFactory();
            final Registry registry = LocateRegistry.createRegistry(
                    Registry.REGISTRY_PORT, clientFactory, serverFactory);

            registry.bind("WorldRegistry", WorldRegistryImpl.getInstance());
        } catch (final AccessException ex) {
            System.err.println("Access denied: you do not have permissions to bind.");
        } catch (final AlreadyBoundException ex) {
            System.err.println("The RMI binding is already in place.");
        } catch (final RemoteException ex) {
            System.err.println("Could not initialize RMI system" + ex);
        }
    }
}
