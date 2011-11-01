package javastory.channel.rmi;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.channel.CharacterTransfer;
import javastory.channel.GuildSummary;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.client.BuddyAddResult;
import javastory.channel.client.BuddyList;
import javastory.channel.client.BuddyListEntry;
import javastory.channel.client.BuddyOperation;
import javastory.channel.client.MemberRank;
import javastory.config.ChannelInfo;
import javastory.db.Database;
import javastory.io.GamePacket;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.GenericRemoteObject;
import javastory.server.channel.PlayerStorage;
import javastory.tools.CollectionUtil;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.CheaterData;
import javastory.world.core.PartyOperation;

public class ChannelWorldInterfaceImpl extends GenericRemoteObject implements ChannelWorldInterface {

	private static final long serialVersionUID = 7815256899088644192L;
	private final ChannelServer server;

	public ChannelWorldInterfaceImpl(final ChannelServer server) throws RemoteException {
		super();
		this.server = server;
	}

	@Override
	public int getChannelId() throws RemoteException {
		return this.server.getChannelId();
	}

	@Override
	public ChannelInfo getChannelInfo() throws RemoteException {
		return this.server.getChannelInfo();
	}

	@Override
	public String getIP() throws RemoteException {
		return this.server.getIP();
	}

	@Override
	public void broadcastMessage(final GamePacket packet) throws RemoteException {
		this.server.broadcastPacket(packet);
	}

	@Override
	public void broadcastSmega(final GamePacket packet) throws RemoteException {
		this.server.broadcastSmegaPacket(packet);
	}

	@Override
	public void broadcastGMMessage(final GamePacket packet) throws RemoteException {
		this.server.broadcastGMPacket(packet);
	}

	@Override
	public void whisper(final String sender, final String target, final int channel, final String message) throws RemoteException {
		if (this.isConnected(target)) {
			ChannelServer.getPlayerStorage().getCharacterByName(target).getClient().write(ChannelPackets.getWhisper(sender, channel, message));
		}
	}

	@Override
	public boolean isConnected(final String charName) throws RemoteException {
		return ChannelServer.getPlayerStorage().getCharacterByName(charName) != null;
	}

