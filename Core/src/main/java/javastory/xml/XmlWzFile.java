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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javastory.wz.WzData;
import javastory.wz.WzDataDirectoryEntry;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDirectoryEntry;
import javastory.wz.WzFileEntry;

public class XmlWzFile implements WzDataProvider {

	private final File root;
	private final WzDirectoryEntry rootForNavigation;

	public XmlWzFile(final File fileIn) {
		this.root = fileIn;
		this.rootForNavigation = new WzDirectoryEntry(fileIn.getName(), 0, 0, null);
		this.fillMapleDataEntitys(this.root, this.rootForNavigation);
	}

	private void fillMapleDataEntitys(final File lroot, final WzDirectoryEntry wzdir) {
		for (final File file : lroot.listFiles()) {
			final String fileName = file.getName();

			if (file.isDirectory() && !fileName.endsWith(".img")) {
				final WzDirectoryEntry newDir = new WzDirectoryEntry(fileName, 0, 0, wzdir);
				wzdir.addDirectory(newDir);
				this.fillMapleDataEntitys(file, newDir);

			} else if (fileName.endsWith(".xml")) { // get the real size here?
				wzdir.addFile(new WzFileEntry(fileName.substring(0, fileName.length() - 4), 0, 0, wzdir));
			}
		}
	}

	@Override
	public WzData getData(final String path) {
		final File dataFile = new File(this.root, path + ".xml");
		final File imageDataDir = new File(this.root, path);
		/*
		 * if (!dataFile.exists()) {
		 * throw new RuntimeException("Datafile " + path + " does not exist in "
		 * + root.getAbsolutePath());
		 * }
		 */
		FileInputStream fis;
		try {
			fis = new FileInputStream(dataFile);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException("Datafile " + path + " does not exist in " + this.root.getAbsolutePath());
		}
		final XmlDomWzData domMapleData;
		try {
			domMapleData = new XmlDomWzData(fis, imageDataDir.getParentFile());
		} finally {
			try {
				fis.close();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return domMapleData;
	}

	@Override
	public WzDataDirectoryEntry getRoot() {
		return this.rootForNavigation;
	}
}
