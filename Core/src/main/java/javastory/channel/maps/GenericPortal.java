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

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.anticheat.CheatingOffense;
import javastory.channel.server.Portal;
import javastory.scripting.PortalScriptManager;
import javastory.tools.packets.ChannelPackets;

public class GenericPortal implements Portal {

	private String name, target, scriptName;
	private Point position;
	private int targetmap, type, id;

	public GenericPortal(final int type) {
		this.type = type;
	}

	@Override
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final Point getPosition() {
		return position;
	}

	@Override
	public final String getTarget() {
		return target;
	}

	@Override
	public final int getTargetMapId() {
		return targetmap;
	}

	@Override
	public final int getType() {
		return type;
	}

	@Override
	public final String getScriptName() {
		return scriptName;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final void setPosition(final Point position) {
		this.position = position;
	}

	public final void setTarget(final String target) {
		this.target = target;
	}

	public final void setTargetMapId(final int targetmapid) {
		this.targetmap = targetmapid;
	}

	@Override
	public final void setScriptName(final String scriptName) {
		this.scriptName = scriptName;
	}

	@Override
	public final void enterPortal(final ChannelClient c) {
		final ChannelCharacter player = c.getPlayer();

		if (getPosition().distanceSq(player.getPosition()) > 22500) {
			player.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
		}
		if (getScriptName() != null) {
			final GameMap currentmap = player.getMap();
			try {
				PortalScriptManager.getInstance().executePortalScript(this, c);
				if (player.getMap() == currentmap) { // Character is still on
														// the same map.
					c.write(ChannelPackets.enableActions());
				}
			} catch (final Exception e) {
				c.write(ChannelPackets.enableActions());
				e.printStackTrace();
			}
		} else if (getTargetMapId() != 999999999) {
			final GameMap to = ChannelServer.getMapFactory().getMap(getTargetMapId());
			player.changeMap(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()));
		}
	}
}