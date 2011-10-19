package javastory.client;

import javastory.io.GamePacket;

/**
 * 
 * @author Tosho
 */
public interface PlayerClient {
	public void write(GamePacket packet);

	public int getId();

	public void disconnect();

	public void disconnect(boolean immediately);
}
