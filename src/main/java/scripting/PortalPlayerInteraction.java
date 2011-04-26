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
package scripting;

import client.ChannelClient;
import server.Portal;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {

    private final Portal portal;

    public PortalPlayerInteraction(final ChannelClient c, final Portal portal) {
	super(c);
	this.portal = portal;
    }

    public final Portal getPortal() {
	return portal;
    }

    public final void inFreeMarket() {
	if (getMapId() != 910000000) {
	    if (getPlayer().getLevel() > 10) {
		saveLocation("FREE_MARKET");
		playPortalSE();
		warp(910000000, "st00");
	    } else {
		playerMessage(5, "You must be over level 10 to enter here.");
	    }
	}
    }
}
