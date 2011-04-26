package tools;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.Mount;
import client.BuddyListEntry;
import client.IEquip;
import client.IItem;
import client.GameConstants;
import client.BuffStat;
import client.ChannelCharacter;
import client.ChannelClient;
import client.Inventory;
import client.InventoryType;
import client.KeyLayout;
import client.Pet;
import client.QuestStatus;
import client.Stat;
import client.IEquip.ScrollResult;
import client.Disease;
import org.javastory.client.ItemType;
import client.Ring;
import client.SkillMacro;
import com.google.common.collect.Lists;
import handling.ByteArrayGamePacket;
import handling.GamePacket;
import handling.ServerPacketOpcode;
import handling.ServerConstants;
import handling.world.Party;
import handling.world.PartyMember;
import handling.world.PartyOperation;
import handling.world.guild.Guild;
import handling.world.guild.GuildSummary;
import handling.world.guild.GuildMember;
import handling.world.guild.GuildUnion;
import org.javastory.client.MemberRank;
import org.javastory.server.channel.GuildRankingInfo;
import server.ItemInfoProvider;
import server.ShopItem;
import server.StatEffect;
import server.Trade;
import server.DueyActions;
import org.javastory.tools.Randomizer;
import server.life.MobSkill;
import server.life.SummonAttackEntry;
import server.maps.Summon;
import server.life.Npc;
import server.life.NpcStats;
import server.maps.Dragon;
import server.maps.GameMap;
import server.maps.Reactor;
import server.maps.Mist;
import server.maps.GameMapItem;
import server.movement.LifeMovementFragment;
import org.javastory.io.PacketBuilder;
import tools.packet.PacketHelper;

public final class MaplePacketCreator {

