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
package javastory.server.life;

import java.util.HashMap;
import java.util.Map;

public class NpcStats {

    private String name;
    private Map<Byte, Integer> equips;
    private int face, hair;
    private byte skin;
    private int foothold, rangeXStart, rangeXEnd, centerY;

    public NpcStats(String name, boolean playerNpc) {
        this.name = name;

        if (playerNpc) {
            equips = new HashMap<>();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Byte, Integer> getEquips() {
        return equips;
    }

    public int getFH() {
        return foothold;
    }

    public int getRX0() {
        return rangeXStart;
    }

    public int getRX1() {
        return rangeXEnd;
    }

    public int getCY() {
        return centerY;
    }

    public byte getSkin() {
        return skin;
    }

    public int getFace() {
        return face;
    }

    public int getHair() {
        return hair;
    }

    public void setEquips(Map<Byte, Integer> equips) {
        this.equips = equips;
    }

    public void setFH(int FH) {
        this.foothold = FH;
    }

    public void setRX0(int RX0) {
        this.rangeXStart = RX0;
    }

    public void setRX1(int RX1) {
        this.rangeXEnd = RX1;
    }

    public void setCY(int CY) {
        this.centerY = CY;
    }

    public void setSkin(byte skin) {
        this.skin = skin;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }
}
