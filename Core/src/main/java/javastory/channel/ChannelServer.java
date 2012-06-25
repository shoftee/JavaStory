package javastory.channel;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javastory.game.data.ItemInfoProvider;
import javastory.game.data.ItemMakerFactory;
import javastory.game.data.RandomRewards;
import javastory.game.data.SkillInfoProvider;
import javastory.io.GamePacket;
import javastory.registry.Universe;
import javastory.registry.WorldRegistry;
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
import com.google.common.collect.Maps;

public final class ChannelServer extends GameService {

	private static ChannelServer instance;

	private ChannelWorldInterface cwi;
	private WorldChannelInterface wci = null;
	private boolean MegaphoneMuteState = false;
	private PlayerStorage players;
	private final GameMapFactory mapFactory = new GameMapFactory();
	private final Map<Integer, GuildSummary> gsStore = Maps.newHashMap();
	private final Map<String, Squad> mapleSquads = Maps.newHashMap();
	private final Map<Integer, HiredMerchantStore> merchants = Maps.newHashMap();
	private String serverMessage;
	private float expRate, mesoRate, itemRate;
	private int channelId, currentMerchantId = 0;
	private final ChannelInfo channelInfo;
	private final Lock merchantMutex = new ReentrantLock();
	private EventScriptManager eventManager;

	private ChannelServer(final ChannelInfo info) {
		super(info);
		this.channelInfo = info;
	}

	public static synchronized boolean initialize(final ChannelInfo info) {
		if (instance == null) {
			instance = new ChannelServer(info);
			return true;
		} else {
			return false;
		}
	}

	public int getChannelId() {
		return this.channelInfo.getId();
	}

	public ChannelInfo getChannelInfo() {
		return this.channelInfo;
	}

	public static void pingWorld() {
		try {
			instance.wci.ping();
		} catch (final RemoteException ex) {
			if (instance.isWorldReady.compareAndSet(true, false)) {
				instance.connectToWorld();
			}
		}
	}

	@Override
	protected void connectToWorld() {
		try {
			final WorldRegistry worldRegistry = Universe.getOrBindWorldRegistry();

			// TODO: implement the interface in this class.
			this.cwi = new ChannelWorldInterfaceImpl(this);
			this.wci = worldRegistry.registerChannelServer(this.channelInfo, this.cwi);
			this.wci.serverReady();
		} catch (NotBoundException | RemoteException ex) {
			Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		this.isWorldReady.compareAndSet(false, true);
	}

	@Override
	protected final void loadSettings() {
		final WorldInfo info = WorldConfig.load(this.channelInfo.getWorldId());
		this.expRate = (float) info.getExpRate() / 100;
		this.mesoRate = (float) info.getMesoRate() / 100;
		this.itemRate = (float) info.getItemRate() / 100;

		// TODO: do this one in the DB too.
		this.serverMessage = "";
	}

	public void initialize() {
		this.connectToWorld();
		this.loadSettings();

		final List<String> events = this.loadEventsFromDb();
		this.eventManager = new EventScriptManager(events);

		TimerManager.getInstance().start();
		TimerManager.getInstance().register(AutobanManager.getInstance(), 60000);
		MapTimer.getInstance().start();

		ItemMakerFactory.getInstance();
		ItemInfoProvider.getInstance();
		RandomRewards.getInstance();
		SkillInfoProvider.getSkill(99999999);
		this.players = new PlayerStorage();

		final PacketHandler serverHandler = new ChannelPacketHandler(this.channelId);
		super.bind(serverHandler);
		try {
			this.wci.serverReady();
		} catch (final RemoteException ex) {
			Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.printf(":: Channel %d : Listening on port %d ::", this.getChannelId(), super.endpointInfo.getPort());
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownListener()));
		this.eventManager.init();
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
		return this.serverMessage;
	}

	public void setServerMessage(final String newMessage) {
		this.serverMessage = newMessage;
		this.broadcastPacket(ChannelPackets.headerMessage(this.serverMessage));
	}

	public void broadcastPacket(final GamePacket data) {
		this.players.broadcastPacket(data);
	}

	public void broadcastSmegaPacket(final GamePacket data) {
		this.players.broadcastSmegaPacket(data);
	}

	public void broadcastGMPacket(final GamePacket data) {
		this.players.broadcastGMPacket(data);
	}

	public String getIP(final int channel) {
		try {
			return getWorldInterface().getIP(channel);
		} catch (final RemoteException e) {
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
		} catch (final RemoteException e) {
			pingWorld();
		}
	}

	public final void shutdownLogin() {
		try {
			getWorldInterface().shutdownLogin();
		} catch (final RemoteException e) {
			pingWorld();
		}
	}

	public final int getLoadedMaps() {
		return getMapFactory().getLoadedMaps();
	}

	public final EventScriptManager getEventSM() {
		return this.eventManager;
	}

