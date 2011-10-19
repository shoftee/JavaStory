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

import java.util.HashMap;
import java.util.Map;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

public final class PetDataFactory {

    private static class PetCommandEntry {

        public int pet;
        public int skill;

        public PetCommandEntry(int petId, int skillId) {
            this.pet = petId;
            this.skill = skillId;
        }
    }
    private static WzDataProvider dataRoot = WzDataProviderFactory.getDataProvider("Item.wz");
    private static Map<PetCommandEntry, PetCommand> petCommands = new HashMap<>();
    private static Map<Integer, Integer> petHunger = new HashMap<>();

    private PetDataFactory() {
    }

    public static PetCommand getPetCommand(final int petId, final int skillId) {
        PetCommand ret = petCommands.get(new PetCommandEntry(petId, skillId));
        if (ret != null) {
            return ret;
        }
        final WzData skillData = dataRoot.getData("Pet/" + petId + ".img");
        int prob = 0;
        int inc = 0;
        if (skillData != null) {
            prob = WzDataTool.getInt("interact/" + skillId + "/prob", skillData, 0);
            inc = WzDataTool.getInt("interact/" + skillId + "/inc", skillData, 0);
        }
        ret = new PetCommand(petId, skillId, prob, inc);
        petCommands.put(new PetCommandEntry(petId, skillId), ret);

        return ret;
    }

    public static int getHunger(final int petId) {
        Integer ret = petHunger.get(Integer.valueOf(petId));
        if (ret != null) {
            return ret;
        }
        final WzData hungerData = dataRoot.getData("Pet/" + petId + ".img").getChildByPath("info/hungry");
        ret = Integer.valueOf(WzDataTool.getInt(hungerData, 1));
        petHunger.put(petId, ret);

        return ret;
    }
}
