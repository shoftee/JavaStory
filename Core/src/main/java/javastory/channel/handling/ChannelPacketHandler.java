/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.channel.handling;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.client.GameClient;
import javastory.cryptography.AesTransform;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.server.handling.ClientPacketOpcode;
import javastory.server.handling.PacketHandler;

import org.apache.mina.core.session.IoSession;

/**
 * 
 * @author shoftee
 */
public final class ChannelPacketHandler extends PacketHandler {

	private int channelId = -1;

	public ChannelPacketHandler(final int channelId) {
		this.channelId = channelId;

	}

	@Override
	protected GameClient createClient(final AesTransform clientCrypto, final AesTransform serverCrypto, final IoSession session) {
		final GameClient client = new ChannelClient(clientCrypto, serverCrypto, session);
		client.setChannelId(this.channelId);
		return client;
	}

	@Override
	protected void handlePacket(final ClientPacketOpcode header, final PacketReader reader, final GameClient client) throws PacketFormatException {
		final ChannelClient channelClient = (ChannelClient) client;
		final ChannelCharacter player = channelClient.getPlayer();
		switch (header) {
		case PONG:
			channelClient.pongReceived();
			break;
		case CHANGE_CHANNEL:
			InterServerHandler.handleChannelChange(reader, channelClient, player);
			break;
		case PLAYER_LOGGEDIN:
			final int playerid = reader.readInt();
			InterServerHandler.handlePlayerLoggedIn(playerid, channelClient);
			break;
		case ENTER_MTS:
			InterServerHandler.handleEnterMTS(channelClient);
			break;
		case MOVE_PLAYER:
			PlayerHandler.handleMovePlayer(reader, channelClient, player);
			break;
		case CHAR_INFO_REQUEST:
			reader.skip(4);
			PlayerHandler.handleCharacterInfoRequest(reader.readInt(), channelClient, player);
			break;
		case CLOSE_RANGE_ATTACK:
			PlayerHandler.handleMeleeAttack(reader, channelClient, player);
			break;
		case RANGED_ATTACK:
			PlayerHandler.handleRangedAttack(reader, channelClient, player);
			break;
		case MAGIC_ATTACK:
			PlayerHandler.MagicDamage(reader, channelClient, player);
			break;
		case SPECIAL_MOVE:
			PlayerHandler.handleSpecialMove(reader, channelClient, player);
			break;
		case PASSIVE_ENERGY:
			break;
		case FACE_EXPRESSION:
			PlayerHandler.handleFaceExpression(reader.readInt(), player);
			break;
		case TAKE_DAMAGE:
			PlayerHandler.handleTakeDamage(reader, channelClient, player);
			break;
		case HEAL_OVER_TIME:
			PlayerHandler.handleHealthRegeneration(reader, player);
			break;
		case CANCEL_BUFF:
			PlayerHandler.handleCancelBuff(reader.readInt(), player);
			break;
		case CANCEL_ITEM_EFFECT:
			PlayerHandler.handleCancelItemEffect(reader.readInt(), player);
			break;
		case USE_CHAIR:
			PlayerHandler.handleUseChair(reader.readInt(), channelClient, player);
			break;
		case CANCEL_CHAIR:
			PlayerHandler.handleCancelChair(reader.readShort(), channelClient, player);
			break;
		case USE_ITEMEFFECT:
			PlayerHandler.handleUseItemEffect(reader.readInt(), channelClient, player);
			break;
		case SKILL_EFFECT:
			PlayerHandler.handleSkillEffect(reader, player);
			break;
		case MESO_DROP:
			reader.skip(4);
			PlayerHandler.handleMesoDrop(reader.readInt(), player);
			break;
		case WHEEL_OF_FORTUNE:
			PlayerHandler.handleWheelOfFortuneEffect(reader.readInt(), player);
			break;
		case MONSTER_BOOK_COVER:
			PlayerHandler.handleChangeMonsterBookCover(reader.readInt(), channelClient, player);
			break;
		case CHANGE_KEYMAP:
			PlayerHandler.HandleChangeKeymap(reader, player);
			break;
		case CHANGE_MAP:
			PlayerHandler.handleChangeMap(reader, channelClient, player);
			break;
		case CHANGE_MAP_SPECIAL:
			reader.skip(1);
			PlayerHandler.handleChangeMapSpecial(reader.readLengthPrefixedString(), channelClient, player);
			break;
		case USE_INNER_PORTAL:
			reader.skip(1);
			PlayerHandler.handleUseInnerPortal(reader, channelClient, player);
			break;
		case TROCK_ADD_MAP:
			PlayerHandler.handleTeleportRockAddMap(reader, channelClient, player);
			break;
		case ARAN_COMBO:
			PlayerHandler.handleAranCombo(channelClient, player);
			break;
		case SKILL_MACRO:
			PlayerHandler.handleSkillMacro(reader, player);
			break;
		case GIVE_FAME:
			PlayersHandler.handleGiveFame(reader, channelClient, player);
			break;
		case TRANSFORM_PLAYER:
			PlayersHandler.handleTransformPlayer(reader, channelClient, player);
			break;
		case NOTE_ACTION:
			PlayersHandler.handleNoteAction(reader, player);
			break;
		case USE_DOOR:
			PlayersHandler.handleUseDoor(reader, player);
			break;
		case DAMAGE_REACTOR:
			PlayersHandler.handleHitReactor(reader, channelClient);
			break;
		case CLOSE_CHALKBOARD:
			player.setChalkboard(null);
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
			InventoryHandler.handleItemLoot(reader, channelClient, player);
			break;
		case USE_CASH_ITEM:
			InventoryHandler.handleUseCashItem(reader, channelClient);
			break;
		case USE_ITEM:
			InventoryHandler.handleUseItem(reader, channelClient, player);
			break;
		case USE_SCRIPTED_NPC_ITEM:
			InventoryHandler.handleUseScriptedNpcItem(reader, channelClient, player);
			break;
		case USE_RETURN_SCROLL:
			InventoryHandler.handleUseReturnScroll(reader, channelClient, player);
			break;
		case USE_UPGRADE_SCROLL:
			InventoryHandler.handleUseUpgradeScroll(reader, channelClient, player);
			break;
		case USE_SUMMON_BAG:
			InventoryHandler.handleUseSummonBag(reader, channelClient, player);
			break;
		case USE_TREASUER_CHEST:
			InventoryHandler.handleUseTreasureChest(reader, channelClient, player);
			break;
		case USE_SKILL_BOOK:
			InventoryHandler.handleUseSkillBook(reader, channelClient, player);
			break;
		case USE_CATCH_ITEM:
			InventoryHandler.handleUseCatchItem(reader, channelClient, player);
			break;
		case USE_MOUNT_FOOD:
			InventoryHandler.handleUseMountFood(reader, channelClient, player);
			break;
		case REWARD_ITEM:
			InventoryHandler.handleUseRewardItem(reader, channelClient, player);
			break;
		case HYPNOTIZE_DMG:
			MobHandler.handleHypnotizeDamage(reader, player);
			break;
		case MOVE_LIFE:
			MobHandler.handleMoveMonster(reader, channelClient, player);
			break;
		case AUTO_AGGRO:
			MobHandler.handleAutoAggro(reader.readInt(), player);
			break;
		case FRIENDLY_DAMAGE:
			MobHandler.handleFriendlyDamage(reader, player);
			break;
		case MONSTER_BOMB:
			MobHandler.handleMonsterBomb(reader.readInt(), player);
			break;
		case NPC_SHOP:
			NpcHandler.handleNpcShop(reader, channelClient, player);
			break;
		case NPC_TALK:
			NpcHandler.handleNpcTalk(reader, channelClient, player);
			break;
		case NPC_TALK_MORE:
			NpcHandler.handleNpcTalkMore(reader, channelClient);
			break;
		case NPC_ACTION:
			NpcHandler.handleNpcAnimation(reader, channelClient);
			break;
		case QUEST_ACTION:
			NpcHandler.handleQuestAction(reader, channelClient, player);
			break;
		case STORAGE:
			NpcHandler.handleStorage(reader, channelClient, player);
			break;
		case GENERAL_CHAT:
			reader.skip(4);
			ChatHandler.handleGeneralChat(reader.readLengthPrefixedString(), reader.readByte(), channelClient, player);
			break;
		case PARTYCHAT:
			ChatHandler.handleGroupChat(reader, channelClient, player);
			break;
		case WHISPER:
			ChatHandler.handleWhisper(reader, channelClient);
			break;
		case MESSENGER:
			ChatHandler.handleMessenger(reader, channelClient);
			break;
		case AUTO_ASSIGN_AP:
			StatsHandling.handleAutoAssignAbilityPoints(reader, channelClient, player);
			break;
		case DISTRIBUTE_AP:
			StatsHandling.handleDistributeAbilityPoints(reader, channelClient, player);
			break;
		case DISTRIBUTE_SP:
			reader.skip(4);
			StatsHandling.handleDistributeSkillPoints(reader.readInt(), channelClient, player);
			break;
		case PLAYER_INTERACTION:
			PlayerInteractionHandler.handlePlayerInteraction(reader, channelClient, player);
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
			// TODO: Family stuff.
			// FamilyHandler.handleFamilyRequest(reader);
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
			SummonHandler.handleSummonDamage(reader, player);
			break;
		case MOVE_SUMMON:
			SummonHandler.handleSummonMove(reader, player);
			break;
		case SUMMON_ATTACK:
			SummonHandler.handleSummonAttack(reader, channelClient, player);
			break;
		case SPAWN_PET:
			PetHandler.handleSpawnPet(reader, channelClient, player);
			break;
		case MOVE_PET:
			PetHandler.handleMovePet(reader, player);
			break;
		case PET_CHAT:
			PetHandler.handlePetChat(reader.readInt(), reader.readShort(), reader.readLengthPrefixedString(), player);
			break;
		case PET_COMMAND:
			PetHandler.handlePetCommand(reader, channelClient, player);
			break;
		case PET_FOOD:
			PetHandler.handlePetFood(reader, channelClient, player);
			break;
		case PET_LOOT:
			InventoryHandler.handlePetLoot(reader, channelClient, player);
			break;
		case PET_AUTO_POT:
			PetHandler.handlePetAutoPotion(reader, channelClient, player);
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
			SummonHandler.handleMoveDragon(reader, player);
			break;
		default:
			// System.out.println("[UNHANDLED] ["+header.toString()+"] found");
			break;
		}
	}
}
