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
package handling.channel.handler;

import java.util.List;
import java.awt.Point;

import client.ISkill;
import client.GameConstants;
import client.ChannelCharacter;
import client.SkillFactory;
import server.StatEffect;
import server.AutobanManager;
import tools.AttackPair;

public class AttackInfo {

    public int skill, charge, lastAttackTickCount;
    public List<AttackPair> allDamage;
    public Point position;
    public byte hits, targets, tbyte, display, animation, speed, csstar, AOE, slot;

    public final StatEffect getAttackEffect(final ChannelCharacter chr, int skillLevel, final ISkill skill_) {
	if (GameConstants.isMulungSkill(skill)) {
	    skillLevel = 1;
	} else if (skillLevel == 0) {
	    return null;
	}
	if (GameConstants.isLinkedAranSkill(skill)) {
	    final ISkill skillLink = SkillFactory.getSkill(skill);
	    if (display > 80) {
		if (!skillLink.getAction()) {
		    //AutobanManager.getInstance().autoban(chr.getClient(), "No delay hack, SkillID : " + skill);
		    return null;
		}
	    }
	    return skillLink.getEffect(chr, skillLevel);
	}
	if (display > 80) {
	    if (!skill_.getAction()) {
		//AutobanManager.getInstance().autoban(chr.getClient(), "No delay hack, SkillID : " + skill);
		return null;
	    }
	}
	return skill_.getEffect(chr, skillLevel);
    }
}
