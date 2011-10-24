package javastory.channel;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javastory.channel.client.SkillFactory;
import javastory.channel.handling.ChannelPacketHandler;
import javastory.channel.maps.GameMapFactory;
import javastory.channel.maps.MapTimer;
import javastory.channel.rmi.ChannelWorldInterfaceImpl;
import javastory.channel.server.AutobanManager;
import javastory.channel.server.Squad;
import javastory.channel.shops.HiredMerchantStore;
import javastory.config.ChannelInfo;
import javastory.config.WorldConfig;
import javastory.config.WorldInfo;
import javastory.game.ItemInfoProvider;
import javastory.game.maker.ItemMakerFactory;
import javastory.game.maker.RandomRewards;
import javastory.io.GamePacket;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.WorldChannelInterface;
import javastory.scripting.EventScriptManager;
import javastory.server.GameService;
import javastory.server.TimerManager;
import javastory.server.channel.PlayerStorage;
import javastory.server.handling.PacketHandler;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.ServerStatus;

import com.google.common.collect.Lists;

public final class ChannelServer extends GameService {

	private static ChannelServer instance;

	private ChannelWorldInterface cwi;
	private WorldChannelInterface wci = null;
	private boolean MegaphoneMuteState = false;
	private PlayerStorage players;
	private final GameMapFactory mapFactory = new GameMapFactory();
	private final Map<Integer, GuildSummary> gsStore = new HashMap<>();
	private final Map<String, Squad> mapleSquads = new HashMap<>();
	private final Map<Integer, HiredMerchantStore> merchants = new HashMap<>();
	private String serverMessage;
	private float expRate, mesoRate, itemRate;
	private int channelId, currentMerchantId = 0;
	private final ChannelInfo channelInfo;
	private final Lock merchantMutex = new ReentrantLock();
	private EventScriptManager eventManager;

	private ChannelServer(ChannelInfo info) {
		super(info);
		this.channelInfo = info;
	}

	public static synchronized boolean initialize(ChannelInfo info) {
		if (instance == null) {
			instance = new ChannelServer(info);
			return true;
		} else {
			return false;
		}
	}

	public int getChannelId() {
		return channelInfo.getId();
	}

	public ChannelInfo getChannelInfo() {
		return channelInfo;
	}

	public static void pingWorld() {
		try {
			instance.wci.ping();
		} catch (RemoteException ex) {
			if (instance.isWorldReady.compareAndSet(true, false)) {
				instance.connectToWorld();
			}
		}
	}

	@Override
	protected void connectToWorld() {
		try {
			worldRegistry = super.getRegistry();

			// TODO: implement the interface in this class.
			cwi = new ChannelWorldInterfaceImpl(this);
			wci = worldRegistry.registerChannelServer(this.channelInfo, cwi);
			wci.serverReady();
		} catch (NotBoundException | RemoteException ex) {
			Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		isWorldReady.compareAndSet(false, true);
	}

	@Override
	protected final void loadSettings() {
		WorldInfo info = WorldConfig.load(this.channelInfo.getWorldId());
		expRate = (float) info.getExpRate() / 100;
		mesoRate = (float) info.getMesoRate() / 100;
		itemRate = (float) info.getItemRate() / 100;

		// TODO: do this one in the DB too.
		serverMessage = "";
	}

	public void initialize() {
		connectToWorld();
		loadSettings();

		List<String> events = loadEventsFromDb();
		eventManager = new EventScriptManager(events);

		TimerManager.getInstance().start();
		TimerManager.getInstance().register(AutobanManager.getInstance(), 60000);
		MapTimer.getInstance().start();

		ItemMakerFactory.getInstance();
		ItemInfoProvider.getInstance();
		RandomRewards.getInstance();
		SkillFactory.getSkill(99999999);
		players = new PlayerStorage();

		final PacketHandler serverHandler = new ChannelPacketHandler(this.channelId);
		super.bind(serverHandler);
		try {
			wci.serverReady();
		} catch (RemoteException ex) {
			Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.printf(":: Channel %d : Listening on port %d ::", this.getChannelId(), super.endpointInfo.getPort());
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownListener()));
		eventManager.init();
	}

