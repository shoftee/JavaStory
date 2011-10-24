package javastory.channel.server;

import java.awt.Point;

import javastory.channel.ChannelClient;

public interface Portal {
	public static final int MAP_PORTAL = 2;
	public static final int DOOR_PORTAL = 6;

	int getType();

	int getId();

	Point getPosition();

	String getName();

	String getTarget();

	String getScriptName();

	void setScriptName(String newName);

	int getTargetMapId();

	void enterPortal(ChannelClient c);
}
