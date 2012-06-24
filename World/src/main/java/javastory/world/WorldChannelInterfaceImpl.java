package javastory.world;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javastory.channel.CharacterTransfer;
import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.PlayerBuffValueHolder;
import javastory.channel.client.MemberRank;
import javastory.io.GamePacket;
import javastory.registry.WorldRegistry;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.GenericRemoteObject;
import javastory.rmi.LoginWorldInterface;
import javastory.rmi.WorldChannelInterface;
import javastory.server.Location;
import javastory.tools.CollectionUtil;
import javastory.world.core.CharacterIdChannelPair;
import javastory.world.core.CheaterData;
import javastory.world.core.PartyOperation;
import javastory.world.core.PlayerCooldownValueHolder;
import javastory.world.core.PlayerDiseaseValueHolder;

public class WorldChannelInterfaceImpl extends GenericRemoteObject implements WorldChannelInterface {

	private static final long serialVersionUID = -5568606556235590482L;
	private final ChannelWorldInterface channel;
	private static final WorldRegistry registry = WorldRegistryImpl.getInstance();
	private boolean ready = false;

	public WorldChannelInterfaceImpl(final ChannelWorldInterface channelInterface, final int dbId) throws RemoteException {
		super();
		this.channel = channelInterface;
	}

	@Override
	public void serverReady() throws RemoteException {
		this.ready = true;
		for (final LoginWorldInterface wli : registry.getLoginServer()) {
			try {
				wli.channelOnline(this.channel.getChannelInfo());
			} catch (final RemoteException e) {
				registry.deregisterLoginServer(wli);
			}
		}
		System.out.println(":: Channel " + this.channel.getChannelId() + " is online ::");
	}

	public boolean isReady() {
		return this.ready;
	}

	@Override
	public String getIP(final int channel) throws RemoteException {
		final ChannelWorldInterface cwi = registry.getChannel(channel);
		if (cwi == null) {
			return "0.0.0.0:0";
		} else {
			try {
				return cwi.getIP();
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(channel);
				return "0.0.0.0:0";
			}
		}
	}

