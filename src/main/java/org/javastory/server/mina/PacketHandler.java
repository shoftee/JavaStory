package org.javastory.server.mina;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import client.MapleClient;
import handling.ClientPacketOpcode;
import handling.GamePacket;
import handling.ServerConstants;
import handling.ServerType;
import handling.cashshop.handler.*;
import handling.channel.handler.*;
import handling.login.handler.*;
import handling.world.remote.ServerStatus;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.javastory.io.PacketFormatException;
import server.Randomizer;
import org.javastory.cryptography.AesTransform;
import tools.packet.LoginPacket;
import org.javastory.io.PacketReader;
import tools.Pair;

import org.javastory.cryptography.VersionType;
import org.javastory.server.channel.ChannelManager;

public class PacketHandler extends IoHandlerAdapter {

    private int channelId = -1;
    private ServerType type = null;
    private final List<String> BlockedIP = new ArrayList();
    private final Map<String, Pair<Long, Byte>> tracker =
            new ConcurrentHashMap<String, Pair<Long, Byte>>();

    public PacketHandler(final ServerType type) {
        this.type = type;
    }

    public PacketHandler(final ServerType type, final int channel) {
        this.channelId = channel;
        this.type = type;
    }

    @Override
    public void messageSent(final IoSession session, final Object message)
            throws Exception {
        final Runnable r = ((GamePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(final IoSession session, final Throwable cause)
            throws Exception {
        // Empty statement
    }

    @Override
    public void sessionOpened(final IoSession session) throws Exception {
        final String address =
                session.getRemoteAddress().toString().split(":")[0];
        if (BlockedIP.contains(address)) {
            session.close(true);
            return;
        }
        final Pair<Long, Byte> track = tracker.get(address);
        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;
            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 10) {
                BlockedIP.add(address);
                tracker.remove(address);
                session.close(true);
                return;
            }
        }
        tracker.put(address, new Pair(System.currentTimeMillis(), count));
        if (channelId > -1) {
            if (ChannelManager.getInstance(channelId).getStatus() != ServerStatus.ONLINE) {
                session.close(true);
                return;
            }
        }
        final byte clientIv[] = {70, 114, 122, (byte) Randomizer.nextInt(255)};
        final byte serverIv[] = {82, 48, 120, (byte) Randomizer.nextInt(255)};

        final AesTransform serverCrypto = new AesTransform(
                serverIv, ServerConstants.GAME_VERSION, VersionType.COMPLEMENT);
        final AesTransform clientCrypto = new AesTransform(
                clientIv, ServerConstants.GAME_VERSION, VersionType.REGULAR);

        final MapleClient client =
                new MapleClient(clientCrypto, serverCrypto, session);
        client.setChannel(channelId);
        PacketDecoder.DecoderState decoderState =
                new PacketDecoder.DecoderState();
        session.setAttribute(PacketDecoder.DECODER_STATE_KEY, decoderState);
        final GamePacket helloPacket = LoginPacket.getHello(
                ServerConstants.GAME_VERSION, clientIv, serverIv);
        session.write(helloPacket);
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.getConfig().setBothIdleTime(30);
        System.out.println(":: IoSession opened " + address + " ::");
    }

    @Override
    public void sessionClosed(final IoSession session) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            try {
                client.disconnect(true, type == ServerType.CASHSHOP ? true : false);
            } finally {
                session.close(false);
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(final IoSession session, final Object message) throws Exception {
        final PacketReader reader = new PacketReader((byte[]) message);
        final short header_num = reader.readShort();
        for (final ClientPacketOpcode code : ClientPacketOpcode.values()) {
            if (code.getValue() == header_num) {
                final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
                if (code.NeedsChecking()) {
                    if (!client.isLoggedIn()) {
                        return;
                    }
                }
                try {
                    handlePacket(code, reader, client, type);
                } catch (PacketFormatException ex) {
                    client.disconnect(true, true);
                }
                return;
            }
        }
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            client.sendPing();
        }
        super.sessionIdle(session, status);
    }

    public static void handlePacket(
            final ClientPacketOpcode header,
            final PacketReader reader,
            final MapleClient client,
            final ServerType type) throws PacketFormatException {
        switch (header) {
            case PONG:
                client.pongReceived();
                break;
            case STRANGE_DATA:
                // Does nothing for now, HackShield's heartbeat
                break;
            case LOGIN_PASSWORD:
                CharLoginHandler.handleLogin(reader, client);
                break;
            case SERVERLIST_REQUEST:
                CharLoginHandler.handleWorldListRequest(client);
                break;
            case CHARLIST_REQUEST:
                CharLoginHandler.handleCharacterListRequest(reader, client);
                break;
            case SERVERSTATUS_REQUEST:
                CharLoginHandler.handleWorldStatusRequest(client);
                break;
            case CHECK_CHAR_NAME:
                CharLoginHandler.handleCharacterNameCheck(reader.readLengthPrefixedString(), client);
                break;
            case CREATE_CHAR:
                CharLoginHandler.handleCreateCharacter(reader, client);
                break;
            case DELETE_CHAR:
                CharLoginHandler.handleDeleteCharacter(reader, client);
                break;
            case CHAR_SELECT:
                CharLoginHandler.handleWithoutSecondPassword(reader, client);
                break;
            case AUTH_SECOND_PASSWORD:
                CharLoginHandler.handleWithSecondPassword(reader, client);
                break;
            case RSA_KEY: // Fix this somehow
                client.getSession().write(LoginPacket.StrangeDATA());
                break;
            // END OF LOGIN SERVER
            case CHANGE_CHANNEL:
                InterServerHandler.handleChannelChange(reader, client, client.getPlayer());
                break;
            case PLAYER_LOGGEDIN:
                final int playerid = reader.readInt();
                if (type == ServerType.CHANNEL) {
                    InterServerHandler.handlePlayerLoggedIn(playerid, client);
                } else {
                    CashShopOperation.enterCashShop(playerid, client);
                }
                break;
            case ENTER_CASH_SHOP:
                InterServerHandler.handleEnterCashShop(reader, client, client.getPlayer());
                break;
            case ENTER_MTS:
                InterServerHandler.handleEnterMTS(client);
                break;
            case MOVE_PLAYER:
                PlayerHandler.handleMovePlayer(reader, client, client.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                reader.skip(4);
                PlayerHandler.handleCharacterInfoRequest(reader.readInt(), client, client.getPlayer());
                break;
            case CLOSE_RANGE_ATTACK:
                PlayerHandler.handleMeleeAttack(reader, client, client.getPlayer());
                break;
            case RANGED_ATTACK:
                PlayerHandler.handleRangedAttack(reader, client, client.getPlayer());
                break;
            case MAGIC_ATTACK:
                PlayerHandler.MagicDamage(reader, client, client.getPlayer());
                break;
            case SPECIAL_MOVE:
                PlayerHandler.handleSpecialMove(reader, client, client.getPlayer());
                break;
            case PASSIVE_ENERGY:
                break;
            case FACE_EXPRESSION:
                PlayerHandler.handleFaceExpression(reader.readInt(), client.getPlayer());
                break;
            case TAKE_DAMAGE:
                PlayerHandler.handleTakeDamage(reader, client, client.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.handleHealthRegeneration(reader, client.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.handleCancelBuff(reader.readInt(), client.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.handleCancelItemEffect(reader.readInt(), client.getPlayer());
                break;
            case USE_CHAIR:
                PlayerHandler.handleUseChair(reader.readInt(), client, client.getPlayer());
                break;
            case CANCEL_CHAIR:
                PlayerHandler.handleCancelChair(reader.readShort(), client, client.getPlayer());
                break;
            case USE_ITEMEFFECT:
                PlayerHandler.handleUseItemEffect(reader.readInt(), client, client.getPlayer());
                break;
            case SKILL_EFFECT:
                PlayerHandler.handleSkillEffect(reader, client.getPlayer());
                break;
            case MESO_DROP:
                reader.skip(4);
                PlayerHandler.handleMesoDrop(reader.readInt(), client.getPlayer());
                break;
            case WHEEL_OF_FORTUNE:
                PlayerHandler.handleWheelOfFortuneEffect(reader.readInt(), client.getPlayer());
                break;
            case MONSTER_BOOK_COVER:
                PlayerHandler.handleChangeMonsterBookCover(reader.readInt(), client, client.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.HandleChangeKeymap(reader, client.getPlayer());
                break;
            case CHANGE_MAP:
                if (type == ServerType.CHANNEL) {
                    PlayerHandler.handleChangeMap(reader, client, client.getPlayer());
                } else {
                    CashShopOperation.leaveCashShop(reader, client, client.getPlayer());
                }
                break;
            case CHANGE_MAP_SPECIAL:
                reader.skip(1);
                PlayerHandler.handleChangeMapSpecial(reader.readLengthPrefixedString(), client, client.getPlayer());
                break;
            case USE_INNER_PORTAL:
                reader.skip(1);
                PlayerHandler.handleUseInnerPortal(reader, client, client.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.handleTeleportRockAddMap(reader, client, client.getPlayer());
                break;
            case ARAN_COMBO:
                PlayerHandler.handleAranCombo(client, client.getPlayer());
                break;
            case SKILL_MACRO:
                PlayerHandler.handleSkillMacro(reader, client.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.handleGiveFame(reader, client, client.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.handleTransformPlayer(reader, client, client.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.handleNoteAction(reader, client.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.handleUseDoor(reader, client.getPlayer());
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.handleHitReactor(reader, client);
                break;
            case CLOSE_CHALKBOARD:
                client.getPlayer().setChalkboard(null);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.handleItemMaker(reader, client);
                break;
            case ITEM_SORT:
                InventoryHandler.handleItemSort(reader, client);
                break;
            case ITEM_MOVE:
                InventoryHandler.handleItemMove(reader, client);
                break;
            case ITEM_PICKUP:
                InventoryHandler.handleItemLoot(reader, client, client.getPlayer());
                break;
            case USE_CASH_ITEM:
                InventoryHandler.handleUseCashItem(reader, client);
                break;
            case USE_ITEM:
                InventoryHandler.handleUseItem(reader, client, client.getPlayer());
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.handleUseScriptedNpcItem(reader, client, client.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.handleUseReturnScroll(reader, client, client.getPlayer());
                break;
            case USE_UPGRADE_SCROLL:
                InventoryHandler.handleUseUpgradeScroll(reader, client, client.getPlayer());
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.handleUseSummonBag(reader, client, client.getPlayer());
                break;
            case USE_TREASUER_CHEST:
                InventoryHandler.handleUseTreasureChest(reader, client, client.getPlayer());
                break;
            case USE_SKILL_BOOK:
                InventoryHandler.handleUseSkillBook(reader, client, client.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.handleUseCatchItem(reader, client, client.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.handleUseMountFood(reader, client, client.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.handleUseRewardItem(reader, client, client.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.handleHypnotizeDamage(reader, client.getPlayer());
                break;
            case MOVE_LIFE:
                MobHandler.handleMoveMonster(reader, client, client.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.handleAutoAggro(reader.readInt(), client.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.handleFriendlyDamage(reader, client.getPlayer());
                break;
            case MONSTER_BOMB:
                MobHandler.handleMonsterBomb(reader.readInt(), client.getPlayer());
                break;
            case NPC_SHOP:
                NpcHandler.handleNpcShop(reader, client, client.getPlayer());
                break;
            case NPC_TALK:
                NpcHandler.handleNpcTalk(reader, client, client.getPlayer());
                break;
            case NPC_TALK_MORE:
                NpcHandler.handleNpcTalkMore(reader, client);
                break;
            case NPC_ACTION:
                NpcHandler.handleNpcAnimation(reader, client);
                break;
            case QUEST_ACTION:
                NpcHandler.handleQuestAction(reader, client, client.getPlayer());
                break;
            case STORAGE:
                NpcHandler.handleStorage(reader, client, client.getPlayer());
                break;
            case GENERAL_CHAT:
                reader.skip(4);
                ChatHandler.handleGeneralChat(reader.readLengthPrefixedString(), reader.readByte(), client, client.getPlayer());
                break;
            case PARTYCHAT:
                ChatHandler.handlePartyChat(reader, client, client.getPlayer());
                break;
            case WHISPER:
                ChatHandler.handleWhisper(reader, client);
                break;
            case MESSENGER:
                ChatHandler.handleMessenger(reader, client);
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.handleAutoAssignAbilityPoints(reader, client, client.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.handleDistributeAbilityPoints(reader, client, client.getPlayer());
                break;
            case DISTRIBUTE_SP:
                reader.skip(4);
                StatsHandling.handleDistributeSkillPoints(reader.readInt(), client, client.getPlayer());
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.handlePlayerInteraction(reader, client, client.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.handleGuildOperation(reader, client);
                break;
            case DENY_GUILD_REQUEST:
                reader.skip(1);
                GuildHandler.handleDenyGuildInvitation(reader.readLengthPrefixedString(), client);
                break;
            case ALLIANCE_OPERATION:
                AllianceHandler.handleAllianceOperation(reader, client);
                break;
            case BBS_OPERATION:
                BbsHandler.handleBbsOperatopn(reader, client);
                break;
            case REQUEST_FAMILY:
                FamilyHandler.handleFamilyRequest(reader);
                break;
            case PARTY_OPERATION:
                PartyHandler.handlePartyOperation(reader, client);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.handleDenyPartyInvitation(reader, client);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.handleBuddyOperation(reader, client);
                break;
            case CYGNUS_SUMMON:
                UserInterfaceHandler.handleNpcRequestCygnusSummon(client);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.handleShipObjectRequest(reader.readInt(), client);
                break;
            case BUY_CS_ITEM:
                CashShopOperation.handleBuyCashItem(reader, client, client.getPlayer());
                break;
            case CS_UPDATE:
                CashShopOperation.handleCashShopUpdate(client, client.getPlayer());
                break;
            case DAMAGE_SUMMON:
                reader.skip(4);
                SummonHandler.handleSummonDamage(reader, client.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.handleSummonMove(reader, client.getPlayer());
                break;
            case SUMMON_ATTACK:
                SummonHandler.handleSummonAttack(reader, client, client.getPlayer());
                break;
            case SPAWN_PET:
                PetHandler.handleSpawnPet(reader, client, client.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.handleMovePet(reader, client.getPlayer());
                break;
            case PET_CHAT:
                PetHandler.handlePetChat(reader.readInt(), reader.readShort(), reader.readLengthPrefixedString(), client.getPlayer());
                break;
            case PET_COMMAND:
                PetHandler.handlePetCommand(reader, client, client.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.handlePetFood(reader, client, client.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.handlePetLoot(reader, client, client.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.handlePetAutoPotion(reader, client, client.getPlayer());
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.handleMonsterCarnival(reader, client);
                break;
            case DUEY_ACTION:
                DueyHandler.handleDueyOperation(reader, client);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.handleUseHiredMerchant(reader, client);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.handleMerchantItemStore(reader, client);
                break;
            case CANCEL_DEBUFF:
                // Ignore for now
                break;
            case MAPLETV:
                // ignore, not done
                break;
            case MOVE_DRAGON:
                SummonHandler.handleMoveDragon(reader, client.getPlayer());
                break;
            default:
                //		System.out.println("[UNHANDLED] ["+header.toString()+"] found");
                break;
        }
    }
}
