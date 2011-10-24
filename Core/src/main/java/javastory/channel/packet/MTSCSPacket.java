package javastory.channel.packet;

import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.game.IItem;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.Notes.Note;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.HexTool;
import javastory.tools.packets.GameCharacterPacket;

public class MTSCSPacket {

	public static GamePacket warpCS(ChannelClient c) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPEN.getValue());
		final ChannelCharacter chr = c.getPlayer();
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
		builder.writeZeroBytes(13);
		builder
			.writeBytes(HexTool
				.getByteArrayFromHexString("30 00 0F A1 98 00 00 04 00 00 00 10 A1 98 00 00 04 00 00 00 1A A1 98 00 00 04 00 00 00 1B A1 98 00 00 04 00 00 00 37 A1 98 00 00 04 00 00 00 38 A1 98 00 00 04 00 00 00 2A A2 98 00 00 04 00 00 00 2B A2 98 00 00 04 00 00 00 2C A2 98 00 00 04 00 00 00 2D A2 98 00 00 04 00 00 00 2E A2 98 00 00 04 00 00 00 30 A2 98 00 00 04 00 00 00 31 A2 98 00 00 04 00 00 00 32 A2 98 00 00 04 00 00 00 33 A2 98 00 00 04 00 00 00 34 A2 98 00 00 04 00 00 00 3C A2 98 00 00 04 00 00 00 3D A2 98 00 00 04 00 00 00 3E A2 98 00 00 04 00 00 00 3F A2 98 00 00 04 00 00 00 40 A2 98 00 00 04 00 00 00 41 A2 98 00 00 04 00 00 00 42 A2 98 00 00 04 00 00 00 43 A2 98 00 00 04 00 00 00 44 A2 98 00 00 04 00 00 00 45 A2 98 00 00 04 00 00 00 46 A2 98 00 00 04 00 00 00 47 A2 98 00 00 04 00 00 00 48 A2 98 00 FF FF 07 00 89 AA 4F 00 01 00 62 DC 05 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 03 00 00 00 00 00 00 00 00 49 A2 98 00 FF FF 07 00 81 C0 4C 00 01 00 63 E8 03 00 00 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 4A A2 98 00 FF FF 07 00 E7 B3 52 00 01 00 5A 00 00 00 00 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 4B A2 98 00 FF FF 07 00 50 36 56 00 01 00 61 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 4C A2 98 00 FF FF 07 00 A0 A6 4F 00 01 00 63 00 00 00 00 00 2D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 64 30 31 01 FF FF 07 00 88 4E 0F 00 01 00 62 AC 0D 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 43 B4 32 01 FF FF 07 00 F0 71 0F 00 01 00 61 DC 05 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 31 C3 35 01 FF FF 07 00 92 0E 10 00 01 00 61 A0 0F 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 32 C3 35 01 FF FF 07 00 84 0E 10 00 01 00 62 94 11 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 FD 55 3A 01 FF FF 07 00 51 5D 10 00 01 00 62 D0 07 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 95 F6 41 01 FF FF 07 00 AD D1 10 00 01 00 62 70 17 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 9A FE FD 02 FF FF 07 00 A0 A6 4F 00 32 00 62 90 E2 00 00 00 2D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 00 00 00 00 00 00 00 84 87 93 03 FF FF 07 00 6D 4B 4C 00 01 00 62 40 1F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 1E 00 1E 00 1E 00 00 09 95 96 03 FF FF 07 00 D4 F4 4F 00 01 00 61 DC 05 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 0A 95 96 03 FF FF 07 00 D4 F4 4F 00 0B 00 61 98 3A 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 09 1E 2C 04 FF FF 07 00 91 E2 8A 00 01 00 62 40 1F 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 02 35 A2 98 00 36 A2 98 00 84 B5 C4 04 FF FF 07 00 E0 ED 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 85 B5 C4 04 FF FF 07 00 E1 ED 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 86 B5 C4 04 FF FF 07 00 6B EE 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 87 B5 C4 04 FF FF 07 00 7F EE 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 00 00 00 06 00 00 00 31 00 32 00 33 00 00 00 00 00 00 00 05 00 11 00 36 00 08 06 A0 01 15 00 C0 E3 0F 05 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 00 16 00 39 01 0C 06 06 00 00 00 31 00 32 00 33 00 00 00 00 00 00 00 03 00 19 00 3E 00 0C 06 90 01 15 00 90 62 10 05 33 00 00 00 00 00 00 00 03 00 1C 00 03 01 0C 06 06 00 00 00 31 00 32 00 01 00 00 00 00 00 00 00 C3 FD FD 02 01 00 00 00 00 00 00 00 1D 99 98 00 01 00 00 00 00 00 00 00 2E 4A CB 01 01 00 00 00 00 00 00 00 0D FE FD 02 01 00 00 00 00 00 00 00 C1 FD FD 02 01 00 00 00 01 00 00 00 C3 FD FD 02 01 00 00 00 01 00 00 00 1D 99 98 00 01 00 00 00 01 00 00 00 2E 4A CB 01 01 00 00 00 01 00 00 00 0D FE FD 02 01 00 00 00 01 00 00 00 C1 FD FD 02 02 00 00 00 00 00 00 00 C3 FD FD 02 02 00 00 00 00 00 00 00 1D 99 98 00 02 00 00 00 00 00 00 00 2E 4A CB 01 02 00 00 00 00 00 00 00 0D FE FD 02 02 00 00 00 00 00 00 00 C1 FD FD 02 02 00 00 00 01 00 00 00 C3 FD FD 02 02 00 00 00 01 00 00 00 1D 99 98 00 02 00 00 00 01 00 00 00 2E 4A CB 01 02 00 00 00 01 00 00 00 0D FE FD 02 02 00 00 00 01 00 00 00 C1 FD FD 02 03 00 00 00 00 00 00 00 C3 FD FD 02 03 00 00 00 00 00 00 00 1D 99 98 00 03 00 00 00 00 00 00 00 2E 4A CB 01 03 00 00 00 00 00 00 00 0D FE FD 02 03 00 00 00 00 00 00 00 C1 FD FD 02 03 00 00 00 01 00 00 00 C3 FD FD 02 03 00 00 00 01 00 00 00 1D 99 98 00 03 00 00 00 01 00 00 00 2E 4A CB 01 03 00 00 00 01 00 00 00 0D FE FD 02 03 00 00 00 01 00 00 00 C1 FD FD 02 04 00 00 00 00 00 00 00 C3 FD FD 02 04 00 00 00 00 00 00 00 1D 99 98 00 04 00 00 00 00 00 00 00 2E 4A CB 01 04 00 00 00 00 00 00 00 0D FE FD 02 04 00 00 00 00 00 00 00 C1 FD FD 02 04 00 00 00 01 00 00 00 C3 FD FD 02 04 00 00 00 01 00 00 00 1D 99 98 00 04 00 00 00 01 00 00 00 2E 4A CB 01 04 00 00 00 01 00 00 00 0D FE FD 02 04 00 00 00 01 00 00 00 C1 FD FD 02 05 00 00 00 00 00 00 00 C3 FD FD 02 05 00 00 00 00 00 00 00 1D 99 98 00 05 00 00 00 00 00 00 00 2E 4A CB 01 05 00 00 00 00 00 00 00 0D FE FD 02 05 00 00 00 00 00 00 00 C1 FD FD 02 05 00 00 00 01 00 00 00 C3 FD FD 02 05 00 00 00 01 00 00 00 1D 99 98 00 05 00 00 00 01 00 00 00 2E 4A CB 01 05 00 00 00 01 00 00 00 0D FE FD 02 05 00 00 00 01 00 00 00 C1 FD FD 02 06 00 00 00 00 00 00 00 C3 FD FD 02 06 00 00 00 00 00 00 00 1D 99 98 00 06 00 00 00 00 00 00 00 2E 4A CB 01 06 00 00 00 00 00 00 00 0D FE FD 02 06 00 00 00 00 00 00 00 C1 FD FD 02 06 00 00 00 01 00 00 00 C3 FD FD 02 06 00 00 00 01 00 00 00 1D 99 98 00 06 00 00 00 01 00 00 00 2E 4A CB 01 06 00 00 00 01 00 00 00 0D FE FD 02 06 00 00 00 01 00 00 00 C1 FD FD 02 07 00 00 00 00 00 00 00 C3 FD FD 02 07 00 00 00 00 00 00 00 1D 99 98 00 07 00 00 00 00 00 00 00 2E 4A CB 01 07 00 00 00 00 00 00 00 0D FE FD 02 07 00 00 00 00 00 00 00 C1 FD FD 02 07 00 00 00 01 00 00 00 C3 FD FD 02 07 00 00 00 01 00 00 00 1D 99 98 00 07 00 00 00 01 00 00 00 2E 4A CB 01 07 00 00 00 01 00 00 00 0D FE FD 02 07 00 00 00 01 00 00 00 C1 FD FD 02 08 00 00 00 00 00 00 00 C3 FD FD 02 08 00 00 00 00 00 00 00 1D 99 98 00 08 00 00 00 00 00 00 00 2E 4A CB 01 08 00 00 00 00 00 00 00 0D FE FD 02 08 00 00 00 00 00 00 00 C1 FD FD 02 08 00 00 00 01 00 00 00 C3 FD FD 02 08 00 00 00 01 00 00 00 1D 99 98 00 08 00 00 00 01 00 00 00 2E 4A CB 01 08 00 00 00 01 00 00 00 0D FE FD 02 08 00 00 00 01 00 00 00 C1 FD FD 02 00 00 01 00 A2 35 4D 00 CE FD FD 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 FF FF FF FF FF FF FF FF 06 00 00 00 1F 1C 32 01 A7 3F 32 01 FF FF FF FF FF FF FF FF 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 00"));

		return builder.getPacket();

	}

	public static GamePacket useCharm(byte charmsleft, byte daysleft) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(6);
		builder.writeAsByte(1);
		builder.writeByte(charmsleft);
		builder.writeByte(daysleft);

		return builder.getPacket();
	}

	public static GamePacket itemExpired(int itemid) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(2);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static GamePacket ViciousHammer(boolean start, int hammered) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.VICIOUS_HAMMER.getValue());
		if (start) {
			builder.writeAsByte(49);
			builder.writeInt(0);
			builder.writeInt(hammered);
		} else {
			builder.writeAsByte(53);
			builder.writeInt(0);
		}

		return builder.getPacket();
	}

	public static GamePacket changePetName(ChannelCharacter chr, String newname, int slot) {
		PacketBuilder builder = new PacketBuilder();
		builder.writeAsShort(ServerPacketOpcode.PET_NAMECHANGE.getValue());

		builder.writeInt(chr.getId());
		builder.writeAsByte(0);
		builder.writeLengthPrefixedString(newname);
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket showNotes(List<Note> notes) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_NOTES.getValue());
		builder.writeAsByte(3);
		builder.writeAsByte(notes.size());
		for (final Note note : notes) {
			builder.writeInt(note.getId());
			builder.writeLengthPrefixedString(note.getSender());
			builder.writeLengthPrefixedString(note.getMessage());
			builder.writeAsFiletime(note.getTimestamp());
		}

		return builder.getPacket();
	}

	public static GamePacket useChalkboard(final int charid, final String msg) {
		PacketBuilder builder = new PacketBuilder();
		builder.writeAsShort(ServerPacketOpcode.CHALKBOARD.getValue());

		builder.writeInt(charid);
		if (msg == null) {
			builder.writeAsByte(0);
		} else {
			builder.writeAsByte(1);
			builder.writeLengthPrefixedString(msg);
		}

		return builder.getPacket();
	}

	public static GamePacket getTrockRefresh(ChannelCharacter chr, boolean vip, boolean delete) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.TROCK_LOCATIONS.getValue());
		builder.writeAsByte(delete ? 2 : 3);
		builder.writeAsByte(vip ? 1 : 0);
		int[] map = chr.getRocks();
		for (int i = 0; i < 10; i++) {
			builder.writeInt(map[i]);
		}
		return builder.getPacket();
	}

	public static GamePacket sendWishList(ChannelCharacter chr, boolean update) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(update ? 0x58 : 0x52);
		int[] list = chr.getWishlist();
		for (int i = 0; i < 10; i++) {
			builder.writeInt(list[i] != -1 ? list[i] : 0);
		}
		return builder.getPacket();
	}

	public static GamePacket showNXMapleTokens(ChannelCharacter chr) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_UPDATE.getValue());
		builder.writeInt(chr.getCSPoints(1)); // A-cash
		builder.writeInt(chr.getCSPoints(2)); // MPoint

		return builder.getPacket();
	}

	public static GamePacket showBoughtCSItem(int itemid, int accountId) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(0x5A);
		builder.writeLong(137235);// HexTool.getByteArrayFromHexString("13 18 02 00 00 00 00 00"));
									// // uniq id
		builder.writeLong(accountId);
		builder.writeInt(itemid);
		builder.writeBytes(HexTool.getByteArrayFromHexString("03 D1 CC 01")); // probably
																				// SN
		builder.writeAsShort(1); // quantity
		builder.writeZeroBytes(13);
		PacketHelper.addExpirationTime(builder, itemid);
		builder.writeLong(0);

		return builder.getPacket();
	}

	public static GamePacket showBoughtCSQuestItem(short position, int itemid) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(0x92); // MapleSEA v1.01
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 00 00 00 01 00")); // probably ID
																					// and
																					// something
																					// else
		builder.writeAsShort(position);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static GamePacket transferFromCSToInv(IItem item, int position) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(0x6D);
		builder.writeAsByte(position);// in csinventory
		PacketHelper.addItemInfo(builder, item, true, false, true);

		return builder.getPacket();
	}

	public static GamePacket transferFromInvToCS(IItem item, int accountId) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(0x4E);
		// addCashItemInformation(builder, item, accountId);

		return builder.getPacket();
	}

	public static GamePacket enableUse0() {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsByte(0x0B);
		builder.writeBytes(HexTool.getByteArrayFromHexString("01 00 00 00 00"));

		return builder.getPacket();
	}

	public static GamePacket enableUse1() {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeBytes(HexTool.getByteArrayFromHexString("4E 00 00 04 00 07 00 00 00 04 00"));

		return builder.getPacket();
	}

	public static GamePacket enableUse2() {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(0x50);
		builder.writeAsShort(0);

		return builder.getPacket();
	}

	public static GamePacket enableUse3() {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CS_OPERATION.getValue());
		builder.writeAsByte(0x52);
		builder.writeZeroBytes(40);

		return builder.getPacket();
	}
}