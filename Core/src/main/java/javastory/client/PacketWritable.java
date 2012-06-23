package javastory.client;

import javastory.io.PacketBuilder;

/**
 * 
 * @author shoftee
 */
public interface PacketWritable {
	public void writeTo(PacketBuilder builder);
}