	@Override
	public boolean isCharacterListConnected(final List<String> charName) throws RemoteException {
		for (final String c : charName) {
			if (ChannelServer.getPlayerStorage().getCharacterByName(c) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void toggleMegaphoneMuteState() throws RemoteException {
		this.server.toggleMegaponeMuteState();
	}

	@Override
	public boolean hasMerchant(final int accountId) throws RemoteException {
		return this.server.hasMerchant(accountId);
	}

	@Override
	public void shutdown(final int time) throws RemoteException {
		this.server.shutdown(time);
	}

	@Override
	public int getConnected() throws RemoteException {
		return ChannelServer.getPlayerStorage().getConnectedClients();
	}

	@Override
	public void loggedOff(final String name, final int characterId, final int channel, final int[] buddies) throws RemoteException {
		this.updateBuddies(characterId, channel, buddies, true);
	}

	@Override
	public void loggedOn(final String name, final int characterId, final int channel, final int buddies[]) throws RemoteException {
		this.updateBuddies(characterId, channel, buddies, false);
	}

	private void updateBuddies(final int characterId, final int channel, final int[] buddies, final boolean offline) {
		final PlayerStorage playerStorage = ChannelServer.getPlayerStorage();
		for (final int buddy : buddies) {
			final ChannelCharacter chr = playerStorage.getCharacterById(buddy);
			if (chr != null) {
				final BuddyListEntry ble = chr.getBuddyList().get(characterId);
				if (ble != null && ble.isVisible()) {
					int mcChannel;
					if (offline) {
						ble.setChannel(-1);
						mcChannel = -1;
					} else {
						ble.setChannel(channel);
						mcChannel = channel - 1;
					}
					chr.getBuddyList().put(ble);
					chr.getClient().write(ChannelPackets.updateBuddyChannel(ble.getCharacterId(), mcChannel));
				}
			}
		}
	}

	@Override
	public void updateParty(final Party party, final PartyOperation operation, final PartyMember target) throws RemoteException {
		for (final PartyMember partychar : party.getMembers()) {
			if (partychar.getChannel() == this.server.getChannelId()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
				if (chr != null) {
					if (operation == PartyOperation.DISBAND) {
						chr.setParty(null);
					} else {
						chr.setParty(party);
					}
					chr.getClient().write(ChannelPackets.updateParty(chr.getClient().getChannelId(), party, operation, target));
				}
			}
		}
		switch (operation) {
		case LEAVE:
		case EXPEL:
			if (target.getChannel() == this.server.getChannelId()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(target.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.updateParty(chr.getClient().getChannelId(), party, operation, target));
					chr.setParty(null);
				}
			}
		}
	}

	@Override
	public void partyChat(final Party party, final String chattext, final String namefrom) throws RemoteException {
		for (final PartyMember partychar : party.getMembers()) {
			if (partychar.getChannel() == this.server.getChannelId() && !partychar.getName().equals(namefrom)) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.multiChat(namefrom, chattext, 1));
				}
			}
		}
	}

	@Override
	public int getLocation(final String name) throws RemoteException {
		final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(name);
		if (chr != null) {
			return chr.getMapId();
		}
		return -1;
	}

	@Override
	public List<CheaterData> getCheaters() throws RemoteException {
		final List<CheaterData> cheaters = ChannelServer.getPlayerStorage().getCheaters();

		Collections.sort(cheaters);
		return CollectionUtil.copyFirst(cheaters, 20);
	}

	@Override
	public BuddyAddResult requestBuddyAdd(final String addName, final int channelFrom, final int cidFrom, final String nameFrom, final int levelFrom, final int jobFrom) {
		final ChannelCharacter addChar = ChannelServer.getPlayerStorage().getCharacterByName(addName);
		if (addChar != null) {
			final BuddyList buddylist = addChar.getBuddyList();
			if (buddylist.isFull()) {
				return BuddyAddResult.BUDDYLIST_FULL;
			}
			if (!buddylist.contains(cidFrom)) {
				buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom, levelFrom, jobFrom);
			} else {
				if (buddylist.containsVisible(cidFrom)) {
					return BuddyAddResult.ALREADY_ON_LIST;
				}
			}
		}
		return BuddyAddResult.OK;
	}

	@Override
	public boolean isConnected(final int characterId) throws RemoteException {
		return ChannelServer.getPlayerStorage().getCharacterById(characterId) != null;
	}

	@Override
	public void buddyChanged(final int cid, final int cidFrom, final String name, final int channel, final BuddyOperation operation, final int level, final int job) {
		final ChannelCharacter addChar = ChannelServer.getPlayerStorage().getCharacterById(cid);
		if (addChar != null) {
			final BuddyList buddylist = addChar.getBuddyList();
			switch (operation) {
			case ADDED:
				if (buddylist.contains(cidFrom)) {
					buddylist.put(new BuddyListEntry(name, cidFrom, "ETC", channel, true, level, job));
					addChar.getClient().write(ChannelPackets.updateBuddyChannel(cidFrom, channel - 1));
				}
				break;
			case DELETED:
				if (buddylist.contains(cidFrom)) {
					buddylist.put(new BuddyListEntry(name, cidFrom, "ETC", -1, buddylist.get(cidFrom).isVisible(), level, job));
					addChar.getClient().write(ChannelPackets.updateBuddyChannel(cidFrom, -1));
				}
				break;
			}
		}
	}

	@Override
	public void buddyChat(final int[] recipientCharacterIds, final int cidFrom, final String nameFrom, final String chattext) throws RemoteException {
		final PlayerStorage playerStorage = ChannelServer.getPlayerStorage();
		for (final int characterId : recipientCharacterIds) {
			final ChannelCharacter chr = playerStorage.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddyList().containsVisible(cidFrom)) {
					chr.getClient().write(ChannelPackets.multiChat(nameFrom, chattext, 0));
				}
			}
		}
	}

	@Override
	public int[] multiBuddyFind(final int charIdFrom, final int[] characterIds) throws RemoteException {
		final List<Integer> ret = Lists.newArrayListWithCapacity(characterIds.length);
		final PlayerStorage playerStorage = ChannelServer.getPlayerStorage();
		for (final int characterId : characterIds) {
			final ChannelCharacter chr = playerStorage.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddyList().containsVisible(charIdFrom)) {
					ret.add(characterId);
				}
			}
		}
		final int[] retArr = new int[ret.size()];
		int pos = 0;
		for (final Integer i : ret) {
			retArr[pos++] = i.intValue();
		}
		return retArr;
	}

	@Override
	public void ChannelChange_Data(final CharacterTransfer transfer, final int characterid) throws RemoteException {
		ChannelServer.getPlayerStorage().registerTransfer(transfer, characterid);
	}

	@Override
	public void sendPacket(final Collection<Integer> targetIds, final GamePacket packet, final int exception) throws RemoteException {
		ChannelCharacter c;
		for (final int i : targetIds) {
			if (i == exception) {
				continue;
			}
			c = ChannelServer.getPlayerStorage().getCharacterById(i);
			if (c != null) {
				c.getClient().write(packet);
			}
		}
	}

	@Override
	public void setGuildAndRank(final Collection<Integer> cids, final int guildid, final MemberRank rank, final int exception) throws RemoteException {
		for (final int cid : cids) {
			if (cid != exception) {
				this.setGuildAndRank(cid, guildid, rank);
			}
		}
	}

	@Override
	public void setGuildAndRank(final int cid, final int guildid, final MemberRank rank) throws RemoteException {
		final ChannelCharacter mc = ChannelServer.getPlayerStorage().getCharacterById(cid);
		if (mc == null) {
			// System.out.println("ERROR: cannot find player in given channel");
			return;
		}

		boolean bDifferentGuild;
		if (guildid == -1 && rank == null) { //just need a respawn
			bDifferentGuild = true;
		} else {
			bDifferentGuild = guildid != mc.getGuildId();
			mc.setGuildId(guildid);
			mc.setGuildRank(rank);
			mc.saveGuildStatus();
		}
		if (bDifferentGuild) {
			mc.getMap().broadcastMessage(mc, ChannelPackets.removePlayerFromMap(cid), false);
			mc.getMap().broadcastMessage(mc, ChannelPackets.spawnPlayerMapObject(mc), false);
		}
	}

	@Override
	public void setOfflineGuildStatus(final int guildId, final MemberRank rank, final int cid) throws RemoteException {
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
			ps.setInt(1, guildId);
			ps.setInt(2, rank.asNumber());
			ps.setInt(3, cid);
			ps.execute();
		} catch (final SQLException se) {
			System.out.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	@Override
	public void changeEmblem(final int gid, final Collection<Integer> affectedPlayers, final GuildSummary mgs) throws RemoteException {
		ChannelServer.getInstance().updateGuildSummary(gid, mgs);
		this.sendPacket(affectedPlayers, ChannelPackets.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
		this.setGuildAndRank(affectedPlayers, -1, null, -1);	//respawn player
	}

	@Override
	public void messengerInvite(final String sender, final int messengerid, final String target, final int fromchannel) throws RemoteException {
		// TODO: No op. Finish when ChannelServer remoting is done.
	}

	@Override
	public void addMessengerPlayer(final Messenger messenger, final String namefrom, final int fromchannel, final int position) throws RemoteException {
		// TODO: No op. Finish when ChannelServer remoting is done.
	}

	@Override
	public void removeMessengerPlayer(final Messenger messenger, final int position) throws RemoteException {
		for (final MessengerMember messengerchar : messenger.getMembers()) {
			if (messengerchar.getChannel() == this.server.getChannelId()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.removeMessengerPlayer(position));
				}
			}
		}
	}

	@Override
	public void messengerChat(final Messenger messenger, final String chattext, final String namefrom) throws RemoteException {
		for (final MessengerMember messengerchar : messenger.getMembers()) {
			if (messengerchar.getChannel() == this.server.getChannelId() && !messengerchar.getName().equals(namefrom)) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.messengerChat(chattext));
				}
			}
		}
	}

	@Override
	public void declineChat(final String target, final String namefrom) throws RemoteException {
		if (this.isConnected(target)) {
			final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(target);
			final Messenger messenger = chr.getMessenger();
			if (messenger != null) {
				chr.getClient().write(ChannelPackets.messengerNote(namefrom, 5, 0));
			}
		}
	}

	@Override
	public void updateMessenger(final Messenger messenger, final String namefrom, final int position, final int fromchannel) throws RemoteException {
		// TODO: No op. Finish when ChannelServer remoting is done.
	}
}
