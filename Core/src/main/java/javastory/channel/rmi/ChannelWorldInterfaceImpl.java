package javastory.channel.rmi;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import javastory.io.ByteArrayGamePacket;
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
	private ChannelServer server;

	public ChannelWorldInterfaceImpl(ChannelServer server) throws RemoteException {
		super();
		this.server = server;
	}

	@Override
	public int getChannelId() throws RemoteException {
		return server.getChannelId();
	}

	@Override
	public ChannelInfo getChannelInfo() throws RemoteException {
		return server.getChannelInfo();
	}

	@Override
	public String getIP() throws RemoteException {
		return server.getIP();
	}

	@Override
	public void broadcastMessage(byte[] message) throws RemoteException {
		server.broadcastPacket(new ByteArrayGamePacket(message));
	}

	@Override
	public void broadcastSmega(byte[] message) throws RemoteException {
		server.broadcastSmegaPacket(new ByteArrayGamePacket(message));
	}

	@Override
	public void broadcastGMMessage(byte[] message) throws RemoteException {
		server.broadcastGMPacket(new ByteArrayGamePacket(message));
	}

	@Override
	public void whisper(String sender, String target, int channel, String message) throws RemoteException {
		if (isConnected(target)) {
			ChannelServer.getPlayerStorage().getCharacterByName(target).getClient().write(ChannelPackets.getWhisper(sender, channel, message));
		}
	}

	@Override
	public boolean isConnected(String charName) throws RemoteException {
		return ChannelServer.getPlayerStorage().getCharacterByName(charName) != null;
	}

	@Override
	public boolean isCharacterListConnected(List<String> charName) throws RemoteException {
		for (final String c : charName) {
			if (ChannelServer.getPlayerStorage().getCharacterByName(c) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void toggleMegaphoneMuteState() throws RemoteException {
		server.toggleMegaponeMuteState();
	}

	@Override
	public boolean hasMerchant(int accountId) throws RemoteException {
		return server.hasMerchant(accountId);
	}

	@Override
	public void shutdown(int time) throws RemoteException {
		server.shutdown(time);
	}

	@Override
	public int getConnected() throws RemoteException {
		return ChannelServer.getPlayerStorage().getConnectedClients();
	}

	@Override
	public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
		updateBuddies(characterId, channel, buddies, true);
	}

	@Override
	public void loggedOn(String name, int characterId, int channel, int buddies[]) throws RemoteException {
		updateBuddies(characterId, channel, buddies, false);
	}

	private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
		final PlayerStorage playerStorage = ChannelServer.getPlayerStorage();
		for (int buddy : buddies) {
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
	public void updateParty(Party party, PartyOperation operation, PartyMember target) throws RemoteException {
		for (PartyMember partychar : party.getMembers()) {
			if (partychar.getChannel() == server.getChannelId()) {
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
			if (target.getChannel() == server.getChannelId()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(target.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.updateParty(chr.getClient().getChannelId(), party, operation, target));
					chr.setParty(null);
				}
			}
		}
	}

	@Override
	public void partyChat(Party party, String chattext, String namefrom) throws RemoteException {
		for (PartyMember partychar : party.getMembers()) {
			if (partychar.getChannel() == server.getChannelId() && !(partychar.getName().equals(namefrom))) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.multiChat(namefrom, chattext, 1));
				}
			}
		}
	}

	@Override
	public int getLocation(String name) throws RemoteException {
		ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(name);
		if (chr != null) {
			return chr.getMapId();
		}
		return -1;
	}

	@Override
	public List<CheaterData> getCheaters() throws RemoteException {
		List<CheaterData> cheaters = ChannelServer.getPlayerStorage().getCheaters();

		Collections.sort(cheaters);
		return CollectionUtil.copyFirst(cheaters, 20);
	}

	@Override
	public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
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
	public boolean isConnected(int characterId) throws RemoteException {
		return ChannelServer.getPlayerStorage().getCharacterById(characterId) != null;
	}

	@Override
	public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation, int level, int job) {
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
	public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
		final PlayerStorage playerStorage = ChannelServer.getPlayerStorage();
		for (int characterId : recipientCharacterIds) {
			final ChannelCharacter chr = playerStorage.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddyList().containsVisible(cidFrom)) {
					chr.getClient().write(ChannelPackets.multiChat(nameFrom, chattext, 0));
				}
			}
		}
	}

	@Override
	public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
		List<Integer> ret = new ArrayList<>(characterIds.length);
		final PlayerStorage playerStorage = ChannelServer.getPlayerStorage();
		for (int characterId : characterIds) {
			ChannelCharacter chr = playerStorage.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddyList().containsVisible(charIdFrom)) {
					ret.add(characterId);
				}
			}
		}
		int[] retArr = new int[ret.size()];
		int pos = 0;
		for (Integer i : ret) {
			retArr[pos++] = i.intValue();
		}
		return retArr;
	}

	@Override
	public void ChannelChange_Data(CharacterTransfer transfer, int characterid) throws RemoteException {
		ChannelServer.getPlayerStorage().registerTransfer(transfer, characterid);
	}

	@Override
	public void sendPacket(Collection<Integer> targetIds, GamePacket packet, int exception) throws RemoteException {
		ChannelCharacter c;
		for (int i : targetIds) {
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
	public void setGuildAndRank(Collection<Integer> cids, int guildid, MemberRank rank, int exception) throws RemoteException {
		for (int cid : cids) {
			if (cid != exception) {
				setGuildAndRank(cid, guildid, rank);
			}
		}
	}

	@Override
	public void setGuildAndRank(int cid, int guildid, MemberRank rank) throws RemoteException {
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
	public void setOfflineGuildStatus(int guildId, MemberRank rank, int cid) throws RemoteException {
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
			ps.setInt(1, guildId);
			ps.setInt(2, rank.asNumber());
			ps.setInt(3, cid);
			ps.execute();
		} catch (SQLException se) {
			System.out.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	@Override
	public void changeEmblem(int gid, Collection<Integer> affectedPlayers, GuildSummary mgs) throws RemoteException {
		ChannelServer.getInstance().updateGuildSummary(gid, mgs);
		this.sendPacket(affectedPlayers, ChannelPackets.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
		this.setGuildAndRank(affectedPlayers, -1, null, -1);	//respawn player
	}

	@Override
	public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
		// TODO: No op. Finish when ChannelServer remoting is done.
	}

	@Override
	public void addMessengerPlayer(Messenger messenger, String namefrom, int fromchannel, int position) throws RemoteException {
		// TODO: No op. Finish when ChannelServer remoting is done.
	}

	@Override
	public void removeMessengerPlayer(Messenger messenger, int position) throws RemoteException {
		for (MessengerMember messengerchar : messenger.getMembers()) {
			if (messengerchar.getChannel() == server.getChannelId()) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.removeMessengerPlayer(position));
				}
			}
		}
	}

	@Override
	public void messengerChat(Messenger messenger, String chattext, String namefrom) throws RemoteException {
		for (MessengerMember messengerchar : messenger.getMembers()) {
			if (messengerchar.getChannel() == server.getChannelId() && !(messengerchar.getName().equals(namefrom))) {
				final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().write(ChannelPackets.messengerChat(chattext));
				}
			}
		}
	}

	@Override
	public void declineChat(String target, String namefrom) throws RemoteException {
		if (isConnected(target)) {
			final ChannelCharacter chr = ChannelServer.getPlayerStorage().getCharacterByName(target);
			final Messenger messenger = chr.getMessenger();
			if (messenger != null) {
				chr.getClient().write(ChannelPackets.messengerNote(namefrom, 5, 0));
			}
		}
	}

	@Override
	public void updateMessenger(Messenger messenger, String namefrom, int position, int fromchannel) throws RemoteException {
		// TODO: No op. Finish when ChannelServer remoting is done.
	}
}
