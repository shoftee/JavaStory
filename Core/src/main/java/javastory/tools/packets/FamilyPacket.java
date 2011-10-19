package javastory.tools.packets;

import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerPacketOpcode;

public class FamilyPacket {
	public static GamePacket getFamilyData() {
		PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.FAMILY.getValue());
		builder.writeInt(11); // Number of events
		builder.writeAsByte(0);
		builder.writeInt(300); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("Transfer to Family Member");
		builder.writeLengthPrefixedString("[Target] Myself\n[Effect] Will be transfered directly to the Map where the family member is located in.");
		builder.writeAsByte(1);
		builder.writeInt(500); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("Summon family member");
		builder.writeLengthPrefixedString("[Target] 1 Family member\n[Effect] Summons one of the family member to the map you are located in.");
		builder.writeAsByte(2);
		builder.writeInt(700); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("1.5 X Drop Rate for Me(15min)");
		builder.writeLengthPrefixedString("[Target] Myself\n[Duration] 15 min\n[Effect]  Drop rate will be #cincreased by 50%#.\nThe effect will be disregarded if overlapped with other drop rate event.");
		builder.writeAsByte(3);
		builder.writeInt(800); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("1.5 X EXP for me(15min)");
		builder.writeLengthPrefixedString("[Target] Myself\n[Duration] 15min\n[Effect] EXP gained from monsters  will be #cincreased by 50%.#\nThe effect will be disregarded if overlapped with other EXP event.");
		builder.writeAsByte(4);
		builder.writeInt(1000); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("Unity of Family(30min)");
		builder.writeLengthPrefixedString("[Condition] 6 juniors online from pedigree\n[Duration] 30min\n[Effect] Drop Rate and EXP gained will be #cincreased by 100%#.\nThe effect will be disregarded if overlapped with other Drop Rate and EXP event.");
		builder.writeAsByte(2);
		builder.writeInt(1200); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("2 X Drop Rate for Me(15min)");
		builder.writeLengthPrefixedString("[Target] Myself\n[Duration] 15min\n[Effect]  Drop rate will be #cincreased by 100%.# \nThe effect will be disregarded if overlapped with other Drop Rate event.");
		builder.writeAsByte(3);
		builder.writeInt(1500); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("2 X EXP event for Me(15min)");
		builder.writeLengthPrefixedString("[Target] Myself\n[Duration] 15min\n[Effect] EXP gained from monsters  will be #cincreased by 100%.#\nThe effect will be disregarded if overlapped with other EXP event.");
		builder.writeAsByte(2);
		builder.writeInt(2000); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("2 X Drop Rate for Me(30min)");
		builder.writeLengthPrefixedString("[Target] Myself\n[Duration] 30min\n[Effect]  drop rate will be #cincreased by 100%.# \nThe effect will be disregarded if overlapped with other Drop Rate event");
		builder.writeAsByte(3);
		builder.writeInt(2500); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("2 X EXP event for Me(30min)");
		builder.writeLengthPrefixedString("[Target] Myself\n[Duration] 30min\n[Effect] EXP gained from monsters  will be #cincreased by 100%.#\nThe effect will be disregarded if overlapped with other EXP event.");
		builder.writeAsByte(2);
		builder.writeInt(4000); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("2 X Drop Rate for Party(30min)");
		builder.writeLengthPrefixedString("[Target] My Party\n[Duration] 30min\n[Effect]  drop rate will be #cincreased by 100%.# \nThe effect will be disregarded if overlapped with other Drop Rate event.");
		builder.writeAsByte(3);
		builder.writeInt(5000); // REP needed
		builder.writeInt(1); // Number of times allowed per day
		builder.writeLengthPrefixedString("2 X EXP event for Party(30min)");
		builder.writeLengthPrefixedString("[Target] My Party\n[Duration] 30min\n[Effect] EXP gained from monsters  will be #cincreased by 100%.#\nThe effect will be disregarded if overlapped with other EXP event.");

		return builder.getPacket();
	}
}