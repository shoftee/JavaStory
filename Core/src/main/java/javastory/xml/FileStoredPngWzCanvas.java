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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javastory.wz.WzCanvas;

import javax.imageio.ImageIO;

public class FileStoredPngWzCanvas implements WzCanvas {

	private final File file;
	private int width;
	private int height;
	private BufferedImage image;

	public FileStoredPngWzCanvas(final int width, final int height, final File fileIn) {
		this.width = width;
		this.height = height;
		this.file = fileIn;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public BufferedImage getImage() {
		this.loadImageIfNecessary();
		return this.image;
	}

	private void loadImageIfNecessary() {
		if (this.image == null) {
			try {
				this.image = ImageIO.read(this.file);
				// replace the dimensions loaded from the wz by the REAL
				// dimensions from the image - should be equal tho
				this.width = this.image.getWidth();
				this.height = this.image.getHeight();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}