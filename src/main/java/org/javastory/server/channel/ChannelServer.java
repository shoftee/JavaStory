package org.javastory.server.channel;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
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
import handling.GamePacket;
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
import server.TimerManager;
import server.ItemMakerFactory;
import server.RandomRewards;
import server.MapleItemInformationProvider;
import server.maps.MapTimer;
import server.maps.MapleMapFactory;
import server.shops.HiredMerchant;
import tools.MaplePacketCreator;

public final class ChannelServer extends GameService {

    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private boolean MegaphoneMuteState = false;
    private PlayerStorage players;
    private final MapleMapFactory mapFactories[] = new MapleMapFactory[2];
    private final Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private final Map<String, MapleSquad> mapleSquads = new HashMap<String, MapleSquad>();
    private final Map<Integer, HiredMerchant> merchants = new HashMap<Integer, HiredMerchant>();
    private String serverMessage, name;
    private byte expRate, mesoRate, dropRate;
    private int channelId, currentMerchantId = 0;
    private final ChannelInfo channelInfo;
    private final Lock merchantMutex = new ReentrantLock();
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

    public void pingWorld() {
        try {
            wci.isAvailable();
        } catch (RemoteException ex) {
            if (isWorldReady.compareAndSet(true, false)) {
                connectToWorld();
            }
        }
    }

    protected void connectToWorld() {
        try {
            worldRegistry = super.getRegistry();
            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(this.channelInfo, cwi);
            wci.serverReady();
        } catch (AccessException ex) {
            Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotBoundException ex) {
            Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        isWorldReady.compareAndSet(false, true);
    }

    protected final void loadSettings() {
        // TODO: load settings from DB;
        expRate = 1;
        mesoRate = 1;
        dropRate = 1;
        serverMessage = "";
    }

    public void initialize() {
        connectToWorld();
        loadSettings();
        
        // TODO: load events from DB.
        final String[] events = new String[0];
        for (int i = 0; i < eventManagers.length; i++) {
            eventManagers[i] = new EventScriptManager(this, events, i == 0 ? 6 : 5);
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
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownListener()));
        for (EventScriptManager manager : eventManagers) {
            manager.init();
        }
    }

    public MapleMapFactory getMapFactory(int world) {
        return mapFactories[world == 6 ? 0 : 1];
    }

    public void addPlayer(final MapleCharacter chr) {
        players.registerPlayer(chr);
        chr.getClient().getSession().write(MaplePacketCreator.serverMessage(serverMessage));
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(final MapleCharacter chr) {
        players.deregisterPlayer(chr);
    }

    public String getServerMessage() {
        return serverMessage;
    }

    public void setServerMessage(final String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    public void broadcastPacket(final GamePacket data) {
        players.broadcastPacket(data);
    }

    public void broadcastSmegaPacket(final GamePacket data) {
        players.broadcastSmegaPacket(data);
    }

    public void broadcastGMPacket(final GamePacket data) {
        players.broadcastGMPacket(data);
    }

    public String getIP(final int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            System.err.println("Lost connection to world server" + e);
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public WorldChannelInterface getWorldInterface() {
        if (isWorldReady.get()) {
            return wci;
        }
        return null;
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
        int i = 0;
        final String[] events = new String[0];

        for (EventScriptManager manager : eventManagers) {
            manager.cancel();
            manager = new EventScriptManager(this, events, i == 0 ? 6 : 5);
            manager.init();
            i++;
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
        final int guildId = mgc.getGuildId();
        MapleGuild guild = null;
        try {
            guild = getWorldInterface().getGuild(guildId, mgc);
        } catch (RemoteException re) {
            System.err.println("RemoteException while fetching MapleGuild." + re);
            return null;
        }
        if (gsStore.get(guildId) == null) {
            gsStore.put(guildId, new MapleGuildSummary(guild));
        }
        return guild;
    }

    public final MapleGuildSummary getGuildSummary(final int guildId) {
        if (gsStore.containsKey(guildId)) {
            return gsStore.get(guildId);
        }
        try {
            final MapleGuild guild = 
                    this.getWorldInterface().getGuild(guildId, null);
            if (guild != null) {
                gsStore.put(guildId, new MapleGuildSummary(guild));
            }
            return gsStore.get(guildId);
        } catch (RemoteException re) {
            System.err.println("RemoteException while fetching GuildSummary." + re);
            return null;
        }
    }

    public final void updateGuildSummary(final int guildId, final MapleGuildSummary summary) {
        gsStore.put(guildId, summary);
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
        merchantMutex.lock();
        final Iterator<HiredMerchant> iterator = 
                merchants.values().iterator();
        try {
            while (iterator.hasNext()) {
                HiredMerchant merchant = iterator.next();
                merchant.closeShop(true, false);
                iterator.remove();
            }
        } finally {
            merchantMutex.unlock();
        }
    }

    public final int addMerchant(final HiredMerchant merchant) {
        merchantMutex.lock();
        int id = 0;
        try {
            id = currentMerchantId;
            merchants.put(id, merchant);
            currentMerchantId++;
        } finally {
            merchantMutex.unlock();
        }
        return id;
    }

    public final void removeMerchant(final HiredMerchant merchant) {
        merchantMutex.lock();
        try {
            merchants.remove(merchant.getStoreId());
        } finally {
            merchantMutex.unlock();
        }
    }

    public final boolean hasMerchant(final int accountId) {
        boolean contains = false;
        merchantMutex.lock();
        try {
            final Iterator<HiredMerchant> iterator = 
                    merchants.values().iterator();
            while (iterator.hasNext()) {
                HiredMerchant merchant = iterator.next();
                if (merchant.getOwnerAccountId() == accountId) {
                    contains = true;
                    break;
                }
            }
        } finally {
            merchantMutex.unlock();
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
            if (partychar.getChannel() == getChannelId()) {
                // Make sure the thing doesn't get duplicate plays due to ccing bug.
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    private class ShutdownListener implements Runnable {

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