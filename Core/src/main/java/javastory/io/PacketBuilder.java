package javastory.io;

import java.awt.Point;
import java.nio.charset.Charset;
import java.util.List;

import javastory.tools.FiletimeUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

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
	 * @param initialCapacity
	 *            the initial capacity for the packet buffer.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>initialCapacity</code> is non-positive.
	 */
	public PacketBuilder(final int initialCapacity) {
		Preconditions.checkArgument(initialCapacity > 0);

		this.buffers = Lists.newLinkedList();
		this.currentCapacity = initialCapacity;
		this.nextCapacity = initialCapacity;

		this.currentBuffer = new byte[initialCapacity];
		this.buffers.add(this.currentBuffer);
	}

	/**
	 * Allocates a new byte buffer at the end of the buffers list. The new
	 * buffer has twice the capacity as the current.
	 * 
	 * The method makes the new buffer the current one.
	 */
	private void allocateNext() {
		this.currentBuffer = new byte[this.currentCapacity];
		this.currentPosition = 0;

		this.buffers.add(this.currentBuffer);

		this.currentCapacity = this.nextCapacity;
		this.nextCapacity *= 2;
	}

	/**
	 * Writes a single byte to the current buffer, or to a newly-allocated one
	 * if the current is full.
	 * 
	 * @param b
	 *            the byte to write.
	 */
	private void writeByteInternal(final byte b) {
		if (this.currentPosition == this.currentCapacity) {
			this.allocateNext();
		}

		this.currentPosition++;
		this.globalPosition++;
		this.currentBuffer[this.currentPosition] = b;
	}

	/**
	 * Writes a byte array.
	 * 
	 * @param bytes
	 *            the byte array to write.
	 */
	private void writeArrayInternal(final byte[] bytes) {
		int written = 0;
		int remaining = bytes.length;
		while (true) {
			final int free = this.currentCapacity - this.currentPosition;
			if (free != 0) {
				// We write as much as we can.
				// current position in source : written
				// current position in destination: this.currentPosition
				// bytes to write: min(remaining, free)
				final int payload = Math.min(free, remaining);
				System.arraycopy(bytes, written, this.currentBuffer, this.currentPosition, payload);
				written += payload;
				remaining -= payload;
			}
			if (remaining == 0) {
				return;
			}
			this.allocateNext();
		}
	}

	/**
	 * Writes a number in reverse byte order.
	 * 
	 * @param number
	 * @param byteCount
	 */
	private void writeReverse(long number, final int byteCount) {
		for (int i = 0; i < byteCount; i++) {
			final byte b = (byte) (number & 0xFF);
			this.writeByteInternal(b);
			number >>>= 8;
		}
	}

	public GamePacket getPacket() {
		final byte[] total = new byte[this.globalPosition];
		int index = 0;
		// We take all the buffers except the current one.
		for (int i = 0; i < this.buffers.size() - 1; i++) {
			// Get the i-th buffer.
			final byte[] buffer = this.buffers.get(i);
			// copy its contents to the big array.
			System.arraycopy(buffer, 0, total, index, buffer.length);
			index += buffer.length;
		}
		// Finally copy the current buffer separately (it may be incomplete)
		System.arraycopy(this.currentBuffer, 0, total, index, this.currentPosition);

		return GamePacket.wrapperOf(total);
	}

	/**
	 * Writes a specified number of null (0x00) bytes.
	 * 
	 * @param count
	 *            the number of null bytes to write.
	 */
	public void writeZeroBytes(final int count) {
		for (int i = 0; i < count; i++) {
			this.writeByteInternal((byte) 0);
		}
	}

	/**
	 * Writes an array.
	 * 
	 * @param bytes
	 *            the array to write.
	 */
	public void writeBytes(final byte[] bytes) {
		for (final byte b : bytes) {
			this.writeByteInternal(b);
		}
	}

	/**
	 * Writes a byte.
	 * 
	 * @param b
	 *            the byte to write.
	 */
	public void writeByte(final byte b) {
		this.writeByteInternal(b);
	}

	/**
	 * Writes a byte with the value 1 if <code>bool</code> is <code>true</code>
	 * or 0 if it's <code>false</code>.
	 * 
	 * @param bool
	 *            The boolean value to write.
	 */
	public void writeAsByte(final boolean bool) {
		this.writeAsByte(bool ? 1 : 0);
	}

	/**
	 * Writes a 16-bit integer with the value 1 if <code>bool</code> is
	 * <code>true</code> or 0 if it's <code>false</code>.
	 * 
	 * @param bool
	 *            The boolean value to write.
	 */
	public void writeAsShort(final boolean bool) {
		this.writeAsShort(bool ? 1 : 0);
	}

	/**
	 * Writes a number as a byte. The major bits will be truncated.
	 * 
	 * @param number
	 *            the number to write.
	 */
	public void writeAsByte(final int number) {
		this.writeByteInternal((byte) number);
	}

	/**
	 * Writes a number as a 16-bit integer. The major bits will be truncated.
	 * 
	 * @param number
	 *            the number to write.
	 */
	public void writeAsShort(final int number) {
		this.writeReverse(number, 2);
	}

	/**
	 * Writes a number as a 32-bit integer.
	 * 
	 * @param number
	 *            the number to write.
	 */
	public void writeInt(final int number) {
		this.writeReverse(number, 4);
	}

	/**
	 * Writes a 64-bit integer.
	 * 
	 * @param number
	 *            the number to write.
	 */
	public void writeLong(final long number) {
		this.writeReverse(number, 8);
	}

	/**
	 * Writes a simple string.
	 * 
	 * @param string
	 *            the string to write.
	 */
	private void writeString(final String string) {
		this.writeArrayInternal(string.getBytes(ASCII));
	}

	/**
	 * Writes a string padded to a specified length.
	 * 
	 * @param string
	 *            the string to write.
	 * @param totalLength
	 *            the length to pad to.
	 * 
	 * @throws IllegalArgumentException
	 *             if the length of the given string is greater than
	 *             <code>totalLength</code>.
	 */
	public void writePaddedString(final String string, final int totalLength) {
		final int length = string.length();
		Preconditions.checkArgument(length <= totalLength);

		this.writeString(string);
		for (int i = length; i < totalLength; i++) {
			this.writeAsByte(0);
		}
	}

	/**
	 * Writes a length-prefixed string.
	 * 
	 * @param string
	 *            the string to write.
	 */
	public void writeLengthPrefixedString(final String string) {
		this.writeAsShort((short) string.length());
		this.writeString(string);
	}

	/**
	 * Writes a null-terminated string.
	 * 
	 * @param string
	 *            the string to write.
	 */
	public void writeNullTerminatedString(final String string) {
		this.writeString(string);
		this.writeAsByte(0);
	}

	/**
	 * Writes a two-dimensional point, represented as two 16-bit integers.
	 * 
	 * @param point
	 *            the point to write.
	 */
	public void writeVector(final Point point) {
		this.writeAsShort((short) point.x);
		this.writeAsShort((short) point.y);
	}

	public void writeAsFiletime(final long unixtime) {
		this.writeLong(FiletimeUtil.getFiletime(unixtime));
	}
}
