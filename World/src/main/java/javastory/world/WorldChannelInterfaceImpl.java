package javastory.world;

import javastory.world.core.PlayerDiseaseValueHolder;
import javastory.world.core.CharacterIdChannelPair;
import javastory.world.core.WorldRegistry;
import javastory.world.core.CheaterData;
import javastory.world.core.PlayerCooldownValueHolder;
import javastory.world.core.PartyOperation;
import javastory.server.Location;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.GenericRemoteObject;
import javastory.rmi.LoginWorldInterface;
import javastory.channel.CharacterTransfer;
import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.PlayerBuffValueHolder;
import javastory.rmi.WorldChannelInterface;
import java.util.Collection;
import javastory.client.MemberRank;
import tools.CollectionUtil;

public class WorldChannelInterfaceImpl extends GenericRemoteObject implements WorldChannelInterface {

    private static final long serialVersionUID = -5568606556235590482L;
    private ChannelWorldInterface channel;
    private static final WorldRegistry registry =
            WorldRegistryImpl.getInstance();
    private boolean ready = false;

    public WorldChannelInterfaceImpl(ChannelWorldInterface channelInterface, int dbId) throws RemoteException {
    	super();
        this.channel = channelInterface;
    }

    public void serverReady() throws RemoteException {
        ready = true;
        for (LoginWorldInterface wli : registry.getLoginServer()) {
            try {
                wli.channelOnline(channel.getChannelInfo());
            } catch (RemoteException e) {
                registry.deregisterLoginServer(wli);
            }
        }
        System.out.println(":: Channel " + channel.getChannelId() + " is online ::");
    }

    public boolean isReady() {
        return ready;
    }

    public String getIP(int channel) throws RemoteException {
        final ChannelWorldInterface cwi = registry.getChannel(channel);
        if (cwi == null) {
            return "0.0.0.0:0";
        } else {
            try {
                return cwi.getIP();
            } catch (RemoteException e) {
                registry.deregisterChannelServer(channel);
                return "0.0.0.0:0";
            }
        }
    }

