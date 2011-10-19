/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 * Matthias Butz <matze@odinms.de>
 * Jan Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation. You may not use, modify
 * or distribute this program under any other version of the
 * GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package client;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javastory.db.DatabaseConnection;
import javastory.io.PacketBuilder;

public class KeyLayout implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;
	private boolean changed = false;
	private final Map<Integer, KeyBinding> keymap = new HashMap<>();

	public final Map<Integer, KeyBinding> Layout() {
		changed = true;
		return keymap;
	}

	public final void writeData(final PacketBuilder builder) {
		KeyBinding binding;
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

	public static KeyLayout loadFromDb(int characterId) throws SQLException {

		Connection con = DatabaseConnection.getConnection();

		KeyLayout instance = new KeyLayout();
		try (PreparedStatement ps = con
				.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?")) {

			ps.setInt(1, characterId);
			try (ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					final Integer keyId = Integer.valueOf(rs.getInt("key"));
					final int type = rs.getInt("type");
					final int action = rs.getInt("action");
					final KeyBinding binding = new KeyBinding(type, action);
					instance.keymap.put(keyId, binding);
				}
			}
		}
		return instance;
	}

	public final void saveKeys(final int charid) throws SQLException {
		if (!changed || keymap.isEmpty()) {
			return;
		}
		Connection con = DatabaseConnection.getConnection();

		PreparedStatement ps = con
				.prepareStatement("DELETE FROM keymap WHERE characterid = ?");
		ps.setInt(1, charid);
		ps.execute();
		ps.close();

		boolean first = true;
		StringBuilder query = new StringBuilder();

		for (Entry<Integer, KeyBinding> keybinding : keymap.entrySet()) {
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
