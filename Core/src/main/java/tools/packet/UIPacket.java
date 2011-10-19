package tools.packet;

import handling.GamePacket;
import handling.ServerPacketOpcode;
import javastory.io.PacketBuilder;

public class UIPacket {

	public static final GamePacket EarnTitleMsg(final String msg) {
		final PacketBuilder builder = new PacketBuilder();

		//"You have acquired the Pig's Weakness skill."
		builder.writeAsShort(ServerPacketOpcode.EARN_TITLE_MSG.getValue());
		builder.writeLengthPrefixedString(msg);

		return builder.getPacket();
	}

	public static GamePacket getStatusMsg(int itemid) {
		PacketBuilder builder = new PacketBuilder();

		// Temporary transformed as a dragon, even with the skill ......
		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(7);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static GamePacket getSPMsg(byte sp) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(4);
		builder.writeAsShort(0);
		builder.writeByte(sp);

		return builder.getPacket();
	}

	public static GamePacket getGPMsg(int itemid) {
		PacketBuilder builder = new PacketBuilder();

		// Temporary transformed as a dragon, even with the skill ......
		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(7);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static final GamePacket MapNameDisplay(final int mapid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
		builder.writeAsByte(0x3);
		builder.writeLengthPrefixedString("maplemap/enter/" + mapid);

		return builder.getPacket();
	}

	public static final GamePacket Aran_Start() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
		builder.writeAsByte(0x4);
		builder.writeLengthPrefixedString("Aran/balloon");

		return builder.getPacket();
	}

	public static final GamePacket AranTutInstructionalBalloon(final String data) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(0x17);
		builder.writeLengthPrefixedString(data);
		builder.writeInt(1);

		return builder.getPacket();
	}

	public static final GamePacket EvanTutInstructionalBalloon(final String data) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
		builder.writeAsByte(3);
		builder.writeLengthPrefixedString(data);

		return builder.getPacket();
	}

	public static final GamePacket EvanDragonEyes() {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(1);
		builder.writeInt(87548);// FC 55 01 00
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static final GamePacket ShowWZEffect(final String data) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		builder.writeAsByte(0x14);
		builder.writeLengthPrefixedString(data);

		return builder.getPacket();
	}

	public static GamePacket summonHelper(boolean summon) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SUMMON_HINT.getValue());
		builder.writeAsByte(summon ? 1 : 0);

		return builder.getPacket();
	}

	public static GamePacket summonMessage(int type) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SUMMON_HINT_MSG.getValue());
		builder.writeAsByte(1);
		builder.writeInt(type);
		builder.writeInt(7000); // probably the delay

		return builder.getPacket();
	}

	public static GamePacket summonMessage(String message) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SUMMON_HINT_MSG.getValue());
		builder.writeAsByte(0);
		builder.writeLengthPrefixedString(message);
		builder.writeInt(200);
		builder.writeInt(4000);

		return builder.getPacket();
	}

	public static GamePacket IntroLock(boolean enable) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CYGNUS_INTRO_LOCK.getValue());
		builder.writeAsByte(enable ? 1 : 0);

		return builder.getPacket();
	}

	public static GamePacket IntroDisableUI(boolean enable) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.CYGNUS_INTRO_DISABLE_UI.getValue());
		builder.writeAsByte(enable ? 1 : 0);

		return builder.getPacket();
	}

	public static GamePacket fishingUpdate(byte type, int id) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FISHING_BOARD_UPDATE.getValue());
		builder.writeByte(type);
		builder.writeInt(id);

		return builder.getPacket();
	}

	public static GamePacket fishingCaught(int chrid) {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FISHING_CAUGHT.getValue());
		builder.writeInt(chrid);

		return builder.getPacket();
	}
}