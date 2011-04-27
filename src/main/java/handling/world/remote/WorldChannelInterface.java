package handling.world.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import handling.channel.remote.ChannelWorldInterface;
import handling.world.CharacterTransfer;
import handling.world.CharacterIdChannelPair;
import handling.world.Messenger;
import handling.world.MessengerMember;
import handling.world.Party;
import handling.world.PartyMember;
import handling.world.PartyOperation;
import handling.world.PlayerBuffValueHolder;
import handling.world.PlayerCooldownValueHolder;
import handling.world.PlayerDiseaseValueHolder;
import handling.world.Guild;
import handling.world.GuildMember;
import java.util.Collection;
import org.javastory.client.MemberRank;

public interface WorldChannelInterface extends Remote, WorldChannelCommonOperations {

    public Properties getGameProperties() throws RemoteException;

    public void serverReady() throws RemoteException;

    public void shutdownLogin() throws RemoteException;

    public String getIP(int channel) throws RemoteException;

    public void toggleMegaphoneMuteState() throws RemoteException;

    public boolean hasMerchant(int accountId) throws RemoteException;

    public int find(String charName) throws RemoteException;

    public int find(int characterId) throws RemoteException;

    public Map<Integer, Integer> getConnected() throws RemoteException;

    Party createParty(PartyMember chrfor) throws RemoteException;

    Party getParty(int partyid) throws RemoteException;

    public void updateParty(int partyid, PartyOperation operation, PartyMember target) throws RemoteException;

    public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException;

    public boolean isAvailable() throws RemoteException;

    public ChannelWorldInterface getChannelInterface(int channel) throws RemoteException;

    public WorldLocation getLocation(String name) throws RemoteException;

    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException;

    public void ChannelChange_Data(CharacterTransfer data, int characterid, int toChannel) throws RemoteException;

    public Guild getGuild(int id, GuildMember mgc) throws RemoteException;

    public void setGuildMemberOnline(GuildMember mgc, boolean bOnline, int channel) throws RemoteException;

    public boolean addGuildMember(GuildMember mgc) throws RemoteException;

    public void leaveGuild(GuildMember mgc) throws RemoteException;

    public void guildChat(int gid, String name, int cid, String msg) throws RemoteException;

    public void allianceChat(int gid, String name, int cid, String msg) throws RemoteException;

    public void changeRank(int gid, int cid, MemberRank newRank) throws RemoteException;

    public void expelMember(GuildMember initiator, String name, int cid) throws RemoteException;

    public void setGuildNotice(int gid, String notice) throws RemoteException;

    public void memberLevelJobUpdate(GuildMember mgc) throws RemoteException;

    public void changeRankTitle(int gid, String[] ranks) throws RemoteException;

    public int createGuild(int leaderId, String name) throws RemoteException;

    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException;

    public void disbandGuild(int gid) throws RemoteException;

    public boolean increaseGuildCapacity(int gid) throws RemoteException;

    public void gainGP(int gid, int amount) throws RemoteException;

    Messenger createMessenger(MessengerMember chrfor) throws RemoteException;

    Messenger getMessenger(int messengerid) throws RemoteException;

    public void leaveMessenger(int messengerid, MessengerMember target) throws RemoteException;

    public void joinMessenger(int messengerid, MessengerMember target, String from, int fromchannel) throws RemoteException;

    public void silentJoinMessenger(int messengerid, MessengerMember target, int position) throws RemoteException;

    public void silentLeaveMessenger(int messengerid, MessengerMember target) throws RemoteException;

    public void messengerChat(int messengerid, String chattext, String namefrom) throws RemoteException;

    public void declineChat(String target, String namefrom) throws RemoteException;

    public void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException;

    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException;

    public Collection<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException;

    public void addCooldownsToStorage(int chrid, List<PlayerCooldownValueHolder> toStore) throws RemoteException;

    public Collection<PlayerCooldownValueHolder> getCooldownsFromStorage(int chrid) throws RemoteException;

    public void addDiseaseToStorage(int chrid, List<PlayerDiseaseValueHolder> toStore) throws RemoteException;

    public Collection<PlayerDiseaseValueHolder> getDiseaseFromStorage(int chrid) throws RemoteException;
}