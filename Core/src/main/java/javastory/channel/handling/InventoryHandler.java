package javastory.channel.handling;

import java.awt.Point;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.anticheat.CheatingOffense;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.ISkill;
import javastory.channel.client.Mount;
import javastory.channel.client.Pet;
import javastory.channel.client.SkillFactory;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.maps.FieldLimitType;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapItem;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.maps.SavedLocationType;
import javastory.channel.packet.MTSCSPacket;
import javastory.channel.packet.PetPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.ShopFactory;
import javastory.client.GameCharacterUtil;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.IEquip;
import javastory.game.IItem;
import javastory.game.IdProbabilityEntry;
import javastory.game.Inventory;
import javastory.game.InventoryType;
import javastory.game.ItemConsumeType;
import javastory.game.ItemFlag;
import javastory.game.ItemInfoProvider;
import javastory.game.Stat;
import javastory.game.StatValue;
import javastory.game.IEquip.ScrollResult;
import javastory.game.maker.RandomRewards;
import javastory.game.maker.RewardItemInfo;
import javastory.game.quest.QuestInfoProvider;
import javastory.game.quest.QuestInfoProvider.QuestInfo;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.scripting.NpcScriptManager;
import javastory.tools.Pair;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

public class InventoryHandler {

	public static void handleItemMove(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		reader.skip(4);
		final byte mode = reader.readByte();
		final Inventory inventory = c.getPlayer().getInventoryByTypeByte(mode);
		final byte src = (byte) reader.readShort();
		final byte dst = (byte) reader.readShort();
		final short quantity = reader.readShort();

		if (src < 0 && dst > 0) {
			InventoryManipulator.unequip(c, src, dst);
		} else if (dst < 0) {
			InventoryManipulator.equip(c, src, dst);
		} else if (dst == 0) {
			InventoryManipulator.drop(c, inventory, src, quantity);
		} else {
			InventoryManipulator.move(c, inventory, src, dst);
		}
	}

	public static void handleItemSort(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		reader.skip(4);
		byte mode = reader.readByte();
		final Inventory inventory = c.getPlayer().getInventoryByTypeByte(mode);
		boolean sorted = false;
		while (!sorted) {
			byte freeSlot = (byte) inventory.getNextFreeSlot();
			if (freeSlot != -1) {
				byte itemSlot = -1;
				for (byte i = (byte) (freeSlot + 1); i <= 100; i++) {
					if (inventory.getItem(i) != null) {
						itemSlot = i;
						break;
					}
				}
				if (itemSlot <= 100 && itemSlot > 0) {
					InventoryManipulator.move(c, inventory, itemSlot, freeSlot);
				} else {
					sorted = true;
				}
			}
		}
		c.write(ChannelPackets.finishedSort(mode));
		c.write(ChannelPackets.enableActions());
	}

	public static void ItemSort2(final PacketReader reader, final ChannelClient c) {
		/*
		 * reader.skip(4); byte mode = reader.readByte(); if (mode < 0 || mode >
		 * 5) { return; } Inventory Inv =
		 * c.getPlayer().getInventory(InventoryType.getByType(mode));
		 * ArrayList<Item> itemarray = new ArrayList<Item>(); for
		 * (Iterator<IItem> it = Inv.iterator(); it.hasNext();) { Item item =
		 * (Item) it.next(); itemarray.add((Item) (item.copy())); }
		 * Collections.sort(itemarray); for (IItem item : itemarray) {
		 * MapleInventoryManipulator.removeById(c,
		 * InventoryType.getByType(mode), item.getItemId(), item.getQuantity(),
		 * false, false); } for (Item i : itemarray) {
		 * MapleInventoryManipulator.addFromDrop(c, i, false, false); }
		 * c.write(MaplePacketCreator.finishedSort2(mode));
		 * c.write(MaplePacketCreator.enableActions());
		 */
	}

