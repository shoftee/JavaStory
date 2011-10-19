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
package javastory.scripting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javastory.channel.ChannelClient;
import javastory.channel.maps.Reactor;
import javastory.channel.maps.ReactorDropEntry;
import javastory.db.DatabaseConnection;

import javax.script.Invocable;
import javax.script.ScriptEngine;


public final class ReactorScriptManager extends AbstractScriptManager {

    private static final ReactorScriptManager instance = new ReactorScriptManager();
    private final Map<Integer, List<ReactorDropEntry>> drops = new HashMap<>();

    public static ReactorScriptManager getInstance() {
	return instance;
    }

    public final void act(final ChannelClient c, final Reactor reactor) {
	try {
	    final Invocable iv = getInvocable("reactor/" + reactor.getReactorId() + ".js", c);
	    final ScriptEngine scriptengine = (ScriptEngine) iv;
	    if (iv == null) {
		return;
	    }
	    ReactorActionManager rm = new ReactorActionManager(c, reactor);

	    scriptengine.put("rm", rm);
	    ReactorScript rs = iv.getInterface(ReactorScript.class);
	    rs.act();
	} catch (Exception e) {
	    System.err.println("Error executing reactor script." + e);
	}
    }

    public final List<ReactorDropEntry> getDrops(final int rid) {
	List<ReactorDropEntry> ret = drops.get(rid);
	if (ret != null) {
	    return ret;
	}
	ret = new LinkedList<>();

	PreparedStatement ps = null;
	ResultSet rs = null;

	try {
	    Connection con = DatabaseConnection.getConnection();
	    ps = con.prepareStatement("SELECT * FROM reactordrops WHERE reactorid = ?");
	    ps.setInt(1, rid);
	    rs = ps.executeQuery();

	    while (rs.next()) {
		ret.add(new ReactorDropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("questid")));
	    }
	    rs.close();
	    ps.close();
	} catch (final SQLException e) {
	    System.err.println("Could not retrieve drops for reactor " + rid + e);
	    return ret;
	} finally {
	    try {
		if (rs != null) {
		    rs.close();
		}
		if (ps != null) {
		    ps.close();
		}
	    } catch (SQLException ignore) {
		return ret;
	    }
	}
	drops.put(rid, ret);
	return ret;
    }

    public final void clearDrops() {
	drops.clear();
    }
}
