package javastory.tools.packets;

import javastory.io.GamePacket;
import javastory.io.PacketBuilder;

public class TestPacket {
	public static final GamePacket EXPTest1() {
		final PacketBuilder builder = new PacketBuilder();

		return builder.getPacket();
	}

	public static final GamePacket EXPTest2() {
		final PacketBuilder builder = new PacketBuilder();

		return builder.getPacket();
	}
}