	public static GameMapFactory getMapFactory() {
		return instance.mapFactory;
	}

	public static void addPlayer(final ChannelCharacter chr) {
		instance.players.registerPlayer(chr);
		chr.getClient().write(ChannelPackets.headerMessage(instance.serverMessage));
	}

	public static PlayerStorage getPlayerStorage() {
		return instance.players;
	}

	public static void removePlayer(final ChannelCharacter chr) {
		instance.players.deregisterPlayer(chr);
	}

	public String getServerMessage() {
		return serverMessage;
	}

	public void setServerMessage(final String newMessage) {
		serverMessage = newMessage;
		broadcastPacket(ChannelPackets.headerMessage(serverMessage));
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

	public static WorldChannelInterface getWorldInterface() {
		if (instance.isWorldReady.get()) {
			return instance.wci;
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

	public final int getLoadedMaps() {
		return getMapFactory().getLoadedMaps();
	}

	public final EventScriptManager getEventSM() {
		return eventManager;
	}

	public final void reloadEvents() {
		List<String> events = loadEventsFromDb();
		eventManager.cancel();
		eventManager = new EventScriptManager(events);
		eventManager.init();
	}

	private List<String> loadEventsFromDb() {
		// TODO: load events from DB.
		List<String> events = Lists.newArrayList();
		return events;
	}

	public final float getExpRate() {
		return expRate;
	}

	public final void setExpRate(final float expRate) {
		this.expRate = expRate;
	}

	public final float getMesoRate() {
		return mesoRate;
	}

	public final void setMesoRate(final float mesoRate) {
		this.mesoRate = mesoRate;
	}

	public final float getItemRate() {
		return itemRate;
	}

	public final void setItemRate(final float dropRate) {
		this.itemRate = dropRate;
	}

	public final Guild getGuild(final int guildId) {
		Guild guild = null;
		try {
			guild = getWorldInterface().getGuild(guildId);
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
			final Guild guild = ChannelServer.getWorldInterface().getGuild(guildId);
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
		try {
			final Iterator<HiredMerchantStore> iterator = merchants.values().iterator();
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
			final Iterator<HiredMerchantStore> iterator = merchants.values().iterator();
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

	public final List<ChannelCharacter> getPartyMembers(final int partyId) {
		List<ChannelCharacter> partym = new LinkedList<>();
		try {
			Party party = ChannelServer.getWorldInterface().getParty(partyId);
			for (final PartyMember partychar : party.getMembers()) {
				if (partychar.getChannel() == getChannelId()) {
					// Make sure the thing doesn't get duplicate plays due to
					// ccing bug.
					ChannelCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
					if (chr != null) {
						partym.add(chr);
					}
				}
			}
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
		return partym;
	}

	private class ShutdownListener implements Runnable {

		@Override
		public void run() {
			shutdown();
		}
	}

	@Override
	public final void shutdown() {
		if (super.getStatus() != ServerStatus.ONLINE) {
			throw new IllegalStateException("The server is not active.");
		}
		super.setStatus(ServerStatus.SHUTTING_DOWN);

		System.out.printf("Channel %d, Saving hired merchants...", this.channelId);
		this.closeAllMerchant();

		System.out.printf("Channel %d, Saving characters...", this.channelId);
		this.players.disconnectAll();

		System.out.printf("Channel %d, Unbinding ports...", this.channelId);
		super.unbind();
		super.setStatus(ServerStatus.OFFLINE);

		this.wci = null;
		this.cwi = null;
	}

	public final void shutdown(final int time) {
		TimerManager.getInstance().schedule(new ShutdownListener(), time);
	}

	public static ChannelServer getInstance() {
		return instance;
	}
}