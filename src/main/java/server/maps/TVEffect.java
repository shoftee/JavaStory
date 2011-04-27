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

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import org.javastory.client.ChannelClient;
import org.javastory.client.ChannelCharacter;
import handling.GamePacket;
import handling.world.remote.WorldChannelInterface;
import server.TimerManager;
import tools.MaplePacketCreator;

public class TVEffect {

    private static List<String> message = new LinkedList<>();
    private static ChannelCharacter user;
    private static boolean active;
    private static int type;
    private static ChannelCharacter partner = null;
    ChannelClient c;

    public TVEffect(ChannelCharacter User, ChannelCharacter Partner, List<String> Msg, int Type) {
        message = Msg;
        user = User;
        type = Type;
        partner = Partner;
        broadCastTV(true);
    }

    public static boolean isActive() {
        return active;
    }

    private static void setActive(boolean set) {
        active = set;
    }

    private static GamePacket removeTV() {
        return MaplePacketCreator.removeTV();
    }

    public static GamePacket startTV() {
        return MaplePacketCreator.sendTV(user, message, type <= 2 ? type : type -
                3, partner);
    }

    public static void broadCastTV(boolean isActive) {
        setActive(isActive);
        WorldChannelInterface wci = user.getClient().getChannelServer().getWorldInterface();
        try {
            if (isActive) {
                wci.broadcastMessage(MaplePacketCreator.enableTV().getBytes());
                wci.broadcastMessage(startTV().getBytes());

                TimerManager.getInstance().schedule(new Runnable() {

                    @Override
                    public void run() {
                        broadCastTV(false);
                    }
                }, getDelayTime(type));

            } else {
                wci.broadcastMessage(removeTV().getBytes());
            }
        } catch (RemoteException e) {
        }
    }

    public static int getDelayTime(int type) {
        switch (type) {
            case 0:
            case 3:
                return 15000;
            case 1:
            case 4:
                return 30000;
            case 2:
            case 5:
                return 60000;
        }
        return 0;
    }
}
