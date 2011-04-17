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
package server.maps;

import java.awt.Point;

public class Foothold implements Comparable<Foothold> {
    private Point p1;
    private Point p2;
    private int id;
    private short next, prev;

    public Foothold(Point p1, Point p2, int id) {
	this.p1 = p1;
	this.p2 = p2;
	this.id = id;
    }

    public boolean isWall() {
	return p1.x == p2.x;
    }

    public int getX1() {
	return p1.x;
    }

    public int getX2() {
	return p2.x;
    }

    public int getY1() {
	return p1.y;
    }

    public int getY2() {
	return p2.y;
    }

    public int compareTo(Foothold o) {
	Foothold other = (Foothold) o;
	if (p2.y < other.getY1())
	    return -1;
	else if (p1.y > other.getY2())
	    return 1;
	else
	    return 0;
    }

    public int getId() {
	return id;
    }

    public short getNext() {
	return next;
    }

    public void setNext(short next) {
	this.next = next;
    }

    public short getPrev() {
	return prev;
    }

    public void setPrev(short prev) {
	this.prev = prev;
    }
}
