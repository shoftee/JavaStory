/**
 * 
 */
package javastory.channel.maps;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javastory.channel.life.Spawns;
import javastory.channel.server.Portal;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

/**
 * @author shoftee
 * 
 */
public final class GameMapInfo {

	private static final WzDataProvider mapDataProvider = WzDataProviderFactory.getDataProvider("Map.wz");

	private final int mapId;
	private final int returnMapId;
	private final int forcedReturnMapId;

	private final float monsterRate;
	private final float recoveryRate;

	private final int decHP, createMobInterval;
	private final int protectItem;

	private final int timeLimit;
	private final int fieldLimit;
//	private final int maxRegularSpawn;

	private final boolean isTown, hasClock, allowsPersonalShop, isEverlast;
//	private final boolean isNoDrops;

	private final String onUserEnterScript, onFirstUserEnterScript;

	// TODO: Load these in constructor
//	private final FootholdTree footholds;
//	private final List<Rectangle> areas;
//	private final Map<Integer, Portal> portals;
//	private final List<Spawns> monsterSpawn;

	public GameMapInfo(final int mapId, final WzData data) {
		this.mapId = mapId;
		this.returnMapId = WzDataTool.getInt("info/returnMap", data);
		this.forcedReturnMapId = WzDataTool.getInt("info/forcedReturn", data, 999_999_999);

		this.monsterRate = WzDataTool.getFloat("info/mobRate", data, 0.0f);
		this.recoveryRate = WzDataTool.getFloat("info/recovery", data, 1.0f);
		this.decHP = WzDataTool.getInt("info/decHP", data, 0);
		this.protectItem = WzDataTool.getInt("info/protectItem", data, 0);

		this.timeLimit = WzDataTool.getInt("info/timeLimit", data, -1);
		this.fieldLimit = WzDataTool.getInt("info/fieldLimit", data, 0);
		
		this.createMobInterval = (short) WzDataTool.getInt("info/createMobInterval", data, 9000);
		
		this.hasClock = data.hasChildAtPath("clock");
		this.isTown = data.hasChildAtPath("info/town");
		this.isEverlast = data.hasChildAtPath("info/everlast");
		this.allowsPersonalShop = data.hasChildAtPath("info/personalShop");
		
		this.onFirstUserEnterScript = WzDataTool.getString("info/onFirstUserEnter", data, "");
		this.onUserEnterScript = WzDataTool.getString("info/onUserEnter", data, "");

	}
}
