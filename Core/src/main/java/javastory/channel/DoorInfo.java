/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.channel;

import java.awt.Point;

/**
 * 
 * @author shoftee
 */
public class DoorInfo {
	private int townId;
	private int targetId;
	private Point position;
	public static final DoorInfo NONE = new DoorInfo();

	private DoorInfo() {
		this.townId = 999999999;
		this.targetId = 999999999;
		this.position = new Point(0, 0);
	}

	public DoorInfo(int townId, int targetId, Point position) {
		this.townId = townId;
		this.targetId = targetId;
		this.position = position;
	}

	public int getTownId() {
		return townId;
	}

	public int getTargetId() {
		return targetId;
	}

	public Point getPosition() {
		return position;
	}

}
