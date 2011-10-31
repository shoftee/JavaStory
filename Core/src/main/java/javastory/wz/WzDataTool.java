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

	public static Point getPoint(final WzData data) {
		return (Point) data.getData();
	}

	public static Point getPoint(final String path, final WzData data) {
		return getPoint(data.getChildByPath(path));
	}

	public static Point getPoint(final String path, final WzData data, final Point def) {
		final WzData pointData = data.getChildByPath(path);
		if (pointData == null) {
			return def;
		}
		return getPoint(pointData);
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
