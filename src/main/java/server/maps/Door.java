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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import client.GameCharacter;
import client.GameClient;
import handling.world.PartyMember;
import server.Portal;
import tools.MaplePacketCreator;

public class Door extends AbstractGameMapObject {

    private GameCharacter owner;
    private GameMap town;
    private Portal townPortal;
    private GameMap target;
    private Point targetPosition;

    public Door(final GameCharacter owner, final Point targetPosition) {
        super();
        this.owner = owner;
        this.target = owner.getMap();
        this.targetPosition = targetPosition;
        setPosition(this.targetPosition);
        this.town = this.target.getReturnMap();
        this.townPortal = getFreePortal();
    }

    public Door(final Door origDoor) {
        super();
        this.owner = origDoor.owner;
        this.town = origDoor.town;
        this.townPortal = origDoor.townPortal;
        this.target = origDoor.target;
        this.targetPosition = origDoor.targetPosition;
        this.townPortal = origDoor.townPortal;
        setPosition(townPortal.getPosition());
    }

    private Portal getFreePortal() {
        final List<Portal> freePortals = new ArrayList<Portal>();

        for (final Portal port : town.getPortals()) {
            if (port.getType() == 6) {
                freePortals.add(port);
            }
        }
        Collections.sort(freePortals, new Comparator<Portal>() {

            @Override
            public final int compare(final Portal o1, final Portal o2) {
                if (o1.getId() < o2.getId()) {
                    return -1;
                } else if (o1.getId() == o2.getId()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        for (final GameMapObject obj : town.getAllDoor()) {
            final Door door = (Door) obj;
            if (door.getOwner().getParty() != null
                    && owner.getParty().containsMembers(new PartyMember(door.getOwner()))) {
                freePortals.remove(door.getTownPortal());
            }
        }
        return freePortals.iterator().next();
    }

    @Override
    public final void sendSpawnData(final GameClient client) {
        if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() && owner.getParty() == null) {
            client.write(MaplePacketCreator.spawnDoor(owner.getId(), town.getId() == client.getPlayer().getMapId() ? townPortal.getPosition() : targetPosition, true));
            if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(new PartyMember(client.getPlayer())))) {
                client.write(MaplePacketCreator.partyPortal(town.getId(), target.getId(), targetPosition));
            }
            client.write(MaplePacketCreator.spawnPortal(town.getId(), target.getId(), targetPosition));
        }
    }

    @Override
    public final void sendDestroyData(final GameClient client) {
        if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() || owner.getParty() != null && owner.getParty().containsMembers(new PartyMember(client.getPlayer()))) {
            if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(new PartyMember(client.getPlayer())))) {
                client.write(MaplePacketCreator.partyPortal(999999999, 999999999, new Point(-1, -1)));
            }
            client.write(MaplePacketCreator.removeDoor(owner.getId(), false));
            client.write(MaplePacketCreator.removeDoor(owner.getId(), true));
        }
    }

    public final void warp(final GameCharacter chr, final boolean toTown) {
        if (chr == owner || owner.getParty() != null && owner.getParty().containsMembers(new PartyMember(chr))) {
            if (!toTown) {
                chr.changeMap(target, targetPosition);
            } else {
                chr.changeMap(town, townPortal);
            }
        } else {
            chr.getClient().write(MaplePacketCreator.enableActions());
        }
    }

    public final GameCharacter getOwner() {
        return owner;
    }

    public final GameMap getTown() {
        return town;
    }

    public final Portal getTownPortal() {
        return townPortal;
    }

    public final GameMap getTarget() {
        return target;
    }

    public final Point getTargetPosition() {
        return targetPosition;
    }

    @Override
    public final GameMapObjectType getType() {
        return GameMapObjectType.DOOR;
    }
}
