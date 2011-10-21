package javastory.channel.handling;

import java.rmi.RemoteException;
import java.util.Collection;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.CharacterTransfer;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.PartyMember;
import javastory.channel.PlayerBuffValueHolder;
import javastory.channel.client.BuddyListEntry;
import javastory.channel.client.SkillFactory;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.SavedLocationType;
import javastory.client.SimpleCharacterInfo;
import javastory.game.quest.QuestStatus;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.rmi.WorldChannelInterface;
import javastory.server.channel.PlayerStorage;
import javastory.tools.LogUtil;
import javastory.tools.packets.ChannelPackets;
import javastory.tools.packets.FamilyPacket;
import javastory.world.core.CharacterIdChannelPair;
import javastory.world.core.PartyOperation;

public class InterServerHandler {

    public static void handleEnterMTS(final ChannelClient c) {
        final GameMap map = c.getChannelServer().getMapFactory().getMap(910000000);
        final ChannelCharacter player = c.getPlayer();
        if ((player.getMapId() < 910000000) || (player.getMapId()
                > 910000022)) {
            if (player.getLevel() >= 10) {
                player.saveLocation(SavedLocationType.FREE_MARKET);
                player.changeMap(map, map.getPortal("out00"));
                c.write(ChannelPackets.enableActions());
            } else {
                player.sendNotice(5, "You do not meet the minimum level requirement to access the Trade Shop.");
                c.write(ChannelPackets.enableActions());
            }
        } else {
            player.sendNotice(5, "You are already in the FREE MARKET");
            c.write(ChannelPackets.enableActions());
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
        client.write(ChannelPackets.getCharInfo(player));
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
            client.write(ChannelPackets.updateBuddyList(player.getBuddyList().getBuddies()));

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
                client.write(ChannelPackets.showGuildInfo(client, guildId));
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
        client.write(ChannelPackets.getKeymap(player.getKeyLayout()));
        for (QuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                client.write(ChannelPackets.updateQuestMobKills(status));
            }
        }
        final SimpleCharacterInfo pendingBuddyRequest = player.getBuddyList().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddyList().put(new BuddyListEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getId(), "ETC", -1, false, pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
            client.write(ChannelPackets.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob()));
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
            c.write(ChannelPackets.enableActions());
            return;
        }
        final int targetChannelId = reader.readByte() + 1;
        return;
        // TODO: Whoops, we can't do it like this.
//        final ChannelServer targetChannel = ChannelManager.getInstance(targetChannelId);
//
//        if (FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())
//                || targetChannelId == c.getChannelId()) {
//            c.disconnect();
//            return;
//        } else if (targetChannel == null || targetChannel.getStatus() != ServerStatus.ONLINE) {
//            c.write(ChannelPackets.serverBlocked(1));
//            return;
//        }
//
//        if (chr.getTrade() != null) {
//            Trade.cancelTrade(chr.getTrade());
//        }
//        if (chr.getPets() != null) {
//            chr.unequipAllPets();
//        }
//        if (chr.getCheatTracker() != null) {
//            chr.getCheatTracker().dispose();
//        }
//        if (chr.getBuffedValue(BuffStat.SUMMON) != null) {
//            chr.cancelEffectFromBuffStat(BuffStat.SUMMON);
//        }
//        if (chr.getBuffedValue(BuffStat.PUPPET) != null) {
//            chr.cancelEffectFromBuffStat(BuffStat.PUPPET);
//        }
//        if (chr.getBuffedValue(BuffStat.MIRROR_TARGET) != null) {
//            chr.cancelEffectFromBuffStat(BuffStat.MIRROR_TARGET);
//        }
//        
//        final PlayerShop shop = chr.getPlayerShop();
//        if (shop != null) {
//            shop.removeVisitor(chr);
//            if (shop.isOwner(chr)) {
//                shop.setOpen(true);
//            }
//        }
//
//        final ChannelServer ch = c.getChannelServer();
//        try {
//            final WorldChannelInterface wci = ch.getWorldInterface();
//
//            if (chr.getMessenger() != null) {
//                wci.silentLeaveMessenger(chr.getMessenger().getId(), new MessengerMember(chr));
//            }
//            wci.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
//            wci.addCooldownsToStorage(chr.getId(), chr.getAllCooldowns());
//            wci.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
//            wci.transfer(new CharacterTransfer(chr), chr.getId(), targetChannelId);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//            c.getChannelServer().pingWorld();
//        }
//        ch.removePlayer(chr);
//
//        final String[] socket = ch.getIP(targetChannelId).split(":");
//        c.write(ChannelPackets.getChannelChange(Integer.parseInt(socket[1])));
//        chr.saveToDb(false);
//        chr.getMap().removePlayer(chr);
//        c.setPlayer(null);
    }
}
