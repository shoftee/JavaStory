package javastory.channel.packet;

import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.client.Pet;
import javastory.channel.movement.LifeMovementFragment;
import javastory.client.Stat;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.FiletimeUtil;
import javastory.tools.HexTool;

public class PetPacket {

	public static final GamePacket updatePet(final Pet pet, final boolean alive) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MODIFY_INVENTORY_ITEM
				.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(2);
		builder.writeAsByte(3);
		builder.writeAsByte(5);

		final short inventoryPosition = pet.getInventoryPosition();
		builder.writeAsByte(inventoryPosition);
		builder.writeAsShort(0);
		builder.writeAsByte(5);
		builder.writeAsByte(inventoryPosition);
		builder.writeAsByte(0);

		builder.writeAsByte(3);

		builder.writeInt(pet.getPetItemId());
		builder.writeAsByte(1);

		builder.writeInt(pet.getUniqueId());
		builder.writeInt(0);
		builder.writeBytes(HexTool
				.getByteArrayFromHexString("00 80 F9 58 8D 3B C7 24"));
		builder.writePaddedString(pet.getName(), 13);
		builder.writeAsByte(pet.getLevel());
		builder.writeAsShort(pet.getCloseness());
		builder.writeAsByte(pet.getFullness());

		long expiration;
		if (alive) {
			expiration = FiletimeUtil.getFiletime(System.currentTimeMillis());
		} else {
			expiration = FiletimeUtil.ITEM_EXPIRATION;
		}
		builder.writeLong(expiration);

		builder.writeAsShort(0);
		builder.writeInt(1);
		builder.writeInt(0);
		builder.writeZeroBytes(5);
		return builder.getPacket();
	}

	public static final GamePacket showPet(final ChannelCharacter chr,
			final Pet pet) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_PET.getValue());
		builder.writeInt(chr.getId());
		builder.writeByte(chr.getPetIndex(pet));

		builder.writeAsByte(1);
		builder.writeAsByte(1);
		builder.writeInt(pet.getPetItemId());
		builder.writeLengthPrefixedString(pet.getName());
		builder.writeInt(pet.getUniqueId());
		builder.writeInt(0);
		builder.writeAsShort(pet.getPosition().x);
		builder.writeAsShort(pet.getPosition().y - 20);
		builder.writeAsByte(pet.getStance());
		builder.writeInt(pet.getFoothold());

		return builder.getPacket();
	}

	public static GamePacket removePet(final ChannelCharacter chr,
			final Pet pet, final boolean hunger) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_PET.getValue());
		builder.writeInt(chr.getId());
		builder.writeByte(chr.getPetIndex(pet));

		builder.writeAsByte(0);
		builder.writeAsByte(hunger);
		return builder.getPacket();
	}

	public static final GamePacket movePet(final int characterId, final int petId,
			final byte slot, final List<LifeMovementFragment> moves) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MOVE_PET.getValue());
		builder.writeInt(characterId);
		builder.writeByte(slot);
		builder.writeLong(petId);
		PacketHelper.serializeMovementList(builder, moves);

		return builder.getPacket();
	}

	public static final GamePacket petChat(final int cid, final int un,
			final String text, final byte slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PET_CHAT.getValue());
		builder.writeInt(cid);
		builder.writeByte(slot);
		builder.writeAsByte(0);
		builder.writeAsByte(un);
		builder.writeLengthPrefixedString(text);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static final GamePacket commandResponse(final int cid,
			final byte command, final byte slot, final boolean success,
			final boolean food) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PET_COMMAND.getValue());
		builder.writeInt(cid);
		builder.writeByte(slot);
		builder.writeAsByte(command == 1 ? 1 : 0);
		builder.writeByte(command);
		if (command == 1) {
			builder.writeAsByte(0);
		} else {
			builder.writeAsShort(success ? 1 : 0);
		}

		return builder.getPacket();
	}

	public static final GamePacket showOwnPetLevelUp(final byte index) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT
				.getValue());
		builder.writeAsByte(4);
		builder.writeAsByte(0);
		builder.writeByte(index); // Pet Index

		return builder.getPacket();
	}

	public static final GamePacket showPetLevelUp(final ChannelCharacter chr,
			final byte index) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(chr.getId());
		builder.writeAsByte(4);
		builder.writeAsByte(0);
		builder.writeByte(index);

		return builder.getPacket();
	}

	public static final GamePacket emptyStatUpdate() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
		builder.writeAsByte(1);
		builder.writeInt(0);

		return builder.getPacket();
	}

	public static final GamePacket petStatUpdate(final ChannelCharacter chr) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
		builder.writeAsByte(0);

		int mask = 0;
		mask |= Stat.PET.getValue();
		builder.writeInt(mask);

		byte count = 0;
		for (final Pet pet : chr.getPets()) {
			if (pet.isSummoned()) {
				builder.writeInt(pet.getUniqueId());
				builder.writeZeroBytes(4);
				count++;
			}
		}
		while (count < 3) {
			builder.writeZeroBytes(8);
			count++;
		}
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static final GamePacket weirdStatUpdate(final Pet pet) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_STATS.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(8);
		builder.writeAsByte(0);
		builder.writeAsByte(0x18);
		builder.writeAsByte(0);
		builder.writeInt(pet.getUniqueId());
		builder.writeLong(0);
		builder.writeLong(0);
		builder.writeInt(0);
		builder.writeAsByte(1);

		return builder.getPacket();
	}
}