package handling.channel.handler;

import java.rmi.RemoteException;
import java.util.List;

import client.BuddylistEntry;
import client.CharacterNameAndId;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleBuffStat;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.CharacterIdChannelPair;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffValueHolder;
import handling.world.remote.WorldChannelInterface;
import org.javastory.io.PacketFormatException;
import server.MapleTrade;
import server.maps.FieldLimitType;
import server.shops.IMaplePlayerShop;
import tools.FileOutputUtil;
import tools.MaplePacketCreator;
import tools.packet.FamilyPacket;
import org.javastory.io.PacketReader;
import server.maps.MapleMap;
import client.MapleCharacter;
import client.SkillFactory;
import handling.world.remote.ServerStatus;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.maps.SavedLocationType;

public class InterServerHandler {

    public static void handleEnterMTS(final MapleClient c) {
        final MapleMap map = c.getChannelServer().getMapFactory(c.getPlayer().getWorld()).getMap(910000000);
        if ((c.getPlayer().getMapId() < 910000000) || (c.getPlayer().getMapId() > 910000022)) {
            if (c.getPlayer().getLevel() >= 10) {
                c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                c.getPlayer().changeMap(map, map.getPortal("out00"));
                c.getSession().write(MaplePacketCreator.enableActions());
            } else {
                c.getPlayer().dropMessage(5, "You do not meet the minimum level requirement to access the Trade Shop.");
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            c.getPlayer().dropMessage(5, "You are already in the FREE MARKET");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void handleEnterCashShop(final PacketReader reader, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final ChannelServer ch = ChannelManager.getInstance(c.getChannelId());
        ServerStatus csStatus = ServerStatus.OFFLINE;
        try {
            csStatus = ch.getWorldInterface().getCashShopStatus();
        } catch (RemoteException e) {
            c.getChannelServer().pingWorld();
        }
        if (csStatus == ServerStatus.OFFLINE) {
            c.getPlayer().dropMessage(5, "You cannot go into the cash shop. Please try again later.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
        if (chr.getTrade() != null) {
            MapleTrade.cancelTrade(chr.getTrade());
        }
        if (chr.getCheatTracker() != null) {
            chr.getCheatTracker().dispose();
        }
        if (chr.getBuffedValue(MapleBuffStat.SUMMON) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
        }
        if (chr.getBuffedValue(MapleBuffStat.PUPPET) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        }
        if (chr.getBuffedValue(MapleBuffStat.MIRROR_TARGET) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.MIRROR_TARGET);
        }
        final IMaplePlayerShop shop = chr.getPlayerShop();
        if (shop != null) {
            shop.removeVisitor(chr);
            if (shop.isOwner(chr)) {
                shop.setOpen(true);
            }
        }
        try {
            final WorldChannelInterface wci = ch.getWorldInterface();
            if (chr.getMessenger() != null) {
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                wci.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
            }
            wci.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
            wci.addCooldownsToStorage(chr.getId(), chr.getAllCooldowns());
            wci.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
            wci.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
        } catch (RemoteException e) {
            c.getChannelServer().pingWorld();
        }
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
        // TODO: I don't know what this is but it's crazy.
        final int cashShopPort = 8686;
        c.getSession().write(MaplePacketCreator.getChannelChange(cashShopPort));
        chr.saveToDb(false, false);
        chr.getMap().removePlayer(chr);
        c.setPlayer(null);
    }

    public static void handlePlayerLoggedIn(final int playerid, final MapleClient c) {
        final ChannelServer channelServer = c.getChannelServer();
        MapleCharacter player;
        final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);
        if (transfer == null) { 
            // Player isn't in storage, probably isn't CC
            player = MapleCharacter.loadFromDb(playerid, c, true);
        } else {
            player = MapleCharacter.reconstructCharacter(transfer, c, true);
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());
        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close(true);
            return;
        }
        final int state = c.getLoginState();
        boolean allowLogin = false;
        try {
            if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
                if (!channelServer.getWorldInterface().isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                    allowLogin = true;
                }
            }
        } catch (RemoteException e) {
            channelServer.pingWorld();
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close(true);
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        final ChannelServer cserv = ChannelManager.getInstance(c.getChannelId());
        cserv.addPlayer(player);
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        player.getMap().addPlayer(player);
        try {
            // Start of cooldown, buffs
            final WorldChannelInterface wci = ChannelManager.getInstance(c.getChannelId()).getWorldInterface();
            final List<PlayerBuffValueHolder> buffs = wci.getBuffsFromStorage(player.getId());
            if (buffs != null) {
                player.silentGiveBuffs(buffs);
            }
            c.getPlayer().giveCoolDowns(wci.getCooldownsFromStorage(player.getId()));
            c.getPlayer().giveSilentDebuff(wci.getDiseaseFromStorage(player.getId()));
            
            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), c.getChannelId(), buddyIds);
            if (player.getParty() != null) {
                channelServer.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
            }
            final CharacterIdChannelPair[] onlineBuddies = cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                final BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
            
            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            final int messenger_pos = player.getMessengerPosition();
            if (player.getMessenger() != null && messenger_pos < 4 && messenger_pos > -1) {
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer(), messenger_pos);
                wci.silentJoinMessenger(messenger.getId(), messengerplayer, messenger_pos);
                wci.updateMessenger(c.getPlayer().getMessenger().getId(), c.getPlayer().getName(), c.getChannelId());
            }
            
            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                c.getChannelServer().getWorldInterface().setGuildMemberOnline(player.getMGC(), true, c.getChannelId());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
            }
        } catch (RemoteException e) {
            channelServer.pingWorld();
        } catch (Exception e) {
            FileOutputUtil.outputFileError(FileOutputUtil.Login_Error, e);
        }
        c.getSession().write(FamilyPacket.getFamilyData());
        player.sendMacros();
        player.showNote();
        player.updatePartyMemberHP();
        c.getSession().write(MaplePacketCreator.getKeymap(player.getKeyLayout()));
        for (MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        final CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getId(), "ETC", -1, false, pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
        }
        player.expirationTask();
        if (player.getJob() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }
        if (player.isGM()) {
            SkillFactory.getSkill(9001001).getEffect(1).applyTo(player); // GM haste
        }
    }

    public static void handleChannelChange(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        if (!chr.isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int channel = reader.readByte() + 1;
        final ChannelServer toch = ChannelManager.getInstance(channel);

        if (FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit()) || channel == c.getChannelId()) {
            c.getSession().close(false);
            return;
        } else if (toch == null || toch.getStatus() != ServerStatus.ONLINE) {
            c.getSession().write(MaplePacketCreator.serverBlocked(1));
            return;
        }

        {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr.getTrade());
            }
            if (chr.getPets() != null) {
                chr.unequipAllPets();
            }
            if (chr.getCheatTracker() != null) {
                chr.getCheatTracker().dispose();
            }
            if (chr.getBuffedValue(MapleBuffStat.SUMMON) != null) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
            }
            if (chr.getBuffedValue(MapleBuffStat.PUPPET) != null) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
            }
            if (chr.getBuffedValue(MapleBuffStat.MIRROR_TARGET) != null) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.MIRROR_TARGET);
            }
            final IMaplePlayerShop shop = chr.getPlayerShop();
            if (shop != null) {
                shop.removeVisitor(chr);
                if (shop.isOwner(chr)) {
                    shop.setOpen(true);
                }
            }
        }

        final ChannelServer ch = ChannelManager.getInstance(c.getChannelId());
        try {
            final WorldChannelInterface wci = ch.getWorldInterface();

            if (chr.getMessenger() != null) {
                wci.silentLeaveMessenger(chr.getMessenger().getId(), new MapleMessengerCharacter(chr));
            }
            wci.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
            wci.addCooldownsToStorage(chr.getId(), chr.getAllCooldowns());
            wci.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
            wci.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), channel);
        } catch (RemoteException e) {
            e.printStackTrace();
            c.getChannelServer().pingWorld();
        }
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());

        final String[] socket = ch.getIP(channel).split(":");
        c.getSession().write(MaplePacketCreator.getChannelChange(Integer.parseInt(socket[1])));
        chr.saveToDb(false, false);
        chr.getMap().removePlayer(chr);
        c.setPlayer(null);
    }
}
