package javastory.channel.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javastory.channel.ChannelCharacter;
import javastory.channel.client.ISkill;
import javastory.channel.client.Ring;
import javastory.channel.client.SkillEntry;
import javastory.client.IEquip;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.client.ItemType;
import javastory.client.Pet;
import javastory.game.GameConstants;
import javastory.game.quest.QuestStatus;
import javastory.io.PacketBuilder;
import javastory.server.movement.LifeMovementFragment;
import javastory.tools.FiletimeUtil;
import javastory.world.core.PlayerCooldownValueHolder;

import com.google.common.collect.Lists;

public class PacketHelper {

	private static final long FT_UT_OFFSET = 116444592000000000L; // EDT
	public static final byte unk1[] = new byte[] {
		(byte) 0x00, (byte) 0x40,
		(byte) 0xE0, (byte) 0xFD
	};
	public static final byte unk2[] = new byte[] {
		(byte) 0x3B, (byte) 0x37,
		(byte) 0x4F, (byte) 0x01
	};

	public static long getKoreanTimestamp(final long realTimestamp) {
		long time = (realTimestamp / 1000 / 60); // convert to minutes
		return ((time * 600000000) + FT_UT_OFFSET);
	}

	public static long getTime(final long realTimestamp) {
		long time = (realTimestamp / 1000); // convert to seconds
		return ((time * 10000000) + FT_UT_OFFSET);
	}

	public static void addQuestInfo(final PacketBuilder builder,
			final ChannelCharacter chr) {
		final List<QuestStatus> started = chr.getStartedQuests();
		builder.writeAsShort(started.size());
		for (final QuestStatus q : started) {
			builder.writeAsShort(q.getQuestId());
			builder.writeLengthPrefixedString(q.getCustomData() != null ? q
					.getCustomData() : "");
		}
		final List<QuestStatus> completed = chr.getCompletedQuests();
		long time;
		builder.writeAsShort(completed.size());
		for (final QuestStatus q : completed) {
			builder.writeAsShort(q.getQuestId());
			time = FiletimeUtil.getFiletime(q.getCompletionTime());
			builder.writeLong(time); // maybe start time? no effect.
		}
	}

	public static void addSkillInfo(final PacketBuilder builder,
			final ChannelCharacter chr) {
		final Map<ISkill, SkillEntry> skills = chr.getSkills();
		builder.writeAsShort(skills.size());
		for (final Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
			builder.writeInt(skill.getKey().getId());
			builder.writeInt(skill.getValue().getCurrentLevel());
			addExpirationTime(builder, -1);
			if (skill.getKey().isFourthJob()) {
				builder.writeInt(skill.getValue().getMasterLevel());
			}
		}
	}

	public static void addCoolDownInfo(final PacketBuilder builder,
			final ChannelCharacter chr) {
		builder.writeAsShort(chr.getAllCooldowns().size());
		for (final PlayerCooldownValueHolder cooling : chr.getAllCooldowns()) {
			builder.writeInt(cooling.skillId);
			builder.writeAsShort((int) (cooling.length + cooling.startTime - System
					.currentTimeMillis()) / 1000);
		}
	}

	public static void addRocksInfo(final PacketBuilder builder,
			final ChannelCharacter chr) {
		builder.writeInt(999999999); // Teleport maps (TODO)
		builder.writeInt(999999999);
		builder.writeInt(999999999);
		builder.writeInt(999999999);
		builder.writeInt(999999999);
		final int[] map = chr.getRocks();
		for (int i = 0; i < 10; i++) { // VIP teleport map
			builder.writeInt(map[i]);
		}
	}

	public static void addMonsterBookInfo(final PacketBuilder builder,
			final ChannelCharacter chr) {
		builder.writeInt(chr.getMonsterBookCover());
		builder.writeAsByte(0);
		chr.getMonsterBook().addCardPacket(builder);
	}

