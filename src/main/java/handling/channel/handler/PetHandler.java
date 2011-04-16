package handling.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import client.IItem;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleStat;
import client.GameConstants;
import client.SkillFactory;
import client.PetCommand;
import client.PetDataFactory;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.movement.LifeMovementFragment;
import tools.Pair;
import tools.MaplePacketCreator;
import tools.packet.PetPacket;

public class PetHandler {

    public static final void handleSpawnPet(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        reader.skip(4);
        final byte slot = reader.readByte();
        final IItem item = chr.getInventory(MapleInventoryType.CASH).getItem(slot);

        switch (item.getItemId()) {
            case 5000047:
            case 5000028: {
                final MaplePet pet = MaplePet.createPet(item.getItemId() + 1);
                if (pet != null) {
                    MapleInventoryManipulator.addById(c, item.getItemId() + 1, (short) 1, null, pet);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
                }
                break;
            }
            default: {
                final MaplePet pet = item.getPet();
                if (pet != null) {
                    if (pet.getSummoned()) { // Already summoned, let's keep it
                        chr.unequipPet(pet, true, false);
                    } else {
                        if (chr.getSkillLevel(SkillFactory.getSkill(8)) == 0 && chr.getPet(0) != null) {
                            chr.unequipPet(chr.getPet(0), false, false);
                        }
                        if (reader.readByte() == 1) { // Follow the Lead
//			    c.getPlayer().shiftPetsRight();
                        }
                        final Point pos = chr.getPosition();
                        pet.setPos(pos);
                        pet.setFh(chr.getMap().getFootholds().findBelow(pet.getPos()).getId());
                        pet.setStance(0);
                        pet.setSummoned(true);

                        chr.addPet(pet);
                        chr.getMap().broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), true);

                        final List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>(1);
                        stats.add(new Pair<MapleStat, Integer>(MapleStat.PET, Integer.valueOf(pet.getUniqueId())));

                        c.getSession().write(PetPacket.petStatUpdate(chr));
                        chr.startFullnessSchedule(PetDataFactory.getHunger(pet.getPetItemId()), pet, chr.getPetIndex(pet));
                    }
                }
                break;
            }
        }
        c.getSession().write(PetPacket.emptyStatUpdate());
    }

    public static final void handlePetAutoPotion(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        reader.skip(13);
        final byte slot = reader.readByte();
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (!chr.isAlive() || toUse == null || toUse.getQuantity() < 1) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr);
    }

    public static final void handlePetChat(final int petid, final short command, final String text, MapleCharacter chr) {
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), command, text, chr.getPetIndex(petid)), true);
    }

    public static final void handlePetCommand(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        final byte petIndex = chr.getPetIndex(reader.readInt());
        if (petIndex == -1) {
            return;
        }
        MaplePet pet = chr.getPet(petIndex);
        reader.skip(5);
        final byte command = reader.readByte();
        final PetCommand petCommand = PetDataFactory.getPetCommand(pet.getPetItemId(), (int) command);
        boolean success = false;
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + petCommand.getIncrease();
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().write(PetPacket.showOwnPetLevelUp(petIndex));
                    chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, petIndex));
                }
                c.getSession().write(PetPacket.updatePet(pet, true));
            }
        }
        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), command, petIndex, success, false), true);
    }

    public static final void handlePetFood(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        int previousFullness = 100;

        for (final MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                if (pet.getFullness() < previousFullness) {
                    previousFullness = pet.getFullness();

                    reader.skip(6);
                    final int itemId = reader.readInt();

                    boolean gainCloseness = false;

                    if (Randomizer.nextInt(99) <= 50) {
                        gainCloseness = true;
                    }
                    if (pet.getFullness() < 100) {
                        int newFullness = pet.getFullness() + 30;
                        if (newFullness > 100) {
                            newFullness = 100;
                        }
                        pet.setFullness(newFullness);
                        final byte index = chr.getPetIndex(pet);

                        if (gainCloseness && pet.getCloseness() < 30000) {
                            int newCloseness = pet.getCloseness() + 1;
                            if (newCloseness > 30000) {
                                newCloseness = 30000;
                            }
                            pet.setCloseness(newCloseness);
                            if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                pet.setLevel(pet.getLevel() + 1);

                                c.getSession().write(PetPacket.showOwnPetLevelUp(index));
                                chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, index));
                            }
                        }
                        c.getSession().write(PetPacket.updatePet(pet, true));
                        chr.getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
                    } else {
                        if (gainCloseness) {
                            int newCloseness = pet.getCloseness() - 1;
                            if (newCloseness < 0) {
                                newCloseness = 0;
                            }
                            pet.setCloseness(newCloseness);
                            if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                                pet.setLevel(pet.getLevel() - 1);
                            }
                        }
                        c.getSession().write(PetPacket.updatePet(pet, true));
                        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, chr.getPetIndex(pet), false, true), true);
                    }
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, true, false);
                    return;
                }
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void handleMovePet(final PacketReader reader, final MapleCharacter chr) throws PacketFormatException {
        final int petId = reader.readInt();
        reader.skip(4);
        reader.skip(8); // Start POS
        final List<LifeMovementFragment> res = MovementParse.parseMovement(reader);

        if (res.size() != 0) { // map crash hack
            final byte slot = chr.getPetIndex(petId);
            if (slot == -1) {
                return;
            }
            chr.getPet(slot).updatePosition(res);
            chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), petId, slot, res), false);
        }
    }
}
