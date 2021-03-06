package javastory.channel.maps;

import java.awt.Point;

import javastory.game.data.ReactorInfo;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public class MapReactorInfo {
	private final ReactorInfo prototype;
	private final byte facingDirection;
	private final Point position;
	private final int delay;
	private final String name;

	public MapReactorInfo(ReactorInfo prototype, WzData data) {
		this.prototype = prototype;
		this.facingDirection = (byte) WzDataTool.getInt("f", data, 0);
		
		final int x = WzDataTool.getInt("x", data);
		final int y = WzDataTool.getInt("y", data);
		this.position = new Point(x, y);

		this.delay = WzDataTool.getInt("reactorTime", data);
		this.name = WzDataTool.getString("name", data, "");
	}
	
	public final ReactorInfo getPrototype() {
		return prototype;
	}
	
	public final int getType(final int stateId) {
		return prototype.getType(stateId);
	}
	
	public final int getNextState(final int stateId) {
		return prototype.getNextState(stateId);
	}

	public final byte getFacingDirection() {
		return facingDirection;
	}

	public final Point getPosition() {
		return position;
	}

	public final int getDelay() {
		return delay;
	}

	public final String getName() {
		return name;
	}
}
