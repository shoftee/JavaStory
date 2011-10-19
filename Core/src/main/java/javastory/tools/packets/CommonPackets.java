package javastory.tools.packets;

import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerConstants;
import javastory.server.handling.ServerPacketOpcode;

public final class CommonPackets {

	private CommonPackets() {
	}

	public static GamePacket getServerIP(final int port, final int clientId) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SERVER_IP.getValue());
		builder.writeAsShort(0);
		builder.writeBytes(ServerConstants.Server_IP);
		builder.writeAsShort(port);
		builder.writeInt(clientId);
		builder.writeZeroBytes(5);

		return builder.getPacket();
	}

}
