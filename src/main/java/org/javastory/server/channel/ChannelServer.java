package org.javastory.server.channel;

import org.javastory.server.ChannelInfo;
import org.javastory.server.GameService;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.MapleCharacter;
import client.SkillFactory;
import database.DatabaseConnection;
import handling.ServerType;
import handling.MaplePacket;
import org.javastory.server.mina.PacketHandler;
import handling.channel.remote.ChannelWorldInterface;
import handling.world.MaplePartyCharacter;
import handling.world.MapleParty;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.world.guild.MapleGuildSummary;
import handling.world.remote.ServerStatus;
import handling.world.remote.WorldChannelInterface;
import handling.world.remote.WorldRegistry;

import scripting.EventScriptManager;
import server.AutobanManager;
import server.MapleSquad;
import server.ShutdownChannelServer;
import server.TimerManager;
import server.ItemMakerFactory;
import server.RandomRewards;
import server.MapleItemInformationProvider;
import server.maps.MapTimer;
import server.maps.MapleMapFactory;
import server.shops.HiredMerchant;
import tools.MaplePacketCreator;

public class ChannelServer extends GameService {

    private static WorldRegistry worldRegistry;
    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private Properties worldProperties;
    private Boolean worldReady = true;
    private boolean MegaphoneMuteState = false;
    private PlayerStorage players;
    private final MapleMapFactory mapFactories[] = new MapleMapFactory[2];
    private final Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private final Map<String, MapleSquad> mapleSquads = new HashMap<String, MapleSquad>();
    private final Map<Integer, HiredMerchant> merchants = new HashMap<Integer, HiredMerchant>();
    private String serverMessage, name;
    private byte expRate, mesoRate, dropRate;
    private int channelId, running_MerchantID = 0;
    private final ChannelInfo channelInfo;
    private final Lock merchant_mutex = new ReentrantLock();
    private EventScriptManager eventManagers[] = new EventScriptManager[2];

    public ChannelServer(ChannelInfo info) {
        super(info);
        this.channelInfo = info;
    }

