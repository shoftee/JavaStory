/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server.handling;

import org.javastory.client.ChannelClient;
import handling.ClientPacketOpcode;
import handling.channel.handler.BbsHandler;
import handling.channel.handler.BuddyListHandler;
import handling.channel.handler.ChatHandler;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.FamilyHandler;
import handling.channel.handler.GuildHandler;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.InterServerHandler;
import handling.channel.handler.InventoryHandler;
import handling.channel.handler.ItemMakerHandler;
import handling.channel.handler.MobHandler;
import handling.channel.handler.MonsterCarnivalHandler;
import handling.channel.handler.NpcHandler;
import handling.channel.handler.PartyHandler;
import handling.channel.handler.PetHandler;
import handling.channel.handler.PlayerHandler;
import handling.channel.handler.PlayerInteractionHandler;
import handling.channel.handler.PlayersHandler;
import handling.channel.handler.StatsHandling;
import handling.channel.handler.SummonHandler;
import handling.channel.handler.UserInterfaceHandler;
import org.apache.mina.core.session.IoSession;
import org.javastory.client.GameClient;
import org.javastory.cryptography.AesTransform;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;

/**
 *
 * @author Tosho
 */
public final class ChannelPacketHandler extends PacketHandler {

    private int channelId = -1;

    public ChannelPacketHandler(int channelId) {
        this.channelId = channelId;

    }

    @Override
    protected GameClient createClient(AesTransform clientCrypto, AesTransform serverCrypto, IoSession session) {
        GameClient client = new ChannelClient(clientCrypto, serverCrypto, session);
        client.setChannelId(channelId);
        return client;
    }

