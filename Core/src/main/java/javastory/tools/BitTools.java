/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package javastory.tools;

/**
 * Provides static methods for working with raw byte sequences.
 *
 * @author Frz
 * @since Revision 206
 * @version 1.0
 */
public class BitTools {

    /**
     * Reads a short from <code>array</code> at <code>index</code>
     *
     * @param array The byte array to read the short integer from.
     * @param index Where reading begins.
     * @return The short integer value.
     */
    public static int getShort(final byte array[], final int index) {
	int ret = array[index];
	ret &= 0xFF;
	ret |= ((array[index + 1]) << 8) & 0xFF00;
	return ret;
    }

    /**
     * Reads a string from <code>array</code> at
     * <code>index</code> <code>length</code> in length.
     *
     * @param array The array to read the string from.
     * @param index Where reading begins.
     * @param length The number of bytes to read.
     * @return The string read.
     */
    public static String getString(final byte array[], final int index, final int length) {
	char[] cret = new char[length];
	for (int x = 0; x < length; x++) {
	    cret[x] = (char) array[x + index];
	}
	return String.valueOf(cret);
    }

    /**
     * Reads a maplestory-convention string from <code>array</code> at
     * <code>index</code>
     *
     * @param array The byte array to read from.
     * @param index Where reading begins.
     * @return The string read.
     */
    public static String getMapleString(final byte array[], final int index) {
	final int length = ((array[index]) & 0xFF) | ((array[index + 1] << 8) & 0xFF00);
	return BitTools.getString(array, index + 2, length);
    }

    /**
     * Turns a double-precision floating point integer into an integer.
     *
     * @param d The double to transform.
     * @return The converted integer.
     */
    public static int doubleshofteertBits(final double d) {
	long l = Double.doubleToLongBits(d);
	return (int) (l >> 48);
    }
}
