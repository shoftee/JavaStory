package javastory.tools.packets;

import java.awt.Point;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.DiseaseValue;
import javastory.channel.DoorInfo;
import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.GuildSummary;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.client.BuddyListEntry;
import javastory.channel.client.BuffStat;
import javastory.channel.client.KeyLayout;
import javastory.channel.client.MemberRank;
import javastory.channel.client.Mount;
import javastory.channel.client.Pet;
import javastory.channel.client.Ring;
import javastory.channel.client.SkillMacro;
import javastory.channel.handling.AttackPair;
import javastory.channel.life.MobSkill;
import javastory.channel.life.Npc;
import javastory.channel.life.SummonAttackEntry;
import javastory.channel.maps.Dragon;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapItem;
import javastory.channel.maps.Mist;
import javastory.channel.maps.Reactor;
import javastory.channel.maps.Summon;
import javastory.channel.movement.LifeMovementFragment;
import javastory.channel.packet.PacketHelper;
import javastory.channel.server.StatEffect;
import javastory.channel.server.Trade;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.Inventory;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.ItemType;
import javastory.game.Jobs;
import javastory.game.ScrollResult;
import javastory.game.Stat;
import javastory.game.StatValue;
import javastory.game.data.ItemInfoProvider;
import javastory.game.data.NpcInfo;
import javastory.game.quest.QuestStatus;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.BuffStatValue;
import javastory.server.DueyActions;
import javastory.server.ShopItem;
import javastory.server.channel.GuildRankingInfo;
import javastory.server.handling.ServerConstants;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.BitTools;
import javastory.tools.FameResponse;
import javastory.tools.FiletimeUtil;
import javastory.tools.HexTool;
import javastory.tools.Randomizer;
import javastory.tools.StringUtil;
import javastory.world.core.PartyOperation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class ChannelPackets {

	private final static byte[] CHAR_INFO_MAGIC = new byte[] { (byte) 0xFF, (byte) 0xC9, (byte) 0x9A, 0x3B };
	public final static List<StatValue> EMPTY_STATUPDATE = Collections.emptyList();

	private ChannelPackets() {
	}

	public static GamePacket getChannelChange(final int port) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CHANGE_CHANNEL.getValue());
		builder.writeAsByte(1);
		builder.writeBytes(ServerConstants.Server_IP);
		builder.writeAsShort(port);

		return builder.getPacket();
	}

	public static GamePacket getCharInfo(final ChannelCharacter chr) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WARP_TO_MAP.getValue());
		builder.writeLong(chr.getClient().getChannelId() - 1);
		builder.writeAsByte(1);
		builder.writeAsByte(1);
		builder.writeZeroBytes(2);
		chr.getRandomStream().connectData(builder); // Random number generator
		builder.writeLong(-1);
		builder.writeAsByte(0);
		GameCharacterPacket.addCharStats(builder, chr);
		builder.writeAsByte(chr.getBuddyList().getCapacity());
		if (chr.getBlessOfFairyOrigin() != null) {
			builder.writeAsByte(1);
			builder.writeLengthPrefixedString(chr.getBlessOfFairyOrigin());
		} else {
			builder.writeAsByte(0);
		}
		PacketHelper.addInventoryInfo(builder, chr);
		PacketHelper.addSkillInfo(builder, chr);
		PacketHelper.addCoolDownInfo(builder, chr);
		PacketHelper.addQuestInfo(builder, chr);
		PacketHelper.addRingInfo(builder, chr);
		PacketHelper.addRocksInfo(builder, chr);
		PacketHelper.addMonsterBookInfo(builder, chr);
		chr.writeQuestInfoPacket(builder); // for every questinfo: int16_t questid,
										// string questdata
		builder.writeAsShort(0); // PQ rank
		builder.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

		return builder.getPacket();
	}

	public static GamePacket enableActions() {
		return updatePlayerStats(EMPTY_STATUPDATE, true, 0);
	}

	public static GamePacket updatePlayerStats(final List<StatValue> stats, final int evan) {
		return updatePlayerStats(stats, false, evan);
	}

	public static GamePacket updatePlayerStats(final List<StatValue> stats, final boolean itemReaction, final int evan) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
		builder.writeAsByte(itemReaction);
		final int updateMask = 0;
		final EnumSet<Stat> mask = EnumSet.noneOf(Stat.class);
		for (final StatValue newValue : stats) {
			mask.add(newValue.stat);
		}
		final List<StatValue> mystats = stats;
		if (mystats.size() > 1) {
			Collections.sort(mystats, new Comparator<StatValue>() {

				@Override
				public int compare(final StatValue o1, final StatValue o2) {
					final int val1 = o1.stat.getValue();
					final int val2 = o2.stat.getValue();
					return val1 < val2 ? -1 : val1 == val2 ? 0 : 1;
				}
			});
		}
		builder.writeInt(updateMask);
		int value;
		for (final StatValue newStatValue : mystats) {
			value = newStatValue.value;
			if (value >= 1) {
				if (value == 0x1) {
					builder.writeAsShort(value);
				} else if (value <= 0x4) {
					builder.writeInt(value);
				} else if (value < 0x20) {
					builder.writeAsByte(value);
				} else if (value == 0x8000) { // availablesp
					if (evan == 2001 || evan >= 2200 && evan <= 2218) {
						throw new UnsupportedOperationException("Evan wrong updating");
					} else {
						builder.writeAsShort(value);
					}
				} else if (value < 0xFFFF) {
					builder.writeAsShort(value);
				} else {
					builder.writeInt(value);
				}
			}
		}
		return builder.getPacket();
	}

	public static GamePacket updateSp(final ChannelCharacter chr, final boolean itemReaction) { // this
																							// will
																							// do..
		return updateSp(chr, itemReaction, false);
	}

	public static GamePacket updateSp(final ChannelCharacter chr, final boolean itemReaction, final boolean overrideJob) { // this
																														// will
																														// do..
		final PacketBuilder builder = new PacketBuilder();
		builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
		builder.writeAsByte(itemReaction);
		builder.writeInt(0x8000);
		if (overrideJob || Jobs.isEvan(chr.getJobId())) {
			builder.writeAsByte(chr.getRemainingSpSize());
			for (int i = 0; i < chr.getRemainingSps().length; i++) {
				if (chr.getRemainingSp(i) > 0) {
					builder.writeAsByte(i + 1);
					builder.writeAsByte(chr.getRemainingSp(i));
				}
			}
		} else {
			builder.writeAsShort(chr.getRemainingSp());
		}
		return builder.getPacket();

	}

	public static GamePacket getWarpToMap(final GameMap to, final int spawnPoint, final ChannelCharacter chr) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WARP_TO_MAP.getValue());
		builder.writeLong(chr.getClient().getChannelId() - 1);
		builder.writeInt(0x2); // count
		builder.writeAsByte(0);
		builder.writeInt(to.getId());
		builder.writeAsByte(spawnPoint);
		builder.writeAsShort(chr.getStats().getHp());
		builder.writeAsByte(0);
		builder.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

		return builder.getPacket();
	}

	public static GamePacket instantMapWarp(final byte portal) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CURRENT_MAP_WARP.getValue());
		builder.writeAsByte(0);
		builder.writeByte(portal); // 6

		return builder.getPacket();
	}

	public static GamePacket spawnPortal(final int townId, final int targetId, final Point pos) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_PORTAL.getValue());
		builder.writeInt(townId);
		builder.writeInt(targetId);
		builder.writeAsByte(2311002);
		if (pos != null) {
			builder.writeVector(pos);
		}

		return builder.getPacket();
	}

	public static GamePacket spawnDoor(final int oid, final Point pos, final boolean town) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_DOOR.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(town);
		builder.writeInt(oid);
		builder.writeVector(pos);

		return builder.getPacket();
	}

	public static GamePacket removeDoor(final int oid, final boolean isTown) {
		final PacketBuilder builder = new PacketBuilder();

		if (isTown) {
			builder.writeAsShort(ServerPacketOpcode.SPAWN_PORTAL.getValue());
			builder.writeInt(999999999);
			builder.writeLong(999999999);
		} else {
			builder.writeAsShort(ServerPacketOpcode.REMOVE_DOOR.getValue());
			builder.writeAsByte(0);
			builder.writeInt(oid);
		}

		return builder.getPacket();
	}

	public static GamePacket spawnSummon(final Summon summon, final int skillLevel, final boolean animated) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_SUMMON.getValue());
		builder.writeInt(summon.getOwnerId());
		builder.writeInt(summon.getObjectId());
		builder.writeInt(summon.getSkill());
		builder.writeAsByte(100); // owner LEVEL
		builder.writeAsByte(skillLevel);
		builder.writeVector(summon.getPosition());
		builder.writeAsByte(4);
		builder.writeAsShort(summon.isPuppet() ? 179 : 14);
		// 0 = don't move, 1 = follow (4th mage summons?), 2/4 = only tele
		// follow, 3 = bird follow
		builder.writeAsByte(summon.getMovementType().getValue());
		// 0 = Summon can't attack - but puppets don't attack with 1 either =.=
		builder.writeAsByte(!summon.isPuppet());
		builder.writeAsShort(!animated);

		return builder.getPacket();
	}

	public static GamePacket removeSummon(final Summon summon, final boolean animated) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_SUMMON.getValue());
		builder.writeInt(summon.getOwnerId());
		builder.writeInt(summon.getObjectId());
		builder.writeAsByte(animated ? 4 : 1);

		return builder.getPacket();
	}

	public static GamePacket getRelogResponse() {
		final PacketBuilder builder = new PacketBuilder(3);

		builder.writeAsShort(ServerPacketOpcode.RELOG_RESPONSE.getValue());
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	/**
	 * Possible values for <code>type</code>:<br>
	 * 1: You cannot move that channel. Please try again later.<br>
	 * 2: You cannot go into the cash shop. Please try again later.<br>
	 * 3: The Item-Trading shop is currently unavailable, please try again
	 * later.<br>
	 * 4: You cannot go into the trade shop, due to the limitation of user
	 * count.<br>
	 * 5: You do not meet the minimum level requirement to access the Trade
	 * Shop.<br>
	 * 
	 * @param type
	 *            The type
	 * @return The "block" packet.
	 */
	public static GamePacket serverBlocked(final int type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MTS_OPEN.getValue());
		builder.writeAsByte(type);

		return builder.getPacket();
	}

	public static GamePacket headerMessage(final String message) {
		return serverMessage(4, 0, message, true, false);
	}

	public static GamePacket serverNotice(final int type, final String message) {
		return serverMessage(type, 0, message, false, false);
	}

	public static GamePacket serverNotice(final int type, final int channel, final String message) {
		return serverMessage(type, channel, message, false, false);
	}

	public static GamePacket serverNotice(final int type, final int channel, final String message, final boolean smegaEar) {
		return serverMessage(type, channel, message, false, smegaEar);
	}

	private static GamePacket serverMessage(final int type, final int channel, final String message, final boolean servermessage, final boolean whisperEnabled) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
		// See ServerMessageType enum
		builder.writeAsByte(type);

		if (servermessage) {
			builder.writeAsByte(1);
		}
		builder.writeLengthPrefixedString(message);
		switch (type) {
		case 3:
		case 9:
		case 10:
			builder.writeAsByte(channel - 1); // channel
			builder.writeAsByte(whisperEnabled);
			break;
		case 6:
			builder.writeInt(0);
			break;
		}
		return builder.getPacket();
	}

	public static GamePacket getGachaponMega(final String name, final String message, final Item item, final byte rareness) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
		builder.writeAsByte(rareness == 2 ? 15 : 14);
		builder.writeLengthPrefixedString(name + message);
		builder.writeInt(0); // 0~3 i think | no its a 5 @ msea 1.01
		builder.writeLengthPrefixedString(name);
		PacketHelper.addItemInfo(builder, item, true, true, true);

		return builder.getPacket();
	}

	public static GamePacket tripleSmega(final List<String> message, final boolean whisperEnabled, final int channel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
		builder.writeAsByte(12);

		if (message.get(0) != null) {
			builder.writeLengthPrefixedString(message.get(0));
		}
		builder.writeAsByte(message.size());
		for (int i = 1; i < message.size(); i++) {
			if (message.get(i) != null) {
				builder.writeLengthPrefixedString(message.get(i));
			}
		}
		builder.writeAsByte(channel - 1);
		builder.writeAsByte(whisperEnabled);

		return builder.getPacket();
	}

	public static GamePacket getAvatarMega(final ChannelCharacter chr, final int channel, final int itemId, final String message, final boolean whisperEnabled) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.AVATAR_MEGA.getValue());
		builder.writeInt(itemId);
		builder.writeLengthPrefixedString(chr.getName());
		builder.writeLengthPrefixedString(message);
		builder.writeInt(channel - 1); // channel
		builder.writeAsByte(whisperEnabled);
		GameCharacterPacket.addCharLook(builder, chr, true);

		return builder.getPacket();
	}

	public static GamePacket itemMegaphone(final String msg, final boolean whisperEnabled, final int channel, final Item item) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
		builder.writeAsByte(8);
		builder.writeLengthPrefixedString(msg);
		builder.writeAsByte(channel - 1);
		builder.writeAsByte(whisperEnabled);

		if (item == null) {
			builder.writeAsByte(0);
		} else {
			PacketHelper.addItemInfo(builder, item, false, false, true);
		}
		return builder.getPacket();
	}

	public static GamePacket spawnNpc(final Npc life, final boolean show) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_NPC.getValue());
		builder.writeInt(life.getObjectId());
		builder.writeInt(life.getId());
		builder.writeAsShort(life.getPosition().x);
		builder.writeAsShort(life.getCy());
		builder.writeAsByte(life.getFacingDirection());
		builder.writeAsShort(life.getFoothold());
		builder.writeAsShort(life.getRx0());
		builder.writeAsShort(life.getRx1());
		builder.writeAsByte(show);

		return builder.getPacket();
	}

	public static GamePacket removeNpc(final int objectid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_NPC.getValue());
		builder.writeInt(objectid);

		return builder.getPacket();
	}

	public static GamePacket spawnNpcRequestController(final Npc life, final boolean miniMap) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
		builder.writeAsByte(1);
		builder.writeInt(life.getObjectId());
		builder.writeInt(life.getId());
		builder.writeAsShort(life.getPosition().x);
		builder.writeAsShort(life.getCy());
		builder.writeAsByte(life.getFacingDirection());
		builder.writeAsShort(life.getFoothold());
		builder.writeAsShort(life.getRx0());
		builder.writeAsShort(life.getRx1());
		builder.writeAsByte(miniMap);

		return builder.getPacket();
	}

	public static GamePacket spawnPlayerNpc(final NpcInfo npc, final int id) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_NPC.getValue());
		builder.writeAsByte(1);
		builder.writeInt(id);
		builder.writeLengthPrefixedString(npc.getName());
		builder.writeAsByte(0);
		builder.writeByte(npc.getSkin());
		builder.writeInt(npc.getFace());
		builder.writeAsByte(0);
		builder.writeInt(npc.getHair());
		final Map<Byte, Integer> equip = npc.getEquips();
		final Map<Byte, Integer> myEquip = Maps.newLinkedHashMap();
		final Map<Byte, Integer> maskedEquip = Maps.newLinkedHashMap();
		for (final byte position : equip.keySet()) {
			byte pos = (byte) (position * -1);
			if (pos < 100 && myEquip.get(pos) == null) {
				myEquip.put(pos, equip.get(position));
			} else if ((pos > 100 || pos == -128) && pos != 111) {
				pos -= 100;
				if (myEquip.get(pos) != null) {
					maskedEquip.put(pos, myEquip.get(pos));
				}
				myEquip.put(pos, equip.get(position));
			} else if (myEquip.get(pos) != null) {
				maskedEquip.put(pos, equip.get(position));
			}
		}
		for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
			builder.writeByte(entry.getKey());
			builder.writeInt(entry.getValue());
		}
		builder.writeAsByte(0xFF);
		for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
			builder.writeByte(entry.getKey());
			builder.writeInt(entry.getValue());
		}
		builder.writeAsByte(0xFF);
		final Integer cWeapon = equip.get((byte) -111);
		if (cWeapon != null) {
			builder.writeInt(cWeapon);
		} else {
			builder.writeInt(0);
		}
		for (int i = 0; i < 3; i++) {
			builder.writeInt(0);
		}

		return builder.getPacket();
	}

	public static GamePacket getChatText(final int cidfrom, final String text, final boolean whiteBG, final int show) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CHATTEXT.getValue());
		builder.writeInt(cidfrom);
		builder.writeAsByte(whiteBG);
		builder.writeLengthPrefixedString(text);
		builder.writeAsByte(show);

		return builder.getPacket();
	}

	public static GamePacket GameMaster_Func(final int value) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeBytes(HexTool.getByteArrayFromHexString("7A 00"));
		builder.writeAsByte(value);
		builder.writeZeroBytes(17);

		return builder.getPacket();
	}

	public static GamePacket testCombo(final int value) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ARAN_COMBO.getValue());
		builder.writeInt(value);

		return builder.getPacket();
	}

	public static GamePacket getPacketFromHexString(final String hex) {
		return GamePacket.wrapperOf(HexTool.getByteArrayFromHexString(hex));
	}

	public static GamePacket GainEXP_Monster(final int gain, final boolean white, final int Event_EXP, final int Wedding_EXP, final int Party_Ring_EXP,
		final int Party_EXP, final int Premium_EXP, final int Item_EXP, final int Rainbow_EXP, final int CLASS_EXP) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
		builder.writeAsByte(white);
		builder.writeInt(gain);
		builder.writeAsByte(0); // 0 = no show in chat 1 = show in chat
		builder.writeInt(Event_EXP); // Event Experience Bonus
		builder.writeAsShort(0);
		builder.writeInt(Wedding_EXP); // Wedding Experience Bonus
		builder.writeInt(Party_Ring_EXP); // Party Ring Bonus EXP
		builder.writeAsByte(0);
		builder.writeInt(Party_EXP); // Bonus EXP for PARTY
		builder.writeInt(Item_EXP); // item equip bonus EXP
		builder.writeInt(Premium_EXP); // Premium bonus exp
		builder.writeInt(Rainbow_EXP); // Rainbow Week bonus EXP
		builder.writeInt(CLASS_EXP); // Class Exp bonus

		return builder.getPacket();
	}

	public static GamePacket GainEXP_Others(final int gain, final boolean inChat, final boolean white) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
		builder.writeAsByte(white);
		builder.writeInt(gain);
		builder.writeAsByte(inChat);
		builder.writeInt(0); // monster book bonus
		builder.writeAsByte(0); // Party percentage
		builder.writeAsShort(0); // Party bouns
		builder.writeZeroBytes(8);
		if (inChat) {
			builder.writeZeroBytes(4); // some ring bonus/ party exp ??
			builder.writeZeroBytes(10);
		} else { // some ring bonus/ party exp
			builder.writeInt(0); // Party size
			builder.writeZeroBytes(4); // Item equip bonus EXP
		}
		builder.writeZeroBytes(4); // Premium bonus EXP
		builder.writeZeroBytes(4); // Class bonus EXP
		builder.writeZeroBytes(5);

		return builder.getPacket();
	}

	public static GamePacket getShowFameGain(final int gain) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(5);
		builder.writeInt(gain);

		return builder.getPacket();
	}

	public static GamePacket showMesoGain(final int gain, final boolean inChat) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		if (!inChat) {
			builder.writeAsByte(0);
			builder.writeAsByte(1);
			builder.writeAsByte(0);
			builder.writeInt(gain);
			builder.writeAsShort(0); // inet cafe meso gain ?.o
		} else {
			builder.writeAsByte(6);
			builder.writeInt(gain);
		}

		return builder.getPacket();
	}

	public static GamePacket getShowItemGain(final int itemId, final short quantity) {
		return getShowItemGain(itemId, quantity, false);
	}

	public static GamePacket getShowItemGain(final int itemId, final short quantity, final boolean inChat) {
		final PacketBuilder builder = new PacketBuilder();

		if (inChat) {
			builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
			builder.writeAsByte(3);
			builder.writeAsByte(1); // item count
			builder.writeInt(itemId);
			builder.writeInt(quantity);
		} else {
			builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
			builder.writeAsShort(0);
			builder.writeInt(itemId);
			builder.writeInt(quantity);
		}

		return builder.getPacket();
	}

	public static GamePacket showRewardItemAnimation(final int itemId, final String effect) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(0x0F);
		builder.writeInt(itemId);
		builder.writeAsByte(1);
		builder.writeLengthPrefixedString(effect);

		return builder.getPacket();
	}

	public static GamePacket showRewardItemAnimation(final int itemId, final String effect, final int from_playerid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(from_playerid);
		builder.writeAsByte(0x0F);
		builder.writeInt(itemId);
		builder.writeAsByte(1);
		builder.writeLengthPrefixedString(effect);

		return builder.getPacket();
	}

	public static GamePacket dropItemFromMapObject(final GameMapItem drop, final Point dropfrom, final Point dropto, final byte mod) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
		builder.writeByte(mod); // 1 animation, 2 no animation, 3 spawn
								// disappearing item [Fade], 4 spawn
								// disappearing item
		builder.writeInt(drop.getObjectId()); // item owner id
		builder.writeAsByte(drop.getMeso() > 0 ? 1 : 0); // 1 mesos, 0 item, 2
															// and above all
															// item meso bag,
		builder.writeInt(drop.getItemId()); // drop object ID
		builder.writeInt(drop.getOwner()); // owner charid
		builder.writeByte(drop.getDropType()); // 0 = timeout for non-owner, 1 =
												// timeout for non-owner's
												// party, 2 = FFA, 3 =
												// explosive/FFA
		builder.writeVector(dropto);
		builder.writeInt(0);
		if (mod != 2) {
			builder.writeVector(dropfrom);
			builder.writeAsShort(0);
		}
		if (drop.getMeso() == 0) {
			PacketHelper.addExpirationTime(builder, drop.getItem().getExpiration());
		}
		builder.writeAsShort(drop.isPlayerDrop() ? 0 : 1); // pet EQP pickup

		return builder.getPacket();
	}

	public static GamePacket spawnPlayerMapObject(final ChannelCharacter chr) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_PLAYER.getValue());
		builder.writeInt(chr.getId());
		builder.writeAsByte(chr.getLevel());
		builder.writeLengthPrefixedString(chr.getName());

		if (chr.getGuildId() <= 0) {
			builder.writeInt(0);
			builder.writeInt(0);
		} else {
			final GuildSummary gs = ChannelServer.getInstance().getGuildSummary(chr.getGuildId());

			if (gs != null) {
				builder.writeLengthPrefixedString(gs.getName());
				builder.writeAsShort(gs.getLogoBG());
				builder.writeByte(gs.getLogoBGColor());
				builder.writeAsShort(gs.getLogo());
				builder.writeByte(gs.getLogoColor());
			} else {
				builder.writeInt(0);
				builder.writeInt(0);
			}
		}
		builder.writeAsShort(0);
		builder.writeAsShort(1016);
		builder.writeInt(0); // i think
		builder.writeInt(chr.getBuffedValue(BuffStat.MORPH) != null ? 2 : 0); // Should
																				// be
																				// a
																				// byte,
																				// but
																				// nvm

		long buffmask = 0;
		Integer buffvalue = null;
		if (chr.getBuffedValue(BuffStat.DARKSIGHT) != null && !chr.isHidden()) {
			buffmask |= BuffStat.DARKSIGHT.getValue();
		}
		if (chr.getBuffedValue(BuffStat.COMBO) != null) {
			buffmask |= BuffStat.COMBO.getValue();
			buffvalue = Integer.valueOf(chr.getBuffedValue(BuffStat.COMBO).intValue());
		}
		if (chr.getBuffedValue(BuffStat.SHADOWPARTNER) != null) {
			buffmask |= BuffStat.SHADOWPARTNER.getValue();
		}
		if (chr.getBuffedValue(BuffStat.SOULARROW) != null) {
			buffmask |= BuffStat.SOULARROW.getValue();
		}
		if (chr.getBuffedValue(BuffStat.DIVINE_BODY) != null) {
			buffmask |= BuffStat.DIVINE_BODY.getValue();
		}
		if (chr.getBuffedValue(BuffStat.BERSERK_FURY) != null) {
			buffmask |= BuffStat.BERSERK_FURY.getValue();
		}
		if (chr.getBuffedValue(BuffStat.MORPH) != null) {
			buffvalue = Integer.valueOf(chr.getBuffedValue(BuffStat.MORPH).intValue());
		}

		builder.writeInt((int) (buffmask >> 32 & 0xffffffffL));
		if (buffvalue != null) {
			if (chr.getBuffedValue(BuffStat.MORPH) != null) {
				builder.writeAsShort(buffvalue);
			} else {
				builder.writeByte(buffvalue.byteValue());
			}
		}
		builder.writeInt((int) (buffmask & 0xffffffffL));

		final int CHAR_MAGIC_SPAWN = Randomizer.nextInt();
		builder.writeInt(0);
		builder.writeAsShort(0);
		builder.writeAsByte(1);
		builder.writeInt(CHAR_MAGIC_SPAWN);
		builder.writeAsShort(0);
		builder.writeLong(0);
		builder.writeAsByte(1);
		builder.writeInt(CHAR_MAGIC_SPAWN);
		builder.writeAsShort(0);
		builder.writeLong(0);
		builder.writeAsByte(1);
		builder.writeInt(CHAR_MAGIC_SPAWN);
		builder.writeAsShort(0);

		if (chr.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			final Item mount = chr.getEquippedItemsInventory().getItem((byte) -22);
			if (mount != null) {
				builder.writeInt(mount.getItemId());
				builder.writeInt(1004);
			} else {
				builder.writeInt(1932000);
				builder.writeInt(5221006);
			}
			builder.writeAsByte(1);
			builder.writeInt(211951617);
		} else {
			builder.writeLong(0);
			builder.writeAsByte(1);
			builder.writeInt(CHAR_MAGIC_SPAWN);
		}
		builder.writeLong(0);
		builder.writeAsByte(1);
		builder.writeInt(CHAR_MAGIC_SPAWN);
		builder.writeAsByte(1);
		builder.writeBytes(HexTool.getByteArrayFromHexString("FB 8E F5 A4")); // wtf
																				// is
																				// this?
		builder.writeLong(0);
		builder.writeAsShort(0);
		builder.writeAsByte(1);
		builder.writeInt(CHAR_MAGIC_SPAWN);
		builder.writeInt(0);
		builder.writeLong(0);
		builder.writeAsByte(1);
		builder.writeInt(CHAR_MAGIC_SPAWN);
		builder.writeAsShort(0);
		builder.writeAsShort(chr.getJobId());
		GameCharacterPacket.addCharLook(builder, chr, false);
		builder.writeLong(0);
		builder.writeLong(0);
		builder.writeInt(chr.getItemEffect());
		builder.writeInt(chr.getChair());
		builder.writeVector(chr.getPosition());
		builder.writeAsByte(chr.getStance());
		builder.writeAsShort(0); // FH
		builder.writeAsByte(0);
		builder.writeInt(chr.getMount().getLevel()); // mount lvl
		builder.writeInt(chr.getMount().getExp()); // exp
		builder.writeInt(chr.getMount().getFatigue()); // tiredness
		builder.writeAsShort(0);
		builder.writeInt(0);
		if (chr.getCarnivalParty() != null) {
			builder.writeAsByte(chr.getCarnivalParty().getTeam());
		}
		return builder.getPacket();
	}

	public static GamePacket removePlayerFromMap(final int cid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
		builder.writeInt(cid);

		return builder.getPacket();
	}

	public static GamePacket facialExpression(final ChannelCharacter from, final int expression) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FACIAL_EXPRESSION.getValue());
		builder.writeInt(from.getId());
		builder.writeInt(expression);
		builder.writeInt(-1);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket movePlayer(final int cid, final List<LifeMovementFragment> moves, final Point startPos) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MOVE_PLAYER.getValue());
		builder.writeInt(cid);
		builder.writeVector(startPos);
		builder.writeInt(0);
		PacketHelper.serializeMovementList(builder, moves);

		return builder.getPacket();
	}

	public static GamePacket moveSummon(final int cid, final int oid, final Point startPos, final List<LifeMovementFragment> moves) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MOVE_SUMMON.getValue());
		builder.writeInt(cid);
		builder.writeInt(oid);
		builder.writeVector(startPos);
		builder.writeInt(0);
		PacketHelper.serializeMovementList(builder, moves);

		return builder.getPacket();
	}

	public static GamePacket summonAttack(final int cid, final int summonSkillId, final byte animation, final List<SummonAttackEntry> allDamage, final int level) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SUMMON_ATTACK.getValue());
		builder.writeInt(cid);
		builder.writeInt(summonSkillId);
		builder.writeAsByte(level - 1);
		builder.writeByte(animation);
		builder.writeAsByte(allDamage.size());

		for (final SummonAttackEntry attackEntry : allDamage) {
			builder.writeInt(attackEntry.getMonster().getObjectId()); // oid
			builder.writeAsByte(7); // who knows
			builder.writeInt(attackEntry.getDamage()); // damage
		}
		return builder.getPacket();
	}

	public static GamePacket closeRangeAttack(final int cid, final int tbyte, final int skill, final int level, final byte display, final byte animation, final byte speed, final List<AttackPair> damage,
		final int lvl) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
		builder.writeInt(cid);
		builder.writeAsByte(tbyte);
		builder.writeAsByte(lvl);
		if (skill > 0) {
			builder.writeAsByte(level);
			builder.writeInt(skill);
		} else {
			builder.writeAsByte(0);
		}
		builder.writeAsByte(0); // Added on v.82
		builder.writeByte(display);
		builder.writeByte(animation);
		builder.writeByte(speed);
		builder.writeAsByte(0); // Mastery
		builder.writeInt(0); // E9 03 BE FC

		if (skill == 4211006) {
			for (final AttackPair oned : damage) {
				if (oned.attack != null) {
					builder.writeInt(oned.objectid);
					builder.writeAsByte(0x07);
					builder.writeAsByte(oned.attack.size());
					for (final Integer eachd : oned.attack) {
						// highest bit set = crit
						builder.writeInt(eachd);
					}
				}
			}
		} else {
			for (final AttackPair oned : damage) {
				if (oned.attack != null) {
					builder.writeInt(oned.objectid);
					builder.writeAsByte(0x07);
					for (final Integer eachd : oned.attack) {
						// highest bit set = crit
						builder.writeInt(eachd);
					}
				}
			}
		}
		return builder.getPacket();
	}

	public static GamePacket rangedAttack(final int cid, final byte tbyte, final int skill, final int level, final byte display, final byte animation, final byte speed, final int itemid,
		final List<AttackPair> damage, final Point pos, final int lvl) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.RANGED_ATTACK.getValue());
		builder.writeInt(cid);
		builder.writeByte(tbyte);
		builder.writeAsByte(lvl);
		if (skill > 0) {
			builder.writeAsByte(level);
			builder.writeInt(skill);
		} else {
			builder.writeAsByte(0);
		}
		builder.writeAsByte(0); // Added on v.82
		builder.writeByte(display);
		builder.writeByte(animation);
		builder.writeByte(speed);
		builder.writeAsByte(0); // Mastery level, who cares
		builder.writeInt(itemid);

		for (final AttackPair oned : damage) {
			if (oned.attack != null) {
				builder.writeInt(oned.objectid);
				builder.writeAsByte(0x07);
				for (final Integer eachd : oned.attack) {
					// highest bit set = crit
					builder.writeInt(eachd.intValue());
				}
			}
		}
		builder.writeVector(pos); // Position

		return builder.getPacket();
	}

	public static GamePacket magicAttack(final int cid, final int tbyte, final int skill, final int level, final byte display, final byte animation, final byte speed, final List<AttackPair> damage,
		final int charge, final int lvl) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MAGIC_ATTACK.getValue());
		builder.writeInt(cid);
		builder.writeAsByte(tbyte);
		builder.writeAsByte(lvl);
		builder.writeAsByte(level);
		builder.writeInt(skill);

		builder.writeAsByte(0); // Added on v.82
		builder.writeByte(display);
		builder.writeByte(animation);
		builder.writeByte(speed);
		builder.writeAsByte(0); // Mastery byte is always 0 because spells don't
								// have a swoosh
		builder.writeInt(0);

		for (final AttackPair oned : damage) {
			if (oned.attack != null) {
				builder.writeInt(oned.objectid);
				builder.writeAsByte(-1);
				for (final Integer eachd : oned.attack) {
					// highest bit set = crit
					builder.writeInt(eachd.intValue());
				}
			}
		}
		if (charge > 0) {
			builder.writeInt(charge);
		}
		return builder.getPacket();
	}

	public static GamePacket getNPCShop(final ChannelClient c, final int sid, final List<ShopItem> items) {
		final PacketBuilder builder = new PacketBuilder();

		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		builder.writeAsShort(ServerPacketOpcode.OPEN_NPC_SHOP.getValue());
		builder.writeInt(sid);
		builder.writeAsShort(items.size());
		for (final ShopItem item : items) {
			builder.writeInt(item.getItemId());
			builder.writeInt(item.getPrice());
			builder.writeZeroBytes(16);
			if (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId())) {
				builder.writeAsShort(1);
				builder.writeAsShort(item.getBuyable());
			} else {
				builder.writeAsShort(0);
				builder.writeInt(0);
				builder.writeAsShort(BitTools.doubleshofteertBits(ii.getPrice(item.getItemId())));
				builder.writeAsShort(ii.getSlotMax(item.getItemId()));
			}
		}

		return builder.getPacket();
	}

	public static GamePacket confirmShopTransaction(final byte code) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
		builder.writeByte(code); // 8 = sell, 0 = buy, 0x20 = due to an error

		return builder.getPacket();
	}

	public static GamePacket addInventorySlot(final InventoryType type, final Item item) {
		return addInventorySlot(type, item, false);
	}

	public static GamePacket addInventorySlot(final InventoryType type, final Item item, final boolean fromDrop) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeAsByte(fromDrop);
		builder.writeAsShort(1); // add mode
		builder.writeByte(type.asNumber()); // iv type
		builder.writeAsByte(item.getPosition()); // slot id
		PacketHelper.addItemInfo(builder, item, true, false);

		return builder.getPacket();
	}

	public static GamePacket updateInventorySlot(final InventoryType type, final Item item, final boolean fromDrop) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeAsByte(fromDrop);
		// builder.writeAsByte((slot2 > 0 ? 1 : 0) + 1);
		builder.writeAsByte(1);
		builder.writeAsByte(1);
		builder.writeByte(type.asNumber()); // iv type
		builder.writeAsShort(item.getPosition()); // slot id
		builder.writeAsShort(item.getQuantity());
		/*
		 * if (slot2 > 0) { builder.writeAsByte(1);
		 * builder.writeAsByte(type.getType()); builder.writeAsShort(slot2);
		 * builder.writeAsShort(amt2); }
		 */
		return builder.getPacket();
	}

	public static GamePacket moveInventoryItem(final InventoryType type, final short src, final short dst) {
		return moveInventoryItem(type, src, dst, (byte) -1);
	}

	public static GamePacket moveInventoryItem(final InventoryType type, final short src, final short dst, final short equipIndicator) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 02"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(src);
		builder.writeAsShort(dst);
		if (equipIndicator != -1) {
			builder.writeAsByte(equipIndicator);
		}
		return builder.getPacket();
	}

	public static GamePacket moveAndMergeInventoryItem(final InventoryType type, final byte src, final byte dst, final short total) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 02 03"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(src);
		builder.writeAsByte(1); // merge mode?
		builder.writeByte(type.asNumber());
		builder.writeAsShort(dst);
		builder.writeAsShort(total);

		return builder.getPacket();
	}

	public static GamePacket moveAndMergeWithRestInventoryItem(final InventoryType type, final byte src, final byte dst, final short srcQ, final short dstQ) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 02 01"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(src);
		builder.writeAsShort(srcQ);
		builder.writeBytes(HexTool.getByteArrayFromHexString("01"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(dst);
		builder.writeAsShort(dstQ);

		return builder.getPacket();
	}

	public static GamePacket clearInventoryItem(final InventoryType type, final short slot, final boolean fromDrop) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeAsByte(fromDrop);
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 03"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(slot);

		return builder.getPacket();
	}

	public static GamePacket updateSpecialItemUse(final Item item, final byte type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeAsByte(0); // could be from drop
		builder.writeAsByte(2); // always 2
		builder.writeAsByte(3); // quantity > 0 (?)
		builder.writeByte(type);
		builder.writeAsShort(item.getPosition()); // item slot
		builder.writeAsByte(0);
		builder.writeByte(type);
		if (item.getType() == ItemType.EQUIP) {
			builder.writeAsShort(item.getPosition()); // wtf repeat
		} else {
			builder.writeAsByte(item.getPosition());
		}
		PacketHelper.addItemInfo(builder, item, true, true);
		if (item.getPosition() < 0) {

			builder.writeAsByte(2); // ?

		}

		return builder.getPacket();
	}

	public static GamePacket scrolledItem(final Item scroll, final Item item, final boolean destroyed) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeAsByte(1); // fromdrop always true
		builder.writeAsByte(destroyed ? 2 : 3);
		builder.writeAsByte(scroll.getQuantity() > 0 ? 1 : 3);
		builder.writeByte(GameConstants.getInventoryType(scroll.getItemId()).asNumber());
		builder.writeAsShort(scroll.getPosition());

		if (scroll.getQuantity() > 0) {
			builder.writeAsShort(scroll.getQuantity());
		}
		builder.writeAsByte(3);
		if (!destroyed) {
			builder.writeByte(InventoryType.EQUIP.asNumber());
			builder.writeAsShort(item.getPosition());
			builder.writeAsByte(0);
		}
		builder.writeByte(InventoryType.EQUIP.asNumber());
		builder.writeAsShort(item.getPosition());
		if (!destroyed) {
			PacketHelper.addItemInfo(builder, item, true, true);
		}
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket getScrollEffect(final int chr, final ScrollResult scrollSuccess, final boolean isLegendarySpirit) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
		builder.writeInt(chr);

		switch (scrollSuccess) {
		case SUCCESS:
			builder.writeAsShort(1);
			builder.writeAsShort(isLegendarySpirit);
			break;
		case FAIL:
			builder.writeAsShort(0);
			builder.writeAsShort(isLegendarySpirit);
			break;
		case CURSE:
			builder.writeAsByte(0);
			builder.writeAsByte(1);
			builder.writeAsShort(isLegendarySpirit);
			break;
		}
		builder.writeAsByte(0);
		return builder.getPacket();
	}

	public static GamePacket ItemMaker_Success() {
		final PacketBuilder builder = new PacketBuilder();
		// D6 00 00 00 00 00 01 00 00 00 00 DC DD 40 00 01 00 00 00 01 00 00 00
		// 8A 1C 3D 00 01 00 00 00 00 00 00 00 00 B0 AD 01 00
		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(0x12);
		builder.writeZeroBytes(4);

		return builder.getPacket();
	}

	public static GamePacket ItemMaker_Success_3rdParty(final int from_playerid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(from_playerid);
		builder.writeAsByte(0x12);
		builder.writeZeroBytes(4);

		return builder.getPacket();
	}

	public static GamePacket explodeDrop(final int oid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
		builder.writeAsByte(4); // 4 = Explode
		builder.writeInt(oid);
		builder.writeAsShort(655);

		return builder.getPacket();
	}

	public static GamePacket removeItemFromMap(final int oid, final int animation, final int cid) {
		return removeItemFromMap(oid, animation, cid, false, 0);
	}

	public static GamePacket removeItemFromMap(final int oid, final int animation, final int cid, final boolean pet, final int slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
		builder.writeAsByte(animation); // 0 = Expire, 1 = without animation, 2
										// = pickup, 4 = explode
		builder.writeInt(oid);
		if (animation >= 2) {
			builder.writeInt(cid);
			if (pet) { // allow pet pickup?
				builder.writeAsByte(slot);
			}
		}
		return builder.getPacket();
	}

	public static GamePacket updateCharLook(final ChannelCharacter chr) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_CHAR_LOOK.getValue());
		builder.writeInt(chr.getId());
		builder.writeAsByte(1);
		GameCharacterPacket.addCharLook(builder, chr, false);

		final Inventory iv = chr.getEquippedItemsInventory();
		final List<Item> equipped = Lists.newLinkedList(iv);
		Collections.sort(equipped);

		final List<Ring> rings = Lists.newArrayList();
		for (final Item item : equipped) {
			final Equip equip = (Equip) item;
			if (equip.getRingId() > -1) {
				rings.add(Ring.loadFromDb(equip.getRingId()));
			}
		}
		Collections.sort(rings);

		if (rings.size() > 0) {
			for (final Ring ring : rings) {
				builder.writeAsByte(1);
				builder.writeInt(ring.getRingId());
				builder.writeInt(0);
				builder.writeInt(ring.getPartnerRingId());
				builder.writeInt(0);
				builder.writeInt(ring.getItemId());
			}
		} else {
			builder.writeAsByte(0);
		}
		builder.writeAsShort(0);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket dropInventoryItem(final InventoryType type, final short src) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 03"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(src);
		if (src < 0) {
			builder.writeAsByte(1);
		}
		return builder.getPacket();
	}

	public static GamePacket dropInventoryItemUpdate(final InventoryType type, final Item item) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 01"));
		builder.writeByte(type.asNumber());
		builder.writeAsShort(item.getPosition());
		builder.writeAsShort(item.getQuantity());

		return builder.getPacket();
	}

	public static GamePacket damagePlayer(final int skill, final int monsteridfrom, final int cid, final int damage, final int fake, final byte direction, final int reflect, final boolean isPowerGuard,
		final int oid, final int pos_x, final int pos_y) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DAMAGE_PLAYER.getValue());
		builder.writeInt(cid);
		builder.writeAsByte(skill);
		builder.writeInt(damage);
		builder.writeInt(monsteridfrom);
		builder.writeByte(direction);
		if (reflect > 0) {
			builder.writeAsByte(reflect);
			builder.writeAsByte(isPowerGuard);
			builder.writeInt(oid);
			builder.writeAsByte(6);
			builder.writeAsShort(pos_x);
			builder.writeAsShort(pos_y);
			builder.writeAsByte(0);
		} else {
			builder.writeAsShort(0);
		}
		builder.writeInt(damage);
		if (fake > 0) {
			builder.writeInt(fake);
		}

		return builder.getPacket();
	}

	public static GamePacket startQuest(final ChannelCharacter c, final int questId, final String data) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(1);
		builder.writeAsShort(questId);
		builder.writeAsByte(1);

		builder.writeLengthPrefixedString(data != null ? data : "");

		return builder.getPacket();
	}

	public static GamePacket forfeitQuest(final ChannelCharacter c, final int questId) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(1);
		builder.writeAsShort(questId);
		builder.writeAsShort(0);
		builder.writeAsByte(0);
		builder.writeInt(0);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket completeQuest(final int questId) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(1);
		builder.writeAsShort(questId);
		builder.writeAsByte(2);
		builder.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

		return builder.getPacket();
	}

	public static GamePacket updateInfoQuest(final int quest, final String data) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(0x0B);
		builder.writeAsShort(quest);
		builder.writeLengthPrefixedString(data);

		return builder.getPacket();
	}

	public static GamePacket updateQuestInfo(final ChannelCharacter c, final int questId, final int npcId, final byte progress) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_QUEST_INFO.getValue());
		builder.writeByte(progress);
		builder.writeAsShort(questId);
		builder.writeInt(npcId);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket charInfo(final ChannelCharacter chr, final boolean isSelf) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CHAR_INFO.getValue());
		builder.writeInt(chr.getId());
		builder.writeAsByte(chr.getLevel());
		builder.writeAsShort(chr.getJobId());
		builder.writeAsShort(chr.getFame());
		builder.writeAsByte(0); // heart red or gray
		if (chr.getGuildId() <= 0) {
			builder.writeLengthPrefixedString("-");
			builder.writeLengthPrefixedString("");
			// builder.writeLengthPrefixedString("Resets: " + chr.getReborns());
		} else {
			final GuildSummary gs = ChannelServer.getInstance().getGuildSummary(chr.getGuildId());
			builder.writeLengthPrefixedString(gs.getName());
			builder.writeLengthPrefixedString("");
		}
		builder.writeAsByte(0);
		final Inventory equippedItemsInventory = chr.getEquippedItemsInventory();
		final Item inv = equippedItemsInventory.getItem((byte) -114);
		final int peteqid = inv != null ? inv.getItemId() : 0;
		for (final Pet pet : chr.getPets()) {
			if (pet.isSummoned()) {
				builder.writeAsByte(pet.getUniqueId());
				builder.writeInt(pet.getPetItemId()); // petid
				builder.writeLengthPrefixedString(pet.getName());
				builder.writeAsByte(pet.getLevel()); // pet level
				builder.writeAsShort(pet.getCloseness()); // pet closeness
				builder.writeAsByte(pet.getFullness()); // pet fullness
				builder.writeAsShort(0);
				builder.writeInt(peteqid);
			}
		}
		builder.writeAsByte(0); // End of pet
		if (equippedItemsInventory.getItem((byte) -22) != null) {
			final Mount mount = chr.getMount();
			builder.writeAsByte(1);
			builder.writeInt(mount.getLevel());
			builder.writeInt(mount.getExp());
			builder.writeInt(mount.getFatigue());
		} else {
			builder.writeAsByte(0);
		}
		final int wishlistSize = chr.getWishlistSize();
		builder.writeAsByte(wishlistSize);
		if (wishlistSize > 0) {
			final int[] wishlist = chr.getWishlist();
			for (int i = 0; i < wishlistSize; i++) {
				builder.writeInt(wishlist[i]);
			}
		}
		chr.getMonsterBook().addCharInfoPacket(chr.getMonsterBookCover(), builder);
		builder.writeZeroBytes(6);

		return builder.getPacket();
	}

	private static void writeLongMask(final PacketBuilder builder, final List<BuffStatValue> statups) {
		long firstmask = 0;
		long secondmask = 0;
		for (final BuffStatValue statup : statups) {
			if (statup.stat.isFirst()) {
				firstmask |= statup.stat.getValue();
			} else {
				secondmask |= statup.stat.getValue();
			}
		}
		builder.writeLong(firstmask);
		builder.writeLong(secondmask);
	}

	private static void writeLongDiseaseMask(final PacketBuilder builder, final List<DiseaseValue> statUpdates) {
		long firstmask = 0;
		long secondmask = 0;
		// TODO: use EnumSet for this instead?
		for (final DiseaseValue update : statUpdates) {
			if (update.getDisease().isFirst()) {
				firstmask |= update.getDisease().getValue();
			} else {
				secondmask |= update.getDisease().getValue();
			}
		}
		builder.writeLong(firstmask);
		builder.writeLong(secondmask);
	}

	private static void writeLongMaskFromList(final PacketBuilder builder, final List<BuffStat> statups) {
		long firstmask = 0;
		long secondmask = 0;
		for (final BuffStat statup : statups) {
			if (statup.isFirst()) {
				firstmask |= statup.getValue();
			} else {
				secondmask |= statup.getValue();
			}
		}
		builder.writeLong(firstmask);
		builder.writeLong(secondmask);
	}

	public static GamePacket giveMount(final int buffid, final int skillid, final List<BuffStatValue> statups) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());

		writeLongMask(builder, statups);

		builder.writeAsShort(0);
		builder.writeInt(buffid); // 1902000 saddle
		builder.writeInt(skillid); // skillid
		builder.writeInt(0); // Server tick value
		builder.writeAsShort(0);
		builder.writeAsByte(0);
		builder.writeAsByte(1); // Total buffed times

		return builder.getPacket();
	}

	public static GamePacket givePirate(final List<BuffStatValue> statups, final int duration, final int skillid) {
		final boolean infusion = skillid == 5121009 || skillid == 15111005;
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
		writeLongMask(builder, statups);

		builder.writeAsShort(0);
		for (final BuffStatValue stat : statups) {
			builder.writeInt(stat.value);
			builder.writeLong(skillid);
			builder.writeZeroBytes(infusion ? 6 : 1);
			builder.writeAsShort(duration);
		}
		builder.writeAsShort(infusion ? 600 : 0);
		if (!infusion) {
			builder.writeAsByte(1); // does this only come in dash?
		}
		return builder.getPacket();
	}

	public static GamePacket giveForeignPirate(final List<BuffStatValue> statups, final int duration, final int cid, final int skillid) {
		final boolean infusion = skillid == 5121009 || skillid == 15111005;
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);
		writeLongMask(builder, statups);
		builder.writeAsShort(0);
		for (final BuffStatValue stat : statups) {
			builder.writeInt(stat.value);
			builder.writeLong(skillid);
			builder.writeZeroBytes(infusion ? 7 : 1);
			builder.writeAsShort(duration);// duration... seconds
		}
		builder.writeAsShort(infusion ? 600 : 0);
		return builder.getPacket();
	}

	public static GamePacket giveEnergyChargeTest(final int bar) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
		long mask = 0;
		mask |= BuffStat.ENERGY_CHARGE.getValue();

		builder.writeLong(mask);
		builder.writeLong(0);
		builder.writeAsShort(0);
		builder.writeAsShort(bar); // 0 = no bar, 10000 = full bar
		builder.writeLong(0);
		builder.writeInt(0);
		builder.writeAsShort(0);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket giveInfusion(final List<BuffStatValue> statups, final int buffid, final int bufflength) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
		// 17 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 07 00 AE E1 3E
		// 00 68 B9 01 00 00 00 00 00

		writeLongMask(builder, statups);

		builder.writeBytes(HexTool.getByteArrayFromHexString("00 00 FF FF FF FF F1 23 4E 00 00 00 00 00 00 00 00 00 00 00 6E 00 58 02"));

		return builder.getPacket();
	}

	public static GamePacket giveHoming(final int skillid, final int mobid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
		builder.writeLong(BuffStat.HOMING_BEACON.getValue());
		builder.writeLong(0);

		builder.writeAsShort(0);
		builder.writeInt(1);
		builder.writeLong(skillid);
		builder.writeAsByte(0);
		builder.writeInt(mobid);
		builder.writeAsShort(0);
		return builder.getPacket();
	}

	public static GamePacket giveForeignInfusion(final int cid, final int speed, final int duration) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);
		builder.writeLong(0);
		builder.writeLong(BuffStat.MORPH.getValue()); // transform buffstat
		builder.writeAsShort(0);
		builder.writeInt(speed);
		builder.writeInt(5121009);
		builder.writeLong(0);
		builder.writeInt(duration);
		builder.writeAsShort(0);

		return builder.getPacket();
	}

	public static GamePacket giveBuff(final int buffid, final int bufflength, final List<BuffStatValue> statups, final StatEffect effect) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
		writeLongMask(builder, statups);
		for (final BuffStatValue statup : statups) {
			builder.writeAsShort(statup.value);
			builder.writeInt(buffid);
			builder.writeInt(bufflength);
		}
		builder.writeAsShort(0); // delay, wk charges have 600 here o.o
		builder.writeAsShort(0); // combo 600, too
		builder.writeAsByte(effect.isMorph() || effect.isPirateMorph() ? 2 : 0);

		return builder.getPacket();
	}

	public static GamePacket giveDebuff(final List<DiseaseValue> statUpdates, final MobSkill skill) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
		writeLongDiseaseMask(builder, statUpdates);
		for (final DiseaseValue update : statUpdates) {
			builder.writeAsShort(update.getValue());
			builder.writeAsShort(skill.getSkillId());
			builder.writeAsShort(skill.getSkillLevel());
			builder.writeInt((int) skill.getDuration());
		}
		builder.writeAsShort(0); // ??? wk charges have 600 here o.o
		builder.writeAsShort(2160); // Delay
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket giveForeignDebuff(final int cid, final List<DiseaseValue> statUpdates, final MobSkill skill) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);

		writeLongDiseaseMask(builder, statUpdates);

		if (skill.getSkillId() == 125) {
			builder.writeAsShort(0);
		}
		builder.writeAsShort(skill.getSkillId());
		builder.writeAsShort(skill.getSkillLevel());
		builder.writeAsShort(0); // same as give_buff
		builder.writeAsShort(900); // Delay

		return builder.getPacket();
	}

	public static GamePacket cancelForeignDebuff(final int cid, final long mask) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);
		builder.writeLong(0);
		builder.writeLong(mask);

		return builder.getPacket();
	}

	public static GamePacket showMonsterRiding(final int cid, final List<BuffStatValue> statups, final int itemId, final int skillId) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);
		writeLongMask(builder, statups);
		builder.writeAsShort(0);
		builder.writeInt(itemId);
		builder.writeInt(skillId);
		builder.writeInt(0);
		builder.writeAsShort(0);
		builder.writeAsByte(0);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket giveForeignBuff(final int cid, final List<BuffStatValue> statups, final StatEffect effect) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);
		writeLongMask(builder, statups);
		for (final BuffStatValue statup : statups) {
			if (effect.isMorph() && !effect.isPirateMorph()) {
				builder.writeAsByte(statup.value);
			} else {
				builder.writeAsShort(statup.value);
			}
		}
		builder.writeAsShort(0); // same as give_buff
		if (effect.isMorph()) {
			builder.writeAsShort(0);
		}
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket cancelForeignBuff(final int cid, final List<BuffStat> statups) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
		builder.writeInt(cid);

		writeLongMaskFromList(builder, statups);

		return builder.getPacket();
	}

	public static GamePacket cancelBuff(final List<BuffStat> statups) {
		return cancelBuff(statups, false);
	}

	public static GamePacket cancelBuff(final List<BuffStat> statups, final boolean mount) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_BUFF.getValue());

		if (statups != null) {
			writeLongMaskFromList(builder, statups);
			builder.writeAsByte(3);
		} else {
			builder.writeLong(0);
			builder.writeInt(0x40);
			builder.writeInt(0x1000);
		}

		return builder.getPacket();
	}

	public static GamePacket cancelDebuff(final long mask) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_BUFF.getValue());
		builder.writeLong(0);
		builder.writeLong(mask);
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket updateMount(final ChannelCharacter chr, final boolean levelup) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_MOUNT.getValue());
		builder.writeInt(chr.getId());
		builder.writeInt(chr.getMount().getLevel());
		builder.writeInt(chr.getMount().getExp());
		builder.writeInt(chr.getMount().getFatigue());
		builder.writeAsByte(levelup);

		return builder.getPacket();
	}

	public static GamePacket mountInfo(final ChannelCharacter chr) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_MOUNT.getValue());
		builder.writeInt(chr.getId());
		builder.writeAsByte(1);
		builder.writeInt(chr.getMount().getLevel());
		builder.writeInt(chr.getMount().getExp());
		builder.writeInt(chr.getMount().getFatigue());

		return builder.getPacket();
	}

	public static GamePacket getPlayerShopChat(final ChannelCharacter c, final String chat, final boolean isOwner) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("06 08"));
		builder.writeAsByte(!isOwner);
		builder.writeLengthPrefixedString(c.getName() + " : " + chat);

		return builder.getPacket();
	}

	public static GamePacket getPlayerShopNewVisitor(final ChannelCharacter c, final int slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("04 0" + slot));
		GameCharacterPacket.addCharLook(builder, c, false);
		builder.writeLengthPrefixedString(c.getName());
		builder.writeAsShort(c.getJobId());

		return builder.getPacket();
	}

	public static GamePacket getPlayerShopRemoveVisitor(final int slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("0A 0" + slot));

		return builder.getPacket();
	}

	public static GamePacket getTradePartnerAdd(final ChannelCharacter c) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(4);
		builder.writeAsByte(1);
		GameCharacterPacket.addCharLook(builder, c, false);
		builder.writeLengthPrefixedString(c.getName());
		builder.writeAsShort(c.getJobId());

		return builder.getPacket();
	}

	public static GamePacket getTradeInvite(final ChannelCharacter c) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(2);
		builder.writeAsByte(3);
		builder.writeLengthPrefixedString(c.getName());
		builder.writeInt(0); // Trade ID

		return builder.getPacket();
	}

	public static GamePacket getTradeMesoSet(final byte number, final int meso) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0xF);
		builder.writeByte(number);
		builder.writeInt(meso);

		return builder.getPacket();
	}

	public static GamePacket getTradeItemAdd(final byte number, final Item item) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0xE);
		builder.writeByte(number);
		PacketHelper.addItemInfo(builder, item, false, false, true);

		return builder.getPacket();
	}

	public static GamePacket getTradeStart(final ChannelClient c, final Trade trade, final byte number) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(5);
		builder.writeAsByte(3);
		builder.writeAsByte(2);
		builder.writeByte(number);

		if (number == 1) {
			builder.writeAsByte(0);
			GameCharacterPacket.addCharLook(builder, trade.getPartner().getChr(), false);
			builder.writeLengthPrefixedString(trade.getPartner().getChr().getName());
			builder.writeAsShort(trade.getPartner().getChr().getJobId());
		}
		builder.writeByte(number);
		final ChannelCharacter player = c.getPlayer();
		GameCharacterPacket.addCharLook(builder, player, false);
		builder.writeLengthPrefixedString(player.getName());
		builder.writeAsShort(player.getJobId());
		builder.writeAsByte(0xFF);

		return builder.getPacket();
	}

	public static GamePacket getTradeConfirmation() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0x10);

		return builder.getPacket();
	}

	public static GamePacket TradeMessage(final byte UserSlot, final byte message) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0xA);
		builder.writeByte(UserSlot);
		builder.writeByte(message);
		// 0x06 = success [tax is automated]
		// 0x07 = unsuccessful
		// 0x08 =
		// "You cannot make the trade because there are some items which you cannot carry more than one."
		// 0x09 =
		// "You cannot make the trade because the other person's on a different map."

		return builder.getPacket();
	}

	public static GamePacket getTradeCancel(final byte UserSlot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0xA);
		builder.writeByte(UserSlot);
		builder.writeAsByte(2);

		return builder.getPacket();
	}

	public static GamePacket getNPCTalk(final int npc, final byte msgType, final String talk, final String endBytes, final byte type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
		builder.writeAsByte(4);
		builder.writeInt(npc);
		builder.writeByte(msgType);
		builder.writeByte(type); // 1 = No ESC, 3 = show character + no sec
		builder.writeLengthPrefixedString(talk);
		builder.writeBytes(HexTool.getByteArrayFromHexString(endBytes));

		return builder.getPacket();
	}

	public static GamePacket getMapSelection(final int npcid, final String sel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
		builder.writeAsByte(4);
		builder.writeInt(npcid);
		builder.writeAsByte(0xE);
		builder.writeZeroBytes(5);
		builder.writeInt(5);
		builder.writeLengthPrefixedString(sel);

		return builder.getPacket();
	}

	public static GamePacket getNPCTalkStyle(final int npc, final String talk, final int... args) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
		builder.writeAsByte(4);
		builder.writeInt(npc);
		builder.writeAsShort(8);
		builder.writeLengthPrefixedString(talk);
		builder.writeAsByte(args.length);
		for (final int arg : args) {
			builder.writeInt(arg);
		}

		return builder.getPacket();
	}

	public static GamePacket getNPCTalkNum(final int npc, final String talk, final int def, final int min, final int max) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
		builder.writeAsByte(4);
		builder.writeInt(npc);
		builder.writeAsShort(4);
		builder.writeLengthPrefixedString(talk);
		builder.writeInt(def);
		builder.writeInt(min);
		builder.writeInt(max);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket getNPCTalkText(final int npc, final String talk) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
		builder.writeAsByte(4);
		builder.writeInt(npc);
		builder.writeAsShort(3);
		builder.writeLengthPrefixedString(talk);
		builder.writeInt(0);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket showForeignEffect(final int cid, final int effect) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(cid);
		builder.writeAsByte(effect); // 0 = Level up, 8 = job change

		return builder.getPacket();
	}

	public static GamePacket showBuffeffect(final int cid, final int skillid, final int effectid) {
		return showBuffeffect(cid, skillid, effectid, (byte) 3);
	}

	public static GamePacket showBuffeffect(final int cid, final int skillid, final int effectid, final byte direction) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(cid);
		builder.writeAsByte(effectid);
		builder.writeInt(skillid);
		builder.writeAsByte(1); // probably buff level but we don't know it and
								// it doesn't really matter
		builder.writeAsByte(1);

		if (direction != (byte) 3) {
			builder.writeByte(direction);
		}
		return builder.getPacket();
	}

	public static GamePacket showOwnBuffEffect(final int skillid, final int effectid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(effectid);
		builder.writeInt(skillid);
		builder.writeAsByte(1); // probably buff level but we don't know it and
								// it doesn't really matter
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket showOwnBerserk(final int skillLevel, final boolean berserk) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(1);
		builder.writeInt(1320006);
		builder.writeAsByte(skillLevel);
		builder.writeAsByte(berserk);

		return builder.getPacket();
	}

	public static GamePacket showBerserk(final int characterId, final int skillLevel, final boolean berserk) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(characterId);
		builder.writeAsByte(1);
		builder.writeInt(1320006);
		builder.writeAsByte(skillLevel);
		builder.writeAsByte(berserk);

		return builder.getPacket();
	}

	public static GamePacket showSpecialEffect(final int effect) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(effect);

		return builder.getPacket();
	}

	public static GamePacket showSpecialEffect(final int cid, final int effect) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(cid);
		builder.writeAsByte(effect);

		return builder.getPacket();
	}

	public static GamePacket updateSkill(final int skillid, final int level, final int masterlevel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_SKILLS.getValue());
		builder.writeAsByte(1);
		builder.writeAsShort(1);
		builder.writeInt(skillid);
		builder.writeInt(level);
		builder.writeInt(masterlevel);
		PacketHelper.addExpirationTime(builder, -1);
		builder.writeAsByte(4);

		return builder.getPacket();
	}

	public static GamePacket updateQuestMobKills(final QuestStatus status) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(1);
		builder.writeAsShort(status.getQuestId());
		builder.writeAsByte(1);

		final StringBuilder sb = new StringBuilder();
		for (final int kills : status.getMobKills().values()) {
			sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
		}
		builder.writeLengthPrefixedString(sb.toString());
		builder.writeZeroBytes(8);

		return builder.getPacket();
	}

	public static GamePacket getShowQuestCompletion(final int id) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
		builder.writeAsShort(id);

		return builder.getPacket();
	}

	public static GamePacket getKeymap(final KeyLayout layout) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.KEYMAP.getValue());
		builder.writeAsByte(0);
		layout.writeData(builder);

		return builder.getPacket();
	}

	public static GamePacket getWhisper(final String sender, final int channel, final String text) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
		builder.writeAsByte(0x12);
		builder.writeLengthPrefixedString(sender);
		builder.writeAsShort(channel - 1);
		builder.writeLengthPrefixedString(text);

		return builder.getPacket();
	}

	public static GamePacket getWhisperReply(final String target, final byte reply) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
		builder.writeAsByte(0x0A); // whisper?
		builder.writeLengthPrefixedString(target);
		builder.writeByte(reply);// 0x0 = cannot find char, 0x1 = success

		return builder.getPacket();
	}

	public static GamePacket getFindReplyWithMap(final String target, final int mapid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
		builder.writeAsByte(9);
		builder.writeLengthPrefixedString(target);
		builder.writeAsByte(1);
		builder.writeInt(mapid);
		builder.writeZeroBytes(8); // ?? official doesn't send zeros here but
									// whatever

		return builder.getPacket();
	}

	public static GamePacket getFindReply(final String target, final int channel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
		builder.writeAsByte(9);
		builder.writeLengthPrefixedString(target);
		builder.writeAsByte(3);
		builder.writeInt(channel - 1);

		return builder.getPacket();
	}

	public static GamePacket getInventoryFull() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		builder.writeAsByte(1);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket getShowInventoryFull() {
		return getShowInventoryStatus(0xff);
	}

	public static GamePacket showItemUnavailable() {
		return getShowInventoryStatus(0xfe);
	}

	public static GamePacket getShowInventoryStatus(final int mode) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(mode);
		builder.writeInt(0);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket getStorage(final int npcId, final byte slots, final Collection<Item> items, final int meso) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
		builder.writeAsByte(0x16);
		builder.writeInt(npcId);
		builder.writeByte(slots);
		builder.writeAsShort(0x7E);
		builder.writeAsShort(0);
		builder.writeInt(0);
		builder.writeInt(meso);
		builder.writeAsShort(0);
		builder.writeByte((byte) items.size());
		for (final Item item : items) {
			PacketHelper.addItemInfo(builder, item, true, true);
		}
		builder.writeAsShort(0);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket getStorageFull() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
		builder.writeAsByte(0x11);

		return builder.getPacket();
	}

	public static GamePacket mesoStorage(final byte slots, final int meso) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
		builder.writeAsByte(0x13);
		builder.writeByte(slots);
		builder.writeAsShort(2);
		builder.writeAsShort(0);
		builder.writeInt(0);
		builder.writeInt(meso);

		return builder.getPacket();
	}

	public static GamePacket storeStorage(final byte slots, final InventoryType type, final Collection<Item> items) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
		builder.writeAsByte(0x0D);
		builder.writeByte(slots);
		builder.writeAsShort(type.getBitfieldEncoding());
		builder.writeAsShort(0);
		builder.writeInt(0);
		builder.writeAsByte(items.size());
		for (final Item item : items) {
			PacketHelper.addItemInfo(builder, item, true, true);
		}
		return builder.getPacket();
	}

	public static GamePacket takeOutStorage(final byte slots, final InventoryType type, final Collection<Item> items) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
		builder.writeAsByte(0x9);
		builder.writeByte(slots);
		builder.writeAsShort(type.getBitfieldEncoding());
		builder.writeAsShort(0);
		builder.writeInt(0);
		builder.writeAsByte(items.size());
		for (final Item item : items) {
			PacketHelper.addItemInfo(builder, item, true, true);
		}
		return builder.getPacket();
	}

	public static GamePacket fairyPendantMessage(final int type, final int percent) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FAIRY_PEND_MSG.getValue());
		builder.writeAsShort(21); // 0x15
		builder.writeInt(0); // idk
		builder.writeAsShort(0); // idk
		builder.writeAsShort(percent); // percent
		builder.writeAsShort(0); // idk

		return builder.getPacket();
	}

	public static GamePacket giveFameResponse(final boolean isIncrease, final String name, final int newFame) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FAME_RESPONSE.getValue());
		builder.writeAsByte(FameResponse.SUCCESS.asNumber());
		builder.writeLengthPrefixedString(name);
		builder.writeAsByte(isIncrease);
		builder.writeInt(newFame);

		return builder.getPacket();
	}

	public static GamePacket giveFameErrorResponse(final FameResponse status) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FAME_RESPONSE.getValue());
		builder.writeAsByte(status.asNumber());

		return builder.getPacket();
	}

	public static GamePacket receiveFame(final boolean isIncrease, final String famerName) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FAME_RESPONSE.getValue());
		builder.writeAsByte(FameResponse.RECEIVED_FAME.asNumber());
		builder.writeLengthPrefixedString(famerName);
		builder.writeAsByte(isIncrease);

		return builder.getPacket();
	}

	public static GamePacket partyCreated() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
		builder.writeAsByte(8);
		builder.writeAsByte(0x81);
		builder.writeAsByte(0x60);
		builder.writeAsShort(0);
		builder.writeBytes(CHAR_INFO_MAGIC);
		builder.writeBytes(CHAR_INFO_MAGIC);
		builder.writeInt(0);
		builder.writeInt(Randomizer.nextInt()); // random craps

		return builder.getPacket();
	}

	public static GamePacket partyInvite(final ChannelCharacter from) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
		builder.writeAsByte(4);
		builder.writeInt(from.getPartyMembership().getPartyId());
		builder.writeLengthPrefixedString(from.getName());
		builder.writeInt(from.getLevel());
		builder.writeInt(from.getJobId());
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket partyStatusMessage(final int message) {
		final PacketBuilder builder = new PacketBuilder();

		/*
		 * 10: A beginner can't create a party. 1/11/14/19: Your request for a
		 * party didn't work due to an unexpected error. 13: You have yet to
		 * join a party. 16: Already have joined a party. 17: The party you're
		 * trying to join is already in full capacity. 19: Unable to find the
		 * requested character in this channel.
		 */
		builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
		builder.writeAsByte(message);

		return builder.getPacket();
	}

	public static GamePacket partyStatusMessage(final int message, final String charname) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
		builder.writeAsByte(message); // 23: 'Char' have denied request to the
										// party.
		builder.writeLengthPrefixedString(charname);

		return builder.getPacket();
	}

	private static void addPartyStatus(final int channelId, final Party party, final PacketBuilder builder, final boolean leaving) {
		final List<PartyMember> partymembers = Lists.newArrayList(party.getMembers());
		while (partymembers.size() < 6) {
			partymembers.add(new PartyMember());
		}
		for (final PartyMember partychar : partymembers) {
			builder.writeInt(partychar.getCharacterId());
		}
		for (final PartyMember partychar : partymembers) {
			builder.writePaddedString(partychar.getName(), 13);
		}
		for (final PartyMember partychar : partymembers) {
			builder.writeInt(partychar.getJobId());
		}
		for (final PartyMember partychar : partymembers) {
			builder.writeInt(partychar.getLevel());
		}
		for (final PartyMember partychar : partymembers) {
			if (partychar.isOnline()) {
				builder.writeInt(partychar.getChannel() - 1);
			} else {
				builder.writeInt(-2);
			}
		}
		builder.writeInt(party.getLeader().getCharacterId());
		for (final PartyMember partychar : partymembers) {
			if (partychar.getChannel() == channelId) {
				builder.writeInt(partychar.getMapId());
			} else {
				builder.writeInt(0);
			}
		}
		for (final Map.Entry<PartyMember, DoorInfo> entry : party.getDoors().entrySet()) {
			final PartyMember member = entry.getKey();
			final DoorInfo door = entry.getValue();
			if (member.getChannel() == channelId && !leaving) {
				builder.writeInt(door.getTownId());
				builder.writeInt(door.getTargetId());
				builder.writeInt(2311002);
				builder.writeInt(door.getPosition().x);
				builder.writeInt(door.getPosition().y);
			} else {
				builder.writeInt(leaving ? 999999999 : 0);
				builder.writeLong(leaving ? 999999999 : 0);
				builder.writeLong(leaving ? -1 : 0);
			}
		}
	}

	public static GamePacket updateParty(final int forChannel, final Party party, final PartyOperation operation, final PartyMember target) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
		final boolean leave = operation == PartyOperation.LEAVE || operation == PartyOperation.LOG_ONOFF;
		switch (operation) {
		case LEAVE:
		case DISBAND:
		case EXPEL:
			builder.writeAsByte(0xC);
			builder.writeInt(party.getId());
			builder.writeInt(target.getCharacterId());
			if (operation == PartyOperation.DISBAND) {
				builder.writeAsByte(0);
				builder.writeInt(target.getCharacterId());
			} else {
				builder.writeAsByte(1);
				if (operation == PartyOperation.EXPEL) {
					builder.writeAsByte(1);
				} else {
					builder.writeAsByte(0);
				}
				builder.writeLengthPrefixedString(target.getName());
				addPartyStatus(forChannel, party, builder, leave);
			}
			break;
		case JOIN:
			builder.writeAsByte(0xF);
			builder.writeInt(24725);
			builder.writeLengthPrefixedString(target.getName());
			addPartyStatus(forChannel, party, builder, false);
			break;
		case SILENT_UPDATE:
		case LOG_ONOFF:
			builder.writeAsByte(0x7);
			builder.writeInt(party.getId());
			addPartyStatus(forChannel, party, builder, leave);
			break;
		case CHANGE_LEADER:
			builder.writeAsByte(0x1F);
			builder.writeInt(target.getCharacterId());
			builder.writeAsByte(0);
			break;
		}
		return builder.getPacket();
	}

	public static GamePacket partyPortal(final int townId, final int targetId, final Point position) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
		builder.writeAsShort(0x28);
		builder.writeInt(townId);
		builder.writeInt(targetId);
		builder.writeInt(2311002);
		builder.writeVector(position);

		return builder.getPacket();
	}

	public static GamePacket updatePartyMemberHP(final int cid, final int curhp, final int maxhp) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
		builder.writeInt(cid);
		builder.writeInt(curhp);
		builder.writeInt(maxhp);

		return builder.getPacket();
	}

	public static GamePacket multiChat(final String name, final String chattext, final int mode) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MULTICHAT.getValue());
		builder.writeAsByte(mode); // 0 buddychat; 1 partychat; 2 guildchat
		builder.writeLengthPrefixedString(name);
		builder.writeLengthPrefixedString(chattext);

		return builder.getPacket();
	}

	public static GamePacket getClock(final int time) { // time in seconds
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CLOCK.getValue());
		builder.writeAsByte(2); // clock type. if you send 3 here you have to
								// send another byte (which does not matter at
								// all) before the timestamp
		builder.writeInt(time);

		return builder.getPacket();
	}

	public static GamePacket getClockTime(final Calendar cal) {
		final int hour = cal.get(Calendar.HOUR_OF_DAY);
		final int minute = cal.get(Calendar.MINUTE);
		final int second = cal.get(Calendar.SECOND);
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CLOCK.getValue());
		builder.writeAsByte(1); // Clock-Type
		builder.writeAsByte(hour);
		builder.writeAsByte(minute);
		builder.writeAsByte(second);

		return builder.getPacket();
	}

	public static GamePacket spawnMist(final Mist mist) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_MIST.getValue());
		builder.writeInt(mist.getObjectId());
		builder.writeInt(mist.isMobMist() ? 0 : mist.isPoisonMist() ? 1 : 2);

		if (mist.getOwner() != null) {
			builder.writeInt(mist.getOwner().getId());
			builder.writeInt(mist.getSourceSkill().getId());
			builder.writeAsByte(mist.getSkillLevel());
		} else {
			builder.writeInt(mist.getMobOwner().getId());
			builder.writeInt(mist.getMobSkill().getSkillId());
			builder.writeAsByte(mist.getMobSkill().getSkillLevel());
		}
		builder.writeAsShort(mist.getSkillDelay());
		builder.writeInt(mist.getBox().x);
		builder.writeInt(mist.getBox().y);
		builder.writeInt(mist.getBox().x + mist.getBox().width);
		builder.writeInt(mist.getBox().y + mist.getBox().height);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket removeMist(final int oid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_MIST.getValue());
		builder.writeInt(oid);

		return builder.getPacket();
	}

	public static GamePacket damageSummon(final int cid, final int summonSkillId, final int damage, final int unkByte, final int monsterIdFrom) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DAMAGE_SUMMON.getValue());
		builder.writeInt(cid);
		builder.writeInt(summonSkillId);
		builder.writeAsByte(unkByte);
		builder.writeInt(damage);
		builder.writeInt(monsterIdFrom);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket buddylistMessage(final byte message) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
		builder.writeByte(message);

		return builder.getPacket();
	}

	public static GamePacket updateBuddyList(final Collection<BuddyListEntry> buddylist) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
		builder.writeAsByte(7);
		builder.writeAsByte(buddylist.size());

		for (final BuddyListEntry buddy : buddylist) {
			if (buddy.isVisible()) {
				builder.writeInt(buddy.getCharacterId());
				builder.writePaddedString(buddy.getName(), 13);
				builder.writeAsByte(0);
				builder.writeInt(buddy.getChannel() == -1 ? -1 : buddy.getChannel() - 1);
				builder.writePaddedString(buddy.getGroup(), 17);
			}
		}
		for (int x = 0; x < buddylist.size(); x++) {
			builder.writeInt(0);
		}
		return builder.getPacket();
	}

	public static GamePacket requestBuddylistAdd(final int cidFrom, final String nameFrom, final int levelFrom, final int jobFrom) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
		builder.writeAsByte(9);
		builder.writeInt(cidFrom);
		builder.writeLengthPrefixedString(nameFrom);
		builder.writeInt(levelFrom);
		builder.writeInt(jobFrom);
		builder.writeInt(cidFrom);
		builder.writePaddedString(nameFrom, 13);
		builder.writeAsByte(1);
		builder.writeInt(0);
		builder.writeBytes(HexTool.getByteArrayFromHexString("44 65 66 61 75 6C 74 20 47 72 6F 75 70 00 6E 67")); // default
																													// group
		builder.writeAsShort(1);

		return builder.getPacket();
	}

	public static GamePacket updateBuddyChannel(final int characterid, final int channel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
		builder.writeAsByte(0x14);
		builder.writeInt(characterid);
		builder.writeAsByte(0);
		builder.writeInt(channel);

		return builder.getPacket();
	}

	public static GamePacket itemEffect(final int characterid, final int itemid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_EFFECT.getValue());
		builder.writeInt(characterid);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static GamePacket updateBuddyCapacity(final int capacity) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
		builder.writeAsByte(0x15);
		builder.writeAsByte(capacity);

		return builder.getPacket();
	}

	public static GamePacket showChair(final int characterid, final int itemid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_CHAIR.getValue());
		builder.writeInt(characterid);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static GamePacket cancelChair(final int id) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_CHAIR.getValue());
		if (id == -1) {
			builder.writeAsByte(0);
		} else {
			builder.writeAsByte(1);
			builder.writeAsShort(id);
		}
		return builder.getPacket();
	}

	public static GamePacket spawnReactor(final Reactor reactor) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REACTOR_SPAWN.getValue());
		builder.writeInt(reactor.getObjectId());
		builder.writeInt(reactor.getReactorId());
		builder.writeAsByte(reactor.getStateId());
		builder.writeVector(reactor.getPosition());
		builder.writeByte(reactor.getFacingDirection()); // stance
		builder.writeAsShort(0);

		return builder.getPacket();
	}

	public static GamePacket triggerReactor(final Reactor reactor, final int stance) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REACTOR_HIT.getValue());
		builder.writeInt(reactor.getObjectId());
		builder.writeAsByte(reactor.getStateId());
		builder.writeVector(reactor.getPosition());
		builder.writeAsShort(stance);
		builder.writeAsByte(0);
		builder.writeAsByte(4); // frame delay, set to 5 since there doesn't
								// appear to be a fixed formula for it

		return builder.getPacket();
	}

	public static GamePacket destroyReactor(final Reactor reactor) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REACTOR_DESTROY.getValue());
		builder.writeInt(reactor.getObjectId());
		builder.writeAsByte(reactor.getStateId());
		builder.writeVector(reactor.getPosition());

		return builder.getPacket();
	}

	public static GamePacket musicChange(final String song) {
		return environmentChange(song, 6);
	}

	public static GamePacket showEffect(final String effect) {
		return environmentChange(effect, 3);
	}

	public static GamePacket playSound(final String sound) {
		return environmentChange(sound, 4);
	}

	public static GamePacket environmentChange(final String env, final int mode) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
		builder.writeAsByte(mode);
		builder.writeLengthPrefixedString(env);

		return builder.getPacket();
	}

	public static GamePacket startMapEffect(final String msg, final int itemid, final boolean active) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MAP_EFFECT.getValue());
		builder.writeAsByte(active ? 0 : 1);
		builder.writeInt(itemid);
		if (active) {
			builder.writeLengthPrefixedString(msg);
		}
		return builder.getPacket();
	}

	public static GamePacket removeMapEffect() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MAP_EFFECT.getValue());
		builder.writeAsByte(0);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket showNullGuildInfo() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x1A); // signature for showing guild info

		builder.writeAsByte(0);
		return builder.getPacket();
	}

	public static GamePacket showGuildInfo(final ChannelClient c, final int guildId) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x1A); // signature for showing guild info

		// TODO: Client is only used to get the guild information. Retarded.
		final Guild guild = ChannelServer.getInstance().getGuild(guildId);

		builder.writeAsByte(1); // bInGuild
		builder.writeInt(guildId); // not entirely sure about this one
		builder.writeLengthPrefixedString(guild.getName());
		for (int i = 1; i <= 5; i++) {
			final MemberRank rank = MemberRank.fromNumber(i);
			builder.writeLengthPrefixedString(guild.getRankTitle(rank));
		}
		guild.addMemberData(builder);

		builder.writeInt(guild.getCapacity());
		builder.writeAsShort(guild.getLogoBG());
		builder.writeAsByte(guild.getLogoBGColor());
		builder.writeAsShort(guild.getLogo());
		builder.writeAsByte(guild.getLogoColor());
		builder.writeLengthPrefixedString(guild.getNotice());
		builder.writeInt(guild.getGuildPoints());
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static GamePacket guildMemberOnline(final int gid, final int cid, final boolean bOnline) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x3d);
		builder.writeInt(gid);
		builder.writeInt(cid);
		builder.writeAsByte(bOnline ? 1 : 0);

		return builder.getPacket();
	}

	public static GamePacket guildInvite(final int guildId, final String sender, final int senderLevel, final int senderJob) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x05);
		builder.writeInt(guildId);
		builder.writeLengthPrefixedString(sender);
		builder.writeInt(senderLevel);
		builder.writeInt(senderJob);

		return builder.getPacket();
	}

	public static GamePacket denyGuildInvitation(final String charname) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x37);
		builder.writeLengthPrefixedString(charname);

		return builder.getPacket();
	}

	public static GamePacket genericGuildMessage(final byte code) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeByte(code);

		return builder.getPacket();
	}

	public static GamePacket newGuildMember(final GuildMember mgc) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x27);
		builder.writeInt(mgc.getGuildId());
		builder.writeInt(mgc.getCharacterId());
		builder.writePaddedString(mgc.getName(), 13);
		builder.writeInt(mgc.getJobId());
		builder.writeInt(mgc.getLevel());
		builder.writeInt(mgc.getRank().asNumber() + 1); // should be always 5
														// but whatevs
		builder.writeInt(mgc.isOnline() ? 1 : 0); // should always be 1 too
		builder.writeInt(1); // ? could be guild signature, but doesn't seem to
								// matter
		builder.writeInt(3);

		return builder.getPacket();
	}

	// someone leaving, mode == 0x2c for leaving, 0x2f for expelled
	public static GamePacket memberLeft(final GuildMember mgc, final boolean bExpelled) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(bExpelled ? 0x2f : 0x2c);

		builder.writeInt(mgc.getGuildId());
		builder.writeInt(mgc.getCharacterId());
		builder.writeLengthPrefixedString(mgc.getName());

		return builder.getPacket();
	}

	public static GamePacket changeRank(final GuildMember mgc) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x40);
		builder.writeInt(mgc.getGuildId());
		builder.writeInt(mgc.getCharacterId());
		builder.writeAsByte(mgc.getRank().asNumber() + 1);

		return builder.getPacket();
	}

	public static GamePacket guildNotice(final int gid, final String notice) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x44);
		builder.writeInt(gid);
		builder.writeLengthPrefixedString(notice);

		return builder.getPacket();
	}

	public static GamePacket guildMemberInfoUpdate(final GuildMember member) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x3C);
		builder.writeInt(member.getGuildId());
		builder.writeInt(member.getCharacterId());
		builder.writeInt(member.getLevel());
		builder.writeInt(member.getJobId());

		return builder.getPacket();
	}

	public static GamePacket rankTitleChange(final int gid, final String[] ranks) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x3e);
		builder.writeInt(gid);
		for (final String r : ranks) {
			builder.writeLengthPrefixedString(r);
		}

		return builder.getPacket();
	}

	public static GamePacket guildDisband(final int gid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x32);
		builder.writeInt(gid);
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket guildEmblemChange(final int gid, final short bg, final byte bgcolor, final short logo, final byte logocolor) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x42);
		builder.writeInt(gid);
		builder.writeAsShort(bg);
		builder.writeByte(bgcolor);
		builder.writeAsShort(logo);
		builder.writeByte(logocolor);

		return builder.getPacket();
	}

	public static GamePacket guildCapacityChange(final int gid, final int capacity) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x3a);
		builder.writeInt(gid);
		builder.writeAsByte(capacity);

		return builder.getPacket();
	}

	public static GamePacket showNullGuildUnionInfo() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ALLIANCE_OPERATION.getValue());
		builder.writeAsByte(0x0C);
		builder.writeAsByte(0);
		return builder.getPacket();
	}

	public static GamePacket showBbsThreadList(final ResultSet rs, int start) throws SQLException {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BBS_OPERATION.getValue());
		builder.writeAsByte(6);
		int threadCount = rs.getRow();
		if (!rs.last()) {
			builder.writeAsByte(0);
		} else if (rs.getInt("localthreadid") == 0) { // has a notice
			builder.writeAsByte(1);
			addThread(builder, rs);
			threadCount--; // one thread didn't count (because it's a notice)
		} else {
			builder.writeAsByte(0);
		}
		if (!rs.absolute(start + 1)) { // seek to the thread before where we
										// start
			rs.first(); // uh, we're trying to start at a place past possible
			start = 0;
		}
		builder.writeInt(threadCount);
		builder.writeInt(Math.min(10, threadCount - start));
		for (int i = 0; i < Math.min(10, threadCount - start); i++) {
			addThread(builder, rs);
			rs.next();
		}

		return builder.getPacket();
	}

	private static void addThread(final PacketBuilder builder, final ResultSet rs) throws SQLException {
		builder.writeInt(rs.getInt("localthreadid"));
		builder.writeInt(rs.getInt("postercid"));
		builder.writeLengthPrefixedString(rs.getString("name"));
		builder.writeLong(PacketHelper.getFiletimeFromMillis(rs.getLong("timestamp")));
		builder.writeInt(rs.getInt("icon"));
		builder.writeInt(rs.getInt("replycount"));
	}

	public static GamePacket showThread(final int localthreadid, final ResultSet threadRS, final ResultSet repliesRS) throws SQLException, RuntimeException {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BBS_OPERATION.getValue());
		builder.writeAsByte(7);
		builder.writeInt(localthreadid);
		builder.writeInt(threadRS.getInt("postercid"));
		builder.writeLong(PacketHelper.getFiletimeFromMillis(threadRS.getLong("timestamp")));
		builder.writeLengthPrefixedString(threadRS.getString("name"));
		builder.writeLengthPrefixedString(threadRS.getString("startpost"));
		builder.writeInt(threadRS.getInt("icon"));
		if (repliesRS != null) {
			final int replyCount = threadRS.getInt("replycount");
			builder.writeInt(replyCount);
			int i;
			for (i = 0; i < replyCount && repliesRS.next(); i++) {
				builder.writeInt(repliesRS.getInt("replyid"));
				builder.writeInt(repliesRS.getInt("postercid"));
				builder.writeLong(PacketHelper.getFiletimeFromMillis(repliesRS.getLong("timestamp")));
				builder.writeLengthPrefixedString(repliesRS.getString("content"));
			}
			if (i != replyCount || repliesRS.next()) {
				// in the unlikely event that we lost count of replyid
				throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
				/*
				 * we need to fix the database and stop the packet sending or
				 * else it'll probably error 38 whoever tries to read it there
				 * is ONE case not checked, and that's when the thread has a
				 * replycount of 0 and there is one or more replies to the
				 * thread in bbs_replies
				 */
			}
		} else {
			builder.writeInt(0); // 0 replies
		}
		return builder.getPacket();
	}

	public static GamePacket showGuildRanks(final int npcid, final List<GuildRankingInfo> all) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x49);
		builder.writeInt(npcid);
		builder.writeInt(all.size());
		for (final GuildRankingInfo info : all) {
			builder.writeLengthPrefixedString(info.getName());
			builder.writeInt(info.getGP());
			builder.writeInt(info.getLogo());
			builder.writeInt(info.getLogoBg());
			builder.writeInt(info.getLogoBgColor());
			builder.writeInt(info.getLogoColor());
		}

		return builder.getPacket();
	}

	public static GamePacket updateGuildPoints(final int gid, final int GP) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
		builder.writeAsByte(0x48);
		builder.writeInt(gid);
		builder.writeInt(GP);

		return builder.getPacket();
	}

	public static GamePacket skillEffect(final ChannelCharacter from, final int skillId, final byte level, final byte flags, final byte speed, final byte unk) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SKILL_EFFECT.getValue());
		builder.writeInt(from.getId());
		builder.writeInt(skillId);
		builder.writeByte(level);
		builder.writeByte(flags);
		builder.writeByte(speed);
		builder.writeByte(unk); // Direction ??

		return builder.getPacket();
	}

	public static GamePacket skillCancel(final ChannelCharacter from, final int skillId) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
		builder.writeInt(from.getId());
		builder.writeInt(skillId);

		return builder.getPacket();
	}

	public static GamePacket showMagnet(final int mobid, final byte success) { // Monster
																	// Magnet
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_MAGNET.getValue());
		builder.writeInt(mobid);
		builder.writeByte(success);

		return builder.getPacket();
	}

	public static GamePacket sendHint(final String hint, int width, int height) {
		final PacketBuilder builder = new PacketBuilder();

		if (width < 1) {
			width = hint.length() * 10;
			if (width < 40) {
				width = 40;
			}
		}
		if (height < 5) {
			height = 5;
		}
		builder.writeAsShort(ServerPacketOpcode.PLAYER_HINT.getValue());
		builder.writeLengthPrefixedString(hint);
		builder.writeAsShort(width);
		builder.writeAsShort(height);
		builder.writeAsByte(1);

		return builder.getPacket();
	}

	public static GamePacket messengerInvite(final String from, final int messengerid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(0x03);
		builder.writeLengthPrefixedString(from);
		builder.writeAsByte(0x00);
		builder.writeInt(messengerid);
		builder.writeAsByte(0x00);

		return builder.getPacket();
	}

	public static GamePacket addMessengerPlayer(final String from, final ChannelCharacter chr, final int position, final int channel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(0x00);
		builder.writeAsByte(position);
		GameCharacterPacket.addCharLook(builder, chr, true);
		builder.writeLengthPrefixedString(from);
		builder.writeAsByte(channel);
		builder.writeAsByte(0x00);

		return builder.getPacket();
	}

	public static GamePacket removeMessengerPlayer(final int position) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(0x02);
		builder.writeAsByte(position);

		return builder.getPacket();
	}

	public static GamePacket updateMessengerPlayer(final String from, final ChannelCharacter chr, final int position, final int channel) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(0x07);
		builder.writeAsByte(position);
		GameCharacterPacket.addCharLook(builder, chr, true);
		builder.writeLengthPrefixedString(from);
		builder.writeAsByte(channel);
		builder.writeAsByte(0x00);

		return builder.getPacket();
	}

	public static GamePacket joinMessenger(final int position) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(0x01);
		builder.writeAsByte(position);

		return builder.getPacket();
	}

	public static GamePacket messengerChat(final String text) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(0x06);
		builder.writeLengthPrefixedString(text);

		return builder.getPacket();
	}

	public static GamePacket messengerNote(final String text, final int mode, final int mode2) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
		builder.writeAsByte(mode);
		builder.writeLengthPrefixedString(text);
		builder.writeAsByte(mode2);

		return builder.getPacket();
	}

	public static GamePacket getFindReplyWithCS(final String target) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
		builder.writeAsByte(9);
		builder.writeLengthPrefixedString(target);
		builder.writeAsByte(2);
		builder.writeInt(-1);

		return builder.getPacket();
	}

	public static GamePacket showEquipEffect() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_EQUIP_EFFECT.getValue());

		return builder.getPacket();
	}

	public static GamePacket summonSkill(final int cid, final int summonSkillId, final int newStance) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SUMMON_SKILL.getValue());
		builder.writeInt(cid);
		builder.writeInt(summonSkillId);
		builder.writeAsByte(newStance);

		return builder.getPacket();
	}

	public static GamePacket skillCooldown(final int sid, final int time) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.COOLDOWN.getValue());
		builder.writeInt(sid);
		builder.writeAsShort(time);

		return builder.getPacket();
	}

	public static GamePacket useSkillBook(final ChannelCharacter chr, final int skillid, final int maxlevel, final boolean canuse, final boolean success) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.USE_SKILL_BOOK.getValue());
		builder.writeInt(chr.getId());
		builder.writeAsByte(1);
		builder.writeInt(skillid);
		builder.writeInt(maxlevel);
		builder.writeAsByte(canuse ? 1 : 0);
		builder.writeAsByte(success ? 1 : 0);

		return builder.getPacket();
	}

	public static GamePacket getMacros(final SkillMacro[] macros) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SKILL_MACRO.getValue());
		int count = 0;
		for (int i = 0; i < 5; i++) {
			if (macros[i] != null) {
				count++;
			}
		}
		builder.writeAsByte(count); // number of macros
		for (int i = 0; i < 5; i++) {
			final SkillMacro macro = macros[i];
			if (macro != null) {
				builder.writeLengthPrefixedString(macro.getName());
				builder.writeAsByte(macro.getShout());
				builder.writeInt(macro.getSkill1());
				builder.writeInt(macro.getSkill2());
				builder.writeInt(macro.getSkill3());
			}
		}

		return builder.getPacket();
	}

	public static GamePacket updateAriantPQRanking(final String name, final int score, final boolean empty) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ARIANT_PQ_START.getValue());
		builder.writeAsByte(empty ? 0 : 1);
		if (!empty) {
			builder.writeLengthPrefixedString(name);
			builder.writeInt(score);
		}
		return builder.getPacket();
	}

	public static GamePacket catchMonster(final int mobid, final int itemid, final byte success) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CATCH_MONSTER.getValue());
		builder.writeInt(mobid);
		builder.writeInt(itemid);
		builder.writeByte(success);

		return builder.getPacket();
	}

	public static GamePacket showAriantScoreBoard() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ARIANT_SCOREBOARD.getValue());

		return builder.getPacket();
	}

	public static GamePacket showZakumShrineTimeLeft(final int timeleft) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ZAKUM_SHRINE.getValue());
		builder.writeAsByte(0);
		builder.writeInt(timeleft);

		return builder.getPacket();
	}

	public static GamePacket boatPacket(final int effect) {
		final PacketBuilder builder = new PacketBuilder();

		// 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
		builder.writeAsShort(ServerPacketOpcode.BOAT_EFFECT.getValue());
		builder.writeAsShort(effect); // 0A 04 balrog

		return builder.getPacket();
	}

	public static GamePacket removeItemFromDuey(final boolean remove, final int Package) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DUEY.getValue());
		builder.writeAsByte(0x18);
		builder.writeInt(Package);
		builder.writeAsByte(remove ? 3 : 4);

		return builder.getPacket();
	}

	public static GamePacket sendDuey(final byte operation, final List<DueyActions> packages) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DUEY.getValue());
		builder.writeByte(operation);
		switch (operation) {
		case 9: { // Request 13 Digit AS
			builder.writeAsByte(1);
			// 0xFF = error
			break;
		}
		case 10: { // Open duey
			builder.writeAsByte(0);
			builder.writeAsByte(packages.size());
			for (final DueyActions dp : packages) {
				builder.writeInt(dp.getPackageId());
				builder.writePaddedString(dp.getSender(), 13);
				builder.writeInt(dp.getMesos());
				builder.writeLong(FiletimeUtil.getFileTimestamp(dp.getSentTime(), false));
				builder.writeAsByte(0);
				builder.writeZeroBytes(51 * 4);
				if (dp.getItem() != null) {
					builder.writeAsByte(1);
					PacketHelper.addItemInfo(builder, dp.getItem(), true, true);
				} else {
					builder.writeAsByte(0);
				}
			}
			builder.writeAsByte(0);
			break;
		}
		}

		return builder.getPacket();
	}

	public static GamePacket enableTV() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ENABLE_TV.getValue());
		builder.writeInt(0);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket removeTV() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.REMOVE_TV.getValue());

		return builder.getPacket();
	}

	public static GamePacket sendTV(final ChannelCharacter chr, final List<String> messages, final int type, final ChannelCharacter partner) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SEND_TV.getValue());
		builder.writeAsByte(partner != null ? 2 : 0);
		builder.writeAsByte(type); // type Heart = 2 Star = 1 Normal = 0
		GameCharacterPacket.addCharLook(builder, chr, false);
		builder.writeLengthPrefixedString(chr.getName());
		if (partner != null) {
			builder.writeLengthPrefixedString(partner.getName());
		} else {
			builder.writeAsShort(0);
		}
		for (int i = 0; i < messages.size(); i++) {
			if (i == 4 && messages.get(4).length() > 15) {
				builder.writeLengthPrefixedString(messages.get(4).substring(0, 15)); // hmm
																						// ?
			} else {
				builder.writeLengthPrefixedString(messages.get(i));
			}
		}
		builder.writeInt(1337); // time limit shit lol 'Your thing still start
								// in blah blah seconds'
		if (partner != null) {
			GameCharacterPacket.addCharLook(builder, partner, false);
		}

		return builder.getPacket();
	}

	public static GamePacket Mulung_DojoUp() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(0x0B);
		builder.writeAsShort(1207); // ???
		builder.writeLengthPrefixedString("pt=5599;min=4;belt=3;tuto=1"); // todo

		return builder.getPacket();
	}

	public static GamePacket Mulung_DojoUp2() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(7);

		return builder.getPacket();
	}

	public static GamePacket spawnDragon(final Dragon d) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DRAGON_SPAWN.getValue());
		builder.writeInt(d.getOwner());
		builder.writeInt(d.getPosition().x);
		builder.writeInt(d.getPosition().y);
		builder.writeAsByte(d.getStance()); // stance?
		builder.writeAsShort(0);
		builder.writeAsShort(d.getJobId());

		return builder.getPacket();
	}

	public static GamePacket removeDragon(final int chrid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DRAGON_REMOVE.getValue());
		builder.writeInt(chrid);

		return builder.getPacket();
	}

	public static GamePacket moveDragon(final Dragon d, final Point startPos, final List<LifeMovementFragment> moves) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DRAGON_MOVE.getValue()); // not
																			// sure
		builder.writeInt(d.getOwner());
		builder.writeVector(startPos);
		builder.writeInt(0);
		PacketHelper.serializeMovementList(builder, moves);

		return builder.getPacket();
	}

	public static GamePacket Mulung_Pts(final int recv, final int total) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(10);
		builder.writeLengthPrefixedString("You have received " + recv + " training points, for the accumulated total of " + total + " training points.");

		return builder.getPacket();
	}

	public static GamePacket MulungEnergy(final int energy) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ENERGY.getValue());
		builder.writeLengthPrefixedString("energy");
		builder.writeLengthPrefixedString(String.valueOf(energy));

		return builder.getPacket();
	}

	public static GamePacket PyramidEnergy(final byte type, final int amount) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.ENERGY.getValue());
		builder.writeLengthPrefixedString("massacre_" + (type == 0 ? "cool" : type == 1 ? "kill" : "miss"));
		builder.writeLengthPrefixedString(Integer.toString(amount));
//mc.getClient().getSession().writeAsByte(MaplePacketCreator.updatePyramidInfo(1, mc.getInstance(PartyQuest.NETT_PYRAMID).gainReturnKills());  
		return builder.getPacket();
	}

	public static GamePacket cancelHoming() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CANCEL_BUFF.getValue());
		builder.writeLong(BuffStat.HOMING_BEACON.getValue());
		builder.writeLong(0);

		return builder.getPacket();
	}

	public static GamePacket sendMapleTip(final String message) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(0x46); // Header
		builder.writeAsByte(0xFF);
		builder.writeLengthPrefixedString(message);

		return builder.getPacket();
	}

	public static GamePacket finishedSort(final int type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FINISH_SORT.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(type);

		return builder.getPacket();
	}

	public static GamePacket finishedGather(final int type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FINISH_GATHER.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(type);

		return builder.getPacket();
	}
}