	public static void addRingInfo(final PacketBuilder builder,
			final ChannelCharacter chr) {
		List<Ring> rings = new ArrayList<>();
		final Inventory equipped = chr.getEquippedItemsInventory();
		for (final IItem item : equipped) {
			final IEquip equip = (IEquip) item;
			if (equip.getRingId() > -1) {
				rings.add(Ring.loadFromDb(equip.getRingId()));
			}
		}
		final Inventory equips = chr.getEquipInventory();
		for (final IItem item : equips) {
			final IEquip equip = (IEquip) item;
			if (equip.getRingId() > -1) {
				rings.add(Ring.loadFromDb(equip.getRingId()));
			}
		}
		Collections.sort(rings);
		boolean FR_last = false;
		for (final Ring ring : rings) {
			final int ringItemId = ring.getItemId();
			if ((ringItemId >= 1112800 && ringItemId <= 1112803
					|| ringItemId <= 1112806 || ringItemId <= 1112807 || ringItemId <= 1112809)
					&& rings.indexOf(ring) == 0) {
				builder.writeAsShort(0);
			}
			builder.writeAsShort(0);
			builder.writeAsShort(1);
			builder.writeInt(ring.getPartnerCharacterId());
			builder.writePaddedString(ring.getPartnerName(), 13);
			builder.writeInt(ring.getRingId());
			builder.writeInt(0);
			builder.writeInt(ring.getPartnerRingId());
			if (ringItemId >= 1112800 && ringItemId <= 1112803
					|| ringItemId <= 1112806 || ringItemId <= 1112807
					|| ringItemId <= 1112809) {
				FR_last = true;
				builder.writeInt(0);
				builder.writeInt(ringItemId);
				builder.writeAsShort(0);
			} else {
				if (rings.size() > 1) {
					builder.writeAsShort(0);
				}
				FR_last = false;
			}
		}
		if (!FR_last) {
			builder.writeLong(0);
		}
	}

	public static void addInventoryInfo(PacketBuilder builder,
			ChannelCharacter chr) {
		builder.writeInt(chr.getMeso()); // mesos
		builder.writeByte(chr.getEquipInventory().getSlotLimit()); // equip
																	// slots
		builder.writeByte(chr.getUseInventory().getSlotLimit()); // use slots
		builder.writeByte(chr.getSetupInventory().getSlotLimit()); // set-up
																	// slots
		builder.writeByte(chr.getEtcInventory().getSlotLimit()); // etc slots
		builder.writeByte(chr.getCashInventory().getSlotLimit()); // cash slots
		builder.writeBytes(unk1);
		builder.writeBytes(unk2);
		Inventory inventory = chr.getEquippedItemsInventory();
		List<IItem> equipped = Lists.newArrayList(inventory);

		Collections.sort(equipped);
		for (IItem item : equipped) {
			if (item.getPosition() < 0 && item.getPosition() > -100) {
				addItemInfo(builder, item, false, false);
			}
		}
		builder.writeAsShort(0); // start of equipped nx
		for (IItem item : equipped) {
			if (item.getPosition() < -100) {
				addItemInfo(builder, item, false, false);
			}
		}

		builder.writeAsShort(0); // start of equip inventory
		inventory = chr.getEquipInventory();
		for (IItem item : inventory) {
			addItemInfo(builder, item, false, false);
		}
		builder.writeInt(0); // start of use inventory
		inventory = chr.getUseInventory();
		for (IItem item : inventory) {
			addItemInfo(builder, item, false, false);
		}
		builder.writeAsByte(0); // start of set-up inventory
		inventory = chr.getSetupInventory();
		for (IItem item : inventory) {
			addItemInfo(builder, item, false, false);
		}
		builder.writeAsByte(0); // start of etc inventory
		inventory = chr.getEtcInventory();
		for (IItem item : inventory) {
			addItemInfo(builder, item, false, false);
		}
		builder.writeAsByte(0); // start of cash inventory
		inventory = chr.getCashInventory();
		for (IItem item : inventory) {
			addItemInfo(builder, item, false, false);
		}
		builder.writeAsByte(0);
	}

	public static void addExpirationTime(final PacketBuilder builder,
			final long time) {
		if (time != -1) {
			builder.writeLong(FiletimeUtil.getFiletime(time));
		} else {
			builder.writeLong(FiletimeUtil.ITEM_EXPIRATION);
		}
	}

