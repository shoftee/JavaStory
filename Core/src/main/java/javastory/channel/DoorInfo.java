package javastory.channel;

import java.awt.Point;

/**
 * 
 * @author shoftee
 */
public class DoorInfo {
	private final int townId;
	private final int targetId;
	private final Point position;
	public static final DoorInfo NONE = new DoorInfo();

	private DoorInfo() {
		this.townId = 999999999;
		this.targetId = 999999999;
		this.position = new Point(0, 0);
	}

	public DoorInfo(final int townId, final int targetId, final Point position) {
		this.townId = townId;
		this.targetId = targetId;
		this.position = position;
	}

	public int getTownId() {
		return this.townId;
	}

	public int getTargetId() {
		return this.targetId;
	}

	public Point getPosition() {
		return this.position;
	}

}
