package javastory.io;

import java.awt.Point;

import com.google.common.base.Preconditions;

/**
 * Provides forward-only readBytes access to a game packet.
 * 
 * @author shoftee
 */
public final class PacketReader {

	private int position = 0;
	private final byte[] buffer;

	/**
	 * Class constructor
	 * 
	 * @param buffer
	 *            the byte array containing the packet data.
	 */
	public PacketReader(final byte[] buffer) {
		this.buffer = buffer;
	}

	/**
	 * Advances a number of bytes ahead.
	 * 
	 * @param count
	 *            the number of bytes to advance.
	 * 
	 * @return the position at which the advance started.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	private int checkedAdvance(final int count) throws PacketFormatException {
		final int oldPosition = this.position;
		this.position += count;
		if (this.position >= this.buffer.length) {
			throw new PacketFormatException();
		}
		return oldPosition;
	}

	/**
	 * Reads a byte as a boolean value.
	 * 
	 * @return whether the byte was equal to 1.
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public boolean readBoolean() throws PacketFormatException {
		return this.buffer[this.checkedAdvance(1)] == 1;
	}

	/**
	 * Reads a byte.
	 * 
	 * @return the byte that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public byte readByte() throws PacketFormatException {
		return this.buffer[this.checkedAdvance(1)];
	}

	/**
	 * Checks how many bytes are remaining in the stream.
	 * 
	 * @return the number of bytes until the end of the stream.
	 */
	public long remaining() {
		return this.buffer.length - this.position;
	}

	/**
	 * Gets the current position of the stream.
	 */
	public long getPosition() {
		return this.position;
	}

	/**
	 * Advances a specified number of bytes ahead.
	 * 
	 * @param count
	 *            the number of bytes to skip.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>count</code> is negative.
	 */
	public void skip(final int count) throws PacketFormatException {
		Preconditions.checkArgument(count >= 0);

		this.checkedAdvance(count);
	}

	private long readReverse(final int count) throws PacketFormatException {
		final int start = this.checkedAdvance(count);
		final int end = this.position;
		long number = 0;
		for (int i = start; i < end; i++) {
			number <<= 8;
			number |= this.buffer[i] & 0xFF;
		}
		return number;
	}

	/**
	 * Reads a 32-bit integer.
	 * 
	 * @return the integer that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final int readInt() throws PacketFormatException {
		return (int) (this.readReverse(4) & 0xFFFFFFFF);
	}

	/**
	 * Reads a 32-bit potentially unsigned integer.
	 * 
	 * @return the integer that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final long readUnsignedInt() throws PacketFormatException {
		return this.readReverse(4) & 0xFFFFFFFF;
	}

	/**
	 * Reads a 16-bit integer.
	 * 
	 * @return the integer that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final short readShort() throws PacketFormatException {
		return (short) (this.readReverse(2) & 0xFFFF);
	}

	/**
	 * Reads a 16-bit potentially unsigned integer.
	 * 
	 * @return the integer that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final int readUnsignedShort() throws PacketFormatException {
		return (int) (this.readReverse(2) & 0xFFFF);
	}

	/**
	 * Reads a single character.
	 * 
	 * @return the character that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final char readChar() throws PacketFormatException {
		return (char) this.readShort();
	}

	/**
	 * Reads a 64-bit integer.
	 * 
	 * @return the integer that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final long readLong() throws PacketFormatException {
		return this.readReverse(8);
	}

	/**
	 * Reads a 32-bit IEEE-754 floating-point "single float" decimal.
	 * 
	 * @return the decimal that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final float readFloat() throws PacketFormatException {
		final int bits = (int) this.readReverse(4);
		return Float.intBitsToFloat(bits);
	}

	/**
	 * Reads a 64-bit IEEE 754 floating-point "double format" decimal..
	 * 
	 * @return the decimal that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final double readDouble() throws PacketFormatException {
		final long bits = this.readReverse(8);
		return Double.longBitsToDouble(bits);
	}

	/**
	 * Reads an ASCII string with the specified length.
	 * 
	 * @param length
	 *            the length of the string.
	 * @return the string that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>length</code> is negative.
	 */
	public final String readString(final int length) throws PacketFormatException {
		Preconditions.checkArgument(length >= 0);

		final StringBuilder b = new StringBuilder(length);

		final int start = this.checkedAdvance(length);
		final int end = this.position;
		for (int i = start; i < end; i++) {
			b.append((char) this.buffer[i]);
		}
		return b.toString();
	}

	/**
	 * Reads a null-terminated string.
	 * 
	 * @return the string that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final String readNullTerminatedString() throws PacketFormatException {
		final int start = this.position;
		final StringBuilder builder = new StringBuilder();
		int i = start;
		while (this.buffer[i = this.checkedAdvance(1)] != 0x00) {
			builder.append((char) this.buffer[i]);
		}
		return builder.toString();
	}

	/**
	 * Reads a length-prefixed ASCII string.
	 * 
	 * @return the string that was readBytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final String readLengthPrefixedString() throws PacketFormatException {
		final int length = this.readShort();
		final String string = this.readString(length);
		return string;
	}

	/**
	 * Reads a two-dimensional point consisting of two 16-bit integers.
	 * 
	 * @return the Point object constructed from the data.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 */
	public final Point readVector() throws PacketFormatException {
		final int x = this.readShort();
		final int y = this.readShort();
		return new Point(x, y);
	}

	/**
	 * Reads a number of bytes off the stream.
	 * 
	 * @param count
	 *            The number of bytes to readBytes.
	 * @return An array of the readBytes bytes.
	 * 
	 * @throws PacketFormatException
	 *             if the advance put the current position past the end of the
	 *             buffer.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>count</code> is negative.
	 */
	public final byte[] readBytes(final int count) throws PacketFormatException {
		Preconditions.checkArgument(count >= 0);

		final int start = this.checkedAdvance(count);
		final byte[] bytes = new byte[count];
		System.arraycopy(this.buffer, start, bytes, 0, count);
		return bytes;
	}
}
