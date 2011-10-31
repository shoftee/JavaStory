package javastory.channel.server;

import java.awt.Point;

import javastory.channel.maps.GameMapPortal;
import javastory.channel.maps.GenericPortal;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public class PortalFactory {

	private int nextDoorPortal = 0x80;

	public Portal makePortal(final int type, final WzData portal) {
		GenericPortal ret = null;
		if (type == Portal.MAP_PORTAL) {
			ret = new GameMapPortal();
		} else {
			ret = new GenericPortal(type);
		}
		this.loadPortal(ret, portal);
		return ret;
	}

	private void loadPortal(final GenericPortal myPortal, final WzData portal) {
		myPortal.setName(WzDataTool.getString(portal.getChildByPath("pn")));
		myPortal.setTarget(WzDataTool.getString(portal.getChildByPath("tn")));
		myPortal.setTargetMapId(WzDataTool.getInt(portal.getChildByPath("tm")));
		myPortal.setPosition(new Point(WzDataTool.getInt(portal.getChildByPath("x")), WzDataTool.getInt(portal.getChildByPath("y"))));
		String script = WzDataTool.getString("script", portal, null);
		if (script != null && script.equals("")) {
			script = null;
		}
		myPortal.setScriptName(script);

		if (myPortal.getType() == Portal.DOOR_PORTAL) {
			myPortal.setId(this.nextDoorPortal);
			this.nextDoorPortal++;
		} else {
			myPortal.setId(Integer.parseInt(portal.getName()));
		}
	}
}
