package javastory.cryptography;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.security.Security;

import javastory.tools.HexTool;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * Provides AES cryptographic rolling transformation.
 * 
 * @author shoftee
 */
public class AesTransform {

	private static final int IV_LENGTH = 16;

	private static final int BLOCK_LENGTH = 1460;

	private static final byte[] SHUFFLE_TABLE = {
		(byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4,
		(byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF,
		(byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC,
		(byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1,
		(byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C,
		(byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD,
		(byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3,
		(byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0,
		(byte) 0xFB, (byte) 0xA1, (byte) 0x6E, (byte) 0x66,
		(byte) 0xEA, (byte) 0xAE, (byte) 0xD6, (byte) 0xCE,
		(byte) 0x06, (byte) 0x18, (byte) 0x4E, (byte) 0xEB,
		(byte) 0x78, (byte) 0x95, (byte) 0xDB, (byte) 0xBA,
		(byte) 0xB6, (byte) 0x42, (byte) 0x7A, (byte) 0x2A,
		(byte) 0x83, (byte) 0x0B, (byte) 0x54, (byte) 0x67,
		(byte) 0x6D, (byte) 0xE8, (byte) 0x65, (byte) 0xE7,
		(byte) 0x2F, (byte) 0x07, (byte) 0xF3, (byte) 0xAA,
		(byte) 0x27, (byte) 0x7B, (byte) 0x85, (byte) 0xB0,
		(byte) 0x26, (byte) 0xFD, (byte) 0x8B, (byte) 0xA9,
		(byte) 0xFA, (byte) 0xBE, (byte) 0xA8, (byte) 0xD7,
		(byte) 0xCB, (byte) 0xCC, (byte) 0x92, (byte) 0xDA,
		(byte) 0xF9, (byte) 0x93, (byte) 0x60, (byte) 0x2D,
		(byte) 0xDD, (byte) 0xD2, (byte) 0xA2, (byte) 0x9B,
		(byte) 0x39, (byte) 0x5F, (byte) 0x82, (byte) 0x21,
		(byte) 0x4C, (byte) 0x69, (byte) 0xF8, (byte) 0x31,
		(byte) 0x87, (byte) 0xEE, (byte) 0x8E, (byte) 0xAD,
		(byte) 0x8C, (byte) 0x6A, (byte) 0xBC, (byte) 0xB5,
		(byte) 0x6B, (byte) 0x59, (byte) 0x13, (byte) 0xF1,
		(byte) 0x04, (byte) 0x00, (byte) 0xF6, (byte) 0x5A,
		(byte) 0x35, (byte) 0x79, (byte) 0x48, (byte) 0x8F,
		(byte) 0x15, (byte) 0xCD, (byte) 0x97, (byte) 0x57,
		(byte) 0x12, (byte) 0x3E, (byte) 0x37, (byte) 0xFF,
		(byte) 0x9D, (byte) 0x4F, (byte) 0x51, (byte) 0xF5,
		(byte) 0xA3, (byte) 0x70, (byte) 0xBB, (byte) 0x14,
		(byte) 0x75, (byte) 0xC2, (byte) 0xB8, (byte) 0x72,
		(byte) 0xC0, (byte) 0xED, (byte) 0x7D, (byte) 0x68,
		(byte) 0xC9, (byte) 0x2E, (byte) 0x0D, (byte) 0x62,
		(byte) 0x46, (byte) 0x17, (byte) 0x11, (byte) 0x4D,
		(byte) 0x6C, (byte) 0xC4, (byte) 0x7E, (byte) 0x53,
		(byte) 0xC1, (byte) 0x25, (byte) 0xC7, (byte) 0x9A,
		(byte) 0x1C, (byte) 0x88, (byte) 0x58, (byte) 0x2C,
		(byte) 0x89, (byte) 0xDC, (byte) 0x02, (byte) 0x64,
		(byte) 0x40, (byte) 0x01, (byte) 0x5D, (byte) 0x38,
		(byte) 0xA5, (byte) 0xE2, (byte) 0xAF, (byte) 0x55,
		(byte) 0xD5, (byte) 0xEF, (byte) 0x1A, (byte) 0x7C,
		(byte) 0xA7, (byte) 0x5B, (byte) 0xA6, (byte) 0x6F,
		(byte) 0x86, (byte) 0x9F, (byte) 0x73, (byte) 0xE6,
		(byte) 0x0A, (byte) 0xDE, (byte) 0x2B, (byte) 0x99,
		(byte) 0x4A, (byte) 0x47, (byte) 0x9C, (byte) 0xDF,
		(byte) 0x09, (byte) 0x76, (byte) 0x9E, (byte) 0x30,
		(byte) 0x0E, (byte) 0xE4, (byte) 0xB2, (byte) 0x94,
		(byte) 0xA0, (byte) 0x3B, (byte) 0x34, (byte) 0x1D,
		(byte) 0x28, (byte) 0x0F, (byte) 0x36, (byte) 0xE3,
		(byte) 0x23, (byte) 0xB4, (byte) 0x03, (byte) 0xD8,
		(byte) 0x90, (byte) 0xC8, (byte) 0x3C, (byte) 0xFE,
		(byte) 0x5E, (byte) 0x32, (byte) 0x24, (byte) 0x50,
		(byte) 0x1F, (byte) 0x3A, (byte) 0x43, (byte) 0x8A,
		(byte) 0x96, (byte) 0x41, (byte) 0x74, (byte) 0xAC,
		(byte) 0x52, (byte) 0x33, (byte) 0xF0, (byte) 0xD9,
		(byte) 0x29, (byte) 0x80, (byte) 0xB1, (byte) 0x16,
		(byte) 0xD3, (byte) 0xAB, (byte) 0x91, (byte) 0xB9,
		(byte) 0x84, (byte) 0x7F, (byte) 0x61, (byte) 0x1E,
		(byte) 0xCF, (byte) 0xC5, (byte) 0xD1, (byte) 0x56,
		(byte) 0x3D, (byte) 0xCA, (byte) 0xF4, (byte) 0x05,
		(byte) 0xC6, (byte) 0xE5, (byte) 0x08, (byte) 0x49
	};

	private static final byte[] SHUFFLE_IV = {
		(byte) 0xF2, (byte) 0x53, (byte) 0x50, (byte) 0xC6
	};

	private static final byte[] AES_KEY = {
		(byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x1B, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x33, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x52, (byte) 0x00, (byte) 0x00, (byte) 0x00
	};

	private static final Cipher cipher;

	static {
		Security.addProvider(new BouncyCastleProvider());

		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_KEY, "AES"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] iv;
	private short version;

	/**
	 * Class constructor.
	 * 
	 * @param iv
	 *            the initialization vector
	 * @param version
	 *            the game version
	 * @param versionType
	 *            the version representation type
	 * 
	 * @throws NullPointerException
	 *             if any of the arguments is null.
	 * @throws IllegalArgumentException
	 *             if the array 'iv' doesn't have exactly 4 elements.
	 * 
	 * @see VersionType
	 */
	public AesTransform(final byte[] iv, final short version,
			final VersionType versionType) {
		checkNotNull(iv);
		checkNotNull(version);
		checkNotNull(versionType);
		checkArgument(iv.length == 4, "'iv' must have exactly 4 elements.");

		this.iv = new byte[4];
		System.arraycopy(this.iv, 0, iv, 0, 4);

		if (versionType == VersionType.COMPLEMENT) {
			this.version = (short) (~version);
		}

		this.version = (short)
				((this.version >> 8) |
				((this.version & 0xFF) << 8));
	}

	/**
	 * Determines whether the specified byte array has a valid header.
	 * 
	 * @param header
	 *            the byte array to check
	 * @return true if the header is valid; otherwise, false.
	 */
	public boolean validateHeader(final byte[] header) {
		short extractedVersion = getVersion(header, this.iv);
		return extractedVersion == this.version;
	}

	/**
	 * Constructs a header for a packet.
	 * 
	 * @param length
	 *            the length of the packet
	 * @return a 4-element byte array
	 * 
	 * @throws IllegalArgumentException
	 *             if the 'length' is less than 2.
	 */
	public byte[] constructHeader(final int length) {
		checkArgument(length >= 2);

		int encodedVersion = (((this.iv[2] << 8) | this.iv[3]) ^ this.version);
		int encodedLength = encodedVersion ^
				(((length & 0xFF) << 8) | (length >> 8));

		byte[] header = new byte[4];
		header[0] = (byte) (encodedVersion >> 8);
		header[1] = (byte) encodedVersion;
		header[2] = (byte) (encodedLength >> 8);
		header[3] = (byte) encodedLength;

		return header;
	}

	/**
	 * Applies the cryptographic transformation to an array.
	 * The given array will be modified.
	 * 
	 * @param data
	 *            the array to transform.
	 * 
	 * @throws NullPointerException
	 *             if 'data' is <code>null</code>
	 */
	public void transform(final byte[] data) {
		checkNotNull(data);

		transformArraySegment(data, this.iv, 0, data.length);

		updateIv();
	}

	private void updateIv() {
		byte[] newIv = new byte[4];
		System.arraycopy(SHUFFLE_IV, 0, newIv, 0, 4);

		for (int i = 0; i < 4; i++) {
			byte input = this.iv[i], tableInput = SHUFFLE_TABLE[input];

			newIv[0] += (byte) (SHUFFLE_TABLE[newIv[1]] - input);
			newIv[1] -= (byte) (newIv[2] ^ tableInput);
			newIv[2] ^= (byte) (SHUFFLE_TABLE[newIv[3]] + input);
			newIv[3] -= (byte) (newIv[0] - tableInput);

			int merged = ((newIv[3] << 24) | (newIv[2] << 16) |
					(newIv[1] << 8) | newIv[0]);
			int shifted = (merged << 3) | (merged >> 0x1D);

			newIv[0] = (byte) shifted;
			newIv[1] = (byte) (shifted >> 8);
			newIv[2] = (byte) (shifted >> 16);
			newIv[3] = (byte) (shifted >> 24);
		}
		this.iv = newIv;
	}

	/**
	 * Applies the cryptographic transformation to an array.
	 * 
	 * @param data
	 *            the array to transform
	 * @param iv
	 *            the initialization vector to use for the transformation.
	 * @return the transformed array.
	 */
	public static byte[] transformWithIv(final byte[] data, final byte[] iv) {
		int length = data.length;
		byte[] transformedData = new byte[length];

		System.arraycopy(data, 0, transformedData, 0, length);

		transformArraySegment(data, iv, 0, length);

		return transformedData;
	}

	/**
	 * Decodes the packet length of a packet.
	 * 
	 * @param data
	 *            the packet byte array, including header
	 * @return the decoded length
	 * 
	 * @throws NullPointerException
	 *             if 'data' is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if 'data' has less than 4 elements.
	 */
	public static int getPacketLength(final byte[] data) {
		checkNotNull(data);
		checkArgument(data.length >= 4,
						"'data' must have at least 4 elements.");

		return ((data[1] ^ data[3]) << 8) | (data[0] ^ data[2]);
	}

	/**
	 * Extracts a version number from a packet.
	 * 
	 * @param data
	 *            the packet byte array, including header
	 * @param iv
	 *            the initialization vector to use
	 * @return the extracted version
	 * 
	 * @throws NullPointerException
	 *             if 'data' is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if 'data' has less than 4 elements.
	 */
	public static short getVersion(final byte[] data, final byte[] iv) {
		checkNotNull(iv);
		checkArgument(data.length >= 4,
						"'data' must have at least 4 elements.");

		short encodedVersion = (short) ((data[0] << 8) | data[1]);
		short xorSegment = (short) ((iv[2] << 8) | iv[3]);

		return (short) (encodedVersion ^ xorSegment);
	}

	private static void transformArraySegment(final byte[] data,
			final byte[] iv, final int segmentStart, final int segmentEnd) {
		byte[] xorBlock = new byte[IV_LENGTH];

		// First block is 4 elements shorter because of the header.
		final int FIRST_BLOCK_LENGTH = BLOCK_LENGTH - 4;

		int blockStart = segmentStart;
		int blockEnd = Math.min(blockStart + FIRST_BLOCK_LENGTH,
								segmentEnd);

		transformBlock(data, iv, blockStart, blockEnd, xorBlock);

		blockStart += FIRST_BLOCK_LENGTH;
		while (blockStart < segmentEnd) {
			blockEnd = Math.min(blockStart + BLOCK_LENGTH, segmentEnd);

			transformBlock(data, iv, blockStart, blockEnd, xorBlock);

			blockStart += BLOCK_LENGTH;
		}
	}

	private static void transformBlock(final byte[] data, final byte[] iv,
			final int blockStart, final int blockEnd, byte[] xorBlock) {
		fillXorBlock(iv, xorBlock);

		int xorBlockPosition = 0;
		for (int position = blockStart; position < blockEnd; position++) {
			if (xorBlockPosition == 0) {
				try {
					xorBlock = cipher.doFinal(xorBlock, 0, IV_LENGTH);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			data[position] ^= xorBlock[xorBlockPosition++];

			if (xorBlockPosition == IV_LENGTH) {
				xorBlockPosition = 0;
			}
		}
	}

	private static void fillXorBlock(byte[] iv, byte[] xorBlock) {
		for (int i = 0; i < IV_LENGTH; i += 4) {
			System.arraycopy(iv, 0, xorBlock, i, 4);
		}
	}

	/**
	 * Returns the IV of this instance as a string.
	 */
	@Override
	public String toString() {
		return "IV: " + HexTool.toString(this.iv);
	}
}
