package javastory.game.data;

import java.util.concurrent.TimeUnit;

import javastory.wz.WzData;
import javastory.wz.WzDataProviderFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

public final class MapNameInfoProvider {

	private final class MapNameLoader extends CacheLoader<Integer, MapNameInfo> {
		@Override
		public MapNameInfo load(final Integer mapId) throws Exception {
			final String path = MapNameInfoProvider.this.getMapAreaDataPath(mapId.intValue());
			final WzData data = nameData.getChildByPath(path);

			if (data == null) {
				return null;
			}

			final MapNameInfo info = new MapNameInfo(data);
			return info;
		}
	}

	private static final WzData nameData = WzDataProviderFactory.getDataProvider("String.wz").getData("Map.img");

	private static final MapNameInfoProvider instance = new MapNameInfoProvider();

	public final Cache<Integer, MapNameInfo> cache;

	private MapNameInfoProvider() {
		this.cache = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.build(new MapNameLoader());
	}

	public static MapNameInfoProvider getInstance() {
		return instance;
	}

	private String getMapAreaDataPath(final int mapId) {
		final StringBuilder builder = new StringBuilder();
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
