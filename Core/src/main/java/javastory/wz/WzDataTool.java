/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.wz;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class WzDataTool {

	public static String getString(final WzData data) {
		return (String) data.getData();
	}

	public static String getString(final WzData data, final String def) {
		if (data == null || data.getData() == null) {
			return def;
		} else {
			return (String) data.getData();
		}
	}

	public static String getString(final String path, final WzData data) {
		return getString(data.getChildByPath(path));
	}

	public static String getString(final String path, final WzData data, final String def) {
		return getString(data.getChildByPath(path), def);
	}

	public static double getDouble(final WzData data) {
		return ((Double) data.getData()).doubleValue();
	}

	public static float getFloat(final WzData data) {
		return ((Float) data.getData()).floatValue();
	}

	public static float getFloat(final WzData data, final float def) {
		if (data == null || data.getData() == null) {
			return def;
		} else {
			return ((Float) data.getData()).floatValue();
		}
	}
	
	public static float getFloat(final String path, final WzData data) {
		return getFloat(data.getChildByPath(path));
	}
	
	public static float getFloat(final String path, final WzData data, final float def) {
		return getFloat(data.getChildByPath(path), def);
	}

	public static int getInt(final WzData data) {
		return ((Integer) data.getData()).intValue();
	}

	public static int getInt(final WzData data, final int def) {
		if (data == null || data.getData() == null) {
			return def;
		} else {
			if (data.getType() == WzDataType.STRING) {
				return Integer.parseInt(getString(data));
			} else {
				return ((Integer) data.getData()).intValue();
			}
		}
	}

	public static int getInt(final String path, final WzData data) {
		return getInt(data.getChildByPath(path));
	}

	public static int getIntConvert(final WzData data) {
		if (data.getType() == WzDataType.STRING) {
			return Integer.parseInt(getString(data));
		} else {
			return getInt(data);
		}
	}

	public static int getIntConvert(final String path, final WzData data) {
		final WzData d = data.getChildByPath(path);
		if (d.getType() == WzDataType.STRING) {
			return Integer.parseInt(getString(d));
		} else {
			return getInt(d);
		}
	}

	public static int getInt(final String path, final WzData data, final int def) {
		return getInt(data.getChildByPath(path), def);
	}

	public static int getIntConvert(final String path, final WzData data, final int def) {
		final WzData d = data.getChildByPath(path);
		if (d == null) {
			return def;
		}
		if (d.getType() == WzDataType.STRING) {
			try {
				return Integer.parseInt(getString(d));
			} catch (final NumberFormatException nfe) {
				return def;
			}
		} else {
			return getInt(d, def);
		}
	}

	public static BufferedImage getImage(final WzData data) {
		return ((WzCanvas) data.getData()).getImage();
	}

	public static Point getVector(final WzData data) {
		return (Point) data.getData();
	}

	public static Point getVector(final String path, final WzData data) {
		return getVector(data.getChildByPath(path));
	}

	public static Point getVector(final String path, final WzData data, final Point def) {
		final WzData pointData = data.getChildByPath(path);
		if (pointData == null) {
			return def;
		}
		return getVector(pointData);
	}
	
	public static Point getPoint(final String xPath, final String yPath, final WzData data) {
		final int x = getInt(xPath, data);
		final int y = getInt(yPath, data);
		return new Point(x, y);
	}
	
	public static Rectangle getRectangle(final String x1Path, final String y1Path, final String x2Path, final String y2Path, final WzData data) {
		final int x1 = WzDataTool.getInt(x1Path, data);
		final int y1 = WzDataTool.getInt(y1Path, data);
		final int x2 = WzDataTool.getInt(x2Path, data);
		final int y2 = WzDataTool.getInt(y2Path, data);
		return new Rectangle(x1, y1, x2 - x1, y2 - y1);
	}

	public static String getFullDataPath(final WzData data) {
		String path = "";
		WzDataEntity myData = data;
		while (myData != null) {
			path = myData.getName() + "/" + path;
			myData = myData.getParent();
		}
		return path.substring(0, path.length() - 1);
	}
}
