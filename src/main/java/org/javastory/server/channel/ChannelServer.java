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

import client.GameCharacter;
import client.SkillFactory;
import database.DatabaseConnection;
import handling.ServerType;
import handling.GamePacket;
import org.javastory.server.mina.PacketHandler;
import handling.channel.remote.ChannelWorldInterface;
import handling.world.PartyMember;
import handling.world.Party;
import handling.world.guild.Guild;
import handling.world.guild.GuildMember;
import handling.world.guild.GuildSummary;
import handling.world.remote.ServerStatus;
import handling.world.remote.WorldChannelInterface;
import handling.world.remote.WorldRegistry;

import scripting.EventScriptManager;
import server.AutobanManager;
import server.Squad;
import server.TimerManager;
import server.ItemMakerFactory;
import server.RandomRewards;
import server.ItemInfoProvider;
import server.maps.MapTimer;
import server.maps.GameMapFactory;
import server.shops.HiredMerchantStore;
import tools.MaplePacketCreator;

public final class ChannelServer extends GameService {

    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private boolean MegaphoneMuteState = false;
    private PlayerStorage players;
    private final GameMapFactory mapFactories[] = new GameMapFactory[2];
    private final Map<Integer, GuildSummary> gsStore = new HashMap<Integer, GuildSummary>();
    private final Map<String, Squad> mapleSquads = new HashMap<String, Squad>();
    private final Map<Integer, HiredMerchantStore> merchants = new HashMap<Integer, HiredMerchantStore>();
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
        ItemInfoProvider.getInstance();
        RandomRewards.getInstance();
        SkillFactory.getSkill(99999999);
        players = new PlayerStorage();

        for (int i = 0; i < mapFactories.length; i++) {
            mapFactories[i] = new GameMapFactory();
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

    public GameMapFactory getMapFactory(int world) {
        return mapFactories[world == 6 ? 0 : 1];
    }

    public void addPlayer(final GameCharacter chr) {
        players.registerPlayer(chr);
        chr.getClient().write(MaplePacketCreator.serverMessage(serverMessage));
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(final GameCharacter chr) {
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

    public final Guild getGuild(final GuildMember mgc) {
        final int guildId = mgc.getGuildId();
        Guild guild = null;
        try {
            guild = getWorldInterface().getGuild(guildId, mgc);
        } catch (RemoteException re) {
            System.err.println("RemoteException while fetching MapleGuild." + re);
            return null;
        }
        if (gsStore.get(guildId) == null) {
            gsStore.put(guildId, new GuildSummary(guild));
        }
        return guild;
    }

    public final GuildSummary getGuildSummary(final int guildId) {
        if (gsStore.containsKey(guildId)) {
            return gsStore.get(guildId);
        }
        try {
            final Guild guild = 
                    this.getWorldInterface().getGuild(guildId, null);
            if (guild != null) {
                gsStore.put(guildId, new GuildSummary(guild));
            }
            return gsStore.get(guildId);
        } catch (RemoteException re) {
            System.err.println("RemoteException while fetching GuildSummary." + re);
            return null;
        }
    }

    public final void updateGuildSummary(final int guildId, final GuildSummary summary) {
        gsStore.put(guildId, summary);
    }

    public final Squad getMapleSquad(final String type) {
        return mapleSquads.get(type);
    }

    public final boolean addMapleSquad(final Squad squad, final String type) {
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
        final Iterator<HiredMerchantStore> iterator = 
                merchants.values().iterator();
        try {
            while (iterator.hasNext()) {
                HiredMerchantStore merchant = iterator.next();
                merchant.closeShop(true, false);
                iterator.remove();
            }
        } finally {
            merchantMutex.unlock();
        }
    }

    public final int addMerchant(final HiredMerchantStore merchant) {
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

    public final void removeMerchant(final HiredMerchantStore merchant) {
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
            final Iterator<HiredMerchantStore> iterator = 
                    merchants.values().iterator();
            while (iterator.hasNext()) {
                HiredMerchantStore merchant = iterator.next();
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

    public final List<GameCharacter> getPartyMembers(final Party party) {
        List<GameCharacter> partym = new LinkedList<GameCharacter>();
        for (final PartyMember partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannelId()) {
                // Make sure the thing doesn't get duplicate plays due to ccing bug.
                GameCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
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