	public final void reloadEvents() {
		final List<String> events = this.loadEventsFromDb();
		this.eventManager.cancel();
		this.eventManager = new EventScriptManager(events);
		this.eventManager.init();
	}

	private List<String> loadEventsFromDb() {
		// TODO: load events from DB.
		final List<String> events = Lists.newArrayList();
		return events;
	}

	public final float getExpRate() {
		return this.expRate;
	}

	public final void setExpRate(final float expRate) {
		this.expRate = expRate;
	}

	public final float getMesoRate() {
		return this.mesoRate;
	}

	public final void setMesoRate(final float mesoRate) {
		this.mesoRate = mesoRate;
	}

	public final float getItemRate() {
		return this.itemRate;
	}

	public final void setItemRate(final float dropRate) {
		this.itemRate = dropRate;
	}

	public final Guild getGuild(final int guildId) {
		Guild guild = null;
		try {
			guild = getWorldInterface().getGuild(guildId);
		} catch (final RemoteException re) {
			System.err.println("RemoteException while fetching MapleGuild." + re);
			return null;
		}
		if (this.gsStore.get(guildId) == null) {
			this.gsStore.put(guildId, new GuildSummary(guild));
		}
		return guild;
	}

	public final GuildSummary getGuildSummary(final int guildId) {
		if (this.gsStore.containsKey(guildId)) {
			return this.gsStore.get(guildId);
		}
		try {
			final Guild guild = ChannelServer.getWorldInterface().getGuild(guildId);
			if (guild != null) {
				this.gsStore.put(guildId, new GuildSummary(guild));
			}
			return this.gsStore.get(guildId);
		} catch (final RemoteException re) {
			System.err.println("RemoteException while fetching GuildSummary." + re);
			return null;
		}
	}

	public final void updateGuildSummary(final int guildId, final GuildSummary summary) {
		this.gsStore.put(guildId, summary);
	}

	public final Squad getMapleSquad(final String type) {
		return this.mapleSquads.get(type);
	}

	public final boolean addMapleSquad(final Squad squad, final String type) {
		if (this.mapleSquads.get(type) == null) {
			this.mapleSquads.remove(type);
			this.mapleSquads.put(type, squad);
			return true;
		}
		return false;
	}

	public final boolean removeMapleSquad(final String type) {
		if (this.mapleSquads.containsKey(type)) {
			this.mapleSquads.remove(type);
			return true;
		}
		return false;
	}

	public final void closeAllMerchant() {
		this.merchantMutex.lock();
		try {
			final Iterator<HiredMerchantStore> iterator = this.merchants.values().iterator();
			while (iterator.hasNext()) {
				final HiredMerchantStore merchant = iterator.next();
				merchant.closeShop(true, false);
				iterator.remove();
			}
		} finally {
			this.merchantMutex.unlock();
		}
	}

	public final int addMerchant(final HiredMerchantStore merchant) {
		this.merchantMutex.lock();
		int id = 0;
		try {
			id = this.currentMerchantId;
			this.merchants.put(id, merchant);
			this.currentMerchantId++;
		} finally {
			this.merchantMutex.unlock();
		}
		return id;
	}

	public final void removeMerchant(final HiredMerchantStore merchant) {
		this.merchantMutex.lock();
		try {
			this.merchants.remove(merchant.getStoreId());
		} finally {
			this.merchantMutex.unlock();
		}
	}

	public final boolean hasMerchant(final int accountId) {
		boolean contains = false;
		this.merchantMutex.lock();
		try {
			final Iterator<HiredMerchantStore> iterator = this.merchants.values().iterator();
			while (iterator.hasNext()) {
				final HiredMerchantStore merchant = iterator.next();
				if (merchant.getOwnerAccountId() == accountId) {
					contains = true;
					break;
				}
			}
		} finally {
			this.merchantMutex.unlock();
		}
		return contains;
	}

	public final void toggleMegaponeMuteState() {
		this.MegaphoneMuteState = !this.MegaphoneMuteState;
	}

	public final boolean getMegaphoneMuteState() {
		return this.MegaphoneMuteState;
	}

	public final List<ChannelCharacter> getPartyMembers(final int partyId) {
		final List<ChannelCharacter> partym = Lists.newLinkedList();
		try {
			final Party party = ChannelServer.getWorldInterface().getParty(partyId);
			for (final PartyMember partychar : party.getMembers()) {
				if (partychar.getChannel() == this.getChannelId()) {
					// Make sure the thing doesn't get duplicate plays due to
					// ccing bug.
					final ChannelCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
					if (chr != null) {
						partym.add(chr);
					}
				}
			}
		} catch (final RemoteException ex) {
			ex.printStackTrace();
		}
		return partym;
	}

	private class ShutdownListener implements Runnable {

		@Override
		public void run() {
			ChannelServer.this.shutdown();
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