    public void whisper(String sender, String target, int channel, String message) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.whisper(sender, target, channel, message);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public boolean isConnected(String charName) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                if (cwi.isConnected(charName)) {
                    return true;
                }
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        return false;
    }

    public boolean isCharacterListConnected(List<String> charName) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                if (cwi.isCharacterListConnected(charName)) {
                    return true;
                }
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        return false;
    }

    public void broadcastMessage(byte[] message) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.broadcastMessage(message);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void broadcastSmega(byte[] message) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.broadcastSmega(message);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void broadcastGMMessage(byte[] message) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.broadcastGMMessage(message);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void toggleMegaphoneMuteState() throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.toggleMegaphoneMuteState();
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public boolean hasMerchant(int accountId) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                return cwi.hasMerchant(accountId);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        return false;
    }

    public int find(String charName) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                if (cwi.isConnected(charName)) {
                    return cwi.getChannelId();
                }
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        return -1;
    }

    // can we generify this
    @Override
    public int find(int characterId) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                if (cwi.isConnected(characterId)) {
                    return cwi.getChannelInfo().getId();
                }
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        return -1;
    }

    public void shutdownLogin() throws RemoteException {
        for (LoginWorldInterface lwi : registry.getLoginServer()) {
            try {
                lwi.shutdown();
            } catch (RemoteException e) {
                registry.deregisterLoginServer(lwi);
            }
        }
    }

    public void shutdown(int time) throws RemoteException {
        for (LoginWorldInterface lwi : registry.getLoginServer()) {
            try {
                lwi.shutdown();
            } catch (RemoteException e) {
                registry.deregisterLoginServer(lwi);
            }
        }
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.shutdown(time);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public Map<Integer, Integer> getConnected() throws RemoteException {
        Map<Integer, Integer> ret = new HashMap<>();
        int total = 0;
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                int curConnected = cwi.getConnected();
                ret.put(i, curConnected);
                total += curConnected;
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        ret.put(0, total);
        return ret;
    }

    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.loggedOn(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.loggedOff(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    //TODO only notify channels where partymembers are?
    public void updateParty(int partyid, PartyOperation operation, PartyMember target) throws RemoteException {
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
                throw new RuntimeException("Unhandeled updateParty operation "
                        + operation.name());
        }
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.updateParty(party, operation, target);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public Party createParty() throws RemoteException {
        return registry.createParty();
    }

    public Party getParty(int partyid) throws RemoteException {
        return registry.getParty(partyid);
    }

    @Override
    public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException {
        final Party party = registry.getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.partyChat(party, chattext, namefrom);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public Location getLocation(String charName) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                if (cwi.isConnected(charName)) {
                    return new Location(cwi.getLocation(charName), (byte) cwi.getChannelId());
                }
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        return null;
    }

    public List<CheaterData> getCheaters() throws RemoteException {
        List<CheaterData> allCheaters = new ArrayList<>();
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                allCheaters.addAll(cwi.getCheaters());
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    @Override
    public ChannelWorldInterface getChannelInterface(int channel) throws RemoteException {
        return registry.getChannel(channel);
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        for (ChannelWorldInterface cwi : registry.getAllChannelServers()) {
            cwi.buddyChat(recipientCharacterIds, cidFrom, nameFrom, chattext);
        }
    }

    @Override
    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            for (int charid : cwi.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, i));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    @Override
    public void transfer(CharacterTransfer Data, int characterid, int toChannel) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            if (i == toChannel) {
                final ChannelWorldInterface cwi = registry.getChannel(i);
                try {
                    cwi.ChannelChange_Data(Data, characterid);
                } catch (RemoteException e) {
                    registry.deregisterChannelServer(i);
                }
            }
        }

    }

    @Override
    public Guild getGuild(int id) throws RemoteException {
        return registry.getGuild(id);
    }

    @Override
    public void setGuildMemberOnline(GuildMember mgc, boolean bOnline, int channel) throws RemoteException {
        registry.setGuildMemberOnline(mgc, bOnline, channel);
    }

    @Override
    public boolean addGuildMember(GuildMember mgc) throws RemoteException {
        return registry.addGuildMember(mgc);
    }

    @Override
    public void guildChat(int gid, String name, int cid, String msg) throws RemoteException {
        registry.guildChat(gid, name, cid, msg);
    }

    @Override
    public void leaveGuild(GuildMember mgc) throws RemoteException {
        registry.leaveGuild(mgc);
    }

    @Override
    public void changeRank(int gid, int cid, MemberRank newRank) throws RemoteException {
        registry.changeRank(gid, cid, newRank);
    }

    @Override
    public void expelMember(GuildMember initiator, int cid) throws RemoteException {
        registry.expelMember(initiator, cid);
    }

    @Override
    public void setGuildNotice(int gid, String notice) throws RemoteException {
        registry.setGuildNotice(gid, notice);
    }

    @Override
    public void updateGuildMemberJob(int guildId, int characterId, int jobId) throws RemoteException {
        registry.updateGuildMemberJob(guildId, characterId, jobId);
    }

    @Override
    public void updateGuildMemberLevel(int guildId, int characterId, int level) throws RemoteException {
        registry.updateGuildMemberLevel(guildId, characterId, level);
    }

    @Override
    public void changeRankTitle(int gid, String[] ranks) throws RemoteException {
        registry.changeRankTitle(gid, ranks);
    }

    @Override
    public int createGuild(int leaderId, String name) throws RemoteException {
        return registry.createGuild(leaderId, name);
    }

    @Override
    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException {
        registry.setGuildEmblem(gid, bg, bgcolor, logo, logocolor);
    }

    @Override
    public void disbandGuild(int gid) throws RemoteException {
        registry.disbandGuild(gid);
    }

    @Override
    public boolean increaseGuildCapacity(int gid) throws RemoteException {
        return registry.increaseGuildCapacity(gid);
    }

    @Override
    public void gainGuildPoints(int gid, int amount) throws RemoteException {
        registry.gainGP(gid, amount);
    }

    public Messenger createMessenger(MessengerMember chrfor) throws RemoteException {
        return registry.createMessenger(chrfor);
    }

    public Messenger getMessenger(int messengerid) throws RemoteException {
        return registry.getMessenger(messengerid);
    }

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.messengerInvite(sender, messengerid, target, fromchannel);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void leaveMessenger(int messengerid, MessengerMember target) throws RemoteException {
        final Messenger messenger = registry.getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        final int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.removeMessengerPlayer(messenger, position);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void joinMessenger(int messengerid, MessengerMember target, String from, int fromchannel) throws RemoteException {
        final Messenger messenger = registry.getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void messengerChat(int messengerid, String namefrom, String chattext) throws RemoteException {
        final Messenger messenger = registry.getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.messengerChat(messenger, chattext, namefrom);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void declineChat(String target, String namefrom) throws RemoteException {
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.declineChat(target, namefrom);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException {
        final Messenger messenger = registry.getMessenger(messengerid);
        final int position = messenger.getPositionByName(namefrom);
        for (int i : registry.getActiveChannels()) {
            final ChannelWorldInterface cwi = registry.getChannel(i);
            try {
                cwi.updateMessenger(messenger, namefrom, position, fromchannel);
            } catch (RemoteException e) {
                registry.deregisterChannelServer(i);
            }
        }
    }

    public void silentLeaveMessenger(int messengerid, MessengerMember target) throws RemoteException {
        final Messenger messenger = registry.getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    public void silentJoinMessenger(int messengerid, MessengerMember target, int position) throws RemoteException {
        final Messenger messenger = registry.getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target, position);
    }

    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException {
        registry.getPlayerBuffStorage().addBuffsToStorage(chrid, toStore);
    }

    public Collection<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException {
        return registry.getPlayerBuffStorage().getBuffsFromStorage(chrid);
    }

    public void addCooldownsToStorage(int chrid, List<PlayerCooldownValueHolder> toStore) throws RemoteException {
        registry.getPlayerBuffStorage().addCooldownsToStorage(chrid, toStore);
    }

    public Collection<PlayerCooldownValueHolder> getCooldownsFromStorage(int chrid) throws RemoteException {
        return registry.getPlayerBuffStorage().getCooldownsFromStorage(chrid);
    }

    public void addDiseaseToStorage(int chrid, List<PlayerDiseaseValueHolder> toStore) throws RemoteException {
        registry.getPlayerBuffStorage().addDiseaseToStorage(chrid, toStore);
    }

    public Collection<PlayerDiseaseValueHolder> getDiseaseFromStorage(int chrid) throws RemoteException {
        return registry.getPlayerBuffStorage().getDiseaseFromStorage(chrid);
    }
}