    @Override
    protected void handlePacket(ClientPacketOpcode header, PacketReader reader, GameClient client) throws PacketFormatException {
        ChannelClient channelClient = (ChannelClient) client;        
        switch (header) {
            case PONG:
                channelClient.pongReceived();
                break;
            case CHANGE_CHANNEL:
                InterServerHandler.handleChannelChange(reader, channelClient, channelClient.getPlayer());
                break;
            case PLAYER_LOGGEDIN:
                final int playerid = reader.readInt();
                InterServerHandler.handlePlayerLoggedIn(playerid, channelClient);
                break;
            case ENTER_MTS:
                InterServerHandler.handleEnterMTS(channelClient);
                break;
            case MOVE_PLAYER:
                PlayerHandler.handleMovePlayer(reader, channelClient, channelClient.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                reader.skip(4);
                PlayerHandler.handleCharacterInfoRequest(reader.readInt(), channelClient, channelClient.getPlayer());
                break;
            case CLOSE_RANGE_ATTACK:
                PlayerHandler.handleMeleeAttack(reader, channelClient, channelClient.getPlayer());
                break;
            case RANGED_ATTACK:
                PlayerHandler.handleRangedAttack(reader, channelClient, channelClient.getPlayer());
                break;
            case MAGIC_ATTACK:
                PlayerHandler.MagicDamage(reader, channelClient, channelClient.getPlayer());
                break;
            case SPECIAL_MOVE:
                PlayerHandler.handleSpecialMove(reader, channelClient, channelClient.getPlayer());
                break;
            case PASSIVE_ENERGY:
                break;
            case FACE_EXPRESSION:
                PlayerHandler.handleFaceExpression(reader.readInt(), channelClient.getPlayer());
                break;
            case TAKE_DAMAGE:
                PlayerHandler.handleTakeDamage(reader, channelClient, channelClient.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.handleHealthRegeneration(reader, channelClient.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.handleCancelBuff(reader.readInt(), channelClient.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.handleCancelItemEffect(reader.readInt(), channelClient.getPlayer());
                break;
            case USE_CHAIR:
                PlayerHandler.handleUseChair(reader.readInt(), channelClient, channelClient.getPlayer());
                break;
            case CANCEL_CHAIR:
                PlayerHandler.handleCancelChair(reader.readShort(), channelClient, channelClient.getPlayer());
                break;
            case USE_ITEMEFFECT:
                PlayerHandler.handleUseItemEffect(reader.readInt(), channelClient, channelClient.getPlayer());
                break;
            case SKILL_EFFECT:
                PlayerHandler.handleSkillEffect(reader, channelClient.getPlayer());
                break;
            case MESO_DROP:
                reader.skip(4);
                PlayerHandler.handleMesoDrop(reader.readInt(), channelClient.getPlayer());
                break;
            case WHEEL_OF_FORTUNE:
                PlayerHandler.handleWheelOfFortuneEffect(reader.readInt(), channelClient.getPlayer());
                break;
            case MONSTER_BOOK_COVER:
                PlayerHandler.handleChangeMonsterBookCover(reader.readInt(), channelClient, channelClient.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.HandleChangeKeymap(reader, channelClient.getPlayer());
                break;
            case CHANGE_MAP:
                PlayerHandler.handleChangeMap(reader, channelClient, channelClient.getPlayer());
                break;
            case CHANGE_MAP_SPECIAL:
                reader.skip(1);
                PlayerHandler.handleChangeMapSpecial(reader.readLengthPrefixedString(), channelClient, channelClient.getPlayer());
                break;
            case USE_INNER_PORTAL:
                reader.skip(1);
                PlayerHandler.handleUseInnerPortal(reader, channelClient, channelClient.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.handleTeleportRockAddMap(reader, channelClient, channelClient.getPlayer());
                break;
            case ARAN_COMBO:
                PlayerHandler.handleAranCombo(channelClient, channelClient.getPlayer());
                break;
            case SKILL_MACRO:
                PlayerHandler.handleSkillMacro(reader, channelClient.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.handleGiveFame(reader, channelClient, channelClient.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.handleTransformPlayer(reader, channelClient, channelClient.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.handleNoteAction(reader, channelClient.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.handleUseDoor(reader, channelClient.getPlayer());
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.handleHitReactor(reader, channelClient);
                break;
            case CLOSE_CHALKBOARD:
                channelClient.getPlayer().setChalkboard(null);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.handleItemMaker(reader, channelClient);
                break;
            case ITEM_SORT:
                InventoryHandler.handleItemSort(reader, channelClient);
                break;
            case ITEM_MOVE:
                InventoryHandler.handleItemMove(reader, channelClient);
                break;
            case ITEM_PICKUP:
                InventoryHandler.handleItemLoot(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_CASH_ITEM:
                InventoryHandler.handleUseCashItem(reader, channelClient);
                break;
            case USE_ITEM:
                InventoryHandler.handleUseItem(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.handleUseScriptedNpcItem(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.handleUseReturnScroll(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_UPGRADE_SCROLL:
                InventoryHandler.handleUseUpgradeScroll(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.handleUseSummonBag(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_TREASUER_CHEST:
                InventoryHandler.handleUseTreasureChest(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_SKILL_BOOK:
                InventoryHandler.handleUseSkillBook(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.handleUseCatchItem(reader, channelClient, channelClient.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.handleUseMountFood(reader, channelClient, channelClient.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.handleUseRewardItem(reader, channelClient, channelClient.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.handleHypnotizeDamage(reader, channelClient.getPlayer());
                break;
            case MOVE_LIFE:
                MobHandler.handleMoveMonster(reader, channelClient, channelClient.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.handleAutoAggro(reader.readInt(), channelClient.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.handleFriendlyDamage(reader, channelClient.getPlayer());
                break;
            case MONSTER_BOMB:
                MobHandler.handleMonsterBomb(reader.readInt(), channelClient.getPlayer());
                break;
            case NPC_SHOP:
                NpcHandler.handleNpcShop(reader, channelClient, channelClient.getPlayer());
                break;
            case NPC_TALK:
                NpcHandler.handleNpcTalk(reader, channelClient, channelClient.getPlayer());
                break;
            case NPC_TALK_MORE:
                NpcHandler.handleNpcTalkMore(reader, channelClient);
                break;
            case NPC_ACTION:
                NpcHandler.handleNpcAnimation(reader, channelClient);
                break;
            case QUEST_ACTION:
                NpcHandler.handleQuestAction(reader, channelClient, channelClient.getPlayer());
                break;
            case STORAGE:
                NpcHandler.handleStorage(reader, channelClient, channelClient.getPlayer());
                break;
            case GENERAL_CHAT:
                reader.skip(4);
                ChatHandler.handleGeneralChat(reader.readLengthPrefixedString(), reader.readByte(), channelClient, channelClient.getPlayer());
                break;
            case PARTYCHAT:
                ChatHandler.handlePartyChat(reader, channelClient, channelClient.getPlayer());
                break;
            case WHISPER:
                ChatHandler.handleWhisper(reader, channelClient);
                break;
            case MESSENGER:
                ChatHandler.handleMessenger(reader, channelClient);
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.handleAutoAssignAbilityPoints(reader, channelClient, channelClient.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.handleDistributeAbilityPoints(reader, channelClient, channelClient.getPlayer());
                break;
            case DISTRIBUTE_SP:
                reader.skip(4);
                StatsHandling.handleDistributeSkillPoints(reader.readInt(), channelClient, channelClient.getPlayer());
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.handlePlayerInteraction(reader, channelClient, channelClient.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.handleGuildOperation(reader, channelClient);
                break;
            case DENY_GUILD_REQUEST:
                reader.skip(1);
                GuildHandler.handleDenyGuildInvitation(reader.readLengthPrefixedString(), channelClient);
                break;
            case BBS_OPERATION:
                BbsHandler.handleBbsOperatopn(reader, channelClient);
                break;
            case REQUEST_FAMILY:
                FamilyHandler.handleFamilyRequest(reader);
                break;
            case PARTY_OPERATION:
                PartyHandler.handlePartyOperation(reader, channelClient);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.handleDenyPartyInvitation(reader, channelClient);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.handleBuddyOperation(reader, channelClient);
                break;
            case CYGNUS_SUMMON:
                UserInterfaceHandler.handleNpcRequestCygnusSummon(channelClient);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.handleShipObjectRequest(reader.readInt(), channelClient);
                break;
            case DAMAGE_SUMMON:
                reader.skip(4);
                SummonHandler.handleSummonDamage(reader, channelClient.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.handleSummonMove(reader, channelClient.getPlayer());
                break;
            case SUMMON_ATTACK:
                SummonHandler.handleSummonAttack(reader, channelClient, channelClient.getPlayer());
                break;
            case SPAWN_PET:
                PetHandler.handleSpawnPet(reader, channelClient, channelClient.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.handleMovePet(reader, channelClient.getPlayer());
                break;
            case PET_CHAT:
                PetHandler.handlePetChat(reader.readInt(), reader.readShort(), reader.readLengthPrefixedString(), channelClient.getPlayer());
                break;
            case PET_COMMAND:
                PetHandler.handlePetCommand(reader, channelClient, channelClient.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.handlePetFood(reader, channelClient, channelClient.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.handlePetLoot(reader, channelClient, channelClient.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.handlePetAutoPotion(reader, channelClient, channelClient.getPlayer());
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.handleMonsterCarnival(reader, channelClient);
                break;
            case DUEY_ACTION:
                DueyHandler.handleDueyOperation(reader, channelClient);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.handleUseHiredMerchant(reader, channelClient);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.handleMerchantItemStore(reader, channelClient);
                break;
            case CANCEL_DEBUFF:
                // Ignore for now
                break;
            case MAPLETV:
                // ignore, not done
                break;
            case MOVE_DRAGON:
                SummonHandler.handleMoveDragon(reader, channelClient.getPlayer());
                break;
            default:
                //		System.out.println("[UNHANDLED] ["+header.toString()+"] found");
                break;
        }
    }
}