	public static void handleUseRewardItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final Inventory useInventory = c.getPlayer().getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
			if (chr.getEquipInventory().getNextFreeSlot() > -1 && chr.getUseInventory().getNextFreeSlot() > -1
				&& chr.getSetupInventory().getNextFreeSlot() > -1 && chr.getEtcInventory().getNextFreeSlot() > -1) {
				final ItemInfoProvider ii = ItemInfoProvider.getInstance();
				final Pair<Integer, List<RewardItemInfo>> rewards = ii.getRewardItem(itemId);

				if (rewards != null) {
					for (RewardItemInfo reward : rewards.getRight()) {
						if (Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total
																					// prob
							if (GameConstants.getInventoryType(reward.itemid) == InventoryType.EQUIP) {
								final IItem item = ii.getEquipById(reward.itemid);
								if (reward.period != -1) {
									item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10L));
								}
								InventoryManipulator.addbyItem(c, item);
							} else {
								InventoryManipulator.addById(c, reward.itemid, reward.quantity);
							}
							InventoryManipulator.removeById(c, useInventory, itemId, 1, false, false);

							c.write(ChannelPackets.showRewardItemAnimation(reward.itemid, reward.effect));
							chr.getMap().broadcastMessage(chr, ChannelPackets.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
							break;
						}
					}
				}
			} else {
				chr.sendNotice(6, "Insufficient inventory slot.");
			}
		}
		c.write(ChannelPackets.enableActions());
	}

	public static void handleUseItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
			if (ItemInfoProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
				InventoryManipulator.removeFromSlot(c, useInventory, slot, (short) 1, false);
			}
		} else {
			c.write(ChannelPackets.enableActions());
		}
	}

	public static void handleUseReturnScroll(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
			if (ItemInfoProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
				InventoryManipulator.removeFromSlot(c, useInventory, slot, (short) 1, false);
			} else {
				c.write(ChannelPackets.enableActions());
			}
		} else {
			c.write(ChannelPackets.enableActions());
		}
	}

	public static void handleUseUpgradeScroll(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		// Lunar Gloves unlimited scroll
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final byte dst = (byte) reader.readShort();
		final byte ws = (byte) reader.readShort();
		boolean whiteScroll = false; // white scroll being used?
		boolean legendarySpirit = false; // legendary spirit skill
		final ItemInfoProvider ii = ItemInfoProvider.getInstance();

		if ((ws & 2) == 2) {
			whiteScroll = true;
		}

		IEquip toScroll;
		final Inventory equippedInventory = chr.getEquippedItemsInventory();
		final Inventory equipInventory = chr.getEquipInventory();
		if (dst < 0) {
			toScroll = (IEquip) equippedInventory.getItem(dst);
		} else { // legendary spirit
			legendarySpirit = true;
			toScroll = (IEquip) equipInventory.getItem(dst);
		}
		final byte oldLevel = toScroll.getLevel();
		final byte oldFlag = toScroll.getFlag();

		if (!GameConstants.isSpecialScroll(toScroll.getItemId()) && !GameConstants.isCleanSlate(toScroll.getItemId())) {
			if (toScroll.getUpgradeSlots() < 1) {
				c.write(ChannelPackets.getInventoryFull());
				return;
			}
		}
		final Inventory useInventory = chr.getUseInventory();
		IItem scroll = useInventory.getItem(slot);
		IItem wscroll = null;

		// Anti cheat and validation
		List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
		if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
			c.write(ChannelPackets.getInventoryFull());
			return;
		}

		if (whiteScroll) {
			wscroll = useInventory.findById(2340000);
			if (wscroll == null || wscroll.getItemId() != 2340000) {
				whiteScroll = false;
			}
		}
		if (!GameConstants.isChaosScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId())) {
			if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
				return;
			}
		}
		if (scroll.getQuantity() <= 0) {
			return;
		}

		if (legendarySpirit) {
			if (chr.getCurrentSkillLevel(SkillFactory.getSkill(1003)) <= 0) {
				// AutobanManager.getInstance().addPoints(c, 50, 120000,
				// "Using the Skill 'Legendary Spirit' without having it.");
				return;
			}
		}

		// Scroll Success/ Failure/ Curse
		final IEquip scrolled = (IEquip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll);
		ScrollResult scrollSuccess;
		if (scrolled == null) {
			scrollSuccess = IEquip.ScrollResult.CURSE;
		} else if (scrolled.getLevel() > oldLevel) {
			scrollSuccess = IEquip.ScrollResult.SUCCESS;
		} else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getLevel() == oldLevel + 1)) {
			scrollSuccess = IEquip.ScrollResult.SUCCESS;
		} else if ((GameConstants.isSpecialScroll(scroll.getItemId()) && scrolled.getFlag() > oldFlag)) {
			scrollSuccess = IEquip.ScrollResult.SUCCESS;
		} else {
			scrollSuccess = IEquip.ScrollResult.FAIL;
		}

		// Update
		useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		if (whiteScroll) {
			InventoryManipulator.removeFromSlot(c, useInventory, wscroll.getPosition(), (short) 1, false, false);
		}

		if (scrollSuccess == IEquip.ScrollResult.CURSE) {
			c.write(ChannelPackets.scrolledItem(scroll, toScroll, true));
			if (dst < 0) {
				if (toScroll.getItemId() != ItemInfoProvider.UNLIMITED_SLOT_ITEM) { // unlimited
																					// slot
																					// item
																					// check
					equippedInventory.removeItem(toScroll.getPosition());
				}
			} else {
				if (toScroll.getItemId() != ItemInfoProvider.UNLIMITED_SLOT_ITEM) { // unlimited
																					// slot
																					// item
																					// check
					equipInventory.removeItem(toScroll.getPosition());
				}
			}
		} else {
			c.write(ChannelPackets.scrolledItem(scroll, scrolled, false));
		}

		chr.getMap().broadcastMessage(ChannelPackets.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit));

		// equipped item was scrolled and changed
		if (dst < 0 && (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE)) {
			chr.equipChanged();
		}
	}

	public static void handleUseSkillBook(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
			return;
		}
		final Map<String, Integer> skilldata = ItemInfoProvider.getInstance().getSkillStats(toUse.getItemId());
		if (skilldata == null) { // Hacking or used an unknown item
			return;
		}
		boolean canuse = false, success = false;
		int skill = 0, maxlevel = 0;

		final int SuccessRate = skilldata.get("success");
		final int ReqSkillLevel = skilldata.get("reqSkillLevel");
		final int MasterLevel = skilldata.get("masterLevel");

		byte i = 0;
		Integer CurrentLoopedSkillId;
		for (;;) {
			CurrentLoopedSkillId = skilldata.get("skillid" + i);
			i++;
			if (CurrentLoopedSkillId == null) {
				break; // End of data
			}
			if (Math.floor(CurrentLoopedSkillId / 100000) == chr.getJobId() / 10) {
				final ISkill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
				if (chr.getCurrentSkillLevel(CurrSkillData) >= ReqSkillLevel && chr.getMasterSkillLevel(CurrSkillData) < MasterLevel) {
					canuse = true;
					if (Randomizer.nextInt(99) <= SuccessRate && SuccessRate != 0) {
						success = true;
						final ISkill skill2 = CurrSkillData;
						chr.changeSkillLevel(skill2, chr.getCurrentSkillLevel(skill2), (byte) MasterLevel);
					} else {
						success = false;
					}
					InventoryManipulator.removeFromSlot(c, useInventory, slot, (short) 1, false);
					break;
				} else { // Failed to meet skill requirements
					canuse = false;
				}
			}
		}
		c.write(ChannelPackets.useSkillBook(chr, skill, maxlevel, canuse, success));
	}

	public static void handleUseCatchItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemid = reader.readInt();
		final Monster mob = chr.getMap().getMonsterByOid(reader.readInt());
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null) {
			switch (itemid) {
			case 2270002: { // Characteristic Stone
				final GameMap map = chr.getMap();

				if (mob.getHp() <= mob.getMobMaxHp() / 2) {
					map.broadcastMessage(ChannelPackets.catchMonster(mob.getId(), itemid, (byte) 1));
					map.killMonster(mob, chr, true, false, (byte) 0);
					InventoryManipulator.removeById(c, useInventory, itemid, 1, false, false);
				} else {
					map.broadcastMessage(ChannelPackets.catchMonster(mob.getId(), itemid, (byte) 0));
					chr.sendNotice(5, "The monster has too much physical strength, so you cannot catch it.");
				}
				break;
			}
			case 2270000: { // Pheromone Perfume
				if (mob.getId() != 9300101) {
					break;
				}
				final GameMap map = c.getPlayer().getMap();

				map.broadcastMessage(ChannelPackets.catchMonster(mob.getId(), itemid, (byte) 1));
				map.killMonster(mob, chr, true, false, (byte) 0);
				InventoryManipulator.addById(c, 1902000, (short) 1, null);
				InventoryManipulator.removeById(c, useInventory, itemid, 1, false, false);
				break;
			}
			case 2270003: { // Cliff's Magic Cane
				if (mob.getId() != 9500320) {
					break;
				}
				final GameMap map = c.getPlayer().getMap();

				if (mob.getHp() <= mob.getMobMaxHp() / 2) {
					map.broadcastMessage(ChannelPackets.catchMonster(mob.getId(), itemid, (byte) 1));
					map.killMonster(mob, chr, true, false, (byte) 0);
					InventoryManipulator.removeById(c, useInventory, itemid, 1, false, false);
				} else {
					map.broadcastMessage(ChannelPackets.catchMonster(mob.getId(), itemid, (byte) 0));
					chr.sendNotice(5, "The monster has too much physical strength, so you cannot catch it.");
				}
				break;
			}
			}
		}
		c.write(ChannelPackets.enableActions());
		// c.getPlayer().setAPQScore(c.getPlayer().getAPQScore() + 1);
		// c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.updateAriantPQRanking(c.getPlayer().getName(),
		// c.getPlayer().getAPQScore(), false));
	}

	public static void handleUseMountFood(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemid = reader.readInt(); // 2260000 usually
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);
		final Mount mount = chr.getMount();

		if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null) {
			final int fatigue = mount.getFatigue();

			boolean levelup = false;
			mount.setFatigue(-30);

			if (fatigue > 0) {
				mount.increaseExp();
				final int level = mount.getLevel();
				if (mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1) && level < 31) {
					mount.setLevel(level + 1);
					levelup = true;
				}
			}
			chr.getMap().broadcastMessage(ChannelPackets.updateMount(chr, levelup));
			InventoryManipulator.removeFromSlot(c, useInventory, slot, (short) 1, false);
		}
		c.write(ChannelPackets.enableActions());
	}

	public static void handleUseScriptedNpcItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
			switch (toUse.getItemId()) {
			case 2430007: {
				// Blank Compass
				final Inventory setupInventory = chr.getSetupInventory();
				InventoryManipulator.removeFromSlot(c, useInventory, slot, (byte) 1, false);

				if (setupInventory.countById(3994102) >= 20 // Compass Letter
															// "North"
					&& setupInventory.countById(3994103) >= 20 // Compass
																// Letter
																// "South"
					&& setupInventory.countById(3994104) >= 20 // Compass
																// Letter
																// "East"
					&& setupInventory.countById(3994105) >= 20) { // Compass
																	// Letter
																	// "West"
					InventoryManipulator.addById(c, 2430008, (short) 1); // Gold
																			// Compass
					InventoryManipulator.removeById(c, setupInventory, 3994102, 20, false, false);
					InventoryManipulator.removeById(c, setupInventory, 3994103, 20, false, false);
					InventoryManipulator.removeById(c, setupInventory, 3994104, 20, false, false);
					InventoryManipulator.removeById(c, setupInventory, 3994105, 20, false, false);
				} else {
					InventoryManipulator.addById(c, 2430007, (short) 1); // Blank
																			// Compass
				}
				NpcScriptManager.getInstance().start(c, 2084001);
				break;
			}
			case 2430008: {
				// Gold Compass
				chr.saveLocation(SavedLocationType.RICHIE);
				GameMap map;
				boolean warped = false;

				for (int i = 390001000; i <= 390001004; i++) {
					map = ChannelServer.getMapFactory().getMap(i);

					if (map.getCharactersSize() == 0) {
						chr.changeMap(map, map.getPortal(0));
						warped = true;
						break;
					}
				}
				if (warped) {
					InventoryManipulator.removeById(c, useInventory, 2430008, 1, false, false);
				} else {
					c.getPlayer().sendNotice(5, "All maps are currently in use, please try again later.");
				}
				break;
			}
			}
		}
		c.write(ChannelPackets.enableActions());
	}

	public static void handleUseSummonBag(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (!chr.isAlive()) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final Inventory useInventory = chr.getUseInventory();
		final IItem toUse = useInventory.getItem(slot);

		if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {

			InventoryManipulator.removeFromSlot(c, useInventory, slot, (short) 1, false);

			if (c.getPlayer().isGM() || !FieldLimitType.SummoningBag.check(chr.getMap().getFieldLimit())) {
				final List<IdProbabilityEntry> toSpawn = ItemInfoProvider.getInstance().getSummonMobs(itemId);

				if (toSpawn == null) {
					c.write(ChannelPackets.enableActions());
					return;
				}
				Monster ht;
				int type = 0;

				for (int i = 0; i < toSpawn.size(); i++) {
					if (Randomizer.nextInt(99) <= toSpawn.get(i).Probability) {
						ht = LifeFactory.getMonster(toSpawn.get(i).Id);
						chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
					}
				}
			}
		}
		c.write(ChannelPackets.enableActions());
	}

	public static void handleUseTreasureChest(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final short slot = reader.readShort();
		final int itemid = reader.readInt();
		final Inventory etcInventory = chr.getEtcInventory();

		final IItem toUse = etcInventory.getItem((byte) slot);
		if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemid) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		int reward;
		int keyIDforRemoval = 0;

		switch (toUse.getItemId()) {
		case 4280000: // Gold box
			reward = RandomRewards.getInstance().getGoldBoxReward();
			keyIDforRemoval = 5490000;
			break;
		case 4280001: // Silver box
			reward = RandomRewards.getInstance().getSilverBoxReward();
			keyIDforRemoval = 5490001;
			break;
		default: // Up to no good
			c.disconnect(true);
			return;
		}

		// Get the quantity
		int amount = 1;
		switch (reward) {
		case 2000004:
			amount = 200; // Elixir
			break;
		case 2000005:
			amount = 100; // Power Elixir
			break;
		}
		final Inventory cashInventory = chr.getCashInventory();
		if (cashInventory.countById(keyIDforRemoval) > 0) {
			final IItem item = InventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);
			if (item == null) {
				chr.sendNotice(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
				c.write(ChannelPackets.enableActions());
				return;
			}
			InventoryManipulator.removeFromSlot(c, etcInventory, (byte) slot, (short) 1, true);
			InventoryManipulator.removeById(c, cashInventory, keyIDforRemoval, 1, true, false);
			c.write(ChannelPackets.getShowItemGain(reward, (short) amount, true));
			if (GameConstants.gachaponRareItem(item.getItemId()) > 0) {
				try {
					ChannelServer.getWorldInterface().broadcastMessage(
						ChannelPackets.getGachaponMega(c.getPlayer().getName(), " : Lucky winner of Gachapon! Congratulations~", item, (byte) 2).getBytes());
				} catch (RemoteException e) {
					ChannelServer.pingWorld();
				}
			}
		} else {
			chr.sendNotice(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
			c.write(ChannelPackets.enableActions());
		}
	}

	public static void handleUseCashItem(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		reader.skip(4);
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		final ChannelCharacter player = c.getPlayer();
		final IItem toUse = player.getCashInventory().getItem(slot);
		if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		boolean used = false;
		switch (itemId) {
		case 5043000: { // NPC Teleport Rock
			final short questid = reader.readShort();
			final int npcid = reader.readInt();
			final QuestInfo quest = QuestInfoProvider.getInfo(questid);
			if (player.getQuestStatus(questid).getState() == 1 && quest.canComplete(player, npcid)) {
				final GameMap map = ChannelServer.getMapFactory().getMap(LifeFactory.getNpcLocation(npcid));
				if (map.containsNPC(npcid) != -1) {
					player.changeMap(map, map.getPortal(0));
				}
				used = true;
			}
			break;
		}
		case 2320000: // The Teleport Rock
		case 5041000: // VIP Teleport Rock
		case 5040000: // The Teleport Rock
		case 5040001: { // Teleport Coke
			if (reader.readByte() == 0) { // Rocktype
				final GameMap target = ChannelServer.getMapFactory().getMap(reader.readInt());
				if (!FieldLimitType.VipRock.check(player.getMap().getFieldLimit())) { // Makes
																						// sure
																						// this
																						// map
																						// doesn't
																						// have
																						// a
																						// forced
																						// return
																						// map
					player.changeMap(target, target.getPortal(0));
					used = true;
				}
			} else {
				final ChannelCharacter victim = ChannelServer.getPlayerStorage().getCharacterByName(reader.readLengthPrefixedString());
				if (victim != null && !victim.isGM()) {
					if (!FieldLimitType.VipRock.check(ChannelServer.getMapFactory().getMap(victim.getMapId()).getFieldLimit())) {
						if (itemId == 5041000 || (victim.getMapId() / 100000000) == (player.getMapId() / 100000000)) { // Viprock
																														// or
																														// same
																														// continent
							player.changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
							used = true;
						}
					}
				}
			}
			break;
		}
		case 5050000: { // AP Reset
			List<StatValue> statupdate = new ArrayList<>(2);
			final int apto = reader.readInt();
			final int apfrom = reader.readInt();

			if (apto == apfrom) {
				break; // Hack
			}
			final int job = player.getJobId();
			final ActivePlayerStats playerst = player.getStats();
			used = true;

			switch (apto) { // AP to
			case 64: // str
				if (playerst.getStr() >= 999) {
					used = false;
				}
				break;
			case 128: // dex
				if (playerst.getDex() >= 999) {
					used = false;
				}
				break;
			case 256: // int
				if (playerst.getInt() >= 999) {
					used = false;
				}
				break;
			case 512: // luk
				if (playerst.getLuk() >= 999) {
					used = false;
				}
				break;
			case 2048: // hp
				if (playerst.getMaxHp() >= 30000) {
					used = false;
				}
			case 8192: // mp
				if (playerst.getMaxMp() >= 30000) {
					used = false;
				}
			}
			switch (apfrom) { // AP to
			case 64: // str
				if (playerst.getStr() <= 4) {
					used = false;
				}
				break;
			case 128: // dex
				if (playerst.getDex() <= 4) {
					used = false;
				}
				break;
			case 256: // int
				if (playerst.getInt() <= 4) {
					used = false;
				}
				break;
			case 512: // luk
				if (playerst.getLuk() <= 4) {
					used = false;
				}
				break;
			case 2048: // hp
				if (/*
					 * playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) +
					 * 134) ||
					 */player.getHpApUsed() <= 0 || player.getHpApUsed() >= 10000) {
					used = false;
				}
				break;
			case 8192: // mp
				if (/*
					 * playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) +
					 * 134) ||
					 */player.getHpApUsed() <= 0 || player.getHpApUsed() >= 10000) {
					used = false;
				}
				break;
			}
			if (used) {
				switch (apto) { // AP to
				case 64: { // str
					final int toSet = playerst.getStr() + 1;
					playerst.setStr(toSet);
					statupdate.add(new StatValue(Stat.STR, toSet));
					break;
				}
				case 128: { // dex
					final int toSet = playerst.getDex() + 1;
					playerst.setDex(toSet);
					statupdate.add(new StatValue(Stat.DEX, toSet));
					break;
				}
				case 256: { // int
					final int toSet = playerst.getInt() + 1;
					playerst.setInt(toSet);
					statupdate.add(new StatValue(Stat.INT, toSet));
					break;
				}
				case 512: { // luk
					final int toSet = playerst.getLuk() + 1;
					playerst.setLuk(toSet);
					statupdate.add(new StatValue(Stat.LUK, toSet));
					break;
				}
				case 2048: // hp
					int maxhp = playerst.getMaxHp();

					if (job == 0) { // Beginner
						maxhp += Randomizer.rand(8, 12);
					} else if (job >= 100 && job <= 132) { // Warrior
						ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
						int improvingMaxHPLevel = player.getCurrentSkillLevel(improvingMaxHP);
						maxhp += Randomizer.rand(20, 25);
						if (improvingMaxHPLevel >= 1) {
							maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
						}
					} else if (job >= 200 && job <= 232) { // Magician
						maxhp += Randomizer.rand(10, 20);
					} else if (job >= 300 && job <= 322) { // Bowman
						maxhp += Randomizer.rand(16, 20);
					} else if (job >= 400 && job <= 434) { // Thief
						maxhp += Randomizer.rand(16, 20);
					} else if (job >= 500 && job <= 522) { // Pirate
						ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
						int improvingMaxHPLevel = player.getCurrentSkillLevel(improvingMaxHP);
						maxhp += 20;
						if (improvingMaxHPLevel >= 1) {
							maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
						}
					} else if (job >= 1100 && job <= 1111) { // Soul Master
						ISkill improvingMaxHP = SkillFactory.getSkill(11000000);
						int improvingMaxHPLevel = player.getCurrentSkillLevel(improvingMaxHP);
						maxhp += Randomizer.rand(36, 42);
						if (improvingMaxHPLevel >= 1) {
							maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
						}
					} else if (job >= 1200 && job <= 1211) { // Flame Wizard
						maxhp += Randomizer.rand(15, 21);
					} else if ((job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411)) { // Wind
																								// Breaker
																								// and
																								// Night
																								// Walker
						maxhp += Randomizer.rand(30, 36);
					} else if (job >= 2000 && job <= 2112) { // Aran
						maxhp += Randomizer.rand(20, 25);
					} else { // GameMaster
						maxhp += Randomizer.rand(50, 100);
					}
					maxhp = Math.min(30000, maxhp);
					player.setHpApUsed(player.getHpApUsed() + 1);
					playerst.setMaxHp(maxhp);
					statupdate.add(new StatValue(Stat.HP, maxhp));
					break;

				case 8192: // mp
					int maxmp = playerst.getMaxMp();

					if (job == 0) { // Beginner
						maxmp += Randomizer.rand(6, 8);
					} else if (job >= 100 && job <= 132) {
						maxmp += Randomizer.rand(2, 4);
					} else if (job >= 200 && job <= 232) {
						ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
						int improvingMaxMPLevel = player.getCurrentSkillLevel(improvingMaxMP);
						maxmp += Randomizer.rand(18, 20);
						if (improvingMaxMPLevel >= 1) {
							maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
						}
					} else if (job >= 300 && job <= 322) {
						maxmp += Randomizer.rand(10, 12);
					} else if (job >= 400 && job <= 434) {
						maxmp += Randomizer.rand(10, 12);
					} else if (job >= 500 && job <= 522) {
						maxmp += Randomizer.rand(10, 12);
					} else if (job >= 1100 && job <= 1111) {
						maxmp += Randomizer.rand(6, 9);
					} else if (job >= 1200 && job <= 1211) {
						ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
						int improvingMaxMPLevel = player.getCurrentSkillLevel(improvingMaxMP);
						maxmp += Randomizer.rand(33, 36);
						if (improvingMaxMPLevel >= 1) {
							maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
						}
					} else if ((job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411)) {
						maxmp += Randomizer.rand(21, 24);
					} else if (job >= 2000 && job <= 2112) {
						maxmp += Randomizer.rand(4, 6);
					} else {
						maxmp += Randomizer.rand(50, 100);
					}
					maxmp = Math.min(30000, maxmp);
					player.setMpApUsed(player.getMpApUsed() + 1);
					playerst.setMaxMp(maxmp);
					statupdate.add(new StatValue(Stat.MP, maxmp));
					break;
				}
				switch (apfrom) {
				// AP from
				case 64: {
					// str
					final int toSet = playerst.getStr() - 1;
					playerst.setStr(toSet);
					statupdate.add(new StatValue(Stat.STR, toSet));
					break;
				}
				case 128: {
					// dex
					final int toSet = playerst.getDex() - 1;
					playerst.setDex(toSet);
					statupdate.add(new StatValue(Stat.DEX, toSet));
					break;
				}
				case 256: {
					// int
					final int toSet = playerst.getInt() - 1;
					playerst.setInt(toSet);
					statupdate.add(new StatValue(Stat.INT, toSet));
					break;
				}
				case 512: {
					// luk
					final int toSet = playerst.getLuk() - 1;
					playerst.setLuk(toSet);
					statupdate.add(new StatValue(Stat.LUK, toSet));
					break;
				}
				case 2048:
					// HP
					int maxhp = playerst.getMaxHp();
					if (job == 0) {
						// Beginner
						maxhp -= 12;
					} else if (job >= 100 && job <= 132) {
						// Warrior
						ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
						int improvingMaxHPLevel = player.getCurrentSkillLevel(improvingMaxHP);
						maxhp -= 24;
						if (improvingMaxHPLevel >= 1) {
							maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
						}
					} else if (job >= 200 && job <= 232) {
						// Magician
						maxhp -= 10;
					} else if (job >= 300 && job <= 322 || job >= 400 && job <= 434) {
						// Bowman, Thief
						maxhp -= 15;
					} else if (job >= 500 && job <= 522) {
						// Pirate
						ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
						int improvingMaxHPLevel = player.getCurrentSkillLevel(improvingMaxHP);
						maxhp -= 15;
						if (improvingMaxHPLevel > 0) {
							maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
						}
					} else if (job >= 1100 && job <= 1111) {
						// Soul Master
						ISkill improvingMaxHP = SkillFactory.getSkill(11000000);
						int improvingMaxHPLevel = player.getCurrentSkillLevel(improvingMaxHP);
						maxhp -= 27;
						if (improvingMaxHPLevel >= 1) {
							maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
						}
					} else if (job >= 1200 && job <= 1211) {
						// Flame Wizard
						maxhp -= 12;
					} else if ((job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411)) {
						// Wind	Breaker	and	Night Walker
						maxhp -= 17;
					} else if (job >= 2000 && job <= 2112) {
						// Aran
						maxhp -= 20;
					} else {
						// GameMaster
						maxhp -= 20;
					}
					player.setHpApUsed(player.getHpApUsed() - 1);
					playerst.setHp(maxhp);
					playerst.setMaxHp(maxhp);
					statupdate.add(new StatValue(Stat.HP, maxhp));
					break;
				case 8192: // MP
					int maxmp = playerst.getMaxMp();
					if (job == 0) { // Beginner
						maxmp -= 8;
					} else if (job >= 100 && job <= 132) { // Warrior
						maxmp -= 4;
					} else if (job >= 200 && job <= 232) { // Magician
						ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
						int improvingMaxMPLevel = player.getCurrentSkillLevel(improvingMaxMP);
						maxmp -= 20;
						if (improvingMaxMPLevel >= 1) {
							maxmp -= improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
						}
					} else if ((job >= 500 && job <= 522) || (job >= 300 && job <= 322) || (job >= 400 && job <= 434)) { // Pirate,
																															// Bowman.
																															// Thief
						maxmp -= 10;
					} else if (job >= 1100 && job <= 1111) { // Soul Master
						maxmp -= 6;
					} else if (job >= 1200 && job <= 1211) { // Flame Wizard
						ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
						int improvingMaxMPLevel = player.getCurrentSkillLevel(improvingMaxMP);
						maxmp -= 25;
						if (improvingMaxMPLevel >= 1) {
							maxmp -= improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
						}
					} else if ((job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411)) { // Wind
																								// Breaker
																								// and
																								// Night
																								// Walker
						maxmp -= 15;
					} else if (job >= 2000 && job <= 2112) { // Aran
						maxmp -= 5;
					} else { // GameMaster
						maxmp -= 20;
					}
					player.setMpApUsed(player.getMpApUsed() - 1);
					playerst.setMp(maxmp);
					playerst.setMaxMp(maxmp);
					statupdate.add(new StatValue(Stat.MP, maxmp));
					break;
				}
				c.write(ChannelPackets.updatePlayerStats(statupdate, true, player.getJobId()));
			}
			break;
		}
		case 5050005:
		case 5050006:
		case 5050007:
		case 5050008:
		case 5050009:
		case 5050001: // SP Reset (1st job)
		case 5050002: // SP Reset (2nd job)
		case 5050003: // SP Reset (3rd job)
		case 5050004: { // SP Reset (4th job)
			int skill1 = reader.readInt();
			int skill2 = reader.readInt();

			ISkill skillSPTo = SkillFactory.getSkill(skill1);
			ISkill skillSPFrom = SkillFactory.getSkill(skill2);

			if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
				break;
			}
			if ((player.getCurrentSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && player.getCurrentSkillLevel(skillSPFrom) > 0) {
				player.changeSkillLevel(skillSPFrom, (byte) (player.getCurrentSkillLevel(skillSPFrom) - 1), player.getMasterSkillLevel(skillSPFrom));
				player.changeSkillLevel(skillSPTo, (byte) (player.getCurrentSkillLevel(skillSPTo) + 1), player.getMasterSkillLevel(skillSPTo));
				used = true;
			}
			break;
		}
		case 5060000: { // Item Tag
			final IItem item = player.getEquippedItemsInventory().getItem(reader.readByte());

			if (item != null && item.getOwner().equals("")) {
				item.setOwner(player.getName());
				used = true;
			}
			break;
		}
		case 5520001:
		case 5520000: {
			// Karma
			final byte mode = (byte) reader.readInt();
			final Inventory inventory = player.getInventoryByTypeByte(mode);
			final IItem item = inventory.getItem((byte) reader.readInt());

			if (item != null) {
				if (ItemInfoProvider.getInstance().isKarmaEnabled(item.getItemId(), itemId)) {
					byte flag = item.getFlag();
					if (inventory.getType() == InventoryType.EQUIP) {
						flag |= ItemFlag.KARMA_EQ.getValue();
					} else {
						flag |= ItemFlag.KARMA_USE.getValue();
					}
					item.setFlag(flag);

					c.write(ChannelPackets.updateSpecialItemUse(item, inventory.getType().asByte()));
					used = true;
				}
			}
			break;
		}
		case 5570000: { // Vicious Hammer
			final byte invType = (byte) reader.readInt(); // Inventory type,
															// Hammered eq is
															// always EQ.
			final Equip item = (Equip) player.getEquipInventory().getItem((byte) reader.readInt());
			// another int here, D3 49 DC 00
			if (item != null) {
				if (item.getViciousHammer() <= 2) {
					item.setViciousHammer((byte) (item.getViciousHammer() + 1));
					item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));

					c.write(ChannelPackets.updateSpecialItemUse(item, invType));
					// c.write(MTSCSPacket.ViciousHammer(true, (byte)
					// item.getViciousHammer()));
					// c.write(MTSCSPacket.ViciousHammer(false, (byte) 0));
					used = true;
				}
			}
			break;
		}
		case 5060001: {
			// Sealing Lock
			final byte inventoryType = (byte) reader.readInt();
			final Inventory inventory = player.getInventoryByTypeByte(inventoryType);
			final IItem item = inventory.getItem((byte) reader.readInt());
			// another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
			if (item != null && item.getExpiration() == -1) {
				byte flag = item.getFlag();
				flag |= ItemFlag.LOCK.getValue();
				item.setFlag(flag);

				c.write(ChannelPackets.updateSpecialItemUse(item, inventoryType));
				used = true;
			}
			break;
		}
		case 5061000: { // Sealing Lock 7 days
			final byte inventoryType = (byte) reader.readInt();
			final Inventory inventory = player.getInventoryByTypeByte(inventoryType);
			final IItem item = inventory.getItem((byte) reader.readInt());
			// another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
			if (item != null && item.getExpiration() == -1) {
				byte flag = item.getFlag();
				flag |= ItemFlag.LOCK.getValue();
				item.setFlag(flag);
				item.setExpiration(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

				c.write(ChannelPackets.updateSpecialItemUse(item, inventoryType));
				used = true;
			}
			break;
		}
		case 5061001: { // Sealing Lock 30 days
			final byte inventoryType = (byte) reader.readInt();
			final Inventory inventory = player.getInventoryByTypeByte(inventoryType);
			final IItem item = inventory.getItem((byte) reader.readInt());
			// another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
			if (item != null && item.getExpiration() == -1) {
				byte flag = item.getFlag();
				flag |= ItemFlag.LOCK.getValue();
				item.setFlag(flag);

				item.setExpiration(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000));

				c.write(ChannelPackets.updateSpecialItemUse(item, inventoryType));
				used = true;
			}
			break;
		}
		case 5061002: { // Sealing Lock 90 days
			final byte inventoryType = (byte) reader.readInt();
			final Inventory inventory = player.getInventoryByTypeByte(inventoryType);
			final IItem item = inventory.getItem((byte) reader.readInt());
			// another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
			if (item != null && item.getExpiration() == -1) {
				byte flag = item.getFlag();
				flag |= ItemFlag.LOCK.getValue();
				item.setFlag(flag);

				item.setExpiration(System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000));

				c.write(ChannelPackets.updateSpecialItemUse(item, inventoryType));
				used = true;
			}
			break;
		}
		case 5071000: { // Megaphone
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final String message = reader.readLengthPrefixedString();

				if (message.length() > 65) {
					break;
				}
				final StringBuilder sb = new StringBuilder();
				addMedalString(player, sb);
				sb.append(player.getName());
				sb.append(" : ");
				sb.append(message);

				player.getMap().broadcastMessage(ChannelPackets.serverNotice(2, sb.toString()));
				used = true;
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5077000: { // 3 line Megaphone
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final byte numLines = reader.readByte();
				if (numLines > 3) {
					return;
				}
				final List<String> messages = new LinkedList<>();
				String message;
				for (int i = 0; i < numLines; i++) {
					message = reader.readLengthPrefixedString();
					if (message.length() > 65) {
						break;
					}
					messages.add(player.getName() + " : " + message);
				}
				final boolean ear = reader.readByte() > 0;

				try {
					ChannelServer.getWorldInterface().broadcastSmega(ChannelPackets.tripleSmega(messages, ear, c.getChannelId()).getBytes());
					used = true;
				} catch (RemoteException e) {
					System.out.println("RemoteException occured, triple megaphone");
				}
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5073000: { // Heart Megaphone
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final String message = reader.readLengthPrefixedString();

				if (message.length() > 65) {
					break;
				}
				final StringBuilder sb = new StringBuilder();
				addMedalString(player, sb);
				sb.append(player.getName());
				sb.append(" : ");
				sb.append(message);

				final boolean ear = reader.readByte() != 0;

				try {
					ChannelServer.getWorldInterface().broadcastSmega(ChannelPackets.serverNotice(9, c.getChannelId(), sb.toString(), ear).getBytes());
					used = true;
				} catch (RemoteException e) {
					System.out.println("RemoteException occured, heart megaphone");
				}
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5074000: { // Skull Megaphone
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final String message = reader.readLengthPrefixedString();

				if (message.length() > 65) {
					break;
				}
				final StringBuilder sb = new StringBuilder();
				addMedalString(player, sb);
				sb.append(player.getName());
				sb.append(" : ");
				sb.append(message);

				final boolean ear = reader.readByte() != 0;

				try {
					ChannelServer.getWorldInterface().broadcastSmega(ChannelPackets.serverNotice(10, c.getChannelId(), sb.toString(), ear).getBytes());
					used = true;
				} catch (RemoteException e) {
					System.out.println("RemoteException occured, skull megaphone");
				}
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5072000: { // Super Megaphone
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final String message = reader.readLengthPrefixedString();
				if (message.length() > 65) {
					break;
				}
				final StringBuilder sb = new StringBuilder();
				addMedalString(player, sb);
				sb.append(player.getName());
				sb.append(" : ");
				sb.append(message);
				final boolean ear = reader.readByte() != 0;
				try {
					ChannelServer.getWorldInterface().broadcastSmega(ChannelPackets.serverNotice(3, c.getChannelId(), sb.toString(), ear).getBytes());
					used = true;
				} catch (RemoteException e) {
					System.out.println("RemoteException occured, super megaphone");
				}
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5076000: { // Item Megaphone
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final String message = reader.readLengthPrefixedString();

				if (message.length() > 65) {
					break;
				}
				final StringBuilder sb = new StringBuilder();
				addMedalString(player, sb);
				sb.append(player.getName());
				sb.append(" : ");
				sb.append(message);

				final boolean ear = reader.readByte() > 0;

				IItem item = null;
				if (reader.readByte() == 1) { // item
					byte invType = (byte) reader.readInt();
					byte pos = (byte) reader.readInt();
					item = player.getInventoryByTypeByte(invType).getItem(pos);
				}

				try {
					ChannelServer.getWorldInterface().broadcastSmega(ChannelPackets.itemMegaphone(sb.toString(), ear, c.getChannelId(), item).getBytes());
					used = true;
				} catch (RemoteException e) {
					System.out.println("RemoteException occured, item megaphone");
				}
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5075000: // MapleTV Messenger
		case 5075001: // MapleTV Star Messenger
		case 5075002: // MapleTV Heart Messenger
			break;
		case 5090100: // Wedding Invitation Card
		case 5090000: { // Note
			final String sendTo = reader.readLengthPrefixedString();
			final String msg = reader.readLengthPrefixedString();
			player.sendNote(sendTo, msg);
			used = true;
			break;
		}
		case 5100000: { // Congratulatory Song
			player.getMap().broadcastMessage(ChannelPackets.musicChange("Jukebox/Congratulation"));
			used = true;
			break;
		}
		case 5170000: { // Pet name change
			if (player.getPet(0) == null) {
				break;
			}
			String nName = reader.readLengthPrefixedString();
			if (GameCharacterUtil.validatePetName(nName)) {
				player.getPet(0).setName(nName);
				c.write(PetPacket.updatePet(player.getPet(0), true));
				c.write(ChannelPackets.enableActions());
				player.getMap().broadcastMessage(player, MTSCSPacket.changePetName(player, nName, 1), true);
				used = true;
			}
			break;
		}
		case 5200000: { // Bronze Sack of Mesos
			player.gainMeso(1000000, true, false, true);
			c.write(ChannelPackets.enableActions());
			used = true;
			break;
		}
		case 5200001: { // Silver Sack of Mesos
			player.gainMeso(5000000, true, false, true);
			c.write(ChannelPackets.enableActions());
			used = true;
			break;
		}
		case 5200002: { // Gold Sack of Mesos
			player.gainMeso(10000000, true, false, true);
			c.write(ChannelPackets.enableActions());
			used = true;
			break;
		}
		case 5240000:
		case 5240001:
		case 5240002:
		case 5240003:
		case 5240004:
		case 5240005:
		case 5240006:
		case 5240007:
		case 5240008:
		case 5240009:
		case 5240010:
		case 5240011:
		case 5240012:
		case 5240013:
		case 5240014:
		case 5240015:
		case 5240016:
		case 5240017:
		case 5240018:
		case 5240019:
		case 5240020:
		case 5240021:
		case 5240022:
		case 5240023:
		case 5240025:
		case 5240026:
		case 5240027:
		case 5240028:
		case 5240024: { // Pet food
			Pet pet = player.getPet(0);

			if (pet == null) {
				break;
			}
			if (!pet.canConsume(itemId)) {
				pet = player.getPet(1);
				if (pet != null) {
					if (!pet.canConsume(itemId)) {
						pet = player.getPet(2);
						if (pet != null) {
							if (!pet.canConsume(itemId)) {
								break;
							}
						} else {
							break;
						}
					}
				} else {
					break;
				}
			}
			final byte petindex = player.getPetIndex(pet);
			pet.setFullness(100);
			if (pet.getCloseness() < 30000) {
				if (pet.getCloseness() + 100 > 30000) {
					pet.setCloseness(30000);
				} else {
					pet.setCloseness(pet.getCloseness() + 100);
				}
				if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
					pet.setLevel(pet.getLevel() + 1);
					c.write(PetPacket.showOwnPetLevelUp(player.getPetIndex(pet)));
					player.getMap().broadcastMessage(PetPacket.showPetLevelUp(player, petindex));
				}
			}
			c.write(PetPacket.updatePet(pet, true));
			player.getMap().broadcastMessage(player, PetPacket.commandResponse(player.getId(), (byte) 1, petindex, true, true), true);
			used = true;
			break;
		}
		// case 5280001: // Gas Skill
		case 5281000: { // Passed gas
			/*
			 * Rectangle bounds = new Rectangle((int)
			 * c.getPlayer().getPosition().getX(), (int)
			 * c.getPlayer().getPosition().getY(), 1, 1); MapleStatEffect mse =
			 * new MapleStatEffect(); mse.setSourceId(2111003); MapleMist mist =
			 * new MapleMist(bounds, c.getPlayer(), mse);
			 * c.getPlayer().getMap().spawnMist(mist, 10000, false, true);
			 * c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.
			 * getChatText (c.getPlayer().getId(), "Oh no, I farted!", false,
			 * 1)); c.write(MaplePacketCreator.enableActions()); used = true;
			 */
			break;
		}
		case 5370000: { // Chalkboard
			player.setChalkboard(reader.readLengthPrefixedString());
			break;
		}
		case 5370001: { // BlackBoard
			if (player.getMapId() / 1000000 == 910) {
				player.setChalkboard(reader.readLengthPrefixedString());
			}
			break;
		}
		case 5390000: // Diablo Messenger
		case 5390001: // Cloud 9 Messenger
		case 5390002: // Loveholic Messenger
		case 5390003: // New Year Megassenger 1
		case 5390004: // New Year Megassenger 2
		case 5390005: // Cute Tiger Messenger
		case 5390006: { // Tiger Roar's Messenger
			if (!ChannelServer.getInstance().getMegaphoneMuteState()) {
				final String text = reader.readLengthPrefixedString();
				if (text.length() > 55) {
					break;
				}
				final boolean ear = reader.readByte() != 0;
				try {
					ChannelServer.getWorldInterface().broadcastSmega(ChannelPackets.getAvatarMega(player, c.getChannelId(), itemId, text, ear).getBytes());
					used = true;
				} catch (RemoteException e) {
					System.out.println("RemoteException occured, TV megaphone");
				}
			} else {
				player.sendNotice(5, "The usage of Megapone is currently disabled.");
			}
			break;
		}
		case 5450000: { // Mu Mu the Travelling Merchant
			ShopFactory.getInstance().getShop(61).sendShop(c);
			used = true;
			break;
		}
		default:
			if (itemId / 10000 == 512) {
				final ItemInfoProvider ii = ItemInfoProvider.getInstance();
				final String msg = ii.getMsg(itemId).replaceFirst("%s", player.getName()).replaceFirst("%s", reader.readLengthPrefixedString());
				player.getMap().startMapEffect(msg, itemId);

				final int buff = ii.getStateChangeItem(itemId);
				if (buff != 0) {
					for (ChannelCharacter mChar : player.getMap().getCharacters()) {
						ii.getItemEffect(buff).applyTo(mChar);
					}
				}
				used = true;
			} else {
				System.out.println(":: Unhandled CS item : " + itemId + " ::");
				System.out.println(reader.toString());
			}
			break;
		}

		if (used) {
			InventoryManipulator.removeById(c, player.getCashInventory(), itemId, 1, true, false);
		} else {
			c.write(ChannelPackets.enableActions());
		}
	}

	public static void handleItemLoot(final PacketReader reader, ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		reader.skip(5); // [4] Seems to be tickcount, [1] always 0
		final Point Client_Reportedpos = reader.readVector();
		final GameMapObject ob = chr.getMap().getMapObject(reader.readInt());

		if (ob == null || ob.getType() != GameMapObjectType.ITEM) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		final GameMapItem mapItem = (GameMapItem) ob;

		if (mapItem.isPickedUp()) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		if (mapItem.getOwner() != chr.getId() && chr.getMap().isEverlast()) {
			c.write(ChannelPackets.enableActions());
			return;
		}
		final double Distance = Client_Reportedpos.distanceSq(mapItem.getPosition());
		if (Distance > 2500) {
			chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_CLIENT, String.valueOf(Distance));
		} else if (chr.getPosition().distanceSq(mapItem.getPosition()) > 90000.0) {
			chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_SERVER);
		}
		if (mapItem.getMeso() > 0) {
			PartyMember member = chr.getPartyMembership();
			if (member != null && mapItem.getOwner() == chr.getId()) {
				final List<ChannelCharacter> toGive = new LinkedList<>();
				final ChannelServer channelServer = ChannelServer.getInstance();

				Party party = chr.getParty();
				for (final ChannelCharacter m : channelServer.getPartyMembers(party.getId())) {
					// TODO, store info in MaplePartyCharacter instead
					if (m != null) {
						if (m.getMapId() == chr.getMapId()) {
							toGive.add(m);
						}
					}
				}
				for (final ChannelCharacter m : toGive) {
					m.gainMeso(mapItem.getMeso() / toGive.size(), true, true);
				}
			} else {
				chr.gainMeso(mapItem.getMeso(), true, true);
			}
			removeItem(chr, mapItem, ob);
		} else {
			if (GameConstants.isUseableItem(mapItem.getItemId())) {
				useItem(c, mapItem.getItemId());
				removeItem(c.getPlayer(), mapItem, ob);
			} else if (InventoryManipulator.addFromDrop(c, mapItem.getItem(), true)) {
				removeItem(chr, mapItem, ob);
			}
		}
	}

	public static void handlePetLoot(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final Pet pet = chr.getPet(chr.getPetIndex(reader.readInt()));
		reader.skip(9); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
		final Point Client_Reportedpos = reader.readVector();
		final GameMapObject ob = chr.getMap().getMapObject(reader.readInt());

		if (ob == null || pet == null || ob.getType() != GameMapObjectType.ITEM) {
			return;
		}
		final GameMapItem mapitem = (GameMapItem) ob;

		if (mapitem.isPickedUp()) {
			c.write(ChannelPackets.getInventoryFull());
			return;
		}
		if (mapitem.getOwner() != chr.getId() || mapitem.isPlayerDrop()) {
			return;
		}
		final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
		if (Distance > 2500) {
			chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_CLIENT, String.valueOf(Distance));
		} else if (pet.getPosition().distanceSq(mapitem.getPosition()) > 90000.0) {
			chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_SERVER);
		}

		if (mapitem.getMeso() > 0) {
			if (chr.getEquippedItemsInventory().findById(1812000) == null) {
				c.write(ChannelPackets.enableActions());
				return;
			}
			if (chr.getParty() != null && mapitem.getOwner() == chr.getId()) {
				final List<ChannelCharacter> toGive = new LinkedList<>();

				for (final ChannelCharacter m : ChannelServer.getInstance().getPartyMembers(chr.getParty().getId())) {
					// TODO, store info in MaplePartyCharacter instead
					if (m != null) {
						if (m.getMapId() == chr.getMapId()) {
							toGive.add(m);
						}
					}
				}
				for (final ChannelCharacter m : toGive) {
					m.gainMeso(mapitem.getMeso() / toGive.size(), true, true);
				}
			} else {
				chr.gainMeso(mapitem.getMeso(), true, true);
			}
			removeItem(chr, mapitem, ob);
		} else {
			if (GameConstants.isUseableItem(mapitem.getItemId())) {
				useItem(c, mapitem.getItemId());
				removeItem(chr, mapitem, ob);
			} else {
				if (InventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
					removeItem(chr, mapitem, ob);
				}
			}
		}
	}

	private static void useItem(final ChannelClient c, final int id) {
		if (!GameConstants.isUseableItem(id)) {
			throw new IllegalArgumentException("Parameter 'id' is not an id of a useable item.");
		}

		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		final ItemConsumeType type = ii.isConsumeOnPickup(id);
		final ChannelCharacter player = c.getPlayer();

		switch (type) {
		case ON_PICKUP_PARTY:
			if (player.getParty() != null) {
				for (final PartyMember pc : player.getParty().getMembers()) {
					final ChannelCharacter chr = player.getMap().getCharacterById_InMap(pc.getCharacterId());
					if (chr != null) {
						ii.getItemEffect(id).applyTo(chr);
					}
				}
			} else {
				ii.getItemEffect(id).applyTo(player);
			}
			break;
		case ON_PICKUP:
			ii.getItemEffect(id).applyTo(player);
			break;
		}
	}

	private static void removeItem(final ChannelCharacter chr, final GameMapItem mapitem, final GameMapObject ob) {
		mapitem.setPickedUp(true);
		chr.getMap().broadcastMessage(ChannelPackets.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
		chr.getMap().removeMapObject(ob);
	}

	private static void addMedalString(final ChannelCharacter c, final StringBuilder sb) {
		final IItem medal = c.getEquippedItemsInventory().getItem((byte) -46);
		if (medal != null) { // Medal
			sb.append("<");
			sb.append(ItemInfoProvider.getInstance().getName(medal.getItemId()));
			sb.append("> ");
		}
	}
}
