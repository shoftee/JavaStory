/**
 * 
 */
package javastory.channel.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Lists;

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

	private final FootholdTree footholds;
	private final List<Rectangle> areas;

	// TODO: Load these in constructor
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

		this.footholds = loadFootholds(data);
		
		final WzData areaData = data.getChildByPath("area");
		if (areaData != null) {
			this.areas = loadAreas(data);
		} else {
			this.areas = Lists.newArrayList();
		}
		
		
	}

	private FootholdTree loadFootholds(WzData data) {
		final List<Foothold> footholds = Lists.newLinkedList();
		final Point lBound = new Point();
		final Point uBound = new Point();
		for (final WzData footholdData : data.getChildByPath("foothold")) {
			for (final WzData category : footholdData) {
				for (final WzData entry : category) {
					final Foothold foothold = new Foothold(entry);

					if (foothold.getX1() < lBound.x) {
						lBound.x = foothold.getX1();
					}
					if (foothold.getX2() > uBound.x) {
						uBound.x = foothold.getX2();
					}
					if (foothold.getY1() < lBound.y) {
						lBound.y = foothold.getY1();
					}
					if (foothold.getY2() > uBound.y) {
						uBound.y = foothold.getY2();
					}

					footholds.add(foothold);
				}
			}
		}

		final FootholdTree footholdTree = new FootholdTree(lBound, uBound);
		for (final Foothold item : footholds) {
			footholdTree.insert(item);
		}
		return footholdTree;
	}

	private ArrayList<Rectangle> loadAreas(WzData data) {
		ArrayList<Rectangle> areas = Lists.newArrayList();

		for (final WzData area : data) {
			final int x1 = WzDataTool.getInt("x1", area);
			final int y1 = WzDataTool.getInt("y1", area);
			final int x2 = WzDataTool.getInt("x2", area);
			final int y2 = WzDataTool.getInt("y2", area);
			final Rectangle rectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1);
			areas.add(rectangle);
		}

		return areas;
	}
}
