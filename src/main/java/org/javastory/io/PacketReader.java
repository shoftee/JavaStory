package org.javastory.io;

import com.google.common.base.Preconditions;
import java.awt.Point;
import org.javastory.io.PacketFormatException;

/**
 * Provides forward-only readBytes access to a game packet.
 * 
 * @author shoftee
 */
public final class PacketReader {

    private int position = 0;
    private byte[] buffer;

    /**
     * Class constructor
     *
     * @param buffer the byte array containing the packet data.
     */
    public PacketReader(byte[] buffer) {
        this.buffer = buffer;
    }

    /**
     * Advances a number of bytes ahead.
     * 
     * @param count the number of bytes to advance.
     * 
     * @return      the position at which the advance started.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    private int checkedAdvance(int count) throws PacketFormatException {
        int oldPosition = position;
        position += count;
        if (position >= buffer.length) {
            throw new PacketFormatException();
        }
        return oldPosition;
    }

    /**
     * Reads a byte from.
     * 
     * @return the byte that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public byte readByte() throws PacketFormatException {
        return buffer[checkedAdvance(1)];
    }

    /**
     * Checks how many bytes are remaining in the stream.
     *
     * @return the number of bytes until the end of the stream.
     */
    public long remaining() {
        return buffer.length - position;
    }

    /**
     * Gets the current position of the stream.
     */
    public long getPosition() {
        return position;
    }

    /**
     * Advances a specified number of bytes ahead.
     * 
     * @param   count the number of bytes to skip.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     * 
     * @throws IllegalArgumentException
     *      if <code>count</code> is negative.
     */
    public void skip(int count) throws PacketFormatException {
        Preconditions.checkArgument(count >= 0);
        
        checkedAdvance(count);
    }

    private long readReverse(int count) throws PacketFormatException {
        int start = checkedAdvance(count);
        int end = this.position;
        long number = 0;
        for (int i = start; i < end; i++) {
            number <<= 8;
            number |= buffer[i];
        }
        return number;
    }

    /**
     * Reads a 32-bit integer.
     *
     * @return the integer that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final int readInt() throws PacketFormatException {
        return (int) (readReverse(4) & 0xFFFFFFFF);
    }

    /**
     * Reads a 32-bit potentially unsigned integer.
     *
     * @return the integer that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final long readUnsignedInt() throws PacketFormatException {
        return (long) (readReverse(4) & 0xFFFFFFFF);
    }

    /**
     * Reads a 16-bit integer.
     *
     * @return the integer that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final short readShort() throws PacketFormatException {
        return (short) (readReverse(2) & 0xFFFF);
    }

    /**
     * Reads a 16-bit potentially unsigned integer.
     *
     * @return the integer that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final int readUnsignedShort() throws PacketFormatException {
        return (int) (readReverse(2) & 0xFFFF);
    }

    /**
     * Reads a single character.
     *
     * @return the character that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final char readChar() throws PacketFormatException {
        return (char) readShort();
    }

    /**
     * Reads a 64-bit integer.
     *
     * @return the integer that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final long readLong() throws PacketFormatException {
        return readReverse(8);
    }

    /**
     * Reads a 32-bit IEEE-754 floating-point "single float" decimal.
     *
     * @return the decimal that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final float readFloat() throws PacketFormatException {
        int bits = (int) readReverse(4);
        return Float.intBitsToFloat(bits);
    }

    /**
     * Reads a 64-bit IEEE 754 floating-point "double format" decimal..
     *
     * @return the decimal that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final double readDouble() throws PacketFormatException {
        long bits = readReverse(8);
        return Double.longBitsToDouble(bits);
    }

    /**
     * Reads an ASCII string with the specified length.
     *
     * @param length    the length of the string.
     * @return          the string that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     * 
     * @throws IllegalArgumentException
     *      if <code>length</code> is negative.
     */
    public final String readString(final int length) throws PacketFormatException {
        Preconditions.checkArgument(length >= 0);

        StringBuilder b = new StringBuilder(length);

        int start = checkedAdvance(length);
        int end = position;
        for (int i = start; i < end; i++) {
            b.append((char) buffer[i]);
        }
        return b.toString();
    }

    /**
     * Reads a null-terminated string.
     *
     * @return the string that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final String readNullTerminatedString() throws PacketFormatException {
        int start = position;
        StringBuilder builder = new StringBuilder();
        int i = start;
        while (buffer[i = checkedAdvance(1)] != 0x00) {
            builder.append((char) buffer[i]);
        }
        return builder.toString();
    }

    /**
     * Reads a length-prefixed ASCII string.
     *
     * @return the string that was readBytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final String readLengthPrefixedString() throws PacketFormatException {
        int length = readShort();
        String string = readString(length);
        return string;
    }

    /**
     * Reads a two-dimensional point consisting of two 16-bit integers.
     *
     * @return the Point object constructed from the data.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     */
    public final Point readVector() throws PacketFormatException {
        final int x = readShort();
        final int y = readShort();
        return new Point(x, y);
    }

    /**
     * Reads a number of bytes off the stream.
     *
     * @param count The number of bytes to readBytes.
     * @return      An array of the readBytes bytes.
     * 
     * @throws PacketFormatException 
     *      if the advance put the current position past the end of the buffer.
     * 
     * @throws IllegalArgumentException
     *      if <code>count</code> is negative.
     */
    public final byte[] readBytes(final int count) throws PacketFormatException {
        Preconditions.checkArgument(count >= 0);
        
        int start = checkedAdvance(count);
        byte[] bytes = new byte[count];
        System.arraycopy(buffer, start, bytes, 0, count);
        return bytes;
    }
}
