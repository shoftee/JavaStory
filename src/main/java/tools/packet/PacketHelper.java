package tools.packet;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import client.IEquip;
import client.Item;
import client.ISkill;
import client.GameConstants;
import client.Ring;
import client.Pet;
import client.GameCharacter;
import client.Inventory;
import client.InventoryType;
import client.QuestStatus;
import client.IItem;
import client.SkillEntry;
import handling.world.PlayerCoolDownValueHolder;
import server.movement.LifeMovementFragment;
import tools.FiletimeUtil;
import org.javastory.io.PacketBuilder;
import org.javastory.io.PacketBuilder;
import tools.StringUtil;

public class PacketHelper {

    private static final long FT_UT_OFFSET = 116444592000000000L; // EDT
    public static final byte unk1[] = new byte[]{(byte) 0x00, (byte) 0x40, (byte) 0xE0, (byte) 0xFD};
    public static final byte unk2[] = new byte[]{(byte) 0x3B, (byte) 0x37, (byte) 0x4F, (byte) 0x01};

    public static long getKoreanTimestamp(final long realTimestamp) {
        long time = (realTimestamp / 1000 / 60); // convert to minutes
        return ((time * 600000000) + FT_UT_OFFSET);
    }

    public static long getTime(final long realTimestamp) {
        long time = (realTimestamp / 1000); // convert to seconds
        return ((time * 10000000) + FT_UT_OFFSET);
    }

    public static void addQuestInfo(final PacketBuilder builder, final GameCharacter chr) {
        final List<QuestStatus> started = chr.getStartedQuests();
        builder.writeAsShort(started.size());
        for (final QuestStatus q : started) {
            builder.writeAsShort(q.getQuest().getId());
            builder.writeLengthPrefixedString(q.getCustomData() != null ? q.getCustomData() : "");
        }
        final List<QuestStatus> completed = chr.getCompletedQuests();
        long time;
        builder.writeAsShort(completed.size());
        for (final QuestStatus q : completed) {
            builder.writeAsShort(q.getQuest().getId());
            time = FiletimeUtil.getFiletime(q.getCompletionTime());
            builder.writeLong(time); // maybe start time? no effect.
        }
    }

    public static void addSkillInfo(final PacketBuilder builder, final GameCharacter chr) {
        final Map<ISkill, SkillEntry> skills = chr.getSkills();
        builder.writeAsShort(skills.size());
        for (final Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
            builder.writeInt(skill.getKey().getId());
            builder.writeInt(skill.getValue().skillevel);
            addExpirationTime(builder, -1);
            if (skill.getKey().isFourthJob()) {
                builder.writeInt(skill.getValue().masterlevel);
            }
        }
    }

