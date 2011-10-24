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
package javastory.server.channel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javastory.db.Database;

public class GuildRanking {

	private static GuildRanking instance = new GuildRanking();
	private List<GuildRankingInfo> ranks = new LinkedList<GuildRankingInfo>();
	private long lastUpdate = System.currentTimeMillis();
	private boolean hasLoaded = false;

	public static GuildRanking getInstance() {
		return instance;
	}

	public List<GuildRankingInfo> getRank() {
		if ((ranks.isEmpty() && !hasLoaded) || (System.currentTimeMillis() - lastUpdate) > 3600000) {
			hasLoaded = true; // TO prevent loading when there's no guild for the server
			reload();
		}
		return ranks;
	}

	private void reload() {
		ranks.clear();
		try {
			Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds ORDER BY `GP` DESC LIMIT 50");
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				final GuildRankingInfo rank = new GuildRankingInfo(rs.getString("name"), rs.getInt("GP"), rs.getInt("logo"), rs.getInt("logoColor"), rs
					.getInt("logoBG"), rs.getInt("logoBGColor"));

				ranks.add(rank);
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {
			System.err.println("Error handling guildRanking" + e);
		}
		lastUpdate = System.currentTimeMillis();
	}
}
