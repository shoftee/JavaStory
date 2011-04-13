package tools.packet;

import handling.MaplePacket;
import handling.ServerPacketOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class FamilyPacket {
	public static MaplePacket getFamilyData() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

		mplew.writeShort(ServerPacketOpcode.FAMILY.getValue());
		mplew.writeInt(11); // Number of events
		mplew.write(0);
		mplew.writeInt(300); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("Transfer to Family Member");
		mplew.writeMapleAsciiString("[Target] Myself\n[Effect] Will be transfered directly to the Map where the family member is located in.");
		mplew.write(1);
		mplew.writeInt(500); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("Summon family member");
		mplew.writeMapleAsciiString("[Target] 1 Family member\n[Effect] Summons one of the family member to the map you are located in.");
		mplew.write(2);
		mplew.writeInt(700); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("1.5 X Drop Rate for Me(15min)");
		mplew.writeMapleAsciiString("[Target] Myself\n[Duration] 15 min\n[Effect]  Drop rate will be #cincreased by 50%#.\nThe effect will be disregarded if overlapped with other drop rate event.");
		mplew.write(3);
		mplew.writeInt(800); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("1.5 X EXP for me(15min)");
		mplew.writeMapleAsciiString("[Target] Myself\n[Duration] 15min\n[Effect] EXP gained from monsters  will be #cincreased by 50%.#\nThe effect will be disregarded if overlapped with other EXP event.");
		mplew.write(4);
		mplew.writeInt(1000); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("Unity of Family(30min)");
		mplew.writeMapleAsciiString("[Condition] 6 juniors online from pedigree\n[Duration] 30min\n[Effect] Drop Rate and EXP gained will be #cincreased by 100%#.\nThe effect will be disregarded if overlapped with other Drop Rate and EXP event.");
		mplew.write(2);
		mplew.writeInt(1200); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("2 X Drop Rate for Me(15min)");
		mplew.writeMapleAsciiString("[Target] Myself\n[Duration] 15min\n[Effect]  Drop rate will be #cincreased by 100%.# \nThe effect will be disregarded if overlapped with other Drop Rate event.");
		mplew.write(3);
		mplew.writeInt(1500); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("2 X EXP event for Me(15min)");
		mplew.writeMapleAsciiString("[Target] Myself\n[Duration] 15min\n[Effect] EXP gained from monsters  will be #cincreased by 100%.#\nThe effect will be disregarded if overlapped with other EXP event.");
		mplew.write(2);
		mplew.writeInt(2000); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("2 X Drop Rate for Me(30min)");
		mplew.writeMapleAsciiString("[Target] Myself\n[Duration] 30min\n[Effect]  drop rate will be #cincreased by 100%.# \nThe effect will be disregarded if overlapped with other Drop Rate event");
		mplew.write(3);
		mplew.writeInt(2500); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("2 X EXP event for Me(30min)");
		mplew.writeMapleAsciiString("[Target] Myself\n[Duration] 30min\n[Effect] EXP gained from monsters  will be #cincreased by 100%.#\nThe effect will be disregarded if overlapped with other EXP event.");
		mplew.write(2);
		mplew.writeInt(4000); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("2 X Drop Rate for Party(30min)");
		mplew.writeMapleAsciiString("[Target] My Party\n[Duration] 30min\n[Effect]  drop rate will be #cincreased by 100%.# \nThe effect will be disregarded if overlapped with other Drop Rate event.");
		mplew.write(3);
		mplew.writeInt(5000); // REP needed
		mplew.writeInt(1); // Number of times allowed per day
		mplew.writeMapleAsciiString("2 X EXP event for Party(30min)");
		mplew.writeMapleAsciiString("[Target] My Party\n[Duration] 30min\n[Effect] EXP gained from monsters  will be #cincreased by 100%.#\nThe effect will be disregarded if overlapped with other EXP event.");

		return mplew.getPacket();
	}
}