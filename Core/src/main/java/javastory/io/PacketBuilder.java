package javastory.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.awt.Point;

import handling.GamePacket;
import java.nio.charset.Charset;
import java.util.List;
import tools.FiletimeUtil;

/**
 * Provides methods to construct a game packet.
 * 
 * This class uses an internal LinkedList structure to auto-expand the buffer.
 * This makes it pro as hell.
 * 
 * @author shoftee
 */
public class PacketBuilder {

    private static final Charset ASCII = Charset.forName("US-ASCII");
    private final List<byte[]> buffers;
    private byte[] currentBuffer;
    private int currentCapacity;
    private int nextCapacity;
    private int currentPosition;
    private int globalPosition;

    /**
     * Class constructor.
     * 
     * The initial capacity is set to 32.
     */
    public PacketBuilder() {
        this(32);
    }

    /**
     * Class constructor.
     * 
     * @param initialCapacity the initial capacity for the packet buffer.
     * 
     * @throws IllegalArgumentException
     *      if <code>initialCapacity</code> is non-positive.
     */
    public PacketBuilder(int initialCapacity) {
        Preconditions.checkArgument(initialCapacity > 0);

        buffers = Lists.newLinkedList();
        currentCapacity = initialCapacity;
        nextCapacity = initialCapacity;

        currentBuffer = new byte[initialCapacity];
        buffers.add(currentBuffer);
    }

    /**
     * Allocates a new byte buffer at the end of the buffers list.
     * The new buffer has twice the capacity as the current.
     * 
     * The method makes the new buffer the current one.
     */
    private void allocateNext() {
        currentBuffer = new byte[currentCapacity];
        currentPosition = 0;

        buffers.add(currentBuffer);

        currentCapacity = nextCapacity;
        nextCapacity *= 2;
    }

    /**
     * Writes a single byte to the current buffer, 
     * or to a newly-allocated one if the current is full.
     * 
     * @param b the byte to write.
     */
    private void writeByteInternal(byte b) {
        if (currentPosition == currentCapacity) {
            allocateNext();
        }

        currentPosition++;
        globalPosition++;
        currentBuffer[currentPosition] = b;
    }

    /**
     * Writes a byte array.
     * 
     * @param bytes the byte array to write.
     */
    private void writeArrayInternal(byte[] bytes) {
        int written = 0;
        int remaining = bytes.length;
        while (true) {
            int free = currentCapacity - currentPosition;
            if (free != 0) {
                // We write as much as we can.
                // current position in source : written
                // current position in destination: this.currentPosition
                // bytes to write: min(remaining, free)
                int payload = Math.min(free, remaining);
                System.arraycopy(bytes, written,
                                 currentBuffer, currentPosition,
                                 payload);
                written += payload;
                remaining -= payload;
            }
            if (remaining == 0) {
                return;
            }
            allocateNext();
        }
    }

    /**
     * Writes a number in reverse byte order.
     * 
     * @param number
     * @param byteCount 
     */
    private void writeReverse(long number, int byteCount) {
        for (int i = 0; i < byteCount; i++) {
            byte b = (byte) (number & 0xFF);
            writeByteInternal(b);
            number >>>= 8;
        }
    }

    public GamePacket getPacket() {
        byte[] total = new byte[globalPosition];
        int index = 0;
        // We take all the buffers except the current one.
        for (int i = 0; i < buffers.size() - 1; i++) {
            // Get the i-th buffer.
            byte[] buffer = buffers.get(i);
            // copy its contents to the big array.
            System.arraycopy(buffer, 0, total, index, buffer.length);
            index += buffer.length;
        }
        // Finally copy the current buffer separately (it may be incomplete)
        System.arraycopy(currentBuffer, 0, total, index, currentPosition);

        return new ByteArrayGamePacket(total);
    }

    /**
     * Writes a specified number of null (0x00) bytes.
     * 
     * @param count the number of null bytes to write.
     */
    public void writeZeroBytes(int count) {
        for (int i = 0; i < count; i++) {
            writeByteInternal((byte) 0);
        }
    }

    /**
     * Writes an array.
     * 
     * @param bytes the array to write.
     */
    public void writeBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            writeByteInternal(bytes[i]);
        }
    }

    /**
     * Writes a byte.
     * 
     * @param b the byte to write.
     */
    public void writeByte(byte b) {
        writeByteInternal(b);
    }

    /**
     * Writes a byte with the value 1 if <code>bool</code> is <code>true</code>
     * or 0 if it's <code>false</code>.
     * 
     * @param bool The boolean value to write.
     */
    public void writeAsByte(boolean bool) {
        writeAsByte(bool ? 1 : 0);
    }

    /**
     * Writes a 16-bit integer with the value 1 if <code>bool</code> is <code>true</code>
     * or 0 if it's <code>false</code>.
     * 
     * @param bool The boolean value to write.
     */
    public void writeAsShort(boolean bool) {
        writeAsShort(bool ? 1 : 0);
    }

    /**
     * Writes a number as a byte.
     * The major bits will be truncated.
     * 
     * @param number the number to write.
     */
    public void writeAsByte(int number) {
        writeByteInternal((byte) number);
    }

    /**
     * Writes a number as a 16-bit integer.
     * The major bits will be truncated.
     * 
     * @param number the number to write.
     */
    public void writeAsShort(int number) {
        writeReverse(number, 2);
    }

    /**
     * Writes a number as a 32-bit integer.
     * 
     * @param number the number to write.
     */
    public void writeInt(int number) {
        writeReverse(number, 4);
    }

    /**
     * Writes a 64-bit integer.
     * 
     * @param number the number to write.
     */
    public void writeLong(long number) {
        writeReverse(number, 8);
    }

    /**
     * Writes a simple string.
     * @param string the string to write.
     */
    private void writeString(String string) {
        final int length = string.length();
        writeArrayInternal(string.getBytes(ASCII));
    }

    /**
     * Writes a string padded to a specified length.
     * 
     * @param string        the string to write.
     * @param totalLength   the length to pad to.
     * 
     * @throws IllegalArgumentException
     *      if the length of the given string is 
     *      greater than <code>totalLength</code>.
     */
    public void writePaddedString(String string, int totalLength) {
        final int length = string.length();
        Preconditions.checkArgument(length <= totalLength);

        writeString(string);
        for (int i = length; i < totalLength; i++) {
            writeAsByte(0);
        }
    }

    /**
     * Writes a length-prefixed string.
     * 
     * @param string the string to write.
     */
    public void writeLengthPrefixedString(String string) {
        writeAsShort((short) string.length());
        writeString(string);
    }

    /**
     * Writes a null-terminated string.
     * 
     * @param string the string to write.
     */
    public void writeNullTerminatedString(String string) {
        writeString(string);
        writeAsByte(0);
    }

    /**
     * Writes a two-dimensional point, represented as two 16-bit integers.
     * @param point the point to write.
     */
    public void writeVector(Point point) {
        writeAsShort((short) point.x);
        writeAsShort((short) point.y);
    }

    public void writeAsFiletime(long unixtime) {
        writeLong(FiletimeUtil.getFiletime(unixtime));
    }
}
