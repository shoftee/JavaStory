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
package server.quest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import provider.WzData;
import provider.WzDataEntity;
import provider.xml.WzDataType;

public class CustomQuestData implements WzData, Serializable {

    private static final long serialVersionUID = -8600005891655365066L;
    private List<CustomQuestData> children = new LinkedList<CustomQuestData>();
    private String name;
    private Object data;
    private WzDataEntity parent;

    public CustomQuestData(String name, Object data, WzDataEntity parent) {
	this.name = name;
	this.data = data;
	this.parent = parent;
    }

    public void addChild(WzData child) {
	children.add((CustomQuestData) child);
    }

    public String getName() {
	return name;
    }

    public WzDataType getType() {
	return WzDataType.UNKNOWN_TYPE;
    }

    public List<WzData> getChildren() {
	WzData[] ret = new WzData[children.size()];
	ret = children.toArray(ret);
	return new ArrayList<WzData>(Arrays.asList(ret));
    }

    public WzData getChildByPath(String name) {
	if (name.equals(this.name)) {
	    return this;
	}
	String lookup, nextName;
	if (name.indexOf("/") == -1) {
	    lookup = name;
	    nextName = name;
	} else {
	    lookup = name.substring(0, name.indexOf("/"));
	    nextName = name.substring(name.indexOf("/") + 1);
	}
	for (WzData child : children) {
	    if (child.getName().equals(lookup)) {
		return child.getChildByPath(nextName);
	    }
	}
	return null;
    }

    public Object getData() {
	return data;
    }

    public Iterator<WzData> iterator() {
	return getChildren().iterator();
    }

    public WzDataEntity getParent() {
	return parent;
    }
}
