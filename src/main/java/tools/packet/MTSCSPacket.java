package tools.packet;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

import client.IItem;
import client.MapleClient;
import client.MapleCharacter;
import handling.MaplePacket;
import handling.SendPacketOpcode;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MTSCSPacket {

	public static MaplePacket warpCS(MapleClient c) {
		final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPEN.getValue());
		final MapleCharacter chr = c.getPlayer();
		mplew.writeLong(-1);
		mplew.write(0);
		PacketHelper.addCharStats(mplew, chr);
		mplew.write(chr.getBuddylist().getCapacity());
		if (chr.getBlessOfFairyOrigin() != null) {
			mplew.write(1);
			mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
		} else {
			mplew.write(0);
		}
		PacketHelper.addInventoryInfo(mplew, chr);
		PacketHelper.addSkillInfo(mplew, chr);
		PacketHelper.addCoolDownInfo(mplew, chr);
		PacketHelper.addQuestInfo(mplew, chr);
		PacketHelper.addRingInfo(mplew, chr);
		PacketHelper.addRocksInfo(mplew, chr);
		mplew.writeZeroBytes(13);
		mplew.write(HexTool.getByteArrayFromHexString("30 00 0F A1 98 00 00 04 00 00 00 10 A1 98 00 00 04 00 00 00 1A A1 98 00 00 04 00 00 00 1B A1 98 00 00 04 00 00 00 37 A1 98 00 00 04 00 00 00 38 A1 98 00 00 04 00 00 00 2A A2 98 00 00 04 00 00 00 2B A2 98 00 00 04 00 00 00 2C A2 98 00 00 04 00 00 00 2D A2 98 00 00 04 00 00 00 2E A2 98 00 00 04 00 00 00 30 A2 98 00 00 04 00 00 00 31 A2 98 00 00 04 00 00 00 32 A2 98 00 00 04 00 00 00 33 A2 98 00 00 04 00 00 00 34 A2 98 00 00 04 00 00 00 3C A2 98 00 00 04 00 00 00 3D A2 98 00 00 04 00 00 00 3E A2 98 00 00 04 00 00 00 3F A2 98 00 00 04 00 00 00 40 A2 98 00 00 04 00 00 00 41 A2 98 00 00 04 00 00 00 42 A2 98 00 00 04 00 00 00 43 A2 98 00 00 04 00 00 00 44 A2 98 00 00 04 00 00 00 45 A2 98 00 00 04 00 00 00 46 A2 98 00 00 04 00 00 00 47 A2 98 00 00 04 00 00 00 48 A2 98 00 FF FF 07 00 89 AA 4F 00 01 00 62 DC 05 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 03 00 00 00 00 00 00 00 00 49 A2 98 00 FF FF 07 00 81 C0 4C 00 01 00 63 E8 03 00 00 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 4A A2 98 00 FF FF 07 00 E7 B3 52 00 01 00 5A 00 00 00 00 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 4B A2 98 00 FF FF 07 00 50 36 56 00 01 00 61 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 4C A2 98 00 FF FF 07 00 A0 A6 4F 00 01 00 63 00 00 00 00 00 2D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 FF 00 00 00 00 00 00 00 00 64 30 31 01 FF FF 07 00 88 4E 0F 00 01 00 62 AC 0D 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 43 B4 32 01 FF FF 07 00 F0 71 0F 00 01 00 61 DC 05 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 31 C3 35 01 FF FF 07 00 92 0E 10 00 01 00 61 A0 0F 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 32 C3 35 01 FF FF 07 00 84 0E 10 00 01 00 62 94 11 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 FD 55 3A 01 FF FF 07 00 51 5D 10 00 01 00 62 D0 07 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 95 F6 41 01 FF FF 07 00 AD D1 10 00 01 00 62 70 17 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 00 9A FE FD 02 FF FF 07 00 A0 A6 4F 00 32 00 62 90 E2 00 00 00 2D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 00 00 00 00 00 00 00 84 87 93 03 FF FF 07 00 6D 4B 4C 00 01 00 62 40 1F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 02 00 1E 00 1E 00 1E 00 00 09 95 96 03 FF FF 07 00 D4 F4 4F 00 01 00 61 DC 05 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 0A 95 96 03 FF FF 07 00 D4 F4 4F 00 0B 00 61 98 3A 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 FF 00 00 00 00 00 00 00 00 09 1E 2C 04 FF FF 07 00 91 E2 8A 00 01 00 62 40 1F 00 00 00 5A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00 00 00 02 35 A2 98 00 36 A2 98 00 84 B5 C4 04 FF FF 07 00 E0 ED 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 85 B5 C4 04 FF FF 07 00 E1 ED 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 86 B5 C4 04 FF FF 07 00 6B EE 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 87 B5 C4 04 FF FF 07 00 7F EE 2D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00 00 00 00 06 00 00 00 31 00 32 00 33 00 00 00 00 00 00 00 05 00 11 00 36 00 08 06 A0 01 15 00 C0 E3 0F 05 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 00 16 00 39 01 0C 06 06 00 00 00 31 00 32 00 33 00 00 00 00 00 00 00 03 00 19 00 3E 00 0C 06 90 01 15 00 90 62 10 05 33 00 00 00 00 00 00 00 03 00 1C 00 03 01 0C 06 06 00 00 00 31 00 32 00 01 00 00 00 00 00 00 00 C3 FD FD 02 01 00 00 00 00 00 00 00 1D 99 98 00 01 00 00 00 00 00 00 00 2E 4A CB 01 01 00 00 00 00 00 00 00 0D FE FD 02 01 00 00 00 00 00 00 00 C1 FD FD 02 01 00 00 00 01 00 00 00 C3 FD FD 02 01 00 00 00 01 00 00 00 1D 99 98 00 01 00 00 00 01 00 00 00 2E 4A CB 01 01 00 00 00 01 00 00 00 0D FE FD 02 01 00 00 00 01 00 00 00 C1 FD FD 02 02 00 00 00 00 00 00 00 C3 FD FD 02 02 00 00 00 00 00 00 00 1D 99 98 00 02 00 00 00 00 00 00 00 2E 4A CB 01 02 00 00 00 00 00 00 00 0D FE FD 02 02 00 00 00 00 00 00 00 C1 FD FD 02 02 00 00 00 01 00 00 00 C3 FD FD 02 02 00 00 00 01 00 00 00 1D 99 98 00 02 00 00 00 01 00 00 00 2E 4A CB 01 02 00 00 00 01 00 00 00 0D FE FD 02 02 00 00 00 01 00 00 00 C1 FD FD 02 03 00 00 00 00 00 00 00 C3 FD FD 02 03 00 00 00 00 00 00 00 1D 99 98 00 03 00 00 00 00 00 00 00 2E 4A CB 01 03 00 00 00 00 00 00 00 0D FE FD 02 03 00 00 00 00 00 00 00 C1 FD FD 02 03 00 00 00 01 00 00 00 C3 FD FD 02 03 00 00 00 01 00 00 00 1D 99 98 00 03 00 00 00 01 00 00 00 2E 4A CB 01 03 00 00 00 01 00 00 00 0D FE FD 02 03 00 00 00 01 00 00 00 C1 FD FD 02 04 00 00 00 00 00 00 00 C3 FD FD 02 04 00 00 00 00 00 00 00 1D 99 98 00 04 00 00 00 00 00 00 00 2E 4A CB 01 04 00 00 00 00 00 00 00 0D FE FD 02 04 00 00 00 00 00 00 00 C1 FD FD 02 04 00 00 00 01 00 00 00 C3 FD FD 02 04 00 00 00 01 00 00 00 1D 99 98 00 04 00 00 00 01 00 00 00 2E 4A CB 01 04 00 00 00 01 00 00 00 0D FE FD 02 04 00 00 00 01 00 00 00 C1 FD FD 02 05 00 00 00 00 00 00 00 C3 FD FD 02 05 00 00 00 00 00 00 00 1D 99 98 00 05 00 00 00 00 00 00 00 2E 4A CB 01 05 00 00 00 00 00 00 00 0D FE FD 02 05 00 00 00 00 00 00 00 C1 FD FD 02 05 00 00 00 01 00 00 00 C3 FD FD 02 05 00 00 00 01 00 00 00 1D 99 98 00 05 00 00 00 01 00 00 00 2E 4A CB 01 05 00 00 00 01 00 00 00 0D FE FD 02 05 00 00 00 01 00 00 00 C1 FD FD 02 06 00 00 00 00 00 00 00 C3 FD FD 02 06 00 00 00 00 00 00 00 1D 99 98 00 06 00 00 00 00 00 00 00 2E 4A CB 01 06 00 00 00 00 00 00 00 0D FE FD 02 06 00 00 00 00 00 00 00 C1 FD FD 02 06 00 00 00 01 00 00 00 C3 FD FD 02 06 00 00 00 01 00 00 00 1D 99 98 00 06 00 00 00 01 00 00 00 2E 4A CB 01 06 00 00 00 01 00 00 00 0D FE FD 02 06 00 00 00 01 00 00 00 C1 FD FD 02 07 00 00 00 00 00 00 00 C3 FD FD 02 07 00 00 00 00 00 00 00 1D 99 98 00 07 00 00 00 00 00 00 00 2E 4A CB 01 07 00 00 00 00 00 00 00 0D FE FD 02 07 00 00 00 00 00 00 00 C1 FD FD 02 07 00 00 00 01 00 00 00 C3 FD FD 02 07 00 00 00 01 00 00 00 1D 99 98 00 07 00 00 00 01 00 00 00 2E 4A CB 01 07 00 00 00 01 00 00 00 0D FE FD 02 07 00 00 00 01 00 00 00 C1 FD FD 02 08 00 00 00 00 00 00 00 C3 FD FD 02 08 00 00 00 00 00 00 00 1D 99 98 00 08 00 00 00 00 00 00 00 2E 4A CB 01 08 00 00 00 00 00 00 00 0D FE FD 02 08 00 00 00 00 00 00 00 C1 FD FD 02 08 00 00 00 01 00 00 00 C3 FD FD 02 08 00 00 00 01 00 00 00 1D 99 98 00 08 00 00 00 01 00 00 00 2E 4A CB 01 08 00 00 00 01 00 00 00 0D FE FD 02 08 00 00 00 01 00 00 00 C1 FD FD 02 00 00 01 00 A2 35 4D 00 CE FD FD 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 FF FF FF FF FF FF FF FF 06 00 00 00 1F 1C 32 01 A7 3F 32 01 FF FF FF FF FF FF FF FF 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 00"));

		return mplew.getPacket();
	
	}

	public static MaplePacket useCharm(byte charmsleft, byte daysleft) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		mplew.write(6);
		mplew.write(1);
		mplew.write(charmsleft);
		mplew.write(daysleft);

		return mplew.getPacket();
	}

	public static MaplePacket itemExpired(int itemid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
		mplew.write(2);
		mplew.writeInt(itemid);

		return mplew.getPacket();
	}

	public static MaplePacket ViciousHammer(boolean start, int hammered) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
		if (start) {
			mplew.write(49);
			mplew.writeInt(0);
			mplew.writeInt(hammered);
		} else {
			mplew.write(53);
			mplew.writeInt(0);
		}

		return mplew.getPacket();
	}

	public static MaplePacket changePetName(MapleCharacter chr, String newname, int slot) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());

		mplew.writeInt(chr.getId());
		mplew.write(0);
		mplew.writeMapleAsciiString(newname);
		mplew.write(0);

		return mplew.getPacket();
	}

	public static MaplePacket showNotes(ResultSet notes, int count) throws SQLException {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
		mplew.write(3);
		mplew.write(count);
		for (int i = 0; i < count; i++) {
			mplew.writeInt(notes.getInt("id"));
			mplew.writeMapleAsciiString(notes.getString("from"));
			mplew.writeMapleAsciiString(notes.getString("message"));
			mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
			mplew.write(0);
			notes.next();
		}

		return mplew.getPacket();
	}

	public static MaplePacket useChalkboard(final int charid, final String msg) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());

		mplew.writeInt(charid);
		if (msg == null) {
			mplew.write(0);
		} else {
			mplew.write(1);
			mplew.writeMapleAsciiString(msg);
		}

		return mplew.getPacket();
	}

	public static MaplePacket getTrockRefresh(MapleCharacter chr, boolean vip, boolean delete) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
		mplew.write(delete ? 2 : 3);
		mplew.write(vip ? 1 : 0);
		int[] map = chr.getRocks();
		for (int i = 0; i < 10; i++) {
			mplew.writeInt(map[i]);
		}
		return mplew.getPacket();
	}

	public static MaplePacket sendWishList(MapleCharacter chr, boolean update) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(update ? 0x58 : 0x52);
		int[] list = chr.getWishlist();
		for (int i = 0; i < 10; i++) {
			mplew.writeInt(list[i] != -1 ? list[i] : 0);
		}
		return mplew.getPacket();
	}

	public static MaplePacket showNXMapleTokens(MapleCharacter chr) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
		mplew.writeInt(chr.getCSPoints(1)); // A-cash
		mplew.writeInt(chr.getCSPoints(2)); // MPoint

		return mplew.getPacket();
	}

	public static MaplePacket showBoughtCSItem(int itemid , int accountId) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(0x5A);
		mplew.writeLong(137235);//HexTool.getByteArrayFromHexString("13 18 02 00 00 00 00 00")); // uniq id
		mplew.writeLong(accountId);
		mplew.writeInt(itemid);
		mplew.write(HexTool.getByteArrayFromHexString("03 D1 CC 01")); // probably SN
		mplew.writeShort(1); // quantity
		mplew.writeZeroBytes(14);
		PacketHelper.addExpirationTime(mplew, itemid);
		mplew.writeLong(0);

		return mplew.getPacket();
	}

	public static MaplePacket showBoughtCSQuestItem(short position, int itemid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(0x92); // MapleSEA v1.01
		mplew.write(HexTool.getByteArrayFromHexString("01 00 00 00 01 00")); // probably ID and something else
		mplew.writeShort(position);
		mplew.writeInt(itemid);

		return mplew.getPacket();
	}

	public static MaplePacket transferFromCSToInv(IItem item, int position) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(0x6D);
		mplew.write(position);//in csinventory
		PacketHelper.addItemInfo(mplew, item, true, false, true);

		return mplew.getPacket();
	}

	public static MaplePacket transferFromInvToCS(IItem item, int accountId) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(0x4E);
		//addCashItemInformation(mplew, item, accountId);

		return mplew.getPacket();
	}

	public static MaplePacket enableUse0() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.write(0x0B);
		mplew.write(HexTool.getByteArrayFromHexString("01 00 00 00 00"));

		return mplew.getPacket();
	}

	public static MaplePacket enableUse1() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("4E 00 00 04 00 07 00 00 00 04 00"));

		return mplew.getPacket();
	}

	public static MaplePacket enableUse2() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(0x50);
		mplew.writeShort(0);

		return mplew.getPacket();
	}

	public static MaplePacket enableUse3() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
		mplew.write(0x52);
		mplew.writeZeroBytes(40);

		return mplew.getPacket();
	}
}