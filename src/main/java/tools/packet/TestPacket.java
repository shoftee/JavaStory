package tools.packet;

import handling.MaplePacket;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

public class TestPacket {
	public static final MaplePacket EXPTest1() {
		final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		return mplew.getPacket();
	}

	public static final MaplePacket EXPTest2() {
		final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		return mplew.getPacket();
	}
}