	public static void addItemInfo(final PacketBuilder builder,
			final IItem item, final boolean zeroPosition, final boolean leaveOut) {
		addItemInfo(builder, item, zeroPosition, leaveOut, false);
	}

	public static void addItemInfo(final PacketBuilder builder,
			final IItem item, final boolean zeroPosition,
			final boolean leaveOut, final boolean trade) {
		short pos = item.getPosition();
		if (zeroPosition) {
			if (!leaveOut) {
				builder.writeAsByte(0);
			}
		} else {
			if (pos <= -1) {
				pos *= -1;
				if (pos > 100) {
					pos -= 100;
				}
			}
			if (!trade && item.getType() == ItemType.EQUIP) {
				builder.writeAsShort(pos);
			} else {
				builder.writeAsByte(pos);
			}
		}
		builder.writeByte(item.getPet() != null ? 3 : item.getType().asByte());
		builder.writeInt(item.getItemId());

		if (item.getPet() != null) { // Pet
			final Pet pet = item.getPet();

			builder.writeAsByte(1);
			builder.writeInt(pet.getUniqueId());
			builder.writeZeroBytes(4);
			addExpirationTime(builder, item.getExpiration()); // 80 F9 58 8D 3B
																// C7 24 Time
																// where it
																// expired?

			builder.writePaddedString(pet.getName(), 13);
			builder.writeAsByte(pet.getLevel());
			builder.writeAsShort(pet.getCloseness());

			builder.writeAsByte(pet.getFullness());
			builder.writeLong(getTime(System.currentTimeMillis()));
			builder.writeAsShort(0);
			builder.writeInt(1);
			builder.writeInt(0);
			builder.writeZeroBytes(5);
		} else {
			builder.writeAsByte(0);
			addExpirationTime(builder, item.getExpiration());

			if (item.getType() == ItemType.EQUIP) {
				final IEquip equip = (IEquip) item;
				builder.writeByte(equip.getUpgradeSlots());
				builder.writeByte(equip.getLevel());
				builder.writeAsShort(equip.getStr());
				builder.writeAsShort(equip.getDex());
				builder.writeAsShort(equip.getInt());
				builder.writeAsShort(equip.getLuk());
				builder.writeAsShort(equip.getHp());
				builder.writeAsShort(equip.getMp());
				builder.writeAsShort(equip.getWatk());
				builder.writeAsShort(equip.getMatk());
				builder.writeAsShort(equip.getWdef());
				builder.writeAsShort(equip.getMdef());
				builder.writeAsShort(equip.getAcc());
				builder.writeAsShort(equip.getAvoid());
				builder.writeAsShort(equip.getHands());
				builder.writeAsShort(equip.getSpeed());
				builder.writeAsShort(equip.getJump());
				builder.writeLengthPrefixedString(equip.getOwner());
				builder.writeAsShort(equip.getFlag());
				builder.writeAsByte(0);
				builder.writeByte(equip.getItemLevel()); // Item level
				builder.writeAsShort(0);
				builder.writeAsShort(equip.getItemEXP()); // Item Exp
				builder.writeInt(-1);
				builder.writeInt(equip.getViciousHammer());
				builder.writeZeroBytes(12);
				builder.writeLong(0);
				builder.writeBytes(unk1);
				builder.writeBytes(unk2);
				builder.writeInt(-1);
			} else {
				builder.writeAsShort(item.getQuantity());
				builder.writeLengthPrefixedString(item.getOwner());
				builder.writeAsShort(item.getFlag());

				if (GameConstants.isThrowingStar(item.getItemId())
						|| GameConstants.isBullet(item.getItemId())) {
					builder.writeInt(2);
					builder.writeAsShort(0x54);
					builder.writeAsByte(0);
					builder.writeAsByte(0x34);
				}
			}
		}
	}

	public static void serializeMovementList(final PacketBuilder lew,
			final List<LifeMovementFragment> moves) {
		lew.writeAsByte(moves.size());
		for (LifeMovementFragment move : moves) {
			move.serialize(lew);
		}
	}
}