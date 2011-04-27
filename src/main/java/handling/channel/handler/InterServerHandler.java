package handling.channel.handler;

import java.rmi.RemoteException;

import client.BuddyListEntry;
import client.SimpleCharacterInfo;
import org.javastory.client.ChannelClient;
import client.QuestStatus;
import client.BuffStat;
import handling.world.CharacterTransfer;
import handling.world.Messenger;
import handling.world.MessengerMember;
import handling.world.CharacterIdChannelPair;
import handling.world.PartyMember;
import handling.world.PartyOperation;
import handling.world.PlayerBuffValueHolder;
import handling.world.remote.WorldChannelInterface;
import org.javastory.io.PacketFormatException;
import server.Trade;
import server.maps.FieldLimitType;
import server.shops.PlayerShop;
import tools.LogUtil;
import tools.MaplePacketCreator;
import tools.packet.FamilyPacket;
import org.javastory.io.PacketReader;
import server.maps.GameMap;
import org.javastory.client.ChannelCharacter;
import client.SkillFactory;
import handling.world.remote.ServerStatus;
import java.util.Collection;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import org.javastory.server.channel.PlayerStorage;
import server.maps.SavedLocationType;

public class InterServerHandler {

    public static void handleEnterMTS(final ChannelClient c) {
        final GameMap map = c.getChannelServer().getMapFactory(c.getWorldId()).getMap(910000000);
        if ((c.getPlayer().getMapId() < 910000000) || (c.getPlayer().getMapId() >
                910000022)) {
            if (c.getPlayer().getLevel() >= 10) {
                c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                c.getPlayer().changeMap(map, map.getPortal("out00"));
                c.write(MaplePacketCreator.enableActions());
            } else {
                c.getPlayer().sendNotice(5, "You do not meet the minimum level requirement to access the Trade Shop.");
                c.write(MaplePacketCreator.enableActions());
            }
        } else {
            c.getPlayer().sendNotice(5, "You are already in the FREE MARKET");
            c.write(MaplePacketCreator.enableActions());
        }
    }

    public static void handlePlayerLoggedIn(final int playerId, final ChannelClient client) {
        final ChannelServer channelServer = client.getChannelServer();
        final PlayerStorage playerStorage = channelServer.getPlayerStorage();
        
        // Remote hack
        if (!playerStorage.checkSession(playerId, client.getSessionIP())) {
            client.disconnect(true);
        }
        
        ChannelCharacter player;
        final CharacterTransfer transfer = playerStorage.getPendingTransfer(playerId);
        if (transfer == null) {
            // Player isn't in storage, probably isn't CC
            player = ChannelCharacter.loadFromDb(playerId, client);
        } else {
            player = ChannelCharacter.reconstructCharacter(transfer, client);
        }
        client.setPlayer(player);
        client.setAccountId(player.getAccountId());

        final ChannelServer cserv = ChannelManager.getInstance(client.getChannelId());
        cserv.addPlayer(player);
        client.write(MaplePacketCreator.getCharInfo(player));
        player.getMap().addPlayer(player);
        try {
            // Start of cooldown, buffs
            final WorldChannelInterface wci = ChannelManager.getInstance(client.getChannelId()).getWorldInterface();
            final Collection<PlayerBuffValueHolder> buffs = wci.getBuffsFromStorage(player.getId());
            if (buffs != null) {
                player.silentGiveBuffs(buffs);
            }
            client.getPlayer().giveCooldowns(wci.getCooldownsFromStorage(player.getId()));
            client.getPlayer().giveSilentDebuff(wci.getDiseaseFromStorage(player.getId()));

            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), client.getChannelId(), buddyIds);
            if (player.getParty() != null) {
                channelServer.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new PartyMember(player));
            }
            final CharacterIdChannelPair[] onlineBuddies = cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                final BuddyListEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            client.write(MaplePacketCreator.updateBuddyList(player.getBuddylist().getBuddies()));

            // Start of Messenger
            final Messenger messenger = player.getMessenger();
            final int messenger_pos = player.getMessengerPosition();
            if (player.getMessenger() != null && messenger_pos < 4 &&
                    messenger_pos > -1) {
                MessengerMember messengerplayer = new MessengerMember(client.getPlayer(), messenger_pos);
                wci.silentJoinMessenger(messenger.getId(), messengerplayer, messenger_pos);
                wci.updateMessenger(client.getPlayer().getMessenger().getId(), client.getPlayer().getName(), client.getChannelId());
            }

            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                client.getChannelServer().getWorldInterface().setGuildMemberOnline(player.getGuildMembership(), true, client.getChannelId());
                client.write(MaplePacketCreator.showGuildInfo(player));
            }
        } catch (RemoteException e) {
            channelServer.pingWorld();
        } catch (Exception e) {
            LogUtil.outputFileError(LogUtil.Login_Error, e);
        }
        client.write(FamilyPacket.getFamilyData());
        player.sendMacros();
        player.showNote();
        player.updatePartyMemberHP();
        client.write(MaplePacketCreator.getKeymap(player.getKeyLayout()));
        for (QuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                client.write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        final SimpleCharacterInfo pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist().put(new BuddyListEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getId(), "ETC", -1, false, pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
            client.write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
        }
        player.expirationTask();
        if (player.getJobId() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }
        if (player.isGM()) {
            SkillFactory.getSkill(9001001).getEffect(1).applyTo(player); // GM haste
        }
    }

    public static void handleChannelChange(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
        if (!chr.isAlive()) {
            c.write(MaplePacketCreator.enableActions());
            return;
        }
        final int channel = reader.readByte() + 1;
        final ChannelServer toch = ChannelManager.getInstance(channel);

        if (FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit()) ||
                channel == c.getChannelId()) {
            c.disconnect();
            return;
        } else if (toch == null || toch.getStatus() != ServerStatus.ONLINE) {
            c.write(MaplePacketCreator.serverBlocked(1));
            return;
        }

        if (chr.getTrade() != null) {
            Trade.cancelTrade(chr.getTrade());
        }
        if (chr.getPets() != null) {
            chr.unequipAllPets();
        }
        if (chr.getCheatTracker() != null) {
            chr.getCheatTracker().dispose();
        }
        if (chr.getBuffedValue(BuffStat.SUMMON) != null) {
            chr.cancelEffectFromBuffStat(BuffStat.SUMMON);
        }
        if (chr.getBuffedValue(BuffStat.PUPPET) != null) {
            chr.cancelEffectFromBuffStat(BuffStat.PUPPET);
        }
        if (chr.getBuffedValue(BuffStat.MIRROR_TARGET) != null) {
            chr.cancelEffectFromBuffStat(BuffStat.MIRROR_TARGET);
        }
        final PlayerShop shop = chr.getPlayerShop();
        if (shop != null) {
            shop.removeVisitor(chr);
            if (shop.isOwner(chr)) {
                shop.setOpen(true);
            }
        }

        final ChannelServer ch = ChannelManager.getInstance(c.getChannelId());
        try {
            final WorldChannelInterface wci = ch.getWorldInterface();

            if (chr.getMessenger() != null) {
                wci.silentLeaveMessenger(chr.getMessenger().getId(), new MessengerMember(chr));
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

        final String[] socket = ch.getIP(channel).split(":");
        c.write(MaplePacketCreator.getChannelChange(Integer.parseInt(socket[1])));
        chr.saveToDb(false);
        chr.getMap().removePlayer(chr);
        c.setPlayer(null);
    }
}