	@Override
	public void whisper(final String sender, final String target, final int channel, final String message) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.whisper(sender, target, channel, message);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public boolean isConnected(final String charName) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				if (cwi.isConnected(charName)) {
					return true;
				}
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		return false;
	}

	@Override
	public boolean isCharacterListConnected(final List<String> charName) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				if (cwi.isCharacterListConnected(charName)) {
					return true;
				}
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		return false;
	}

	@Override
	public void broadcastMessage(final GamePacket packet) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.broadcastMessage(packet);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void broadcastSmega(final GamePacket packet) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.broadcastSmega(packet);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void broadcastGMMessage(final GamePacket packet) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.broadcastGMMessage(packet);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void toggleMegaphoneMuteState() throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.toggleMegaphoneMuteState();
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public boolean hasMerchant(final int accountId) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				return cwi.hasMerchant(accountId);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		return false;
	}

	@Override
	public int find(final String charName) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				if (cwi.isConnected(charName)) {
					return cwi.getChannelId();
				}
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		return -1;
	}

	// can we generify this
	@Override
	public int find(final int characterId) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				if (cwi.isConnected(characterId)) {
					return cwi.getChannelInfo().getId();
				}
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		return -1;
	}

	@Override
	public void shutdownLogin() throws RemoteException {
		for (final LoginWorldInterface lwi : registry.getLoginServer()) {
			try {
				lwi.shutdown();
			} catch (final RemoteException e) {
				registry.deregisterLoginServer(lwi);
			}
		}
	}

	@Override
	public void shutdown(final int time) throws RemoteException {
		for (final LoginWorldInterface lwi : registry.getLoginServer()) {
			try {
				lwi.shutdown();
			} catch (final RemoteException e) {
				registry.deregisterLoginServer(lwi);
			}
		}
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.shutdown(time);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public Map<Integer, Integer> getConnected() throws RemoteException {
		final Map<Integer, Integer> ret = Maps.newHashMap();
		int total = 0;
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				final int curConnected = cwi.getConnected();
				ret.put(i, curConnected);
				total += curConnected;
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		ret.put(0, total);
		return ret;
	}

	@Override
	public void loggedOn(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.loggedOn(name, characterId, channel, buddies);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void loggedOff(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.loggedOff(name, characterId, channel, buddies);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	//TODO only notify channels where partymembers are?
	@Override
	public void updateParty(final int partyid, final PartyOperation operation, final PartyMember target) throws RemoteException {
		final Party party = registry.getParty(partyid);
		if (party == null) {
			throw new IllegalArgumentException("no party with the specified partyid exists");
		}
		switch (operation) {
		case JOIN:
			party.addMember(target);
			break;
		case EXPEL:
		case LEAVE:
			party.removeMember(target);
			break;
		case DISBAND:
			registry.disbandParty(partyid);
			break;
		case SILENT_UPDATE:
		case LOG_ONOFF:
			party.updateMember(target);
			break;
		case CHANGE_LEADER:
			party.setLeader(target);
			break;
		default:
			throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
		}
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.updateParty(party, operation, target);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public Party createParty() throws RemoteException {
		return registry.createParty();
	}

	@Override
	public Party getParty(final int partyid) throws RemoteException {
		return registry.getParty(partyid);
	}

	@Override
	public void partyChat(final int partyid, final String chattext, final String namefrom) throws RemoteException {
		final Party party = registry.getParty(partyid);
		if (party == null) {
			throw new IllegalArgumentException("no party with the specified partyid exists");
		}
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.partyChat(party, chattext, namefrom);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	public boolean isAvailable() throws RemoteException {
		return true;
	}

	@Override
	public Location getLocation(final String charName) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				if (cwi.isConnected(charName)) {
					return new Location(cwi.getLocation(charName), (byte) cwi.getChannelId());
				}
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		return null;
	}

	@Override
	public List<CheaterData> getCheaters() throws RemoteException {
		final List<CheaterData> allCheaters = Lists.newArrayList();
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				allCheaters.addAll(cwi.getCheaters());
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
		Collections.sort(allCheaters);
		return CollectionUtil.copyFirst(allCheaters, 20);
	}

	@Override
	public ChannelWorldInterface getChannelInterface(final int channel) throws RemoteException {
		return registry.getChannel(channel);
	}

	@Override
	public void buddyChat(final int[] recipientCharacterIds, final int cidFrom, final String nameFrom, final String chattext) throws RemoteException {
		for (final ChannelWorldInterface cwi : registry.getAllChannelServers()) {
			cwi.buddyChat(recipientCharacterIds, cidFrom, nameFrom, chattext);
		}
	}

	@Override
	public CharacterIdChannelPair[] multiBuddyFind(final int charIdFrom, final int[] characterIds) throws RemoteException {
		final List<CharacterIdChannelPair> foundsChars = Lists.newArrayListWithCapacity(characterIds.length);
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			for (final int charid : cwi.multiBuddyFind(charIdFrom, characterIds)) {
				foundsChars.add(new CharacterIdChannelPair(charid, i));
			}
		}
		return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
	}

	@Override
	public void transfer(final CharacterTransfer Data, final int characterid, final int toChannel) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			if (i == toChannel) {
				final ChannelWorldInterface cwi = registry.getChannel(i);
				try {
					cwi.ChannelChange_Data(Data, characterid);
				} catch (final RemoteException e) {
					registry.deregisterChannelServer(i);
				}
			}
		}

	}

	@Override
	public Guild getGuild(final int id) throws RemoteException {
		return registry.getGuild(id);
	}

	@Override
	public void setGuildMemberOnline(final GuildMember mgc, final boolean bOnline, final int channel) throws RemoteException {
		registry.setGuildMemberOnline(mgc, bOnline, channel);
	}

	@Override
	public boolean addGuildMember(final GuildMember mgc) throws RemoteException {
		return registry.addGuildMember(mgc);
	}

	@Override
	public void guildChat(final int gid, final String name, final int cid, final String msg) throws RemoteException {
		registry.guildChat(gid, name, cid, msg);
	}

	@Override
	public void leaveGuild(final GuildMember mgc) throws RemoteException {
		registry.leaveGuild(mgc);
	}

	@Override
	public void changeRank(final int gid, final int cid, final MemberRank newRank) throws RemoteException {
		registry.changeRank(gid, cid, newRank);
	}

	@Override
	public void expelMember(final GuildMember initiator, final int cid) throws RemoteException {
		registry.expelMember(initiator, cid);
	}

	@Override
	public void setGuildNotice(final int gid, final String notice) throws RemoteException {
		registry.setGuildNotice(gid, notice);
	}

	@Override
	public void updateGuildMemberJob(final int guildId, final int characterId, final int jobId) throws RemoteException {
		registry.updateGuildMemberJob(guildId, characterId, jobId);
	}

	@Override
	public void updateGuildMemberLevel(final int guildId, final int characterId, final int level) throws RemoteException {
		registry.updateGuildMemberLevel(guildId, characterId, level);
	}

	@Override
	public void changeRankTitle(final int gid, final String[] ranks) throws RemoteException {
		registry.changeRankTitle(gid, ranks);
	}

	@Override
	public int createGuild(final int leaderId, final String name) throws RemoteException {
		return registry.createGuild(leaderId, name);
	}

	@Override
	public void setGuildEmblem(final int gid, final short bg, final byte bgcolor, final short logo, final byte logocolor) throws RemoteException {
		registry.setGuildEmblem(gid, bg, bgcolor, logo, logocolor);
	}

	@Override
	public void disbandGuild(final int gid) throws RemoteException {
		registry.disbandGuild(gid);
	}

	@Override
	public boolean increaseGuildCapacity(final int gid) throws RemoteException {
		return registry.increaseGuildCapacity(gid);
	}

	@Override
	public void gainGuildPoints(final int gid, final int amount) throws RemoteException {
		registry.gainGP(gid, amount);
	}

	@Override
	public Messenger createMessenger(final MessengerMember chrfor) throws RemoteException {
		return registry.createMessenger(chrfor);
	}

	@Override
	public Messenger getMessenger(final int messengerid) throws RemoteException {
		return registry.getMessenger(messengerid);
	}

	@Override
	public void messengerInvite(final String sender, final int messengerid, final String target, final int fromchannel) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.messengerInvite(sender, messengerid, target, fromchannel);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void leaveMessenger(final int messengerid, final MessengerMember target) throws RemoteException {
		final Messenger messenger = registry.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		final int position = messenger.getPositionByName(target.getName());
		messenger.removeMember(target);
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.removeMessengerPlayer(messenger, position);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void joinMessenger(final int messengerid, final MessengerMember target, final String from, final int fromchannel) throws RemoteException {
		final Messenger messenger = registry.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.addMember(target);
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void messengerChat(final int messengerid, final String namefrom, final String chattext) throws RemoteException {
		final Messenger messenger = registry.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.messengerChat(messenger, chattext, namefrom);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void declineChat(final String target, final String namefrom) throws RemoteException {
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.declineChat(target, namefrom);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void updateMessenger(final int messengerid, final String namefrom, final int fromchannel) throws RemoteException {
		final Messenger messenger = registry.getMessenger(messengerid);
		final int position = messenger.getPositionByName(namefrom);
		for (final int i : registry.getActiveChannels()) {
			final ChannelWorldInterface cwi = registry.getChannel(i);
			try {
				cwi.updateMessenger(messenger, namefrom, position, fromchannel);
			} catch (final RemoteException e) {
				registry.deregisterChannelServer(i);
			}
		}
	}

	@Override
	public void silentLeaveMessenger(final int messengerid, final MessengerMember target) throws RemoteException {
		final Messenger messenger = registry.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.silentRemoveMember(target);
	}

	@Override
	public void silentJoinMessenger(final int messengerid, final MessengerMember target, final int position) throws RemoteException {
		final Messenger messenger = registry.getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.silentAddMember(target, position);
	}

	@Override
	public void addBuffsToStorage(final int chrid, final List<PlayerBuffValueHolder> toStore) throws RemoteException {
		registry.getPlayerBuffStorage().addBuffsToStorage(chrid, toStore);
	}

	@Override
	public Collection<PlayerBuffValueHolder> getBuffsFromStorage(final int chrid) throws RemoteException {
		return registry.getPlayerBuffStorage().getBuffsFromStorage(chrid);
	}

	@Override
	public void addCooldownsToStorage(final int chrid, final List<PlayerCooldownValueHolder> toStore) throws RemoteException {
		registry.getPlayerBuffStorage().addCooldownsToStorage(chrid, toStore);
	}

	@Override
	public Collection<PlayerCooldownValueHolder> getCooldownsFromStorage(final int chrid) throws RemoteException {
		return registry.getPlayerBuffStorage().getCooldownsFromStorage(chrid);
	}

	@Override
	public void addDiseaseToStorage(final int chrid, final List<PlayerDiseaseValueHolder> toStore) throws RemoteException {
		registry.getPlayerBuffStorage().addDiseaseToStorage(chrid, toStore);
	}

	@Override
	public Collection<PlayerDiseaseValueHolder> getDiseaseFromStorage(final int chrid) throws RemoteException {
		return registry.getPlayerBuffStorage().getDiseaseFromStorage(chrid);
	}
}