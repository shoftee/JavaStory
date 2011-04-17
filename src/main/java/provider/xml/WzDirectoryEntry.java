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
package provider.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.WzDataDirectoryEntry;
import provider.WzDataEntity;
import provider.WzDataEntry;
import provider.WzDataFileEntry;

public class WzDirectoryEntry extends WzEntry implements WzDataDirectoryEntry {

    private List<WzDataDirectoryEntry> subdirs = new ArrayList<WzDataDirectoryEntry>();
    private List<WzDataFileEntry> files = new ArrayList<WzDataFileEntry>();
    private Map<String, WzDataEntry> entries = new HashMap<String, WzDataEntry>();

    public WzDirectoryEntry(String name, int size, int checksum, WzDataEntity parent) {
	super(name, size, checksum, parent);
    }

    public WzDirectoryEntry() {
	super(null, 0, 0, null);
    }

    public void addDirectory(WzDataDirectoryEntry dir) {
	subdirs.add(dir);
	entries.put(dir.getName(), dir);
    }

    public void addFile(WzDataFileEntry fileEntry) {
	files.add(fileEntry);
	entries.put(fileEntry.getName(), fileEntry);
    }

    public List<WzDataDirectoryEntry> getSubdirectories() {
	return Collections.unmodifiableList(subdirs);
    }

    public List<WzDataFileEntry> getFiles() {
	return Collections.unmodifiableList(files);
    }

    public WzDataEntry getEntry(String name) {
	return entries.get(name);
    }
}
