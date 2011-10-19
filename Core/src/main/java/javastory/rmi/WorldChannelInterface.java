package javastory.rmi;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javastory.channel.CharacterTransfer;
import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.PlayerBuffValueHolder;
import javastory.client.MemberRank;
import javastory.server.Location;
import javastory.world.core.CharacterIdChannelPair;
import javastory.world.core.PartyOperation;
import javastory.world.core.PlayerCooldownValueHolder;
import javastory.world.core.PlayerDiseaseValueHolder;
import javastory.world.core.WorldChannelCommonOperations;

public interface WorldChannelInterface extends RemotePingable, WorldChannelCommonOperations {

    public void serverReady() throws RemoteException;

    public void shutdownLogin() throws RemoteException;

    public String getIP(int channelId) throws RemoteException;

    public void toggleMegaphoneMuteState() throws RemoteException;

    public boolean hasMerchant(int accountId) throws RemoteException;

    public int find(String characterName) throws RemoteException;

    public int find(int characterId) throws RemoteException;

    public Map<Integer, Integer> getConnected() throws RemoteException;

    Party createParty() throws RemoteException;

    Party getParty(int partyId) throws RemoteException;

    public void updateParty(int partyId, PartyOperation operation, PartyMember target) throws RemoteException;

    public void partyChat(int partyId, String message, String sender) throws RemoteException;

    public boolean isAvailable() throws RemoteException;

    public ChannelWorldInterface getChannelInterface(int channelId) throws RemoteException;

    public Location getLocation(String name) throws RemoteException;

    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException;

    public void transfer(CharacterTransfer data, int characterId, int targetChannel) throws RemoteException;

    public Guild getGuild(int id) throws RemoteException;

    public void setGuildMemberOnline(GuildMember member, boolean isOnline, int channelId) throws RemoteException;

    public boolean addGuildMember(GuildMember member) throws RemoteException;

    public void leaveGuild(GuildMember member) throws RemoteException;

    public void guildChat(int guildId, String sender, int characterId, String message) throws RemoteException;

    public void changeRank(int guildId, int characterId, MemberRank newRank) throws RemoteException;

    public void expelMember(GuildMember initiator, int characterId) throws RemoteException;

    public void setGuildNotice(int guildId, String notice) throws RemoteException;

    public void changeRankTitle(int guildId, String[] ranks) throws RemoteException;

    public int createGuild(int leaderId, String name) throws RemoteException;

    public void setGuildEmblem(int guildId, short background, byte backgroundColor, short logo, byte logoVariation) throws RemoteException;

    public void disbandGuild(int guildId) throws RemoteException;

    public boolean increaseGuildCapacity(int guildId) throws RemoteException;

    public void gainGuildPoints(int guildId, int amount) throws RemoteException;

    Messenger createMessenger(MessengerMember initiator) throws RemoteException;

    Messenger getMessenger(int messengerId) throws RemoteException;

    public void leaveMessenger(int messengerId, MessengerMember target) throws RemoteException;

    public void joinMessenger(int messengerId, MessengerMember target, String from, int channelId) throws RemoteException;

    public void silentJoinMessenger(int messengerId, MessengerMember target, int position) throws RemoteException;

    public void silentLeaveMessenger(int messengerId, MessengerMember target) throws RemoteException;

    public void messengerChat(int messengerId, String sender, String message) throws RemoteException;

    public void declineChat(String target, String sender) throws RemoteException;

    public void updateMessenger(int messengerId, String sender, int channelId) throws RemoteException;

    public void addBuffsToStorage(int characterId, List<PlayerBuffValueHolder> buffs) throws RemoteException;

    public Collection<PlayerBuffValueHolder> getBuffsFromStorage(int characterId) throws RemoteException;

    public void addCooldownsToStorage(int characterId, List<PlayerCooldownValueHolder> cooldowns) throws RemoteException;

    public Collection<PlayerCooldownValueHolder> getCooldownsFromStorage(int characterId) throws RemoteException;

    public void addDiseaseToStorage(int characterId, List<PlayerDiseaseValueHolder> diseases) throws RemoteException;

    public Collection<PlayerDiseaseValueHolder> getDiseaseFromStorage(int characterId) throws RemoteException;

    void updateGuildMemberJob(int guildId, int characterId, int jobId) throws RemoteException;

    void updateGuildMemberLevel(int guildId, int characterId, int level) throws RemoteException;
}