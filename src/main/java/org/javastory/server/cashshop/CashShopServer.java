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
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javastory.server.EndpointInfo;
import org.javastory.server.GameService;
import org.javastory.tools.PropertyUtil;
import server.TimerManager;
import server.MapleItemInformationProvider;
import server.CashItemFactory;

public class CashShopServer extends GameService {

    private CashShopWorldInterface lwi;
    private CashShopInterface wli;
    private PlayerStorage_CS players;
    private static CashShopServer instance;

    public static CashShopServer getInstance() {
        return instance;
    }

    private CashShopServer(EndpointInfo info) {
        super(info);
    }

    public final void pingWorld() {
        // check if the connection is really gone
        try {
            wli.isAvailable();
        } catch (RemoteException ex) {
            if (isWorldReady.compareAndSet(true, false)) {
                connectToWorld();
            }
        }
    }

    protected final void connectToWorld() {
        try {
            worldRegistry = super.getRegistry();
            lwi = new CashShopWorldInterfaceImpl();
            wli = worldRegistry.registerCSServer(lwi);
        } catch (AccessException ex) {
            Logger.getLogger(CashShopServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotBoundException ex) {
            Logger.getLogger(CashShopServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            Logger.getLogger(CashShopServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        isWorldReady.compareAndSet(false, true);
    }

    protected final void loadSettings() {
    }

    public final void initialize() {
        connectToWorld();

        TimerManager.getInstance().start();
        CashItemFactory.getInstance();
        SkillFactory.getSkill(99999999); // Load
        MapleItemInformationProvider.getInstance();
        players = new PlayerStorage_CS();

        final PacketHandler handler = new PacketHandler(ServerType.CASHSHOP);
        super.bind(handler);

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownListener()));
    }

    private class ShutdownListener implements Runnable {

        @Override
        public void run() {
            shutdown();
        }
    }

    public static void startCashShop_main() {
        try {
            // TODO: Load config from DB.
            instance = new CashShopServer(new EndpointInfo(8686));
            instance.initialize();
        } catch (final Exception ex) {
            System.err.println("Error initializing Cash Shop server" + ex);
        }
    }

    public final PlayerStorage_CS getPlayerStorage() {
        return players;
    }

    public final CashShopInterface getCSInterface() {
        if (!isWorldReady.get()) {
            throw new IllegalStateException("The world server is not ready.");
        }
        return wli;
    }

    public final void shutdown() {
        System.out.println("Saving all connected clients...");
        players.disconnectAll();
        super.unbind();

        try {
            worldRegistry.deregisterCSServer();
        } catch (RemoteException e) {
        }
        System.exit(0);
    }
}