/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import java.util.Map;

import client.IItem;
import client.Equip;
import client.SkillFactory;
import client.GameClient;
import client.InventoryType;
import client.GameConstants;
import org.javastory.io.PacketFormatException;
import server.ItemMakerFactory;
import server.ItemMakerFactory.GemInfo;
import server.ItemMakerFactory.MakerItemInfo;
import org.javastory.tools.Randomizer;
import server.ItemInfoProvider;
import server.InventoryManipulator;
import tools.Pair;
import tools.MaplePacketCreator;
import org.javastory.io.PacketReader;
import org.javastory.server.maker.ItemRecipe;
import org.javastory.server.maker.ItemRecipeEntry;

public class ItemMakerHandler {

    public static void handleItemMaker(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final int makerType = reader.readInt();

        switch (makerType) {
            case 1:
                makeGem(reader, c);
                break;
            case 3:
                makeCrystal(reader, c);
                break;
            case 4:
                disassemble(reader, c);
                break;
        }
    }

    private static boolean disassemble(final PacketReader reader, final GameClient c) throws PacketFormatException {
        // Disassembling EQ.
        final int itemId = reader.readInt();
        reader.skip(4);
        final byte slot = (byte) reader.readInt();
        final IItem toUse = c.getPlayer().getInventoryType(InventoryType.EQUIP).getItem(slot);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() <
                1) {
            return true;
        }
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        if (!ii.isDropRestricted(itemId)) {
            final int[] toGive = getCrystal(itemId, ii.getReqLevel(itemId));
            InventoryManipulator.addById(c, toGive[0], (byte) toGive[1]);
            InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, slot, (byte) 1, false);
        }
        c.write(MaplePacketCreator.ItemMaker_Success());
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
        return false;
    }

    private static void makeCrystal(final PacketReader reader, final GameClient c) throws PacketFormatException {
        // Making Crystals
        final int etc = reader.readInt();
        if (c.getPlayer().haveItem(etc, 100, false, true)) {
            InventoryManipulator.addById(c, getCreateCrystal(etc), (short) 1);
            InventoryManipulator.removeById(c, InventoryType.ETC, etc, 100, false, false);

            c.write(MaplePacketCreator.ItemMaker_Success());
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
        }
    }

    private static boolean makeGem(final PacketReader reader, final GameClient c) throws PacketFormatException {
        // Gem
        final int toCreate = reader.readInt();
        if (GameConstants.isGem(toCreate)) {
            final GemInfo gem = ItemMakerFactory.getInstance().getGemInfo(toCreate);
            if (!hasSkill(c, gem.getRequiredSkillLevel())) {
                return true;
            }
            if (c.getPlayer().getMeso() < gem.getCost()) {
                return true;
            }
            final int gemItemId = gem.chooseRandomReward();
            if (c.getPlayer().getInventoryType(GameConstants.getInventoryType(gemItemId)).isFull()) {
                return true;
            }
            final int taken = checkRequiredNRemove(c, gem.getRecipe());
            if (taken == 0) {
                return true;
            }
            c.getPlayer().gainMeso(-gem.getCost(), false);
            InventoryManipulator.addById(c, gemItemId, (byte) (taken ==
                    gemItemId ? 9 : 1)); // Gem is always 1
            c.write(MaplePacketCreator.ItemMaker_Success());
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
        } else {
            final boolean stimulator = reader.readByte() > 0;
            final int numEnchanter = reader.readInt();
            final MakerItemInfo create = ItemMakerFactory.getInstance().getItemInfo(toCreate);
            if (numEnchanter > create.getTUC()) {
                return true;
            }
            if (!hasSkill(c, create.getRequiredSkillLevel())) {
                return true;
            }
            if (c.getPlayer().getMeso() < create.getCost()) {
                return true;
            }
            if (c.getPlayer().getInventoryType(GameConstants.getInventoryType(toCreate)).isFull()) {
                return true;
            }
            if (checkRequiredNRemove(c, create.getRecipe()) == 0) {
                return true;
            }
            c.getPlayer().gainMeso(-create.getCost(), false);
            final ItemInfoProvider ii = ItemInfoProvider.getInstance();
            final Equip toGive = (Equip) ii.getEquipById(toCreate);
            if (stimulator || numEnchanter > 0) {
                if (c.getPlayer().haveItem(create.getStimulator(), 1, false, true)) {
                    ii.randomizeStats(toGive);
                    InventoryManipulator.removeById(c, InventoryType.ETC, create.getStimulator(), 1, false, false);
                }
                for (int i = 0; i < numEnchanter; i++) {
                    final int enchant = reader.readInt();
                    if (c.getPlayer().haveItem(enchant, 1, false, true)) {
                        final Map<String, Byte> stats = ii.getItemMakeStats(enchant);
                        if (stats != null) {
                            addEnchantStats(stats, toGive);
                            InventoryManipulator.removeById(c, InventoryType.ETC, enchant, 1, false, false);
                        }
                    }
                }
            }
            InventoryManipulator.addbyItem(c, toGive);
            c.write(MaplePacketCreator.ItemMaker_Success());
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId()), false);
        }
        return false;
    }

    private static int getCreateCrystal(final int etc) {
        int itemid;
        final short level = ItemInfoProvider.getInstance().getItemMakeLevel(etc);

        if (level >= 31 && level <= 50) {
            itemid = 4260000;
        } else if (level >= 51 && level <= 60) {
            itemid = 4260001;
        } else if (level >= 61 && level <= 70) {
            itemid = 4260002;
        } else if (level >= 71 && level <= 80) {
            itemid = 4260003;
        } else if (level >= 81 && level <= 90) {
            itemid = 4260004;
        } else if (level >= 91 && level <= 100) {
            itemid = 4260005;
        } else if (level >= 101 && level <= 110) {
            itemid = 4260006;
        } else if (level >= 111 && level <= 120) {
            itemid = 4260007;
        } else if (level >= 121) {
            itemid = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker id");
        }
        return itemid;
    }

    private static int[] getCrystal(final int itemid, final int level) {
        int[] all = new int[2];
        all[0] = -1;
        if (level >= 31 && level <= 50) {
            all[0] = 4260000;
        } else if (level >= 51 && level <= 60) {
            all[0] = 4260001;
        } else if (level >= 61 && level <= 70) {
            all[0] = 4260002;
        } else if (level >= 71 && level <= 80) {
            all[0] = 4260003;
        } else if (level >= 81 && level <= 90) {
            all[0] = 4260004;
        } else if (level >= 91 && level <= 100) {
            all[0] = 4260005;
        } else if (level >= 101 && level <= 110) {
            all[0] = 4260006;
        } else if (level >= 111 && level <= 120) {
            all[0] = 4260007;
        } else if (level >= 121 && level <= 200) {
            all[0] = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker type" + level);
        }
        if (GameConstants.isWeapon(itemid) || GameConstants.isOverall(itemid)) {
            all[1] = Randomizer.rand(5, 11);
        } else {
            all[1] = Randomizer.rand(3, 7);
        }
        return all;
    }

    private static void addEnchantStats(final Map<String, Byte> stats, final Equip item) {
        short s = stats.get("incPAD");
        if (s != 0) {
            item.setWatk((short) (item.getWatk() + s));
        }
        s = stats.get("incMAD");
        if (s != 0) {
            item.setMatk((short) (item.getMatk() + s));
        }
        s = stats.get("incACC");
        if (s != 0) {
            item.setAcc((short) (item.getAcc() + s));
        }
        s = stats.get("incEVA");
        if (s != 0) {
            item.setAvoid((short) (item.getAvoid() + s));
        }
        s = stats.get("incSpeed");
        if (s != 0) {
            item.setSpeed((short) (item.getSpeed() + s));
        }
        s = stats.get("incJump");
        if (s != 0) {
            item.setJump((short) (item.getJump() + s));
        }
        s = stats.get("incMaxHP");
        if (s != 0) {
            item.setHp((short) (item.getHp() + s));
        }
        s = stats.get("incMaxMP");
        if (s != 0) {
            item.setMp((short) (item.getMp() + s));
        }
        s = stats.get("incSTR");
        if (s != 0) {
            item.setStr((short) (item.getStr() + s));
        }
        s = stats.get("incDEX");
        if (s != 0) {
            item.setDex((short) (item.getDex() + s));
        }
        s = stats.get("incINT");
        if (s != 0) {
            item.setInt((short) (item.getInt() + s));
        }
        s = stats.get("incLUK");
        if (s != 0) {
            item.setLuk((short) (item.getLuk() + s));
        }
        s = stats.get("randOption");
        if (s > 0) {
            final boolean success = Randomizer.nextBoolean();
            final int ma = item.getMatk(), wa = item.getWatk();
            if (wa > 0) {
                item.setWatk((short) (success ? (wa + s) : (wa - s)));
            }
            if (ma > 0) {
                item.setMatk((short) (success ? (ma + s) : (ma - s)));
            }
        }
        s = stats.get("randStat");
        if (s > 0) {
            final boolean success = Randomizer.nextBoolean();
            final int str = item.getStr(), dex = item.getDex(), luk = item.getLuk(), int_ = item.getInt();
            if (str > 0) {
                item.setStr((short) (success ? (str + s) : (str - s)));
            }
            if (dex > 0) {
                item.setDex((short) (success ? (dex + s) : (dex - s)));
            }
            if (int_ > 0) {
                item.setInt((short) (success ? (int_ + s) : (int_ - s)));
            }
            if (luk > 0) {
                item.setLuk((short) (success ? (luk + s) : (luk - s)));
            }
        }
    }

    private static int checkRequiredNRemove(final GameClient c, final ItemRecipe recipe) {
        int removed = 0;
        for (final ItemRecipeEntry p : recipe) {
            if (!c.getPlayer().haveItem(p.getItemId(), p.getQuantity(), false, true)) {
                return 0;
            }
            removed++;
        }
        for (final ItemRecipeEntry p : recipe) {
            int itemId = p.getItemId();
            InventoryType type = GameConstants.getInventoryType(itemId);
            InventoryManipulator.removeById(c, type, itemId, p.getQuantity(), false, false);
        }
        return removed;
    }

    private static boolean hasSkill(final GameClient c, final int reqlvl) {
        if (GameConstants.isKOC(c.getPlayer().getJob())) { // KoC Maker skill.
            return c.getPlayer().getCurrentSkillLevel(SkillFactory.getSkill(10001007)) >=
                    reqlvl;
        } else if (GameConstants.isAran(c.getPlayer().getJob())) { // KoC Maker skill.
            return c.getPlayer().getCurrentSkillLevel(SkillFactory.getSkill(20001007)) >=
                    reqlvl;
        } else {
            return c.getPlayer().getCurrentSkillLevel(SkillFactory.getSkill(1007)) >=
                    reqlvl;
        }
    }
}