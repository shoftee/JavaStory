package javastory.channel.handling;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.SkillFactory;
import javastory.channel.packet.PetPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.game.GameConstants;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.server.ItemInfoProvider;
import javastory.tools.Randomizer;
import server.StatValue;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import client.IItem;
import client.Inventory;
import client.Pet;
import client.PetCommand;
import client.PetDataFactory;
import client.Stat;

public class PetHandler {

    public static void handleSpawnPet(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
        reader.skip(4);
        final byte slot = reader.readByte();
        final Inventory cashInventory = chr.getCashInventory();
        final IItem item = cashInventory.getItem(slot);

        switch (item.getItemId()) {
            case 5000047:
            case 5000028: {
                final Pet pet = Pet.createPet(item.getItemId() + 1);
                if (pet != null) {
                    InventoryManipulator.addById(c, item.getItemId() + 1, (short) 1, null, pet);
                    InventoryManipulator.removeFromSlot(c, cashInventory, slot, (short) 1, false);
                }
                break;
            }
            default: {
                final Pet pet = item.getPet();
                if (pet != null) {
                    if (pet.getSummoned()) { // Already summoned, let's keep it
                        chr.unequipPet(pet, true, false);
                    } else {
                        if (chr.getCurrentSkillLevel(SkillFactory.getSkill(8)) ==
                                0 && chr.getPet(0) != null) {
                            chr.unequipPet(chr.getPet(0), false, false);
                        }
                        if (reader.readByte() == 1) { // Follow the Lead
//			    c.getPlayer().shiftPetsRight();
                        }
                        final Point pos = chr.getPosition();
                        pet.setPosition(pos);
                        pet.setFoothold(chr.getMap().getFootholds().findBelow(pet.getPosition()).getId());
                        pet.setStance(0);
                        pet.setSummoned(true);

                        chr.addPet(pet);
                        chr.getMap().broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), true);

                        final List<StatValue> stats = new ArrayList<>(1);
                        stats.add(new StatValue(Stat.PET, Integer.valueOf(pet.getUniqueId())));

                        c.write(PetPacket.petStatUpdate(chr));
                        chr.startFullnessSchedule(PetDataFactory.getHunger(pet.getPetItemId()), pet, chr.getPetIndex(pet));
                    }
                }
                break;
            }
        }
        c.write(PetPacket.emptyStatUpdate());
    }

    public static void handlePetAutoPotion(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
        reader.skip(13);
        final byte slot = reader.readByte();
        final Inventory useInventory = chr.getUseInventory();
        final IItem toUse = useInventory.getItem(slot);

        if (!chr.isAlive() || toUse == null || toUse.getQuantity() < 1) {
            c.write(MaplePacketCreator.enableActions());
            return;
        }
        InventoryManipulator.removeFromSlot(c, useInventory, slot, (short) 1, false);
        ItemInfoProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr);
    }

    public static void handlePetChat(final int petid, final short command, final String text, ChannelCharacter chr) {
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), command, text, chr.getPetIndex(petid)), true);
    }

    public static void handlePetCommand(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
        final byte petIndex = chr.getPetIndex(reader.readInt());
        if (petIndex == -1) {
            return;
        }
        Pet pet = chr.getPet(petIndex);
        reader.skip(5);
        final byte command = reader.readByte();
        final PetCommand petCommand = PetDataFactory.getPetCommand(pet.getPetItemId(), command);
        boolean success = false;
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + petCommand.getIncrease();
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >=
                        GameConstants.getClosenessNeededForLevel(pet.getLevel() +
                        1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.write(PetPacket.showOwnPetLevelUp(petIndex));
                    chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, petIndex));
                }
                c.write(PetPacket.updatePet(pet, true));
            }
        }
        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), command, petIndex, success, false), true);
    }

    public static void handlePetFood(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
        int previousFullness = 100;

        for (final Pet pet : chr.getPets()) {
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
                            if (newCloseness >=
                                    GameConstants.getClosenessNeededForLevel(pet.getLevel() +
                                    1)) {
                                pet.setLevel(pet.getLevel() + 1);

                                c.write(PetPacket.showOwnPetLevelUp(index));
                                chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, index));
                            }
                        }
                        c.write(PetPacket.updatePet(pet, true));
                        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
                    } else {
                        if (gainCloseness) {
                            int newCloseness = pet.getCloseness() - 1;
                            if (newCloseness < 0) {
                                newCloseness = 0;
                            }
                            pet.setCloseness(newCloseness);
                            if (newCloseness <
                                    GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                                pet.setLevel(pet.getLevel() - 1);
                            }
                        }
                        c.write(PetPacket.updatePet(pet, true));
                        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, chr.getPetIndex(pet), false, true), true);
                    }
                    InventoryManipulator.removeById(c, chr.getUseInventory(), itemId, 1, true, false);
                    return;
                }
            }
        }
        c.write(MaplePacketCreator.enableActions());
    }

    public static void handleMovePet(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
        final int petId = reader.readInt();
        reader.skip(4);
        reader.skip(8); // Start POS
        final List<LifeMovementFragment> res = MovementParse.parseMovement(reader);

        if (!res.isEmpty()) { // map crash hack
            final byte slot = chr.getPetIndex(petId);
            if (slot == -1) {
                return;
            }
            chr.getPet(slot).updatePosition(res);
            chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), petId, slot, res), false);
        }
    }
}
