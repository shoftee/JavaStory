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

package client;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.io.Serializable;

import database.DatabaseConnection;
import org.javastory.tools.Randomizer;
import tools.MaplePacketCreator;

public class Mount implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private int itemid, skillid, fatigue, exp, level;
    private transient boolean changed = false;
    private transient ScheduledFuture<?> tirednessSchedule = null;
    private transient WeakReference<ChannelCharacter> owner;

    public Mount(ChannelCharacter owner, int id, int skillid, int fatigue, int level, int exp) {
	this.itemid = id;
	this.skillid = skillid;
	this.fatigue = fatigue;
	this.level = level;
	this.exp = exp;
	this.owner = new WeakReference<ChannelCharacter>(owner);
    }

    public void saveMount(final int charid) throws SQLException {
	if (!changed) {
	    return;
	}
	Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps = con.prepareStatement("UPDATE mountdata set `Level` = ?, `Exp` = ?, `Fatigue` = ? WHERE characterid = ?");
	ps.setInt(1, level);
	ps.setInt(2, exp);
	ps.setInt(3, fatigue);
	ps.setInt(4, charid);
	ps.close();
    }

    public int getId() {
	switch (itemid) {
	    case 1902000:
	    case 1902001:
	    case 1902002:
		return itemid - 1901999;
	case 1902005:
	case 1902006:
	case 1902007:
		return itemid - 1902004;
	case 1902015:
	case 1902016:
	case 1902017:
	case 1902018:
		return itemid - 1902014;
	case 1902040:
	case 1902041:
	case 1902042:
		return itemid - 1902039;
	    default:
		return 4;
	}
    }

    public int getItemId() {
	return itemid;
    }

    public int getSkillId() {
	return skillid;
    }

    public int getFatigue() {
	return fatigue;
    }

    public int getExp() {
	return exp;
    }

    public int getLevel() {
	return level;
    }

    public void setItemId(int c) {
	changed = true;
	this.itemid = c;
    }

    public void setFatigue(int amount) {
	changed = true;
	fatigue += amount;
	if (fatigue < 0) {
	    fatigue = 0;
	}
    }

    public void setExp(int c) {
	changed = true;
	this.exp = c;
    }

    public void setLevel(int c) {
	changed = true;
	this.level = c;
    }

    public void increaseFatigue() {
	changed = true;
	this.fatigue++;
	if (fatigue > 100 && owner.get() != null) {
	    owner.get().dispelSkill(1004);
	}
	update();
    }

/*    public void startSchedule() {
	tirednessSchedule = TimerManager.getInstance().register(new Runnable() {

	    public void run() {
		increaseFatigue();
	    }
	}, 30000, 30000);
    }*/

/*    public void cancelSchedule() {
	if (tirednessSchedule != null) {
	    tirednessSchedule.cancel(false);
	}
    }*/

    public void increaseExp() {
	int e;
	if (level >= 1 && level <= 7) {
	    e = Randomizer.nextInt(10) + 15;
	} else if (level >= 8 && level <= 15) {
	    e = Randomizer.nextInt(13) + 15 / 2;
	} else if (level >= 16 && level <= 24) {
	    e = Randomizer.nextInt(23) + 18 / 2;
	} else {
	    e = Randomizer.nextInt(28) + 25 / 2;
	}
	setExp(exp + e);
    }

    public void update() {
	final ChannelCharacter chr = owner.get();
	if (chr != null && chr != null) {
//	    cancelSchedule();
	    chr.getMap().broadcastMessage(MaplePacketCreator.updateMount(chr, false));
	}
    }
}
