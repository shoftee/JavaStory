package javastory.game.data;

import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public final class MapNameInfo {

	public final String StreetName;
	public final String MapName;

	public MapNameInfo(final WzData data) {
		this.MapName = WzDataTool.getString("mapName", data);
		this.StreetName = WzDataTool.getString("streetName", data);
	}

}
