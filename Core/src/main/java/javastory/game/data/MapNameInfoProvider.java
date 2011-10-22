package javastory.game.data;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javastory.wz.WzData;
import javastory.wz.WzDataProviderFactory;

import com.google.common.collect.MapMaker;

public final class MapNameInfoProvider {

	private static final WzData nameData = WzDataProviderFactory.getDataProvider("String.wz").getData("Map.img");

	private static final MapNameInfoProvider instance = new MapNameInfoProvider();

	public final Map<Integer, MapNameInfo> cache;

	private MapNameInfoProvider() {
		this.cache = new MapMaker().expireAfterAccess(10, TimeUnit.MINUTES).makeMap();
	}

	public static MapNameInfoProvider getInstance() {
		return instance;
	}

	public MapNameInfo getInfo(int mapId) {
		final String path = getMapAreaDataPath(mapId);
		final WzData data = nameData.getChildByPath(path);
		
		if (data == null) {
			return null;
		}
		
		MapNameInfo info = new MapNameInfo(data);
		
		this.cache.put(Integer.valueOf(mapId), info);
		return info;
	}

	private String getMapAreaDataPath(int mapId) {
		StringBuilder builder = new StringBuilder();
		if (mapId < 100_000_000) {
			builder.append("maple");
		} else if (mapId >= 100_000_000 && mapId < 200_000_000) {
			builder.append("victoria");
		} else if (mapId >= 200_000_000 && mapId < 300_000_000) {
			builder.append("ossyria");
		} else if (mapId >= 540_000_000 && mapId < 541_010_110) {
			builder.append("singapore");
		} else if (mapId >= 600_000_000 && mapId < 620_000_000) {
			builder.append("MasteriaGL");
		} else if (mapId >= 670_000_000 && mapId < 682_000_000) {
			builder.append("weddingGL");
		} else if (mapId >= 682_000_000 && mapId < 683_000_000) {
			builder.append("HalloweenGL");
		} else if (mapId >= 800_000_000 && mapId < 900_000_000) {
			builder.append("jp");
		} else {
			builder.append("etc");
		}
		builder.append("/");
		builder.append(mapId);
		return builder.toString();
	}
}
