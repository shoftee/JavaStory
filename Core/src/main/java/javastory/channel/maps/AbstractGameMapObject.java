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
package javastory.channel.maps;

import java.awt.Point;

import server.maps.GameMapObjectType;

public abstract class AbstractGameMapObject implements GameMapObject {
    private Point position = new Point();
    private int objectId;

    @Override
	public abstract GameMapObjectType getType();

    @Override
    public Point getPosition() {
	return new Point(position);
    }

    @Override
    public void setPosition(Point position) {
	this.position.x = position.x;
	this.position.y = position.y;
    }

    @Override
    public int getObjectId() {
	return objectId;
    }

    @Override
    public void setObjectId(int id) {
	this.objectId = id;
    }
}
