package javastory.channel;

import java.awt.Point;
import java.io.Serializable;

/**
 * 
 * @author shoftee
 */
public class DoorInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8520828348027890204L;

	public static final DoorInfo NONE = new DoorInfo();

	public final int TownId;
	public final int TargetId;
	public final Point Position;

	private DoorInfo() {
		this.TownId = 999999999;
		this.TargetId = 999999999;
		this.Position = new Point(0, 0);
	}

	public DoorInfo(final int townId, final int targetId, final Point position) {
		this.TownId = townId;
		this.TargetId = targetId;
		this.Position = position;
	}
}
