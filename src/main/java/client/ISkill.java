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

import server.StatEffect;
import server.life.Element;

public interface ISkill {

    int getId();
    StatEffect getEffect(int level);
    StatEffect getEffect(GameCharacter chr, int level);
    byte getMaxLevel();
    int getAnimationTime();
    public boolean canBeLearnedBy(int job);
    public boolean isFourthJob();
    public boolean getAction();
    public Element getElement();
    public boolean isBeginnerSkill();
    public boolean hasRequiredSkill();
    public boolean isInvisible();
    public boolean isChargeSkill();
    public int getRequiredSkillLevel();
    public int getRequiredSkillId();
    public int getMasterLevel();
}
