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

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.Serializable;

import org.javastory.io.PacketBuilder;

import database.DatabaseConnection;

public class MapleKeyLayout implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private boolean changed = false;
    private final Map<Integer, MapleKeyBinding> keymap = new HashMap<Integer, MapleKeyBinding>();

    public final Map<Integer, MapleKeyBinding> Layout() {
	changed = true;
	return keymap;
    }

    public final void writeData(final PacketBuilder builder) {
	MapleKeyBinding binding;
	for (int x = 0; x < 90; x++) {
	    binding = keymap.get(Integer.valueOf(x));
	    if (binding != null) {
		builder.writeAsByte(binding.getType());
		builder.writeInt(binding.getAction());
	    } else {
		builder.writeAsByte(0);
		builder.writeInt(0);
	    }
	}
    }

    public final void saveKeys(final int charid) throws SQLException {
	if (!changed || keymap.size() == 0) {
	    return;
	}
	Connection con = DatabaseConnection.getConnection();

	PreparedStatement ps = con.prepareStatement("DELETE FROM keymap WHERE characterid = ?");
	ps.setInt(1, charid);
	ps.execute();
	ps.close();

	boolean first = true;
	StringBuilder query = new StringBuilder();

	for (Entry<Integer, MapleKeyBinding> keybinding : keymap.entrySet()) {
	    if (first) {
		first = false;
		query.append("INSERT INTO keymap VALUES (");
	    } else {
		query.append(",(");
	    }
	    query.append("DEFAULT,");
	    query.append(charid).append(",");
	    query.append(keybinding.getKey().intValue()).append(",");
	    query.append(keybinding.getValue().getType()).append(",");
	    query.append(keybinding.getValue().getAction()).append(")");
	}
	ps = con.prepareStatement(query.toString());
	ps.execute();
	ps.close();
    }
}