    public static void addCoolDownInfo(final PacketBuilder builder, final GameCharacter chr) {
        builder.writeAsShort(chr.getAllCooldowns().size());
        for (final PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
            builder.writeInt(cooling.skillId);
            builder.writeAsShort((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static void addRocksInfo(final PacketBuilder builder, final GameCharacter chr) {
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

    public static void addMonsterBookInfo(final PacketBuilder builder, final GameCharacter chr) {
        builder.writeInt(chr.getMonsterBookCover());
        builder.writeAsByte(0);
        chr.getMonsterBook().addCardPacket(builder);
    }

    public static void addRingInfo(final PacketBuilder builder, final GameCharacter chr) {
        List<Ring> rings = new ArrayList<Ring>();
        Inventory iv = chr.getInventory(InventoryType.EQUIPPED);
        for (final IItem item : iv.list()) {
            if (((IEquip) item).getRingId() > -1) {
                rings.add(Ring.loadFromDb(((IEquip) item).getRingId()));
            }
        }
        iv = chr.getInventory(InventoryType.EQUIP);
        for (final IItem item : iv.list()) {
            if (((IEquip) item).getRingId() > -1) {
                rings.add(Ring.loadFromDb(((IEquip) item).getRingId()));
            }
        }
        Collections.sort(rings);
        boolean FR_last = false;
        for (final Ring ring : rings) {
            if ((ring.getItemId() >= 1112800 && ring.getItemId() <= 1112803 || ring.getItemId() <= 1112806 || ring.getItemId() <= 1112807 || ring.getItemId() <= 1112809) && rings.indexOf(ring) == 0) {
                builder.writeAsShort(0);
            }
            builder.writeAsShort(0);
            builder.writeAsShort(1);
            builder.writeInt(ring.getPartnerChrId());
            builder.writePaddedString(ring.getPartnerName(), 13);
            builder.writeInt(ring.getRingId());
            builder.writeInt(0);
            builder.writeInt(ring.getPartnerRingId());
            if (ring.getItemId() >= 1112800 && ring.getItemId() <= 1112803 || ring.getItemId() <= 1112806 || ring.getItemId() <= 1112807 || ring.getItemId() <= 1112809) {
                FR_last = true;
                builder.writeInt(0);
                builder.writeInt(ring.getItemId());
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

    public static void addInventoryInfo(PacketBuilder builder, GameCharacter chr) {
        builder.writeInt(chr.getMeso()); // mesos
        builder.writeByte(chr.getInventory(InventoryType.EQUIP).getSlotLimit()); // equip slots
        builder.writeByte(chr.getInventory(InventoryType.USE).getSlotLimit()); // use slots
        builder.writeByte(chr.getInventory(InventoryType.SETUP).getSlotLimit()); // set-up slots
        builder.writeByte(chr.getInventory(InventoryType.ETC).getSlotLimit()); // etc slots
        builder.writeByte(chr.getInventory(InventoryType.CASH).getSlotLimit()); // cash slots
        builder.writeBytes(unk1);
        builder.writeBytes(unk2);
        Inventory iv = chr.getInventory(InventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        List<Item> equipped = new ArrayList<Item>(equippedC.size());
        for (IItem item : equippedC) {
            equipped.add((Item) item);
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemInfo(builder, item, false, false);
            }
        }
        builder.writeAsShort(0); // start of equipped nx
        for (Item item : equipped) {
            if (item.getPosition() < -100) {
                addItemInfo(builder, item, false, false);
            }
        }

        builder.writeAsShort(0); // start of equip inventory
        iv = chr.getInventory(InventoryType.EQUIP);
        for (IItem item : iv.list()) {
            addItemInfo(builder, item, false, false);
        }
        builder.writeInt(0); // start of use inventory
        iv = chr.getInventory(InventoryType.USE);
        for (IItem item : iv.list()) {
            addItemInfo(builder, item, false, false);
        }
        builder.writeAsByte(0); // start of set-up inventory
        iv = chr.getInventory(InventoryType.SETUP);
        for (IItem item : iv.list()) {
            addItemInfo(builder, item, false, false);
        }
        builder.writeAsByte(0); // start of etc inventory
        iv = chr.getInventory(InventoryType.ETC);
        for (IItem item : iv.list()) {
            addItemInfo(builder, item, false, false);
        }
        builder.writeAsByte(0); // start of cash inventory
        iv = chr.getInventory(InventoryType.CASH);
        for (IItem item : iv.list()) {
            addItemInfo(builder, item, false, false);
        }
        builder.writeAsByte(0);
    }

    public static void addCharStats(final PacketBuilder builder, final GameCharacter chr) {
        builder.writeInt(chr.getId()); // character id
        builder.writePaddedString(chr.getName(), 13);
        builder.writeAsByte(chr.getGender()); // gender (0 = male, 1 = female)
        builder.writeAsByte(chr.getSkinColor()); // skin color
        builder.writeInt(chr.getFace()); // face
        builder.writeInt(chr.getHair()); // hair
        builder.writeZeroBytes(24);
        builder.writeAsByte(chr.getLevel()); // level
        builder.writeAsShort(chr.getJob()); // job
        chr.getStat().connectData(builder);
        builder.writeAsShort(chr.getRemainingAp()); // remaining ap
        if (GameConstants.isEvan(chr.getJob())) {
            final int size = chr.getRemainingSpSize();
            builder.writeAsByte(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    builder.writeAsByte(i + 1);
                    builder.writeAsByte(chr.getRemainingSp(i));
                }
            }
        } else {
            builder.writeAsShort(chr.getRemainingSp()); // remaining sp
        }
        builder.writeInt(chr.getExp()); // exp
        builder.writeAsShort(chr.getFame()); // fame
        builder.writeInt(0); // Gachapon exp
        builder.writeLong(0); // This must be something, just leave it lol
        builder.writeInt(chr.getMapId()); // current map id
        builder.writeAsByte(chr.getInitialSpawnpoint()); // spawnpoint
        builder.writeAsShort(chr.getSubcategory()); // 1 = Dual Blade
    }

    public static void addCharLook(final PacketBuilder builder, final GameCharacter chr, final boolean mega) {
        builder.writeAsByte(chr.getGender());
        builder.writeAsByte(chr.getSkinColor());
        builder.writeInt(chr.getFace());
        builder.writeAsByte(mega ? 0 : 1);
        builder.writeInt(chr.getHair());

        final Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        Inventory equip = chr.getInventory(InventoryType.EQUIPPED);

        for (final IItem item : equip.list()) {
            byte pos = (byte) (item.getPosition() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) {
                pos -= 100;
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }
        for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
            builder.writeByte(entry.getKey());
            builder.writeInt(entry.getValue());
        }
        builder.writeAsByte(0xFF); // end of visible itens
        // masked itens
        for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            builder.writeByte(entry.getKey());
            builder.writeInt(entry.getValue());
        }
        builder.writeAsByte(0xFF); // ending markers

        final IItem cWeapon = equip.getItem((byte) -111);
        builder.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
        builder.writeInt(0);
        builder.writeLong(0);
    }

    public static void addExpirationTime(final PacketBuilder builder, final long time) {
        if (time != -1) {
            builder.writeLong(FiletimeUtil.getFiletime(time));
        } else {
            builder.writeLong(FiletimeUtil.ITEM_EXPIRATION);
        }
    }

    public static void addItemInfo(final PacketBuilder builder, final IItem item, final boolean zeroPosition, final boolean leaveOut) {
        addItemInfo(builder, item, zeroPosition, leaveOut, false);
    }

    public static void addItemInfo(final PacketBuilder builder, final IItem item, final boolean zeroPosition, final boolean leaveOut, final boolean trade) {
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
            if (!trade && item.getType() == 1) {
                builder.writeAsShort(pos);
            } else {
                builder.writeAsByte(pos);
            }
        }
        builder.writeByte(item.getPet() != null ? 3 : item.getType());
        builder.writeInt(item.getItemId());

        if (item.getPet() != null) { // Pet
            final Pet pet = item.getPet();

            builder.writeAsByte(1);
            builder.writeInt(pet.getUniqueId());
            builder.writeZeroBytes(4);
            addExpirationTime(builder, item.getExpiration()); // 80 F9 58 8D 3B C7 24 Time where it expired?

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

            if (item.getType() == 1) {
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

                if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                    builder.writeInt(2);
                    builder.writeAsShort(0x54);
                    builder.writeAsByte(0);
                    builder.writeAsByte(0x34);
                }
            }
        }
    }

    public static void serializeMovementList(final PacketBuilder lew, final List<LifeMovementFragment> moves) {
        lew.writeAsByte(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }
}