    private final static byte[] CHAR_INFO_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xC9, (byte) 0x9A, 0x3B};
    public final static List<Pair<Stat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();

    private MaplePacketCreator() {
    }

    public static GamePacket getServerIP(final int port, final int clientId) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SERVER_IP.getValue());
        builder.writeAsShort(0);
        builder.writeBytes(ServerConstants.Server_IP);
        builder.writeAsShort(port);
        builder.writeInt(clientId);
        builder.writeZeroBytes(5);

        return builder.getPacket();
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
        PacketHelper.addCharStats(builder, chr);
        builder.writeAsByte(chr.getBuddylist().getCapacity());
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
        chr.QuestInfoPacket(builder); // for every questinfo: int16_t questid, string questdata
        builder.writeAsShort(0); // PQ rank
        builder.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

        return builder.getPacket();
    }

    public static GamePacket enableActions() {
        return updatePlayerStats(EMPTY_STATUPDATE, true, 0);
    }

    public static GamePacket updatePlayerStats(final List<Pair<Stat, Integer>> stats, final int evan) {
        return updatePlayerStats(stats, false, evan);
    }

    public static GamePacket updatePlayerStats(final List<Pair<Stat, Integer>> stats, final boolean itemReaction, final int evan) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
        builder.writeAsByte(itemReaction ? 1 : 0);
        int updateMask = 0;
        for (final Pair<Stat, Integer> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        List<Pair<Stat, Integer>> mystats = stats;
        if (mystats.size() > 1) {
            Collections.sort(mystats, new Comparator<Pair<Stat, Integer>>() {

                @Override
                public int compare(final Pair<Stat, Integer> o1, final Pair<Stat, Integer> o2) {
                    int val1 = o1.getLeft().getValue();
                    int val2 = o2.getLeft().getValue();
                    return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
                }
            });
        }
        builder.writeInt(updateMask);
        Integer value;
        for (final Pair<Stat, Integer> statupdate : mystats) {
            value = statupdate.getLeft().getValue();
            if (value >= 1) {
                if (value == 0x1) {
                    builder.writeAsShort(statupdate.getRight().shortValue());
                } else if (value <= 0x4) {
                    builder.writeInt(statupdate.getRight());
                } else if (value < 0x20) {
                    builder.writeByte(statupdate.getRight().byteValue());
                } else if (value == 0x8000) { //availablesp
                    if (evan == 2001 || (evan >= 2200 && evan <= 2218)) {
                        throw new UnsupportedOperationException("Evan wrong updating");
                    } else {
                        builder.writeAsShort(statupdate.getRight().shortValue());
                    }
                } else if (value < 0xFFFF) {
                    builder.writeAsShort(statupdate.getRight().shortValue());
                } else {
                    builder.writeInt(statupdate.getRight().intValue());
                }
            }
        }
        return builder.getPacket();
    }

    public static GamePacket updateSp(ChannelCharacter chr, final boolean itemReaction) { //this will do..
        return updateSp(chr, itemReaction, false);
    }

    public static GamePacket updateSp(ChannelCharacter chr, final boolean itemReaction, final boolean overrideJob) { //this will do..
        final PacketBuilder builder = new PacketBuilder();
        builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
        builder.writeAsByte(itemReaction ? 1 : 0);
        builder.writeInt(0x8000);
        if (overrideJob || GameConstants.isEvan(chr.getJobId())) {
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
        builder.writeAsByte(/*town ? 1 :*/0);
        builder.writeAsByte(town ? 1 : 0);
        builder.writeInt(oid);
        builder.writeVector(pos);

        return builder.getPacket();
    }

    public static GamePacket removeDoor(int oid, boolean town) {
        PacketBuilder builder = new PacketBuilder();

        if (town) {
            builder.writeAsShort(ServerPacketOpcode.SPAWN_PORTAL.getValue());
            builder.writeInt(999999999);
            builder.writeLong(999999999);
        } else {
            builder.writeAsShort(ServerPacketOpcode.REMOVE_DOOR.getValue());
            builder.writeAsByte(/*town ? 1 : */0);
            builder.writeInt(oid);
        }

        return builder.getPacket();
    }

    public static GamePacket spawnSummon(Summon summon, int skillLevel, boolean animated) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SPAWN_SUMMON.getValue());
        builder.writeInt(summon.getOwnerId());
        builder.writeInt(summon.getObjectId());
        builder.writeInt(summon.getSkill());
        builder.writeAsByte(100); //owner LEVEL
        builder.writeAsByte(skillLevel);
        builder.writeVector(summon.getPosition());
        builder.writeAsByte(4);
        builder.writeAsShort(summon.isPuppet() ? 179 : 14);
        builder.writeAsByte(summon.getMovementType().getValue()); // 0 = don't move, 1 = follow (4th mage summons?), 2/4 = only tele follow, 3 = bird follow
        builder.writeAsByte(summon.isPuppet() ? 0 : 1); // 0 = Summon can't attack - but puppets don't attack with 1 either =.=
        builder.writeAsShort(animated ? 0 : 1);

        return builder.getPacket();
    }

    public static GamePacket removeSummon(Summon summon, boolean animated) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_SUMMON.getValue());
        builder.writeInt(summon.getOwnerId());
        builder.writeInt(summon.getObjectId());
        builder.writeAsByte(animated ? 4 : 1);

        return builder.getPacket();
    }

    public static GamePacket getRelogResponse() {
        PacketBuilder builder = new PacketBuilder(3);

        builder.writeAsShort(ServerPacketOpcode.RELOG_RESPONSE.getValue());
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    /**
     * Possible values for <code>type</code>:<br>
     * 1: You cannot move that channel. Please try again later.<br>
     * 2: You cannot go into the cash shop. Please try again later.<br>
     * 3: The Item-Trading shop is currently unavailable, please try again later.<br>
     * 4: You cannot go into the trade shop, due to the limitation of user count.<br>
     * 5: You do not meet the minimum level requirement to access the Trade Shop.<br>
     *
     * @param type The type
     * @return The "block" packet.
     */
    public static GamePacket serverBlocked(int type) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MTS_OPEN.getValue());
        builder.writeAsByte(type);

        return builder.getPacket();
    }

    public static GamePacket serverMessage(String message) {
        return serverMessage(4, 0, message, true, false);
    }

    public static GamePacket serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false, false);
    }

    public static GamePacket serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false, false);
    }

    public static GamePacket serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, false, smegaEar);
    }

    private static GamePacket serverMessage(int type, int channel, String message, boolean servermessage, boolean megaEar) {
        PacketBuilder builder = new PacketBuilder();
        /*	* 0: [Notice]<br>
         * 1: Popup<br>
         * 2: Megaphone<br>
         * 3: Super Megaphone<br>
         * 4: Scrolling message at top<br>
         * 5: Pink Text<br>
         * 6: Lightblue Text
         * 8: Item megaphone
         * 9: Heart megaphone
         * 10: Skull Super megaphone
         * 11: Green megaphone message?
         * 12: Three line of megaphone text
         * 13: End of file =.="
         * 14: Green Gachapon box
         * 15: Red Gachapon box*/
        builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
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
                builder.writeAsByte(megaEar ? 1 : 0);
                break;
            case 6:
                builder.writeInt(0);
                break;
        }
        return builder.getPacket();
    }

    public static GamePacket getGachaponMega(final String name, final String message, final IItem item, final byte rareness) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
        builder.writeAsByte(rareness == 2 ? 15 : 14);
        builder.writeLengthPrefixedString(name + message);
        builder.writeInt(0); // 0~3 i think | no its a 5 @ msea 1.01
        builder.writeLengthPrefixedString(name);
        PacketHelper.addItemInfo(builder, item, true, true, true);

        return builder.getPacket();
    }

    public static GamePacket tripleSmega(List<String> message, boolean ear, int channel) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeAsByte(ear ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket getAvatarMega(ChannelCharacter chr, int channel, int itemId, String message, boolean ear) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.AVATAR_MEGA.getValue());
        builder.writeInt(itemId);
        builder.writeLengthPrefixedString(chr.getName());
        builder.writeLengthPrefixedString(message);
        builder.writeInt(channel - 1); // channel
        builder.writeAsByte(ear ? 1 : 0);
        PacketHelper.addCharLook(builder, chr, true);

        return builder.getPacket();
    }

    public static GamePacket itemMegaphone(String msg, boolean whisper, int channel, IItem item) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SERVERMESSAGE.getValue());
        builder.writeAsByte(8);
        builder.writeLengthPrefixedString(msg);
        builder.writeAsByte(channel - 1);
        builder.writeAsByte(whisper ? 1 : 0);

        if (item == null) {
            builder.writeAsByte(0);
        } else {
            PacketHelper.addItemInfo(builder, item, false, false, true);
        }
        return builder.getPacket();
    }

    public static GamePacket spawnNPC(Npc life, boolean show) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SPAWN_NPC.getValue());
        builder.writeInt(life.getObjectId());
        builder.writeInt(life.getId());
        builder.writeAsShort(life.getPosition().x);
        builder.writeAsShort(life.getCy());
        builder.writeAsByte(life.getF() == 1 ? 0 : 1);
        builder.writeAsShort(life.getFh());
        builder.writeAsShort(life.getRx0());
        builder.writeAsShort(life.getRx1());
        builder.writeAsByte(show ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket removeNPC(final int objectid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_NPC.getValue());
        builder.writeInt(objectid);

        return builder.getPacket();
    }

    public static GamePacket spawnNPCRequestController(Npc life, boolean MiniMap) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        builder.writeAsByte(1);
        builder.writeInt(life.getObjectId());
        builder.writeInt(life.getId());
        builder.writeAsShort(life.getPosition().x);
        builder.writeAsShort(life.getCy());
        builder.writeAsByte(life.getF() == 1 ? 0 : 1);
        builder.writeAsShort(life.getFh());
        builder.writeAsShort(life.getRx0());
        builder.writeAsShort(life.getRx1());
        builder.writeAsByte(MiniMap ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket spawnPlayerNPC(NpcStats npc, int id) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_NPC.getValue());
        builder.writeAsByte(1);
        builder.writeInt(id);
        builder.writeLengthPrefixedString(npc.getName());
        builder.writeAsByte(0);
        builder.writeByte(npc.getSkin());
        builder.writeInt(npc.getFace());
        builder.writeAsByte(0);
        builder.writeInt(npc.getHair());
        Map<Byte, Integer> equip = npc.getEquips();
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        for (byte position : equip.keySet()) {
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
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            builder.writeByte(entry.getKey());
            builder.writeInt(entry.getValue());
        }
        builder.writeAsByte(0xFF);
        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            builder.writeByte(entry.getKey());
            builder.writeInt(entry.getValue());
        }
        builder.writeAsByte(0xFF);
        Integer cWeapon = equip.get((byte) -111);
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

    public static GamePacket getChatText(int cidfrom, String text, boolean whiteBG, int show) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CHATTEXT.getValue());
        builder.writeInt(cidfrom);
        builder.writeAsByte(whiteBG ? 1 : 0);
        builder.writeLengthPrefixedString(text);
        builder.writeAsByte(show);

        return builder.getPacket();
    }

    public static GamePacket GameMaster_Func(int value) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeBytes(HexTool.getByteArrayFromHexString("7A 00"));
        builder.writeAsByte(value);
        builder.writeZeroBytes(17);

        return builder.getPacket();
    }

    public static GamePacket testCombo(int value) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ARAN_COMBO.getValue());
        builder.writeInt(value);

        return builder.getPacket();
    }

    public static GamePacket getPacketFromHexString(String hex) {
        return new ByteArrayGamePacket(HexTool.getByteArrayFromHexString(hex));
    }

    public static GamePacket GainEXP_Monster(final int gain, final boolean white, final int Event_EXP, final int Wedding_EXP, final int Party_Ring_EXP, final int Party_EXP, final int Premium_EXP, final int Item_EXP, final int Rainbow_EXP, final int CLASS_EXP) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
        builder.writeAsByte(white ? 1 : 0);
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
        builder.writeAsByte(white ? 1 : 0);
        builder.writeInt(gain);
        builder.writeAsByte(inChat ? 1 : 0);
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

    public static GamePacket getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static GamePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket showRewardItemAnimation(int itemId, String effect) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        builder.writeAsByte(0x0F);
        builder.writeInt(itemId);
        builder.writeAsByte(1);
        builder.writeLengthPrefixedString(effect);

        return builder.getPacket();
    }

    public static GamePacket showRewardItemAnimation(int itemId, String effect, int from_playerid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        builder.writeInt(from_playerid);
        builder.writeAsByte(0x0F);
        builder.writeInt(itemId);
        builder.writeAsByte(1);
        builder.writeLengthPrefixedString(effect);

        return builder.getPacket();
    }

    public static GamePacket dropItemFromMapObject(GameMapItem drop, Point dropfrom, Point dropto, byte mod) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        builder.writeByte(mod); // 1 animation, 2 no animation, 3 spawn disappearing item [Fade], 4 spawn disappearing item
        builder.writeInt(drop.getObjectId()); // item owner id
        builder.writeAsByte(drop.getMeso() > 0 ? 1 : 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        builder.writeInt(drop.getItemId()); // drop object ID
        builder.writeInt(drop.getOwner()); // owner charid
        builder.writeByte(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
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

    public static GamePacket spawnPlayerMapobject(ChannelCharacter chr) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SPAWN_PLAYER.getValue());
        builder.writeInt(chr.getId());
        builder.writeAsByte(chr.getLevel());
        builder.writeLengthPrefixedString(chr.getName());

        if (chr.getGuildId() <= 0) {
            builder.writeInt(0);
            builder.writeInt(0);
        } else {
            final GuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildId());

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
        builder.writeInt(0); //i think
        builder.writeInt(chr.getBuffedValue(BuffStat.MORPH) != null ? 2 : 0); // Should be a byte, but nvm

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

        builder.writeInt((int) ((buffmask >> 32) & 0xffffffffL));
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
            final IItem mount = chr.getEquippedItemsInventory().getItem((byte) -22);
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
        builder.writeBytes(HexTool.getByteArrayFromHexString("FB 8E F5 A4")); // wtf is this?
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
        PacketHelper.addCharLook(builder, chr, false);
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

    public static GamePacket removePlayerFromMap(int cid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        builder.writeInt(cid);

        return builder.getPacket();
    }

    public static GamePacket facialExpression(ChannelCharacter from, int expression) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.FACIAL_EXPRESSION.getValue());
        builder.writeInt(from.getId());
        builder.writeInt(expression);
        builder.writeInt(-1);
        builder.writeAsByte(0);

        return builder.getPacket();
    }

    public static GamePacket movePlayer(int cid, List<LifeMovementFragment> moves, Point startPos) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MOVE_PLAYER.getValue());
        builder.writeInt(cid);
        builder.writeVector(startPos);
        builder.writeInt(0);
        PacketHelper.serializeMovementList(builder, moves);

        return builder.getPacket();
    }

    public static GamePacket moveSummon(int cid, int oid, Point startPos, List<LifeMovementFragment> moves) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MOVE_SUMMON.getValue());
        builder.writeInt(cid);
        builder.writeInt(oid);
        builder.writeVector(startPos);
        builder.writeInt(0);
        PacketHelper.serializeMovementList(builder, moves);

        return builder.getPacket();
    }

    public static GamePacket summonAttack(final int cid, final int summonSkillId, final byte animation, final List<SummonAttackEntry> allDamage, final int level) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket closeRangeAttack(int cid, int tbyte, int skill, int level, byte display, byte animation, byte speed, List<AttackPair> damage, final int lvl) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeInt(0);  // E9 03 BE FC

        if (skill == 4211006) {
            for (AttackPair oned : damage) {
                if (oned.attack != null) {
                    builder.writeInt(oned.objectid);
                    builder.writeAsByte(0x07);
                    builder.writeAsByte(oned.attack.size());
                    for (Integer eachd : oned.attack) {
                        // highest bit set = crit
                        builder.writeInt(eachd);
                    }
                }
            }
        } else {
            for (AttackPair oned : damage) {
                if (oned.attack != null) {
                    builder.writeInt(oned.objectid);
                    builder.writeAsByte(0x07);
                    for (Integer eachd : oned.attack) {
                        // highest bit set = crit
                        builder.writeInt(eachd);
                    }
                }
            }
        }
        return builder.getPacket();
    }

    public static GamePacket rangedAttack(int cid, byte tbyte, int skill, int level, byte display, byte animation, byte speed, int itemid, List<AttackPair> damage, final Point pos, final int lvl) {
        PacketBuilder builder = new PacketBuilder();

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

        for (AttackPair oned : damage) {
            if (oned.attack != null) {
                builder.writeInt(oned.objectid);
                builder.writeAsByte(0x07);
                for (Integer eachd : oned.attack) {
                    // highest bit set = crit
                    builder.writeInt(eachd.intValue());
                }
            }
        }
        builder.writeVector(pos); // Position

        return builder.getPacket();
    }

    public static GamePacket magicAttack(int cid, int tbyte, int skill, int level, byte display, byte animation, byte speed, List<AttackPair> damage, int charge, final int lvl) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeAsByte(0); // Mastery byte is always 0 because spells don't have a swoosh
        builder.writeInt(0);

        for (AttackPair oned : damage) {
            if (oned.attack != null) {
                builder.writeInt(oned.objectid);
                builder.writeAsByte(-1);
                for (Integer eachd : oned.attack) {
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

    public static GamePacket getNPCShop(ChannelClient c, int sid, List<ShopItem> items) {
        PacketBuilder builder = new PacketBuilder();

        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        builder.writeAsShort(ServerPacketOpcode.OPEN_NPC_SHOP.getValue());
        builder.writeInt(sid);
        builder.writeAsShort(items.size());
        for (ShopItem item : items) {
            builder.writeInt(item.getItemId());
            builder.writeInt(item.getPrice());
            builder.writeZeroBytes(16);
            if (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId())) {
                builder.writeAsShort(1);
                builder.writeAsShort(item.getBuyable());
            } else {
                builder.writeAsShort(0);
                builder.writeInt(0);
                builder.writeAsShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
                builder.writeAsShort(ii.getSlotMax(c, item.getItemId()));
            }
        }

        return builder.getPacket();
    }

    public static GamePacket confirmShopTransaction(byte code) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        builder.writeByte(code); // 8 = sell, 0 = buy, 0x20 = due to an error

        return builder.getPacket();
    }

    public static GamePacket addInventorySlot(InventoryType type, IItem item) {
        return addInventorySlot(type, item, false);
    }

    public static GamePacket addInventorySlot(InventoryType type, IItem item, boolean fromDrop) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeAsByte(fromDrop ? 1 : 0);
        builder.writeAsShort(1); // add mode
        builder.writeByte(type.asByte()); // iv type
        builder.writeAsByte(item.getPosition()); // slot id
        PacketHelper.addItemInfo(builder, item, true, false);

        return builder.getPacket();
    }

    public static GamePacket updateInventorySlot(InventoryType type, IItem item, boolean fromDrop) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeAsByte(fromDrop ? 1 : 0);
        //	builder.writeAsByte((slot2 > 0 ? 1 : 0) + 1);
        builder.writeAsByte(1);
        builder.writeAsByte(1);
        builder.writeByte(type.asByte()); // iv type
        builder.writeAsShort(item.getPosition()); // slot id
        builder.writeAsShort(item.getQuantity());
        /*	if (slot2 > 0) {
        builder.writeAsByte(1);
        builder.writeAsByte(type.getType());
        builder.writeAsShort(slot2);
        builder.writeAsShort(amt2);
        }*/
        return builder.getPacket();
    }

    public static GamePacket moveInventoryItem(InventoryType type, short src, short dst) {
        return moveInventoryItem(type, src, dst, (byte) -1);
    }

    public static GamePacket moveInventoryItem(InventoryType type, short src, short dst, short equipIndicator) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 02"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(src);
        builder.writeAsShort(dst);
        if (equipIndicator != -1) {
            builder.writeAsByte(equipIndicator);
        }
        return builder.getPacket();
    }

    public static GamePacket moveAndMergeInventoryItem(InventoryType type, byte src, byte dst, short total) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 02 03"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(src);
        builder.writeAsByte(1); // merge mode?
        builder.writeByte(type.asByte());
        builder.writeAsShort(dst);
        builder.writeAsShort(total);

        return builder.getPacket();
    }

    public static GamePacket moveAndMergeWithRestInventoryItem(InventoryType type, byte src, byte dst, short srcQ, short dstQ) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 02 01"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(src);
        builder.writeAsShort(srcQ);
        builder.writeBytes(HexTool.getByteArrayFromHexString("01"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(dst);
        builder.writeAsShort(dstQ);

        return builder.getPacket();
    }

    public static GamePacket clearInventoryItem(InventoryType type, short slot, boolean fromDrop) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeAsByte(fromDrop ? 1 : 0);
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 03"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(slot);

        return builder.getPacket();
    }

    public static GamePacket updateSpecialItemUse(IItem item, byte type) {
        PacketBuilder builder = new PacketBuilder();

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

            builder.writeAsByte(2); //?

        }

        return builder.getPacket();
    }

    public static GamePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeAsByte(1); // fromdrop always true
        builder.writeAsByte(destroyed ? 2 : 3);
        builder.writeAsByte(scroll.getQuantity() > 0 ? 1 : 3);
        builder.writeByte(GameConstants.getInventoryType(scroll.getItemId()).asByte());
        builder.writeAsShort(scroll.getPosition());

        if (scroll.getQuantity() > 0) {
            builder.writeAsShort(scroll.getQuantity());
        }
        builder.writeAsByte(3);
        if (!destroyed) {
            builder.writeByte(InventoryType.EQUIP.asByte());
            builder.writeAsShort(item.getPosition());
            builder.writeAsByte(0);
        }
        builder.writeByte(InventoryType.EQUIP.asByte());
        builder.writeAsShort(item.getPosition());
        if (!destroyed) {
            PacketHelper.addItemInfo(builder, item, true, true);
        }
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        builder.writeInt(chr);

        switch (scrollSuccess) {
            case SUCCESS:
                builder.writeAsShort(1);
                builder.writeAsShort(legendarySpirit ? 1 : 0);
                break;
            case FAIL:
                builder.writeAsShort(0);
                builder.writeAsShort(legendarySpirit ? 1 : 0);
                break;
            case CURSE:
                builder.writeAsByte(0);
                builder.writeAsByte(1);
                builder.writeAsShort(legendarySpirit ? 1 : 0);
                break;
        }
        builder.writeAsByte(0);
        return builder.getPacket();
    }

    public static GamePacket ItemMaker_Success() {
        final PacketBuilder builder = new PacketBuilder();
        //D6 00 00 00 00 00 01 00 00 00 00 DC DD 40 00 01 00 00 00 01 00 00 00 8A 1C 3D 00 01 00 00 00 00 00 00 00 00 B0 AD 01 00
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

    public static GamePacket explodeDrop(int oid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        builder.writeAsByte(4); // 4 = Explode
        builder.writeInt(oid);
        builder.writeAsShort(655);

        return builder.getPacket();
    }

    public static GamePacket removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, false, 0);
    }

    public static GamePacket removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        builder.writeAsByte(animation); // 0 = Expire, 1 = without animation, 2 = pickup, 4 = explode
        builder.writeInt(oid);
        if (animation >= 2) {
            builder.writeInt(cid);
            if (pet) { // allow pet pickup?
                builder.writeAsByte(slot);
            }
        }
        return builder.getPacket();
    }

    public static GamePacket updateCharLook(ChannelCharacter chr) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        builder.writeInt(chr.getId());
        builder.writeAsByte(1);
        PacketHelper.addCharLook(builder, chr, false);
        
        Inventory iv = chr.getEquippedItemsInventory();
        List<IItem> equipped = Lists.newLinkedList(iv);
        Collections.sort(equipped);
        
        List<Ring> rings = new ArrayList<Ring>();
        for (IItem item : equipped) {
            final IEquip equip = (IEquip) item;
            if (equip.getRingId() > -1) {
                rings.add(Ring.loadFromDb(equip.getRingId()));
            }
        }
        Collections.sort(rings);

        if (rings.size() > 0) {
            for (Ring ring : rings) {
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

    public static GamePacket dropInventoryItem(InventoryType type, short src) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 03"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(src);
        if (src < 0) {
            builder.writeAsByte(1);
        }
        return builder.getPacket();
    }

    public static GamePacket dropInventoryItemUpdate(InventoryType type, IItem item) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 01"));
        builder.writeByte(type.asByte());
        builder.writeAsShort(item.getPosition());
        builder.writeAsShort(item.getQuantity());

        return builder.getPacket();
    }

    public static GamePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, byte direction, int reflect, boolean is_pg, int oid, int pos_x, int pos_y) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DAMAGE_PLAYER.getValue());
        builder.writeInt(cid);
        builder.writeAsByte(skill);
        builder.writeInt(damage);
        builder.writeInt(monsteridfrom);
        builder.writeByte(direction);
        if (reflect > 0) {
            builder.writeAsByte(reflect);
            builder.writeAsByte(is_pg ? 1 : 0);
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

    public static GamePacket startQuest(final ChannelCharacter c, final short quest, final String data) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(1);
        builder.writeAsShort(quest);
        builder.writeAsByte(1);

        builder.writeLengthPrefixedString(data != null ? data : "");

        return builder.getPacket();
    }

    public static GamePacket forfeitQuest(ChannelCharacter c, short quest) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(1);
        builder.writeAsShort(quest);
        builder.writeAsShort(0);
        builder.writeAsByte(0);
        builder.writeInt(0);
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket completeQuest(final short quest) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(1);
        builder.writeAsShort(quest);
        builder.writeAsByte(2);
        builder.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

        return builder.getPacket();
    }

    public static GamePacket updateInfoQuest(final int quest, final String data) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(0x0B);
        builder.writeAsShort(quest);
        builder.writeLengthPrefixedString(data);

        return builder.getPacket();
    }

    public static GamePacket updateQuestInfo(ChannelCharacter c, short quest, int npc, byte progress) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.UPDATE_QUEST_INFO.getValue());
        builder.writeByte(progress);
        builder.writeAsShort(quest);
        builder.writeInt(npc);
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
            //builder.writeLengthPrefixedString("Resets: " + chr.getReborns());
        } else {
            final GuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildId());
            builder.writeLengthPrefixedString(gs.getName());
            final GuildUnion union = chr.getGuild().getUnion(chr.getClient());
            if (union == null) {
                builder.writeLengthPrefixedString("");
                //builder.writeLengthPrefixedString("Resets: " + chr.getReborns());
            } else {
                builder.writeLengthPrefixedString(union.getName());
            }
        }
        builder.writeAsByte(0);
        final Inventory equippedItemsInventory = chr.getEquippedItemsInventory();
        final IItem inv = equippedItemsInventory.getItem((byte) -114);
        final int peteqid = inv != null ? inv.getItemId() : 0;
        for (final Pet pet : chr.getPets()) {
            if (pet.getSummoned()) {
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

    private static void writeLongMask(PacketBuilder builder, List<Pair<BuffStat, Integer>> statups) {
        long firstmask = 0;
        long secondmask = 0;
        for (Pair<BuffStat, Integer> statup : statups) {
            if (statup.getLeft().isFirst()) {
                firstmask |= statup.getLeft().getValue();
            } else {
                secondmask |= statup.getLeft().getValue();
            }
        }
        builder.writeLong(firstmask);
        builder.writeLong(secondmask);
    }

    // List<Pair<MapleDisease, Integer>>
    private static void writeLongDiseaseMask(PacketBuilder builder, List<Pair<Disease, Integer>> statups) {
        long firstmask = 0;
        long secondmask = 0;
        for (Pair<Disease, Integer> statup : statups) {
            if (statup.getLeft().isFirst()) {
                firstmask |= statup.getLeft().getValue();
            } else {
                secondmask |= statup.getLeft().getValue();
            }
        }
        builder.writeLong(firstmask);
        builder.writeLong(secondmask);
    }

    private static void writeLongMaskFromList(PacketBuilder builder, List<BuffStat> statups) {
        long firstmask = 0;
        long secondmask = 0;
        for (BuffStat statup : statups) {
            if (statup.isFirst()) {
                firstmask |= statup.getValue();
            } else {
                secondmask |= statup.getValue();
            }
        }
        builder.writeLong(firstmask);
        builder.writeLong(secondmask);
    }

    public static GamePacket giveMount(int buffid, int skillid, List<Pair<BuffStat, Integer>> statups) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket givePirate(List<Pair<BuffStat, Integer>> statups, int duration, int skillid) {
        final boolean infusion = skillid == 5121009 || skillid == 15111005;
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
        writeLongMask(builder, statups);

        builder.writeAsShort(0);
        for (Pair<BuffStat, Integer> stat : statups) {
            builder.writeInt(stat.getRight().intValue());
            builder.writeLong(skillid);
            builder.writeZeroBytes(infusion ? 6 : 1);
            builder.writeAsShort(duration);
        }
        builder.writeAsShort(infusion ? 600 : 0);
        if (!infusion) {
            builder.writeAsByte(1); //does this only come in dash?
        }
        return builder.getPacket();
    }

    public static GamePacket giveForeignPirate(List<Pair<BuffStat, Integer>> statups, int duration, int cid, int skillid) {
        final boolean infusion = skillid == 5121009 || skillid == 15111005;
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        builder.writeInt(cid);
        writeLongMask(builder, statups);
        builder.writeAsShort(0);
        for (Pair<BuffStat, Integer> stat : statups) {
            builder.writeInt(stat.getRight().intValue());
            builder.writeLong(skillid);
            builder.writeZeroBytes(infusion ? 7 : 1);
            builder.writeAsShort(duration);//duration... seconds 
        }
        builder.writeAsShort(infusion ? 600 : 0);
        return builder.getPacket();
    }

    public static GamePacket giveEnergyChargeTest(int bar) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket giveInfusion(List<Pair<BuffStat, Integer>> statups, int buffid, int bufflength) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
        // 17 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 07 00 AE E1 3E 00 68 B9 01 00 00 00 00 00

        writeLongMask(builder, statups);

        builder.writeBytes(HexTool.getByteArrayFromHexString("00 00 FF FF FF FF F1 23 4E 00 00 00 00 00 00 00 00 00 00 00 6E 00 58 02"));

        return builder.getPacket();
    }

    public static GamePacket giveHoming(int skillid, int mobid) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket giveForeignInfusion(int cid, int speed, int duration) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        builder.writeInt(cid);
        builder.writeLong(0);
        builder.writeLong(BuffStat.MORPH.getValue()); //transform buffstat
        builder.writeAsShort(0);
        builder.writeInt(speed);
        builder.writeInt(5121009);
        builder.writeLong(0);
        builder.writeInt(duration);
        builder.writeAsShort(0);

        return builder.getPacket();
    }

    public static GamePacket giveBuff(int buffid, int bufflength, List<Pair<BuffStat, Integer>> statups, StatEffect effect) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
        writeLongMask(builder, statups);
        for (Pair<BuffStat, Integer> statup : statups) {
            builder.writeAsShort(statup.getRight().shortValue());
            builder.writeInt(buffid);
            builder.writeInt(bufflength);
        }
        builder.writeAsShort(0); // delay,  wk charges have 600 here o.o
        builder.writeAsShort(0); // combo 600, too
        builder.writeAsByte(effect.isMorph() || effect.isPirateMorph() ? 2 : 0);

        return builder.getPacket();
    }

    public static GamePacket giveDebuff(final List<Pair<Disease, Integer>> statups, final MobSkill skill) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_BUFF.getValue());
        writeLongDiseaseMask(builder, statups);
        for (Pair<Disease, Integer> statup : statups) {
            builder.writeAsShort(statup.getRight().shortValue());
            builder.writeAsShort(skill.getSkillId());
            builder.writeAsShort(skill.getSkillLevel());
            builder.writeInt((int) skill.getDuration());
        }
        builder.writeAsShort(0); // ??? wk charges have 600 here o.o
        builder.writeAsShort(2160); //Delay
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket giveForeignDebuff(int cid, final List<Pair<Disease, Integer>> statups, MobSkill skill) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        builder.writeInt(cid);

        writeLongDiseaseMask(builder, statups);

        if (skill.getSkillId() == 125) {
            builder.writeAsShort(0);
        }
        builder.writeAsShort(skill.getSkillId());
        builder.writeAsShort(skill.getSkillLevel());
        builder.writeAsShort(0); // same as give_buff
        builder.writeAsShort(900); //Delay

        return builder.getPacket();
    }

    public static GamePacket cancelForeignDebuff(int cid, long mask) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        builder.writeInt(cid);
        builder.writeLong(0);
        builder.writeLong(mask);

        return builder.getPacket();
    }

    public static GamePacket showMonsterRiding(int cid, List<Pair<BuffStat, Integer>> statups, int itemId, int skillId) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket giveForeignBuff(int cid, List<Pair<BuffStat, Integer>> statups, StatEffect effect) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        builder.writeInt(cid);
        writeLongMask(builder, statups);
        for (Pair<BuffStat, Integer> statup : statups) {
            if (effect.isMorph() && !effect.isPirateMorph()) {
                builder.writeByte(statup.getRight().byteValue());
            } else {
                builder.writeAsShort(statup.getRight().shortValue());
            }
        }
        builder.writeAsShort(0); // same as give_buff
        if (effect.isMorph()) {
            builder.writeAsShort(0);
        }
        builder.writeAsByte(0);

        return builder.getPacket();
    }

    public static GamePacket cancelForeignBuff(int cid, List<BuffStat> statups) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        builder.writeInt(cid);

        writeLongMaskFromList(builder, statups);

        return builder.getPacket();
    }

    public static GamePacket cancelBuff(List<BuffStat> statups) {
        return cancelBuff(statups, false);
    }

    public static GamePacket cancelBuff(List<BuffStat> statups, boolean mount) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket cancelDebuff(long mask) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_BUFF.getValue());
        builder.writeLong(0);
        builder.writeLong(mask);
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket updateMount(ChannelCharacter chr, boolean levelup) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.UPDATE_MOUNT.getValue());
        builder.writeInt(chr.getId());
        builder.writeInt(chr.getMount().getLevel());
        builder.writeInt(chr.getMount().getExp());
        builder.writeInt(chr.getMount().getFatigue());
        builder.writeAsByte(levelup ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket mountInfo(ChannelCharacter chr) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.UPDATE_MOUNT.getValue());
        builder.writeInt(chr.getId());
        builder.writeAsByte(1);
        builder.writeInt(chr.getMount().getLevel());
        builder.writeInt(chr.getMount().getExp());
        builder.writeInt(chr.getMount().getFatigue());

        return builder.getPacket();
    }

    public static GamePacket getPlayerShopChat(ChannelCharacter c, String chat, boolean owner) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("06 08"));
        builder.writeAsByte(owner ? 0 : 1);
        builder.writeLengthPrefixedString(c.getName() + " : " + chat);

        return builder.getPacket();
    }

    public static GamePacket getPlayerShopNewVisitor(ChannelCharacter c, int slot) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("04 0" + slot));
        PacketHelper.addCharLook(builder, c, false);
        builder.writeLengthPrefixedString(c.getName());
        builder.writeAsShort(c.getJobId());

        return builder.getPacket();
    }

    public static GamePacket getPlayerShopRemoveVisitor(int slot) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeBytes(HexTool.getByteArrayFromHexString("0A 0" + slot));

        return builder.getPacket();
    }

    public static GamePacket getTradePartnerAdd(ChannelCharacter c) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(4);
        builder.writeAsByte(1);
        PacketHelper.addCharLook(builder, c, false);
        builder.writeLengthPrefixedString(c.getName());
        builder.writeAsShort(c.getJobId());

        return builder.getPacket();
    }

    public static GamePacket getTradeInvite(ChannelCharacter c) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(2);
        builder.writeAsByte(3);
        builder.writeLengthPrefixedString(c.getName());
        builder.writeInt(0); // Trade ID

        return builder.getPacket();
    }

    public static GamePacket getTradeMesoSet(byte number, int meso) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(0xF);
        builder.writeByte(number);
        builder.writeInt(meso);

        return builder.getPacket();
    }

    public static GamePacket getTradeItemAdd(byte number, IItem item) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(0xE);
        builder.writeByte(number);
        PacketHelper.addItemInfo(builder, item, false, false, true);

        return builder.getPacket();
    }

    public static GamePacket getTradeStart(ChannelClient c, Trade trade, byte number) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(5);
        builder.writeAsByte(3);
        builder.writeAsByte(2);
        builder.writeByte(number);

        if (number == 1) {
            builder.writeAsByte(0);
            PacketHelper.addCharLook(builder, trade.getPartner().getChr(), false);
            builder.writeLengthPrefixedString(trade.getPartner().getChr().getName());
            builder.writeAsShort(trade.getPartner().getChr().getJobId());
        }
        builder.writeByte(number);
        PacketHelper.addCharLook(builder, c.getPlayer(), false);
        builder.writeLengthPrefixedString(c.getPlayer().getName());
        builder.writeAsShort(c.getPlayer().getJobId());
        builder.writeAsByte(0xFF);

        return builder.getPacket();
    }

    public static GamePacket getTradeConfirmation() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(0x10);

        return builder.getPacket();
    }

    public static GamePacket TradeMessage(final byte UserSlot, final byte message) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(0xA);
        builder.writeByte(UserSlot);
        builder.writeByte(message);
        //0x06 = success [tax is automated]
        //0x07 = unsuccessful
        //0x08 = "You cannot make the trade because there are some items which you cannot carry more than one."
        //0x09 = "You cannot make the trade because the other person's on a different map."

        return builder.getPacket();
    }

    public static GamePacket getTradeCancel(final byte UserSlot) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
        builder.writeAsByte(0xA);
        builder.writeByte(UserSlot);
        builder.writeAsByte(2);

        return builder.getPacket();
    }

    public static GamePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type) {
        PacketBuilder builder = new PacketBuilder();

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
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
        builder.writeAsByte(4);
        builder.writeInt(npcid);
        builder.writeAsByte(0xE);
        builder.writeZeroBytes(5);
        builder.writeInt(5);
        builder.writeLengthPrefixedString(sel);

        return builder.getPacket();
    }

    public static GamePacket getNPCTalkStyle(int npc, String talk, int... args) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
        builder.writeAsByte(4);
        builder.writeInt(npc);
        builder.writeAsShort(8);
        builder.writeLengthPrefixedString(talk);
        builder.writeAsByte(args.length);
        for (int i = 0; i < args.length; i++) {
            builder.writeInt(args[i]);
        }

        return builder.getPacket();
    }

    public static GamePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket getNPCTalkText(int npc, String talk) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
        builder.writeAsByte(4);
        builder.writeInt(npc);
        builder.writeAsShort(3);
        builder.writeLengthPrefixedString(talk);
        builder.writeInt(0);
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket showForeignEffect(int cid, int effect) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        builder.writeInt(cid);
        builder.writeAsByte(effect); // 0 = Level up, 8 = job change

        return builder.getPacket();
    }

    public static GamePacket showBuffeffect(int cid, int skillid, int effectid) {
        return showBuffeffect(cid, skillid, effectid, (byte) 3);
    }

    public static GamePacket showBuffeffect(int cid, int skillid, int effectid, byte direction) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        builder.writeInt(cid);
        builder.writeAsByte(effectid);
        builder.writeInt(skillid);
        builder.writeAsByte(1); // probably buff level but we don't know it and it doesn't really matter
        builder.writeAsByte(1);

        if (direction != (byte) 3) {
            builder.writeByte(direction);
        }
        return builder.getPacket();
    }

    public static GamePacket showOwnBuffEffect(int skillid, int effectid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        builder.writeAsByte(effectid);
        builder.writeInt(skillid);
        builder.writeAsByte(1); // probably buff level but we don't know it and it doesn't really matter
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket showOwnBerserk(int skilllevel, boolean Berserk) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        builder.writeAsByte(1);
        builder.writeInt(1320006);
        builder.writeAsByte(skilllevel);
        builder.writeAsByte(Berserk ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket showBerserk(int cid, int skilllevel, boolean Berserk) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        builder.writeInt(cid);
        builder.writeAsByte(1);
        builder.writeInt(1320006);
        builder.writeAsByte(skilllevel);
        builder.writeAsByte(Berserk ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket showSpecialEffect(int effect) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        builder.writeAsByte(effect);

        return builder.getPacket();
    }

    public static GamePacket showSpecialEffect(int cid, int effect) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        builder.writeInt(cid);
        builder.writeAsByte(effect);

        return builder.getPacket();
    }

    public static GamePacket updateSkill(int skillid, int level, int masterlevel) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeAsShort(status.getQuest().getId());
        builder.writeAsByte(1);

        final StringBuilder sb = new StringBuilder();
        for (final int kills : status.getMobKills().values()) {
            sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
        }
        builder.writeLengthPrefixedString(sb.toString());
        builder.writeZeroBytes(8);

        return builder.getPacket();
    }

    public static GamePacket getShowQuestCompletion(int id) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        builder.writeAsShort(id);

        return builder.getPacket();
    }

    public static GamePacket getKeymap(KeyLayout layout) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.KEYMAP.getValue());
        builder.writeAsByte(0);
        layout.writeData(builder);

        return builder.getPacket();
    }

    public static GamePacket getWhisper(String sender, int channel, String text) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
        builder.writeAsByte(0x12);
        builder.writeLengthPrefixedString(sender);
        builder.writeAsShort(channel - 1);
        builder.writeLengthPrefixedString(text);

        return builder.getPacket();
    }

    public static GamePacket getWhisperReply(String target, byte reply) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
        builder.writeAsByte(0x0A); // whisper?
        builder.writeLengthPrefixedString(target);
        builder.writeByte(reply);//  0x0 = cannot find char, 0x1 = success

        return builder.getPacket();
    }

    public static GamePacket getFindReplyWithMap(String target, int mapid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
        builder.writeAsByte(9);
        builder.writeLengthPrefixedString(target);
        builder.writeAsByte(1);
        builder.writeInt(mapid);
        builder.writeZeroBytes(8); // ?? official doesn't send zeros here but whatever

        return builder.getPacket();
    }

    public static GamePacket getFindReply(String target, int channel) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
        builder.writeAsByte(9);
        builder.writeLengthPrefixedString(target);
        builder.writeAsByte(3);
        builder.writeInt(channel - 1);

        return builder.getPacket();
    }

    public static GamePacket getInventoryFull() {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket getShowInventoryStatus(int mode) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(0);
        builder.writeAsByte(mode);
        builder.writeInt(0);
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket getStorage(int npcId, byte slots, Collection<IItem> items, int meso) {
        PacketBuilder builder = new PacketBuilder();

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
        for (IItem item : items) {
            PacketHelper.addItemInfo(builder, item, true, true);
        }
        builder.writeAsShort(0);
        builder.writeAsByte(0);

        return builder.getPacket();
    }

    public static GamePacket getStorageFull() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
        builder.writeAsByte(0x11);

        return builder.getPacket();
    }

    public static GamePacket mesoStorage(byte slots, int meso) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
        builder.writeAsByte(0x13);
        builder.writeByte(slots);
        builder.writeAsShort(2);
        builder.writeAsShort(0);
        builder.writeInt(0);
        builder.writeInt(meso);

        return builder.getPacket();
    }

    public static GamePacket storeStorage(byte slots, InventoryType type, Collection<IItem> items) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
        builder.writeAsByte(0x0D);
        builder.writeByte(slots);
        builder.writeAsShort(type.getBitfieldEncoding());
        builder.writeAsShort(0);
        builder.writeInt(0);
        builder.writeAsByte(items.size());
        for (IItem item : items) {
            PacketHelper.addItemInfo(builder, item, true, true);
        }
        return builder.getPacket();
    }

    public static GamePacket takeOutStorage(byte slots, InventoryType type, Collection<IItem> items) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.OPEN_STORAGE.getValue());
        builder.writeAsByte(0x9);
        builder.writeByte(slots);
        builder.writeAsShort(type.getBitfieldEncoding());
        builder.writeAsShort(0);
        builder.writeInt(0);
        builder.writeAsByte(items.size());
        for (IItem item : items) {
            PacketHelper.addItemInfo(builder, item, true, true);
        }
        return builder.getPacket();
    }

    public static GamePacket fairyPendantMessage(int type, int percent) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.FAIRY_PEND_MSG.getValue());
        builder.writeAsShort(21); // 0x15
        builder.writeInt(0); // idk
        builder.writeAsShort(0); // idk
        builder.writeAsShort(percent); // percent
        builder.writeAsShort(0); // idk

        return builder.getPacket();
    }

    public static GamePacket giveFameResponse(int mode, String charname, int newfame) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.FAME_RESPONSE.getValue());
        builder.writeAsByte(0);
        builder.writeLengthPrefixedString(charname);
        builder.writeAsByte(mode);
        builder.writeAsShort(newfame);
        builder.writeAsShort(0);

        return builder.getPacket();
    }

    public static GamePacket giveFameErrorResponse(int status) {
        PacketBuilder builder = new PacketBuilder();

        /*	* 0: ok, use giveFameResponse<br>
         * 1: the username is incorrectly entered<br>
         * 2: users under level 15 are unable to toggle with fame.<br>
         * 3: can't raise or drop fame anymore today.<br>
         * 4: can't raise or drop fame for this character for this month anymore.<br>
         * 5: received fame, use receiveFame()<br>
         * 6: level of fame neither has been raised nor dropped due to an unexpected error*/

        builder.writeAsShort(ServerPacketOpcode.FAME_RESPONSE.getValue());
        builder.writeAsByte(status);

        return builder.getPacket();
    }

    public static GamePacket receiveFame(int mode, String charnameFrom) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.FAME_RESPONSE.getValue());
        builder.writeAsByte(5);
        builder.writeLengthPrefixedString(charnameFrom);
        builder.writeAsByte(mode);

        return builder.getPacket();
    }

    public static GamePacket partyCreated() {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket partyInvite(ChannelCharacter from) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
        builder.writeAsByte(4);
        builder.writeInt(from.getParty().getId());
        builder.writeLengthPrefixedString(from.getName());
        builder.writeInt(from.getLevel());
        builder.writeInt(from.getJobId());
        builder.writeAsByte(0);

        return builder.getPacket();
    }

    public static GamePacket partyStatusMessage(int message) {
        PacketBuilder builder = new PacketBuilder();

        /*	* 10: A beginner can't create a party.
         * 1/11/14/19: Your request for a party didn't work due to an unexpected error.
         * 13: You have yet to join a party.
         * 16: Already have joined a party.
         * 17: The party you're trying to join is already in full capacity.
         * 19: Unable to find the requested character in this channel.*/
        builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
        builder.writeAsByte(message);

        return builder.getPacket();
    }

    public static GamePacket partyStatusMessage(int message, String charname) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
        builder.writeAsByte(message); // 23: 'Char' have denied request to the party.
        builder.writeLengthPrefixedString(charname);

        return builder.getPacket();
    }

    private static void addPartyStatus(int forchannel, Party party, PacketBuilder lew, boolean leaving) {
        List<PartyMember> partymembers = new ArrayList<PartyMember>(party.getMembers());
        while (partymembers.size() < 6) {
            partymembers.add(new PartyMember());
        }
        for (PartyMember partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (PartyMember partychar : partymembers) {
            lew.writePaddedString(partychar.getName(), 13);
        }
        for (PartyMember partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (PartyMember partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (PartyMember partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        lew.writeInt(party.getLeader().getId());
        for (PartyMember partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(0);
            }
        }
        for (PartyMember partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                lew.writeInt(partychar.getDoorTown());
                lew.writeInt(partychar.getDoorTarget());
                lew.writeInt(2311002);
                lew.writeInt(partychar.getDoorPosition().x);
                lew.writeInt(partychar.getDoorPosition().y);
            } else {
                lew.writeInt(leaving ? 999999999 : 0);
                lew.writeLong(leaving ? 999999999 : 0);
                lew.writeLong(leaving ? -1 : 0);
            }
        }
    }

    public static GamePacket updateParty(int forChannel, Party party, PartyOperation op, PartyMember target) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                builder.writeAsByte(0xC);
                builder.writeInt(party.getId());
                builder.writeInt(target.getId());
                if (op == PartyOperation.DISBAND) {
                    builder.writeAsByte(0);
                    builder.writeInt(target.getId());
                } else {
                    builder.writeAsByte(1);
                    if (op == PartyOperation.EXPEL) {
                        builder.writeAsByte(1);
                    } else {
                        builder.writeAsByte(0);
                    }
                    builder.writeLengthPrefixedString(target.getName());
                    addPartyStatus(forChannel, party, builder, op == PartyOperation.LEAVE);
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
                addPartyStatus(forChannel, party, builder, op == PartyOperation.LOG_ONOFF);
                break;
            case CHANGE_LEADER:
                builder.writeAsByte(0x1F);
                builder.writeInt(target.getId());
                builder.writeAsByte(0);
                break;
        }
        return builder.getPacket();
    }

    public static GamePacket partyPortal(int townId, int targetId, Point position) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.PARTY_OPERATION.getValue());
        builder.writeAsShort(0x28);
        builder.writeInt(townId);
        builder.writeInt(targetId);
        builder.writeInt(2311002);
        builder.writeVector(position);

        return builder.getPacket();
    }

    public static GamePacket updatePartyMemberHP(int cid, int curhp, int maxhp) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        builder.writeInt(cid);
        builder.writeInt(curhp);
        builder.writeInt(maxhp);

        return builder.getPacket();
    }

    public static GamePacket multiChat(String name, String chattext, int mode) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MULTICHAT.getValue());
        builder.writeAsByte(mode); //  0 buddychat; 1 partychat; 2 guildchat
        builder.writeLengthPrefixedString(name);
        builder.writeLengthPrefixedString(chattext);

        return builder.getPacket();
    }

    public static GamePacket getClock(int time) { // time in seconds
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CLOCK.getValue());
        builder.writeAsByte(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        builder.writeInt(time);

        return builder.getPacket();
    }

    public static GamePacket getClockTime(int hour, int min, int sec) { // Current Time
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CLOCK.getValue());
        builder.writeAsByte(1); //Clock-Type
        builder.writeAsByte(hour);
        builder.writeAsByte(min);
        builder.writeAsByte(sec);

        return builder.getPacket();
    }

    public static GamePacket spawnMist(final Mist mist) {
        PacketBuilder builder = new PacketBuilder();

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
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_MIST.getValue());
        builder.writeInt(oid);

        return builder.getPacket();
    }

    public static GamePacket damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DAMAGE_SUMMON.getValue());
        builder.writeInt(cid);
        builder.writeInt(summonSkillId);
        builder.writeAsByte(unkByte);
        builder.writeInt(damage);
        builder.writeInt(monsterIdFrom);
        builder.writeAsByte(0);

        return builder.getPacket();
    }

    public static GamePacket buddylistMessage(byte message) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
        builder.writeByte(message);

        return builder.getPacket();
    }

    public static GamePacket updateBuddyList(Collection<BuddyListEntry> buddylist) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
        builder.writeAsByte(7);
        builder.writeAsByte(buddylist.size());

        for (BuddyListEntry buddy : buddylist) {
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

    public static GamePacket requestBuddylistAdd(int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
        PacketBuilder builder = new PacketBuilder();

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
        builder.writeBytes(HexTool.getByteArrayFromHexString("44 65 66 61 75 6C 74 20 47 72 6F 75 70 00 6E 67")); // default group
        builder.writeAsShort(1);

        return builder.getPacket();
    }

    public static GamePacket updateBuddyChannel(int characterid, int channel) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
        builder.writeAsByte(0x14);
        builder.writeInt(characterid);
        builder.writeAsByte(0);
        builder.writeInt(channel);

        return builder.getPacket();
    }

    public static GamePacket itemEffect(int characterid, int itemid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        builder.writeInt(characterid);
        builder.writeInt(itemid);

        return builder.getPacket();
    }

    public static GamePacket updateBuddyCapacity(int capacity) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BUDDYLIST.getValue());
        builder.writeAsByte(0x15);
        builder.writeAsByte(capacity);

        return builder.getPacket();
    }

    public static GamePacket showChair(int characterid, int itemid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_CHAIR.getValue());
        builder.writeInt(characterid);
        builder.writeInt(itemid);

        return builder.getPacket();
    }

    public static GamePacket cancelChair(int id) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            builder.writeAsByte(0);
        } else {
            builder.writeAsByte(1);
            builder.writeAsShort(id);
        }
        return builder.getPacket();
    }

    public static GamePacket spawnReactor(Reactor reactor) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REACTOR_SPAWN.getValue());
        builder.writeInt(reactor.getObjectId());
        builder.writeInt(reactor.getReactorId());
        builder.writeByte(reactor.getState());
        builder.writeVector(reactor.getPosition());
        builder.writeByte(reactor.getFacingDirection()); // stance
        builder.writeAsShort(0);

        return builder.getPacket();
    }

    public static GamePacket triggerReactor(Reactor reactor, int stance) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REACTOR_HIT.getValue());
        builder.writeInt(reactor.getObjectId());
        builder.writeByte(reactor.getState());
        builder.writeVector(reactor.getPosition());
        builder.writeAsShort(stance);
        builder.writeAsByte(0);
        builder.writeAsByte(4); // frame delay, set to 5 since there doesn't appear to be a fixed formula for it

        return builder.getPacket();
    }

    public static GamePacket destroyReactor(Reactor reactor) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REACTOR_DESTROY.getValue());
        builder.writeInt(reactor.getObjectId());
        builder.writeByte(reactor.getState());
        builder.writeVector(reactor.getPosition());

        return builder.getPacket();
    }

    public static GamePacket musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static GamePacket showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static GamePacket playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static GamePacket environmentChange(String env, int mode) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
        builder.writeAsByte(mode);
        builder.writeLengthPrefixedString(env);

        return builder.getPacket();
    }

    public static GamePacket startMapEffect(String msg, int itemid, boolean active) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MAP_EFFECT.getValue());
        builder.writeAsByte(active ? 0 : 1);
        builder.writeInt(itemid);
        if (active) {
            builder.writeLengthPrefixedString(msg);
        }
        return builder.getPacket();
    }

    public static GamePacket removeMapEffect() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MAP_EFFECT.getValue());
        builder.writeAsByte(0);
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket showGuildInfo(ChannelCharacter c) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x1A); //signature for showing guild info

        if (c == null) { //show empty guild (used for leaving, expelled)
            builder.writeAsByte(0);
            return builder.getPacket();
        }
        GuildMember initiator = c.getGuildMembership();
        Guild g = c.getClient().getChannelServer().getGuild(initiator);
        if (g == null) { //failed to read from DB - don't show a guild
            builder.writeAsByte(0);
            return builder.getPacket();
        } else {
            //MapleGuild holds the absolute correct value of guild rank after it is initiated
            GuildMember mgc = g.getMember(c.getId());
            c.setGuildRank(mgc.getRank());
        }
        builder.writeAsByte(1); //bInGuild
        builder.writeInt(c.getGuildId()); //not entirely sure about this one
        builder.writeLengthPrefixedString(g.getName());
        for (int i = 1; i <= 5; i++) {
            final MemberRank rank = MemberRank.fromNumber(i);
            builder.writeLengthPrefixedString(g.getRankTitle(rank));
        }
        g.addMemberData(builder);

        builder.writeInt(g.getCapacity());
        builder.writeAsShort(g.getLogoBG());
        builder.writeAsByte(g.getLogoBGColor());
        builder.writeAsShort(g.getLogo());
        builder.writeAsByte(g.getLogoColor());
        builder.writeLengthPrefixedString(g.getNotice());
        builder.writeInt(g.getGP());
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket guildMemberOnline(int gid, int cid, boolean bOnline) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x3d);
        builder.writeInt(gid);
        builder.writeInt(cid);
        builder.writeAsByte(bOnline ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket guildInvite(int gid, String charName, int levelFrom, int jobFrom) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x05);
        builder.writeInt(gid);
        builder.writeLengthPrefixedString(charName);
        builder.writeInt(levelFrom);
        builder.writeInt(jobFrom);

        return builder.getPacket();
    }

    public static GamePacket denyGuildInvitation(String charname) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x37);
        builder.writeLengthPrefixedString(charname);

        return builder.getPacket();
    }

    public static GamePacket genericGuildMessage(byte code) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeByte(code);

        return builder.getPacket();
    }

    public static GamePacket newGuildMember(GuildMember mgc) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x27);
        builder.writeInt(mgc.getGuildId());
        builder.writeInt(mgc.getId());
        builder.writePaddedString(mgc.getName(), 13);
        builder.writeInt(mgc.getJobId());
        builder.writeInt(mgc.getLevel());
        builder.writeInt(mgc.getRank().asNumber() + 1); //should be always 5 but whatevs
        builder.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        builder.writeInt(1); //? could be guild signature, but doesn't seem to matter
        builder.writeInt(3);

        return builder.getPacket();
    }

    //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static GamePacket memberLeft(GuildMember mgc, boolean bExpelled) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(bExpelled ? 0x2f : 0x2c);

        builder.writeInt(mgc.getGuildId());
        builder.writeInt(mgc.getId());
        builder.writeLengthPrefixedString(mgc.getName());

        return builder.getPacket();
    }

    public static GamePacket changeRank(GuildMember mgc) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x40);
        builder.writeInt(mgc.getGuildId());
        builder.writeInt(mgc.getId());
        builder.writeAsByte(mgc.getRank().asNumber() + 1);

        return builder.getPacket();
    }

    public static GamePacket guildNotice(int gid, String notice) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x44);
        builder.writeInt(gid);
        builder.writeLengthPrefixedString(notice);

        return builder.getPacket();
    }

    public static GamePacket guildMemberLevelJobUpdate(GuildMember mgc) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x3C);
        builder.writeInt(mgc.getGuildId());
        builder.writeInt(mgc.getId());
        builder.writeInt(mgc.getLevel());
        builder.writeInt(mgc.getJobId());

        return builder.getPacket();
    }

    public static GamePacket rankTitleChange(int gid, String[] ranks) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x3e);
        builder.writeInt(gid);
        for (String r : ranks) {
            builder.writeLengthPrefixedString(r);
        }

        return builder.getPacket();
    }

    public static GamePacket guildDisband(int gid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x32);
        builder.writeInt(gid);
        builder.writeAsByte(1);

        return builder.getPacket();
    }

    public static GamePacket guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x42);
        builder.writeInt(gid);
        builder.writeAsShort(bg);
        builder.writeByte(bgcolor);
        builder.writeAsShort(logo);
        builder.writeByte(logocolor);

        return builder.getPacket();
    }

    public static GamePacket guildCapacityChange(int gid, int capacity) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x3a);
        builder.writeInt(gid);
        builder.writeAsByte(capacity);

        return builder.getPacket();
    }

    public static GamePacket showGuildUnionInfo(ChannelCharacter chr) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ALLIANCE_OPERATION.getValue());
        builder.writeAsByte(0x0C);
        GuildUnion union = chr.getGuild().getUnion(chr.getClient());
        if (union == null) { //show empty alliance (used for leaving, expelled)
            builder.writeAsByte(0);
            return builder.getPacket();
        }
        builder.writeAsByte(1); //Only happens if you are in an alliance
        builder.writeInt(union.getId());
        builder.writeLengthPrefixedString(union.getName()); // alliance name
        for (String title : union.getTitles()) {
            builder.writeLengthPrefixedString(title);
        }
        builder.writeAsByte(union.getAmountOfGuilds());//ammount of guilds joined
        for (int z = 0; z < 5; z++) {
            if (union.getGuilds().get(z) != null) {
                builder.writeInt(union.getGuilds().get(z).getId());
            }
        }
        builder.writeInt(3);//3..
        builder.writeLengthPrefixedString(union.getNotice());

        return builder.getPacket();
    }

    public static GamePacket createAlliance(String name) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ALLIANCE_OPERATION.getValue());
        builder.writeAsByte(0x0F);

        return builder.getPacket();
    }

    public static GamePacket showAllianceMembers(ChannelCharacter chr) {
        PacketBuilder builder = new PacketBuilder();
        builder.writeAsShort(ServerPacketOpcode.ALLIANCE_OPERATION.getValue());
        builder.writeAsByte(0x0D);
        GuildUnion az = chr.getGuild().getUnion(chr.getClient());
        int e = 0;
        for (int u = 0; u < 5; u++) {
            if (az.getGuilds().get(u) != null) {
                e++;
            }
        }
        builder.writeInt(e);//ammount of guilds joined
        chr.setGuildRank(chr.getGuild().getMember(chr.getId()).getRank());
        for (int i = 0; i < 5; i++) {
            Guild g = az.getGuilds().get(i);
            if (g != null) {
                builder.writeInt(g.getId());
                builder.writeLengthPrefixedString(g.getName());
                for (int ordinal = 1; ordinal <= 5; ordinal++) {
                    final MemberRank rank = MemberRank.fromNumber(ordinal); 
                    builder.writeLengthPrefixedString(g.getRankTitle(rank));
                }
                g.addMemberData(builder);
                builder.writeInt(g.getCapacity());
                builder.writeAsShort(g.getLogoBG());
                builder.writeAsByte(g.getLogoBGColor());
                builder.writeAsShort(g.getLogo());
                builder.writeAsByte(g.getLogoColor());
                builder.writeLengthPrefixedString(g.getNotice());
                builder.writeInt(g.getGP());
                builder.writeBytes(HexTool.getByteArrayFromHexString("0F 03 00 00"));
            }
        }
        return builder.getPacket();
    }

    public static GamePacket BBSThreadList(ResultSet rs, int start) throws SQLException {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BBS_OPERATION.getValue());
        builder.writeAsByte(6);
        int threadCount = rs.getRow();
        if (!rs.last()) {
            builder.writeAsByte(0);
        } else if (rs.getInt("localthreadid") == 0) { //has a notice
            builder.writeAsByte(1);
            addThread(builder, rs);
            threadCount--; //one thread didn't count (because it's a notice)
        } else {
            builder.writeAsByte(0);
        }
        if (!rs.absolute(start + 1)) { //seek to the thread before where we start
            rs.first(); //uh, we're trying to start at a place past possible
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

    private static void addThread(PacketBuilder builder, ResultSet rs) throws SQLException {
        builder.writeInt(rs.getInt("localthreadid"));
        builder.writeInt(rs.getInt("postercid"));
        builder.writeLengthPrefixedString(rs.getString("name"));
        builder.writeLong(PacketHelper.getKoreanTimestamp(rs.getLong("timestamp")));
        builder.writeInt(rs.getInt("icon"));
        builder.writeInt(rs.getInt("replycount"));
    }

    public static GamePacket showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.BBS_OPERATION.getValue());
        builder.writeAsByte(7);
        builder.writeInt(localthreadid);
        builder.writeInt(threadRS.getInt("postercid"));
        builder.writeLong(PacketHelper.getKoreanTimestamp(threadRS.getLong("timestamp")));
        builder.writeLengthPrefixedString(threadRS.getString("name"));
        builder.writeLengthPrefixedString(threadRS.getString("startpost"));
        builder.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            builder.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                builder.writeInt(repliesRS.getInt("replyid"));
                builder.writeInt(repliesRS.getInt("postercid"));
                builder.writeLong(PacketHelper.getKoreanTimestamp(repliesRS.getLong("timestamp")));
                builder.writeLengthPrefixedString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                //in the unlikely event that we lost count of replyid
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
                /*we need to fix the database and stop the packet sending
                or else it'll probably error 38 whoever tries to read it
                there is ONE case not checked, and that's when the thread
                has a replycount of 0 and there is one or more replies to the
                thread in bbs_replies*/
            }
        } else {
            builder.writeInt(0); //0 replies
        }
        return builder.getPacket();
    }

    public static GamePacket showGuildRanks(int npcid, List<GuildRankingInfo> all) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x49);
        builder.writeInt(npcid);
        builder.writeInt(all.size());
        for (GuildRankingInfo info : all) {
            builder.writeLengthPrefixedString(info.getName());
            builder.writeInt(info.getGP());
            builder.writeInt(info.getLogo());
            builder.writeInt(info.getLogoBg());
            builder.writeInt(info.getLogoBgColor());
            builder.writeInt(info.getLogoColor());
        }

        return builder.getPacket();
    }

    public static GamePacket updateGP(int gid, int GP) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.GUILD_OPERATION.getValue());
        builder.writeAsByte(0x48);
        builder.writeInt(gid);
        builder.writeInt(GP);

        return builder.getPacket();
    }

    public static GamePacket skillEffect(ChannelCharacter from, int skillId, byte level, byte flags, byte speed, byte unk) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SKILL_EFFECT.getValue());
        builder.writeInt(from.getId());
        builder.writeInt(skillId);
        builder.writeByte(level);
        builder.writeByte(flags);
        builder.writeByte(speed);
        builder.writeByte(unk); // Direction ??

        return builder.getPacket();
    }

    public static GamePacket skillCancel(ChannelCharacter from, int skillId) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        builder.writeInt(from.getId());
        builder.writeInt(skillId);

        return builder.getPacket();
    }

    public static GamePacket showMagnet(int mobid, byte success) { // Monster Magnet
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_MAGNET.getValue());
        builder.writeInt(mobid);
        builder.writeByte(success);

        return builder.getPacket();
    }

    public static GamePacket sendHint(String hint, int width, int height) {
        PacketBuilder builder = new PacketBuilder();

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

    public static GamePacket messengerInvite(String from, int messengerid) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(0x03);
        builder.writeLengthPrefixedString(from);
        builder.writeAsByte(0x00);
        builder.writeInt(messengerid);
        builder.writeAsByte(0x00);

        return builder.getPacket();
    }

    public static GamePacket addMessengerPlayer(String from, ChannelCharacter chr, int position, int channel) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(0x00);
        builder.writeAsByte(position);
        PacketHelper.addCharLook(builder, chr, true);
        builder.writeLengthPrefixedString(from);
        builder.writeAsByte(channel);
        builder.writeAsByte(0x00);

        return builder.getPacket();
    }

    public static GamePacket removeMessengerPlayer(int position) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(0x02);
        builder.writeAsByte(position);

        return builder.getPacket();
    }

    public static GamePacket updateMessengerPlayer(String from, ChannelCharacter chr, int position, int channel) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(0x07);
        builder.writeAsByte(position);
        PacketHelper.addCharLook(builder, chr, true);
        builder.writeLengthPrefixedString(from);
        builder.writeAsByte(channel);
        builder.writeAsByte(0x00);

        return builder.getPacket();
    }

    public static GamePacket joinMessenger(int position) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(0x01);
        builder.writeAsByte(position);

        return builder.getPacket();
    }

    public static GamePacket messengerChat(String text) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(0x06);
        builder.writeLengthPrefixedString(text);

        return builder.getPacket();
    }

    public static GamePacket messengerNote(String text, int mode, int mode2) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.MESSENGER.getValue());
        builder.writeAsByte(mode);
        builder.writeLengthPrefixedString(text);
        builder.writeAsByte(mode2);

        return builder.getPacket();
    }

    public static GamePacket getFindReplyWithCS(String target) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.WHISPER.getValue());
        builder.writeAsByte(9);
        builder.writeLengthPrefixedString(target);
        builder.writeAsByte(2);
        builder.writeInt(-1);

        return builder.getPacket();
    }

    public static GamePacket showEquipEffect() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_EQUIP_EFFECT.getValue());

        return builder.getPacket();
    }

    public static GamePacket summonSkill(int cid, int summonSkillId, int newStance) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SUMMON_SKILL.getValue());
        builder.writeInt(cid);
        builder.writeInt(summonSkillId);
        builder.writeAsByte(newStance);

        return builder.getPacket();
    }

    public static GamePacket skillCooldown(int sid, int time) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.COOLDOWN.getValue());
        builder.writeInt(sid);
        builder.writeAsShort(time);

        return builder.getPacket();
    }

    public static GamePacket useSkillBook(ChannelCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.USE_SKILL_BOOK.getValue());
        builder.writeInt(chr.getId());
        builder.writeAsByte(1);
        builder.writeInt(skillid);
        builder.writeInt(maxlevel);
        builder.writeAsByte(canuse ? 1 : 0);
        builder.writeAsByte(success ? 1 : 0);

        return builder.getPacket();
    }

    public static GamePacket getMacros(SkillMacro[] macros) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        builder.writeAsByte(count); // number of macros
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
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

    public static GamePacket updateAriantPQRanking(String name, int score, boolean empty) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ARIANT_PQ_START.getValue());
        builder.writeAsByte(empty ? 0 : 1);
        if (!empty) {
            builder.writeLengthPrefixedString(name);
            builder.writeInt(score);
        }
        return builder.getPacket();
    }

    public static GamePacket catchMonster(int mobid, int itemid, byte success) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CATCH_MONSTER.getValue());
        builder.writeInt(mobid);
        builder.writeInt(itemid);
        builder.writeByte(success);

        return builder.getPacket();
    }

    public static GamePacket showAriantScoreBoard() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ARIANT_SCOREBOARD.getValue());

        return builder.getPacket();
    }

    public static GamePacket showZakumShrineTimeLeft(int timeleft) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ZAKUM_SHRINE.getValue());
        builder.writeAsByte(0);
        builder.writeInt(timeleft);

        return builder.getPacket();
    }

    public static GamePacket boatPacket(int effect) {
        PacketBuilder builder = new PacketBuilder();

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        builder.writeAsShort(ServerPacketOpcode.BOAT_EFFECT.getValue());
        builder.writeAsShort(effect); // 0A 04 balrog

        return builder.getPacket();
    }

    public static GamePacket removeItemFromDuey(boolean remove, int Package) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DUEY.getValue());
        builder.writeAsByte(0x18);
        builder.writeInt(Package);
        builder.writeAsByte(remove ? 3 : 4);

        return builder.getPacket();
    }

    public static GamePacket sendDuey(byte operation, List<DueyActions> packages) {
        PacketBuilder builder = new PacketBuilder();

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
                for (DueyActions dp : packages) {
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
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ENABLE_TV.getValue());
        builder.writeInt(0);
        builder.writeAsByte(0);

        return builder.getPacket();
    }

    public static GamePacket removeTV() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.REMOVE_TV.getValue());

        return builder.getPacket();
    }

    public static GamePacket sendTV(ChannelCharacter chr, List<String> messages, int type, ChannelCharacter partner) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SEND_TV.getValue());
        builder.writeAsByte(partner != null ? 2 : 0);
        builder.writeAsByte(type); // type   Heart = 2  Star = 1  Normal = 0
        PacketHelper.addCharLook(builder, chr, false);
        builder.writeLengthPrefixedString(chr.getName());
        if (partner != null) {
            builder.writeLengthPrefixedString(partner.getName());
        } else {
            builder.writeAsShort(0);
        }
        for (int i = 0; i < messages.size(); i++) {
            if (i == 4 && messages.get(4).length() > 15) {
                builder.writeLengthPrefixedString(messages.get(4).substring(0, 15)); // hmm ?
            } else {
                builder.writeLengthPrefixedString(messages.get(i));
            }
        }
        builder.writeInt(1337); // time limit shit lol 'Your thing still start in blah blah seconds'
        if (partner != null) {
            PacketHelper.addCharLook(builder, partner, false);
        }

        return builder.getPacket();
    }

    public static GamePacket Mulung_DojoUp() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(0x0B);
        builder.writeAsShort(1207); // ???
        builder.writeLengthPrefixedString("pt=5599;min=4;belt=3;tuto=1"); // todo

        return builder.getPacket();
    }

    public static GamePacket Mulung_DojoUp2() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        builder.writeAsByte(7);

        return builder.getPacket();
    }

    public static GamePacket spawnDragon(Dragon d) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DRAGON_SPAWN.getValue());
        builder.writeInt(d.getOwner());
        builder.writeInt(d.getPosition().x);
        builder.writeInt(d.getPosition().y);
        builder.writeAsByte(d.getStance()); //stance?
        builder.writeAsShort(0);
        builder.writeAsShort(d.getJobId());

        return builder.getPacket();
    }

    public static GamePacket removeDragon(int chrid) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DRAGON_REMOVE.getValue());
        builder.writeInt(chrid);

        return builder.getPacket();
    }

    public static GamePacket moveDragon(Dragon d, Point startPos, List<LifeMovementFragment> moves) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DRAGON_MOVE.getValue()); //not sure
        builder.writeInt(d.getOwner());
        builder.writeVector(startPos);
        builder.writeInt(0);
        PacketHelper.serializeMovementList(builder, moves);

        return builder.getPacket();
    }

    public static GamePacket Mulung_Pts(int recv, int total) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
        builder.writeAsByte(10);
        builder.writeLengthPrefixedString("You have received " + recv + " training points, for the accumulated total of " + total + " training points.");

        return builder.getPacket();
    }

    public static GamePacket MulungEnergy(int energy) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ENERGY.getValue());
        builder.writeLengthPrefixedString("energy");
        builder.writeLengthPrefixedString(String.valueOf(energy));

        return builder.getPacket();
    }

    public static GamePacket PyramidEnergy(byte type, int amount) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ENERGY.getValue());
        builder.writeLengthPrefixedString("massacre_" + (type == 0 ? "cool" : type == 1 ? "kill" : "miss"));
        builder.writeLengthPrefixedString(Integer.toString(amount));
//mc.getClient().getSession().writeAsByte(MaplePacketCreator.updatePyramidInfo(1, mc.getInstance(PartyQuest.NETT_PYRAMID).gainReturnKills());  
        return builder.getPacket();
    }

    public static GamePacket cancelHoming() {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CANCEL_BUFF.getValue());
        builder.writeLong(BuffStat.HOMING_BEACON.getValue());
        builder.writeLong(0);

        return builder.getPacket();
    }

    public static GamePacket sendMapleTip(String message) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(0x46); // Header
        builder.writeAsByte(0xFF);
        builder.writeLengthPrefixedString(message);

        return builder.getPacket();
    }

    public static GamePacket finishedSort(int type) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.FINISH_SORT.getValue());
        builder.writeAsByte(0);
        builder.writeAsByte(type);

        return builder.getPacket();
    }

    public static GamePacket finishedSort2(int type) {
        PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.FINISH_GATHER.getValue());
        builder.writeAsByte(0);
        builder.writeAsByte(type);

        return builder.getPacket();
    }
}