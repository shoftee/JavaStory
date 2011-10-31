package javastory.cryptography;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides methods for the custom encryption routines of the game.
 */
public class CustomEncryption {

	private CustomEncryption() {
	}

	/**
	 * Encrypts a byte array. The specified array will be modified.
	 * 
	 * @param data
	 *            the array to encrypt
	 * 
	 * @throws NullPointerException
	 *             if 'data' is null.
	 */
	public static void encrypt(final byte[] data) {
		checkNotNull(data);

		final int length = data.length;
		final byte truncatedLength = (byte) length;
		for (int j = 0; j < 6; j++) {
			if ((j & 1) != 0) {
				oddEncryptTransform(data, length, truncatedLength);
			} else {
				evenEncryptTransform(data, length, truncatedLength);
			}
		}
	}

	private static void oddEncryptTransform(final byte[] data, final int length, byte lengthByte) {
		byte remember = 0;
		for (int i = length - 1; i >= 0; i--) {
			byte current = rollLeft(data[i], 4);
			current += lengthByte;

			current ^= remember;
			remember = current;

			current ^= 0x13;
			current = rollRight(current, 3);
			data[i] = current;

			lengthByte--;
		}
	}

	private static void evenEncryptTransform(final byte[] data, final int length, byte lengthByte) {
		byte remember = 0;
		for (int i = 0; i < length; i++) {
			byte current = rollLeft(data[i], 3);
			current += lengthByte;

			current ^= remember;
			remember = current;

			current = rollRight(current, lengthByte);
			current = (byte) ~current;
			current += 0x48;
			data[i] = current;

			lengthByte--;
		}
	}

	/**
	 * Decrypts a byte array. The specified array will be modified.
	 * 
	 * @param data
	 *            the array to decrypt
	 * 
	 * @throws NullPointerException
	 *             if 'data' is null.
	 */
	public static void decrypt(final byte[] data) {
		checkNotNull(data);

		final int length = data.length;
		final byte truncatedLength = (byte) length;
		for (int j = 1; j <= 6; j++) {
			if ((j & 1) != 0) {
				oddDecryptTransform(data, length, truncatedLength);
			} else {
				evenDecryptTransform(data, length, truncatedLength);
			}
		}
	}

	private static void oddDecryptTransform(final byte[] data, final int length, byte lengthByte) {
		byte remember = 0;
		for (int i = length - 1; i >= 0; i--) {
			byte current = rollLeft(data[i], 3);
			current ^= 0x13;

			final byte tmp = current;
			current ^= remember;
			remember = tmp;

			current -= lengthByte;
			data[i] = rollRight(current, 4);

			lengthByte--;
		}
	}

	private static void evenDecryptTransform(final byte[] data, final int length, byte lengthByte) {
		byte remember = 0;
		for (int i = 0; i < length; i++) {
			byte current = data[i];
			current -= 0x48;
			current = (byte) ~current;
			current = rollLeft(current, lengthByte);

			final byte tmp = current;
			current ^= remember;
			remember = tmp;

			current -= lengthByte;
			data[i] = rollRight(current, 3);

			lengthByte--;
		}
	}

	private static byte rollLeft(final byte b, final int count) {
		final int tmp = b << (count & 7);
		return (byte) (tmp | tmp >>> 8);
	}

	private static byte rollRight(final byte b, final int count) {
		final int tmp = b << 8 - (count & 7);
		return (byte) (tmp | tmp >>> 8);
	}
}
