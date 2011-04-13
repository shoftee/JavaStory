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
package tools.data.output;

import java.awt.Point;

/**
 * Provides an interface to a writer class that writes a little-endian sequence
 * of bytes.
 * 
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public interface LittleEndianWriter {

    /**
     * Write the number of zero bytes
     *
     * @param b The bytes to write.
     */
    public void writeZeroBytes(int i);

    /**
     * Write an array of bytes to the sequence.
     *
     * @param b The bytes to write.
     */
    public void write(byte b[]);

    /**
     * Write a byte to the sequence.
     *
     * @param b The byte to write.
     */
    public void write(byte b);

    /**
     * Write a byte in integer form to the sequence.
     *
     * @param b The byte as an <code>Integer</code> to write.
     */
    public void write(int b);

    /**
     * Writes an integer to the sequence.
     *
     * @param i The integer to write.
     */
    public void writeInt(int i);
    public void writeInt(long i);

    /**
     * Write a short integer to the sequence.
     *
     * @param s The short integer to write.
     */
    public void writeShort(int s);

    /**
     * Write a long integer to the sequence.
     * @param l The long integer to write.
     */
    public void writeLong(long l);

    /**
     * Writes an ASCII string the the sequence.
     *
     * @param s The ASCII string to write.
     */
    void writeAsciiString(String s);
    void writeAsciiString(String s, int max);

    /**
     * Writes a null-terminated ASCII string to the sequence.
     *
     * @param s The ASCII string to write.
     */
    void writeNullTerminatedAsciiString(String s);

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s The Point position to write.
     */
    void writePos(Point s);

    /**
     * Writes a maple-convention ASCII string to the sequence.
     *
     * @param s The ASCII string to use maple-convention to write.
     */
    void writeMapleAsciiString(String s);
}
