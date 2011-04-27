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

import org.javastory.client.ChannelCharacter;
import java.util.ArrayList;
import java.util.List;
import org.javastory.game.Jobs;

import provider.WzData;
import provider.WzDataTool;
import server.StatEffect;
import server.life.Element;

public class Skill implements ISkill {

    public static final int[] DUALBLADE_SKILLS = new int[]{
        4311003, 4321000, 4331002, 4331005, 4341004, 4341007
    };
    //
    public static final int[] EVAN_SKILLS_1 = new int[]{
        22171000, 22171002, 22171003, 22171004
    };
    //
    public static final int[] EVAN_SKILLS_2 = new int[]{
        22181000, 22181001, 22181002, 22181003
    };
    //
    private int id;
    private final List<StatEffect> effects = new ArrayList<>();
    private Element element;
    private byte level;
    private int animationTime, requiredSkill, masterLevel;
    private boolean action;
    private boolean invisible;
    private boolean chargeskill;

    private Skill(final int id) {
        super();
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    public static Skill loadFromData(final int id, final WzData data) {
        Skill ret = new Skill(id);

        boolean isBuff = false;
        final int skillType = WzDataTool.getInt("skillType", data, -1);
        final String elem = WzDataTool.getString("elemAttr", data, null);
        if (elem != null) {
            ret.element = Element.getFromChar(elem.charAt(0));
        } else {
            ret.element = Element.NEUTRAL;
        }
        ret.invisible = WzDataTool.getInt("invisible", data, 0) > 0;
        ret.masterLevel = WzDataTool.getInt("masterLevel", data, 0);

        // unfortunatly this is only set for a few skills so we have to do some more to figure out if it's a buff �.o
        final WzData effect = data.getChildByPath("effect");
        if (skillType != -1) {
            if (skillType == 2) {
                isBuff = true;
            }
        } else {
            final WzData action_ = data.getChildByPath("action");
            final WzData hit = data.getChildByPath("hit");
            final WzData ball = data.getChildByPath("ball");

            boolean action = false;
            if (action_ == null) {
                if (data.getChildByPath("prepare/action") != null) {
                    action = true;
                } else {
                    switch (id) {
                        case 5201001:
                        case 5221009:
                            action = true;
                            break;
                    }
                }
            } else {
                action = true;
            }
            ret.action = action;
            isBuff = effect != null && hit == null && ball == null;
            isBuff |= action_ != null &&
                    WzDataTool.getString("0", action_, "").equals("alert2");
            switch (id) {
                case 2301002: // heal is alert2 but not overtime...
                case 2111003: // poison mist
                case 12111005: // Flame Gear
                case 2111002: // explosion
                case 4211001: // chakra
                case 2121001: // Big bang
                case 2221001: // Big bang
                case 2321001: // Big bang
                    isBuff = false;
                    break;
                case 1004: // monster riding
                case 10001004:
                case 20001004:
                case 20011004:
                case 9101004: // hide is a buff -.- atleast for us o.o"
                case 1111002: // combo
                case 4211003: // pickpocket
                case 4111001: // mesoup
                case 15111002: // Super Transformation
                case 5111005: // Transformation
                case 5121003: // Super Transformation
                case 13111005: // Alabtross
                case 21000000: // Aran Combo
                case 21101003: // Body Pressure
                case 5211001: // Pirate octopus summon
                case 5211002:
                case 5220002: // wrath of the octopi
                case 5211006: //homing beacon
                case 5220011: //bullseye

                case 22121001: //element reset
                case 22131001: //magic shield -- NOT CODED
                case 22141002: //magic booster
                case 22151002: //killer wing
                case 22151003: //magic resist -- NOT CODED
                case 22171000: //maple warrior
                case 22171004: //hero will
                case 22181000: //onyx blessing	
                case 22181003: //soul stone -- NOT CODED
                //case 22121000: 
                //case 22141003:
                //case 22151001: 
                //case 22161002:
                //tornado spin too ?
                //case 4341006: //final cut
                case 4331003: //owl spirit
                case 4321000: //tornado spin
                    isBuff = true;
                    break;
            }
        }
        ret.chargeskill = data.getChildByPath("keydown") != null;

        for (final WzData level : data.getChildByPath("level")) {
            ret.effects.add(StatEffect.loadSkillEffectFromData(level, id, isBuff));
        }
        final WzData reqDataRoot = data.getChildByPath("req");
        if (reqDataRoot != null) {
            for (final WzData reqData : reqDataRoot.getChildren()) {
                ret.requiredSkill = Integer.parseInt(reqData.getName());
                ret.level = (byte) WzDataTool.getInt(reqData, 1);
            }
        }
        ret.animationTime = 0;
        if (effect != null) {
            for (final WzData effectEntry : effect) {
                ret.animationTime += WzDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }
        return ret;
    }

    @Override
    public StatEffect getEffect(final int level) {
        return effects.get(level - 1);
    }

    @Override
    public StatEffect getEffect(final ChannelCharacter chr, final int level) {
        return effects.get(level - 1);
    }

    @Override
    public boolean getAction() {
        return action;
    }

    @Override
    public boolean isChargeSkill() {
        return chargeskill;
    }

    @Override
    public boolean isInvisible() {
        return invisible;
    }

    @Override
    public boolean hasRequiredSkill() {
        return level > 0;
    }

    @Override
    public int getRequiredSkillLevel() {
        return level;
    }

    @Override
    public int getRequiredSkillId() {
        return requiredSkill;
    }

    @Override
    public byte getMaxLevel() {
        return (byte) effects.size();
    }

    @Override
    public boolean canBeLearnedBy(int job) {
        int jid = job;
        int skillJob = id / 10000;
        if (skillJob == 2001 && Jobs.isEvan(job)) {
            return true; //special exception for evan -.-
        }
        if (job < 1000) {
            if (jid / 100 != skillJob / 100 && skillJob / 100 != 0) { // wrong job
                return false;
            }
        } else {
            if (jid / 1000 != skillJob / 1000 && skillJob / 1000 != 0) { // wrong job
                return false;
            }
        }
        if (Jobs.isAdventurer(skillJob) && !Jobs.isAdventurer(job)) {
            return false;
        } else if (Jobs.isCygnus(skillJob) && !Jobs.isCygnus(job)) {
            return false;
        } else if (Jobs.isAran(skillJob) && !Jobs.isAran(job)) {
            return false;
        } else if (Jobs.isEvan(skillJob) && !Jobs.isEvan(job)) {
            return false;
        }
        if ((skillJob / 10) % 10 > (jid / 10) % 10) { // wrong 2nd job
            return false;
        }
        if (skillJob % 10 > jid % 10) { // wrong 3rd/4th job
            return false;
        }
        return true;
    }

    @Override
    public boolean isFourthJob() {
        final int jobFamily = id / 10000;
        if (jobFamily >= 2212 && jobFamily < 3000) {
            //evan skill
            return (jobFamily % 10) >= 7;
        }
        if (jobFamily >= 430 && jobFamily <= 434) {
            //db skill
            return (jobFamily % 10) == 4 || isMasterSkill(id);
        }
        return (jobFamily % 10) == 2;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public int getAnimationTime() {
        return animationTime;
    }

    @Override
    public int getMasterLevel() {
        return masterLevel;
    }

    public static boolean isMasterSkill(final int skill) {
        for (int i : DUALBLADE_SKILLS) {
            if (i == skill) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBeginnerSkill() {
        String idString = String.valueOf(id);
        if (idString.length() == 4 || idString.length() == 1) {
            return true;
        }
        return false;
    }
}
