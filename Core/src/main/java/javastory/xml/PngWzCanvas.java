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
package javastory.xml;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javastory.wz.WzCanvas;

public class PngWzCanvas implements WzCanvas {

	private static final int[] ZAHLEN = new int[] { 2, 1, 0, 3 };
	private final int height;
	private final int width;
	private final int dataLength;
	private final int format;
	private final byte[] data;

	public PngWzCanvas(final int width, final int height, final int dataLength, final int format, final byte[] data) {
		super();
		this.height = height;
		this.width = width;
		this.dataLength = dataLength;
		this.format = format;
		this.data = data;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	public int getFormat() {
		return this.format;
	}

	public byte[] getData() {
		return this.data;
	}

	@Override
	public BufferedImage getImage() {
		int sizeUncompressed = 0;
		int size8888 = 0;
		int maxWriteBuf = 2;
		int maxHeight = 3;

		byte[] writeBuf = new byte[maxWriteBuf];

		switch (this.getFormat()) {
		case 1:
		case 513:
			sizeUncompressed = this.getHeight() * this.getWidth() * 4;
			break;
		case 2:
			sizeUncompressed = this.getHeight() * this.getWidth() * 8;
			break;
		case 517:
			sizeUncompressed = this.getHeight() * this.getWidth() / 128;
			break;
		}

		size8888 = this.getHeight() * this.getWidth() * 8;

		if (size8888 > maxWriteBuf) {
			maxWriteBuf = size8888;
			writeBuf = new byte[maxWriteBuf];
		}

		if (this.getHeight() > maxHeight) {
			maxHeight = this.getHeight();
		}

		final Inflater dec = new Inflater();
		dec.setInput(this.getData(), 0, this.dataLength);

		int declen = 0;
		final byte[] uc = new byte[sizeUncompressed];

		try {
			declen = dec.inflate(uc);
		} catch (final DataFormatException ex) {
			throw new RuntimeException("zlib fucks", ex);
		}

		dec.end();
		// fuck the format
		if (this.getFormat() == 1) {
			for (int i = 0; i < sizeUncompressed; i++) {
				final byte low = (byte) (uc[i] & 0x0F);
				final byte high = (byte) (uc[i] & 0xF0);

				writeBuf[i << 1] = (byte) ((low << 4 | low) & 0xFF);
				writeBuf[(i << 1) + 1] = (byte) (high | high >>> 4 & 0xF);
			}
		} else if (this.getFormat() == 2) {
			writeBuf = uc;
		} else if (this.getFormat() == 513) {
			for (int i = 0; i < declen; i += 2) {
				final byte bBits = (byte) ((uc[i] & 0x1F) << 3);
				final byte gBits = (byte) ((uc[i + 1] & 0x07) << 5 | (uc[i] & 0xE0) >> 3);
				final byte rBits = (byte) (uc[i + 1] & 0xF8);

				writeBuf[i << 1] = (byte) (bBits | bBits >> 5);
				writeBuf[(i << 1) + 1] = (byte) (gBits | gBits >> 6);
				writeBuf[(i << 1) + 2] = (byte) (rBits | rBits >> 5);
				writeBuf[(i << 1) + 3] = (byte) 0xFF;
			}
		} else if (this.getFormat() == 517) {
			byte b = 0x00;
			int pixelIndex = 0;

			for (int i = 0; i < declen; i++) {
				for (int j = 0; j < 8; j++) {
					b = (byte) (((uc[i] & 0x01 << 7 - j) >> 7 - j) * 255);
					for (int k = 0; k < 16; k++) {
						pixelIndex = (i << 9) + (j << 6) + k * 2;
						writeBuf[pixelIndex] = b;
						writeBuf[pixelIndex + 1] = b;
						writeBuf[pixelIndex + 2] = b;
						writeBuf[pixelIndex + 3] = (byte) 0xFF;
					}
				}
			}
		}

		final DataBufferByte imgData = new DataBufferByte(writeBuf, sizeUncompressed);

		// SampleModel sm = new
		// PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, c.getWidth(),
		// c.getHeight(), 4, c.getWidth() * 4, new int[] {2, 1, 0, 3});
		final SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, this.getWidth(), this.getHeight(), 4, this.getWidth() * 4, ZAHLEN);
		final WritableRaster imgRaster = Raster.createWritableRaster(sm, imgData, new Point(0, 0));

		final BufferedImage aa = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
		aa.setData(imgRaster);

		return aa;
	}
}
