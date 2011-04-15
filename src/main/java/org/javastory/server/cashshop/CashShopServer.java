package org.javastory.server.cashshop;

import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Properties;

import client.SkillFactory;
import database.DatabaseConnection;
import org.javastory.server.mina.PacketHandler;
import handling.ServerConstants;
import handling.ServerType;
import handling.cashshop.CashShopWorldInterfaceImpl;
import handling.cashshop.CashShopWorldInterfaceImpl;
import handling.cashshop.PlayerStorage_CS;
import handling.cashshop.PlayerStorage_CS;
import handling.world.remote.WorldRegistry;
import handling.cashshop.remote.CashShopWorldInterface;
import handling.world.remote.CashShopInterface;
import java.io.InputStreamReader;
import org.javastory.server.EndpointInfo;
import org.javastory.server.GameService;
import org.javastory.tools.PropertyUtil;
import server.TimerManager;
import server.MapleItemInformationProvider;
import server.CashItemFactory;

public class CashShopServer extends GameService {

    private CashShopWorldInterface lwi;
    private CashShopInterface wli;
    private Properties csProp;
    private Boolean worldReady = Boolean.TRUE;
    private WorldRegistry worldRegistry = null;
    private PlayerStorage_CS players;
    private static CashShopServer instance;

    public static CashShopServer getInstance() {
        return instance;
    }

    private CashShopServer() {
        super(8686);
    }

    public final void pingWorld() {
        // check if the connection is really gone
        try {
            wli.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = Boolean.FALSE;
            }
            synchronized (lwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wli) {
                    reconnectWorld();
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }

    }

    private void reconnectWorld() {
        // completely re-establish the rmi connection
        try {
            final String config =
                    System.getProperty("org.javastory.cashshop.config");
            FileReader fileReader = new FileReader(config);
            super.settings.load(fileReader);
            fileReader.close();

            worldRegistry = super.getRegistry();

            lwi = new CashShopWorldInterfaceImpl();
            wli = worldRegistry.registerCSServer(lwi);
        } catch (Exception e) {
            System.err.println("Reconnecting failed" + e);
        }
        worldReady = Boolean.TRUE;
    }

    public final void initialize() {
        try {
            final String config =
                    System.getProperty("org.javastory.cashshop.config");
            PropertyUtil.loadInto(config, settings);
            
            worldRegistry = super.getRegistry();

            lwi = new CashShopWorldInterfaceImpl(this);
            wli = worldRegistry.registerCSServer(lwi);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not connect to world server.", e);
        }

        TimerManager.getInstance().start();
        CashItemFactory.getInstance();
        SkillFactory.getSkill(99999999); // Load
        MapleItemInformationProvider.getInstance();
        players = new PlayerStorage_CS();

        final PacketHandler handler = new PacketHandler(ServerType.CASHSHOP);
        super.bind(handler);

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownListener()));
    }

    private final class ShutDownListener implements Runnable {

        @Override
        public void run() {
            shutdown();
        }
    }

    public static void startCashShop_main() {
        try {
            instance = new CashShopServer();
            instance.initialize();
        } catch (final Exception ex) {
            System.err.println("Error initializing Cash Shop server" + ex);
        }
    }

    public final PlayerStorage_CS getPlayerStorage() {
        return players;
    }

    public final CashShopInterface getCSInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (final InterruptedException e) {
                }
            }
        }
        return wli;
    }

    public final void shutdown2() {
        System.out.println("Shutting down...");
        try {
            worldRegistry.deregisterCSServer();
        } catch (final RemoteException e) {
            // doesn't matter we're shutting down anyway
        }
        System.exit(0);
    }

    public final void shutdown() {
        System.out.println("Saving all connected clients...");
        players.disconnectAll();
        super.unbind();
        System.exit(0);
    }
}