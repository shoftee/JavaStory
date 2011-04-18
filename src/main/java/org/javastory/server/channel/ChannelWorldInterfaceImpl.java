package org.javastory.server.channel;

import org.javastory.server.ChannelInfo;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import client.BuddyList;
import client.BuddyListEntry;
import client.GameCharacter;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import database.DatabaseConnection;
import handling.ByteArrayGamePacket;
import handling.GamePacket;
import handling.channel.remote.ChannelWorldInterface;
import handling.world.CharacterTransfer;
import handling.world.Messenger;
import handling.world.MessengerCharacter;
import handling.world.Party;
import handling.world.PartyCharacter;
import handling.world.PartyOperation;
import handling.world.guild.MapleGuildSummary;
import handling.world.remote.CheaterData;
import server.TimerManager;
import tools.CollectionUtil;
import tools.MaplePacketCreator;

public class ChannelWorldInterfaceImpl extends UnicastRemoteObject implements ChannelWorldInterface {

    private static final long serialVersionUID = 7815256899088644192L;
    private ChannelServer server;

    public ChannelWorldInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public ChannelWorldInterfaceImpl(ChannelServer server) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.server = server;
    }
    
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
            server.getPlayerStorage().getCharacterByName(target).getClient().write(MaplePacketCreator.getWhisper(sender, channel, message));
        }
    }

    @Override
    public boolean isConnected(String charName) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(charName) != null;
    }

    @Override
    public boolean isCharacterListConnected(List<String> charName) throws RemoteException {
        for (final String c : charName) {
            if (server.getPlayerStorage().getCharacterByName(c) != null) {
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
        TimerManager.getInstance().schedule(new ShutdownChannelServer(server.getChannelId()), time);
    }

    @Override
    public int getConnected() throws RemoteException {
        return server.getPlayerStorage().getConnectedClients();
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
        final PlayerStorage playerStorage = server.getPlayerStorage();
        for (int buddy : buddies) {
            final GameCharacter chr = playerStorage.getCharacterById(buddy);
            if (chr != null) {
                final BuddyListEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    int mcChannel;
                    if (offline) {
                        ble.setChannel(-1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = channel - 1;
                    }
                    chr.getBuddylist().put(ble);
                    chr.getClient().write(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    @Override
    public void updateParty(Party party, PartyOperation operation, PartyCharacter target) throws RemoteException {
        for (PartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannelId()) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    if (operation == PartyOperation.DISBAND) {
                        chr.setParty(null);
                    } else {
                        chr.setParty(party);
                    }
                    chr.getClient().write(MaplePacketCreator.updateParty(chr.getClient().getChannelId(), party, operation, target));
                }
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                if (target.getChannel() == server.getChannelId()) {
                    final GameCharacter chr = server.getPlayerStorage().getCharacterByName(target.getName());
                    if (chr != null) {
                        chr.getClient().write(MaplePacketCreator.updateParty(chr.getClient().getChannelId(), party, operation, target));
                        chr.setParty(null);
                    }
                }
        }
    }

    @Override
    public void partyChat(Party party, String chattext, String namefrom) throws RemoteException {
        for (PartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannelId() && !(partychar.getName().equals(namefrom))) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.getClient().write(MaplePacketCreator.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    @Override
    public boolean isAvailable() throws RemoteException {
        return true;
    }

    @Override
    public int getLocation(String name) throws RemoteException {
        GameCharacter chr = server.getPlayerStorage().getCharacterByName(name);
        if (chr != null) {
            return chr.getMapId();
        }
        return -1;
    }

    @Override
    public List<CheaterData> getCheaters() throws RemoteException {
        List<CheaterData> cheaters = server.getPlayerStorage().getCheaters();

        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 20);
    }

    @Override
    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
        final GameCharacter addChar = server.getPlayerStorage().getCharacterByName(addName);
        if (addChar != null) {
            final BuddyList buddylist = addChar.getBuddylist();
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
        return server.getPlayerStorage().getCharacterById(characterId) != null;
    }

    @Override
    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation, int level, int job) {
        final GameCharacter addChar = server.getPlayerStorage().getCharacterById(cid);
        if (addChar != null) {
            final BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddyListEntry(name, cidFrom, "ETC", channel, true, level, job));
                        addChar.getClient().write(MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1));
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddyListEntry(name, cidFrom, "ETC", -1, buddylist.get(cidFrom).isVisible(), level, job));
                        addChar.getClient().write(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                    }
                    break;
            }
        }
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        final PlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : recipientCharacterIds) {
            final GameCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.getClient().write(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    @Override
    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<Integer> ret = new ArrayList<Integer>(characterIds.length);
        final PlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : characterIds) {
            GameCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
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
        server.getPlayerStorage().registerPendingPlayer(transfer, characterid);
    }

    @Override
    public void sendPacket(List<Integer> targetIds, GamePacket packet, int exception) throws RemoteException {
        GameCharacter c;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            c = server.getPlayerStorage().getCharacterById(i);
            if (c != null) {
                c.getClient().write(packet);
            }
        }
    }

    @Override
    public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) throws RemoteException {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    @Override
    public void setGuildAndRank(int cid, int guildid, int rank) throws RemoteException {
        final GameCharacter mc = server.getPlayerStorage().getCharacterById(cid);
        if (mc == null) {
            // System.out.println("ERROR: cannot find player in given channel");
            return;
        }

        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) { //just need a respawn
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.setGuildId(guildid);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }
        if (bDifferentGuild) {
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(cid), false);
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
        }
    }

    @Override
    public void setOfflineGuildStatus(int guildid, byte guildrank, int cid) throws RemoteException {
        try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage() + se);
        }
    }

    @Override
    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) throws RemoteException {
        ChannelManager.getInstance(this.getChannelId()).updateGuildSummary(gid, mgs);
        this.sendPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        this.setGuildAndRank(affectedPlayers, -1, -1, -1);	//respawn player
    }

    @Override
    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        if (isConnected(target)) {
            final Messenger messenger = server.getPlayerStorage().getCharacterByName(target).getMessenger();
            if (messenger == null) {
                server.getPlayerStorage().getCharacterByName(target).getClient().write(MaplePacketCreator.messengerInvite(sender, messengerid));

                ChannelManager.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender).getClient().write(MaplePacketCreator.messengerNote(target, 4, 1));
            } else {
                ChannelManager.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender).getClient().write(MaplePacketCreator.messengerChat(sender + " : " + target + " is already using Maple Messenger"));
            }
        }
    }

    @Override
    public void addMessengerPlayer(Messenger messenger, String namefrom, int fromchannel, int position) throws RemoteException {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannelId() && !(messengerchar.getName().equals(namefrom))) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    final GameCharacter from = ChannelManager.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient().write(MaplePacketCreator.addMessengerPlayer(namefrom, from, position, fromchannel - 1));
                    from.getClient().write(MaplePacketCreator.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), messengerchar.getChannel() - 1));
                }
            } else if (messengerchar.getChannel() == server.getChannelId() && (messengerchar.getName().equals(namefrom))) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().write(MaplePacketCreator.joinMessenger(messengerchar.getPosition()));
                }
            }
        }
    }

    @Override
    public void removeMessengerPlayer(Messenger messenger, int position) throws RemoteException {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannelId()) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().write(MaplePacketCreator.removeMessengerPlayer(position));
                }
            }
        }
    }

    @Override
    public void messengerChat(Messenger messenger, String chattext, String namefrom) throws RemoteException {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannelId() && !(messengerchar.getName().equals(namefrom))) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().write(MaplePacketCreator.messengerChat(chattext));
                }
            }
        }
    }

    @Override
    public void declineChat(String target, String namefrom) throws RemoteException {
        if (isConnected(target)) {
            final GameCharacter chr = server.getPlayerStorage().getCharacterByName(target);
            final Messenger messenger = chr.getMessenger();
            if (messenger != null) {
                chr.getClient().write(MaplePacketCreator.messengerNote(namefrom, 5, 0));
            }
        }
    }

    @Override
    public void updateMessenger(Messenger messenger, String namefrom, int position, int fromchannel) throws RemoteException {
        for (MessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannelId() && !(messengerchar.getName().equals(namefrom))) {
                final GameCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    GameCharacter from = ChannelManager.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient().write(MaplePacketCreator.updateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                }
            }
        }
    }
}
