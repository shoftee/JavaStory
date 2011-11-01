/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.channel.maps;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.PartyMember;
import javastory.channel.server.Portal;
import javastory.tools.packets.ChannelPackets;

public class Door extends AbstractGameMapObject {

	private final ChannelCharacter owner;
	private final GameMap town;
	private Portal townPortal;
	private final GameMap target;
	private final Point targetPosition;

	public Door(final ChannelCharacter owner, final Point targetPosition) {
		super();
		this.owner = owner;
		this.target = owner.getMap();
		this.targetPosition = targetPosition;
		this.setPosition(this.targetPosition);
		this.town = this.target.getReturnMap();
		this.townPortal = this.getFreePortal();
	}

	public Door(final Door origDoor) {
		super();
		this.owner = origDoor.owner;
		this.town = origDoor.town;
		this.townPortal = origDoor.townPortal;
		this.target = origDoor.target;
		this.targetPosition = origDoor.targetPosition;
		this.townPortal = origDoor.townPortal;
		this.setPosition(this.townPortal.getPosition());
	}

	private Portal getFreePortal() {
		final List<Portal> freePortals = Lists.newArrayList();

		for (final Portal port : this.town.getPortals()) {
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
		for (final GameMapObject obj : this.town.getAllDoor()) {
			final Door door = (Door) obj;
			final ChannelCharacter doorOwner = door.getOwner();
			final PartyMember doorOwnerMember = doorOwner.getPartyMembership();
			if (doorOwnerMember != null) {
				freePortals.remove(door.getTownPortal());
			}
		}
		return freePortals.iterator().next();
	}

	@Override
	public final void sendSpawnData(final ChannelClient client) {
		final ChannelCharacter clientPlayer = client.getPlayer();

		final boolean isInDoorMap = this.target.getId() == clientPlayer.getMapId();
		final boolean isOwner = this.owner == clientPlayer;

		final PartyMember ownerMember = this.owner.getPartyMembership();
		final PartyMember clientMember = clientPlayer.getPartyMembership();

		final Point doorPosition = this.town.getId() == clientPlayer.getMapId() ? this.townPortal.getPosition() : this.targetPosition;
		if (isInDoorMap) {
			client.write(ChannelPackets.spawnDoor(this.owner.getId(), doorPosition, true));
			if (isOwner && ownerMember == null) {
				client.write(ChannelPackets.spawnPortal(this.town.getId(), this.target.getId(), this.targetPosition));
			} else if (ownerMember != null && clientMember.getPartyId() == ownerMember.getPartyId()) {
				client.write(ChannelPackets.partyPortal(this.town.getId(), this.target.getId(), this.targetPosition));
			}
		}
	}

	@Override
	public final void sendDestroyData(final ChannelClient client) {
		final ChannelCharacter clientPlayer = client.getPlayer();

		final boolean isInDoorMap = this.target.getId() == clientPlayer.getMapId();

		final PartyMember ownerMember = this.owner.getPartyMembership();
		final PartyMember clientMember = clientPlayer.getPartyMembership();

		if (isInDoorMap) {
			if (ownerMember == null || ownerMember.getPartyId() == clientMember.getPartyId()) {
				client.write(ChannelPackets.partyPortal(999999999, 999999999, new Point(-1, -1)));

			}
			client.write(ChannelPackets.removeDoor(this.owner.getId(), false));
			client.write(ChannelPackets.removeDoor(this.owner.getId(), true));
		}
	}

	public final void warp(final ChannelCharacter chr, final boolean toTown) {
		final PartyMember ownerMember = this.owner.getPartyMembership();
		final PartyMember clientMember = chr.getPartyMembership();

		final boolean isSameParty = ownerMember != null && clientMember != null && ownerMember.getPartyId() == clientMember.getPartyId();
		final boolean isOwner = this.owner == chr;

		if (isOwner || isSameParty) {
			if (!toTown) {
				chr.changeMap(this.target, this.targetPosition);
			} else {
				chr.changeMap(this.town, this.townPortal);
			}
		} else {
			chr.getClient().write(ChannelPackets.enableActions());
		}
	}

	public final ChannelCharacter getOwner() {
		return this.owner;
	}

	public final GameMap getTown() {
		return this.town;
	}

	public final Portal getTownPortal() {
		return this.townPortal;
	}

	public final GameMap getTarget() {
		return this.target;
	}

	public final Point getTargetPosition() {
		return this.targetPosition;
	}

	@Override
	public final GameMapObjectType getType() {
		return GameMapObjectType.DOOR;
	}
}
