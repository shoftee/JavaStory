package javastory.channel.handling;

import javastory.channel.GuildMember;
import java.rmi.RemoteException;

import client.BuddyListEntry;
import client.SimpleCharacterInfo;
import javastory.channel.ChannelClient;
import client.QuestStatus;
import client.BuffStat;
import javastory.channel.CharacterTransfer;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.world.core.CharacterIdChannelPair;
import javastory.channel.PartyMember;
import javastory.world.core.PartyOperation;
import javastory.channel.PlayerBuffValueHolder;
import javastory.io.PacketFormatException;
import javastory.channel.server.Trade;
import server.maps.FieldLimitType;
import javastory.channel.shops.PlayerShop;
import tools.LogUtil;
import tools.MaplePacketCreator;
import tools.packet.FamilyPacket;
import javastory.io.PacketReader;
import javastory.channel.maps.GameMap;
import javastory.channel.ChannelCharacter;
import javastory.channel.client.SkillFactory;
import javastory.world.core.ServerStatus;
import java.util.Collection;
import javastory.channel.ChannelManager;
import javastory.rmi.WorldChannelInterface;
import javastory.server.ChannelServer;
import javastory.server.channel.PlayerStorage;
import server.maps.SavedLocationType;

public class InterServerHandler {

    public static void handleEnterMTS(final ChannelClient c) {
        final GameMap map = c.getChannelServer().getMapFactory(c.getWorldId()).getMap(910000000);
        final ChannelCharacter player = c.getPlayer();
        if ((player.getMapId() < 910000000) || (player.getMapId()
                > 910000022)) {
            if (player.getLevel() >= 10) {
                player.saveLocation(SavedLocationType.FREE_MARKET);
                player.changeMap(map, map.getPortal("out00"));
                c.write(MaplePacketCreator.enableActions());
            } else {
                player.sendNotice(5, "You do not meet the minimum level requirement to access the Trade Shop.");
                c.write(MaplePacketCreator.enableActions());
            }
        } else {
            player.sendNotice(5, "You are already in the FREE MARKET");
            c.write(MaplePacketCreator.enableActions());
        }
    }

    public static void handlePlayerLoggedIn(final int playerId, final ChannelClient client) {
        final ChannelServer channelServer = client.getChannelServer();
        final PlayerStorage playerStorage = channelServer.getPlayerStorage();
        final int channelId = client.getChannelId();

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

        channelServer.addPlayer(player);
        client.write(MaplePacketCreator.getCharInfo(player));
        player.getMap().addPlayer(player);
        try {
            // Start of cooldown, buffs
            final WorldChannelInterface world = channelServer.getWorldInterface();
            final Collection<PlayerBuffValueHolder> buffs = world.getBuffsFromStorage(player.getId());
            if (buffs != null) {
                player.silentGiveBuffs(buffs);
            }
            player.giveCooldowns(world.getCooldownsFromStorage(player.getId()));
            player.giveSilentDebuff(world.getDiseaseFromStorage(player.getId()));

            // Start of buddylist
            final int buddyIds[] = player.getBuddyList().getBuddyIds();
            world.loggedOn(player.getName(), player.getId(), channelId, buddyIds);
            final CharacterIdChannelPair[] onlineBuddies = world.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                final BuddyListEntry ble = player.getBuddyList().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddyList().put(ble);
            }
            client.write(MaplePacketCreator.updateBuddyList(player.getBuddyList().getBuddies()));

            // Party:
            PartyMember partyMember = player.getPartyMembership();
            if (partyMember != null) {
                world.updateParty(partyMember.getPartyId(), PartyOperation.LOG_ONOFF, partyMember);
            }

            // Start of Messenger
            final Messenger messenger = player.getMessenger();
            final int messenger_pos = player.getMessengerPosition();
            if (player.getMessenger() != null && messenger_pos < 4
                    && messenger_pos > -1) {
                MessengerMember messengerplayer = new MessengerMember(player, messenger_pos);
                world.silentJoinMessenger(messenger.getId(), messengerplayer, messenger_pos);
                world.updateMessenger(player.getMessenger().getId(), player.getName(), channelId);
            }

            // Start of Guild and alliance
            final GuildMember guildMember = player.getGuildMembership();
            if (guildMember != null) {
                int guildId = guildMember.getGuildId();
                world.setGuildMemberOnline(guildMember, true, channelId);
                client.write(MaplePacketCreator.showGuildInfo(client, guildId));
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
        final SimpleCharacterInfo pendingBuddyRequest = player.getBuddyList().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddyList().put(new BuddyListEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getId(), "ETC", -1, false, pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
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

        if (FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())
                || channel == c.getChannelId()) {
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

        final ChannelServer ch = c.getChannelServer();
        try {
            final WorldChannelInterface wci = ch.getWorldInterface();

            if (chr.getMessenger() != null) {
                wci.silentLeaveMessenger(chr.getMessenger().getId(), new MessengerMember(chr));
            }
            wci.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
            wci.addCooldownsToStorage(chr.getId(), chr.getAllCooldowns());
            wci.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
            wci.transfer(new CharacterTransfer(chr), chr.getId(), channel);
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