    public int getChannelId() {
        return channelInfo.getId();
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public final void pingWorld() {
        try {
            wci.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = false;
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wci) {
                    reconnectWorld();
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    private void reconnectWorld() {
        try {

            worldRegistry = super.getRegistry();

            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(this.channelInfo, cwi);
            worldProperties = wci.getGameProperties();
            expRate = Byte.parseByte(worldProperties.getProperty("org.javastory.world.exp"));
            mesoRate = Byte.parseByte(worldProperties.getProperty("org.javastory.world.meso"));
            dropRate = Byte.parseByte(worldProperties.getProperty("org.javastory.world.drop"));
            serverMessage = worldProperties.getProperty("org.javastory.world.serverMessage");

            wci.serverReady();
        } catch (Exception e) {
            System.err.println("Reconnecting failed" + e);
        }
        worldReady = true;
    }

    public void initialize() {
        try {
            DatabaseConnection.initialize();

            worldRegistry = super.getRegistry();

            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(this.channelInfo, cwi);
            worldProperties = wci.getGameProperties();
            expRate = Byte.parseByte(worldProperties.getProperty("org.javastory.world.exp"));
            mesoRate = Byte.parseByte(worldProperties.getProperty("org.javastory.world.meso"));
            dropRate = Byte.parseByte(worldProperties.getProperty("org.javastory.world.drop"));
            serverMessage = worldProperties.getProperty("org.javastory.world.serverMessage");
            final String[] events = worldProperties.getProperty("org.javastory.events").split(",");
            for (int i = 0; i < eventManagers.length; i++) {
                eventManagers[i] = new EventScriptManager(this, events, i == 0 ? 6 : 5);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TimerManager.getInstance().start();
        TimerManager.getInstance().register(AutobanManager.getInstance(), 60000);
        MapTimer.getInstance().start();

        ItemMakerFactory.getInstance();
        MapleItemInformationProvider.getInstance();
        RandomRewards.getInstance();
        SkillFactory.getSkill(99999999);
        players = new PlayerStorage();

        for (int i = 0; i < mapFactories.length; i++) {
            mapFactories[i] = new MapleMapFactory();
            mapFactories[i].setWorld(i == 0 ? 6 : 5);
            mapFactories[i].setChannel(this.channelId);
        }

        final PacketHandler serverHandler =
                new PacketHandler(ServerType.CHANNEL, this.channelId);
        super.bind(serverHandler);
        try {
            wci.serverReady();
        } catch (RemoteException ex) {
            Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.printf(":: Channel %d : Listening on port %d ::",
                          this.getChannelId(), super.endpointInfo.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownListener()));
        for (EventScriptManager esm : eventManagers) {
            esm.init();
        }
    }

    public final MapleMapFactory getMapFactory(int world) {
        return mapFactories[world == 6 ? 0 : 1];
    }

    public final void addPlayer(final MapleCharacter chr) {
        players.registerPlayer(chr);
        chr.getClient().getSession().write(MaplePacketCreator.serverMessage(serverMessage));
    }

    public final PlayerStorage getPlayerStorage() {
        return players;
    }

    public final void removePlayer(final MapleCharacter chr) {
        players.deregisterPlayer(chr);
    }

    public final String getServerMessage() {
        return serverMessage;
    }

    public final void setServerMessage(final String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    public final void broadcastPacket(final MaplePacket data) {
        players.broadcastPacket(data);
    }

    public final void broadcastSmegaPacket(final MaplePacket data) {
        players.broadcastSmegaPacket(data);
    }

    public final void broadcastGMPacket(final MaplePacket data) {
        players.broadcastGMPacket(data);
    }

    public final String getIP(final int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            System.err.println("Lost connection to world server" + e);
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public final WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                    //
                }
            }
        }
        return wci;
    }

    public final void shutdownWorld(final int time) {
        try {
            getWorldInterface().shutdown(time);
        } catch (RemoteException e) {
            pingWorld();
        }
    }

    public final void shutdownLogin() {
        try {
            getWorldInterface().shutdownLogin();
        } catch (RemoteException e) {
            pingWorld();
        }
    }

    public final int getLoadedMaps(int world) {
        return getMapFactory(world).getLoadedMaps();
    }

    public final EventScriptManager getEventSM(int world) {
        return eventManagers[world == 6 ? 0 : 1];
    }

    public final void reloadEvents() {
        final String[] events = worldProperties.getProperty("org.javastory.channel.events").split(",");
        for (int i = 0; i < eventManagers.length; i++) {
            eventManagers[i].cancel();
            eventManagers[i] = new EventScriptManager(this, events, i == 0 ? 6 : 5);
            eventManagers[i].init();
        }
    }

    public final byte getExpRate() {
        return expRate;
    }

    public final void setExpRate(final byte expRate) {
        this.expRate = expRate;
    }

    public final byte getMesoRate() {
        return mesoRate;
    }

    public final void setMesoRate(final byte mesoRate) {
        this.mesoRate = mesoRate;
    }

    public final byte getDropRate() {
        return dropRate;
    }

    public final void setDropRate(final byte dropRate) {
        this.dropRate = dropRate;
    }

    public final MapleGuild getGuild(final MapleGuildCharacter mgc) {
        final int gid = mgc.getGuildId();
        MapleGuild g = null;
        try {
            g = getWorldInterface().getGuild(gid, mgc);
        } catch (RemoteException re) {
            System.err.println("RemoteException while fetching MapleGuild." + re);
            return null;
        }
        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }
        return g;
    }

    public final MapleGuildSummary getGuildSummary(final int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        }
        try {
            final MapleGuild g = this.getWorldInterface().getGuild(gid, null);
            if (g != null) {
                gsStore.put(gid, new MapleGuildSummary(g));
            }
            return gsStore.get(gid);
        } catch (RemoteException re) {
            System.err.println("RemoteException while fetching GuildSummary." + re);
            return null;
        }
    }

    public final void updateGuildSummary(final int gid, final MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public final MapleSquad getMapleSquad(final String type) {
        return mapleSquads.get(type);
    }

    public final boolean addMapleSquad(final MapleSquad squad, final String type) {
        if (mapleSquads.get(type) == null) {
            mapleSquads.remove(type);
            mapleSquads.put(type, squad);
            return true;
        }
        return false;
    }

    public final boolean removeMapleSquad(final String type) {
        if (mapleSquads.containsKey(type)) {
            mapleSquads.remove(type);
            return true;
        }
        return false;
    }

    public final void closeAllMerchant() {
        merchant_mutex.lock();
        final Iterator<HiredMerchant> merchants_ = merchants.values().iterator();
        try {
            while (merchants_.hasNext()) {
                merchants_.next().closeShop(true, false);
                merchants_.remove();
            }
        } finally {
            merchant_mutex.unlock();
        }
    }

    public final int addMerchant(final HiredMerchant hMerchant) {
        merchant_mutex.lock();
        int runningmer = 0;
        try {
            runningmer = running_MerchantID;
            merchants.put(running_MerchantID, hMerchant);
            running_MerchantID++;
        } finally {
            merchant_mutex.unlock();
        }
        return runningmer;
    }

    public final void removeMerchant(final HiredMerchant hMerchant) {
        merchant_mutex.lock();
        try {
            merchants.remove(hMerchant.getStoreId());
        } finally {
            merchant_mutex.unlock();
        }
    }

    public final boolean constainsMerchant(final int accid) {
        boolean contains = false;
        merchant_mutex.lock();
        try {
            final Iterator itr = merchants.values().iterator();
            while (itr.hasNext()) {
                if (((HiredMerchant) itr.next()).getOwnerAccId() == accid) {
                    contains = true;
                    break;
                }
            }
        } finally {
            merchant_mutex.unlock();
        }
        return contains;
    }

    public final void toggleMegaponeMuteState() {
        this.MegaphoneMuteState = !this.MegaphoneMuteState;
    }

    public final boolean getMegaphoneMuteState() {
        return MegaphoneMuteState;
    }

    public final List<MapleCharacter> getPartyMembers(final MapleParty party) {
        List<MapleCharacter> partym = new LinkedList<MapleCharacter>();
        for (final MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannelId()) { // Make sure the thing doesn't get duplicate plays due to ccing bug.
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    private final class ShutDownListener implements Runnable {

        @Override
        public void run() {
            shutdown();
        }
    }

    public final void shutdown() {
        if (super.getStatus() != ServerStatus.ONLINE) {
            throw new IllegalStateException("The server is not active.");
        }
        super.setStatus(ServerStatus.SHUTTING_DOWN);

        System.out.printf("Channel %d, Saving hired merchants...",
                          this.channelId);
        this.closeAllMerchant();

        System.out.printf("Channel %d, Saving characters...",
                          this.channelId);
        this.players.disconnectAll();

        System.out.printf("Channel %d, Unbinding ports...",
                          this.channelId);
        super.unbind();
        super.setStatus(ServerStatus.OFFLINE);

        this.wci = null;
        this.cwi = null;
    }

    public final void shutdown(final int time) {
        TimerManager.getInstance().schedule(
                new ShutdownChannelServer(this.getChannelId()), time);
    }
}