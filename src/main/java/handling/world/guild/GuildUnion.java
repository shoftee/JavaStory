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
package handling.world.guild;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.rmi.RemoteException;

import database.DatabaseConnection;

import client.GameClient;
import com.google.common.collect.Maps;
import handling.GamePacket;
import java.util.Map;
import org.javastory.client.MemberRank;

public class GuildUnion implements java.io.Serializable {

    private List<Guild> guilds = new ArrayList<Guild>();
    private final Map<MemberRank, String> rankTitles = Maps.newEnumMap(MemberRank.class);
    private int guildIds[] = new int[5];
    private String name;
    private String notice;
    private int id;

    public GuildUnion(GameClient c, int id) {
        guilds = new ArrayList<Guild>();
        this.id = id;

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliances WHERE id = ? ");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.first()) { // no result... most likely to be someone from a disbanded alliance that got rolled back
                rs.close();
                ps.close();
                return;
            }
            notice = rs.getString("notice");
            name = rs.getString("name");

            for (int i = 1; i <= 5; i++) {
                guildIds[i - 1] = rs.getInt("guild" + i);
                final MemberRank rank = MemberRank.fromNumber(i);
                rankTitles.put(rank, rs.getString("rank" + i));
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("loading Guild info failed " + ex);
        }

        for (int i = 1; i <= 5; i++) {
            final int guildId = guildIds[i - 1];
            if (guildId != 0) {
                if (c != null && guildId == c.getPlayer().getGuildId()) {
                    try {
                        guilds.add(c.getChannelServer().getWorldInterface().getGuild(guildId, c.getPlayer().getGuildMembership()));
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                } else {
                    guilds.add(new Guild(guildId));
                }
            } else {
                guilds.add(null);
            }
        }
    }

    public void addGuild(GameClient c, int guildId) {
        for (int i = 0; i < 5; i++) {
            if (guilds.get(i) == null) {
                try {
                    guilds.add(c.getChannelServer().getWorldInterface().getGuild(guildId, c.getPlayer().getGuildMembership()));
                } catch (Exception e) {
                    c.getChannelServer().pingWorld();
                }
                broadcast(null);
                break;
            }
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE alliances SET guild1 = ?, guild2 = ?, guild3 = ?, guild4 = ?, guild5 = ?, rank1 = ?, rank2 = ?, rank3 = ?, rank4 = ?, rank5 = ?, notice = ? WHERE id = ?");
            ps.setInt(1, guilds.get(0) != null ? guilds.get(0).getId() : 0);
            ps.setInt(2, guilds.get(1) != null ? guilds.get(1).getId() : 0);
            ps.setInt(3, guilds.get(2) != null ? guilds.get(2).getId() : 0);
            ps.setInt(4, guilds.get(3) != null ? guilds.get(3).getId() : 0);
            ps.setInt(5, guilds.get(4) != null ? guilds.get(4).getId() : 0);
            ps.setString(6, rankTitles.get(MemberRank.MASTER));
            ps.setString(7, rankTitles.get(MemberRank.JR_MASTER));
            ps.setString(8, rankTitles.get(MemberRank.MEMBER_HIGH));
            ps.setString(9, rankTitles.get(MemberRank.MEMBER_MIDDLE));
            ps.setString(10, rankTitles.get(MemberRank.MEMBER_LOW));
            ps.setString(11, notice);
            ps.setInt(12, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error while saving alliances" + e);
        }
    }

    public void setTitles(String[] titles) {
        for (int i = 1; i <= 5; i++) {
            final MemberRank rank = MemberRank.fromNumber(i);
            rankTitles.put(rank, titles[i - 1]);
        }
        broadcast(null);

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE alliances SET rank1 = ?, rank2 = ?, rank3 = ?, rank4 = ?, rank5 = ? WHERE id = ?");
            ps.setString(1, rankTitles.get(MemberRank.MASTER));
            ps.setString(2, rankTitles.get(MemberRank.JR_MASTER));
            ps.setString(3, rankTitles.get(MemberRank.MEMBER_HIGH));
            ps.setString(4, rankTitles.get(MemberRank.MEMBER_MIDDLE));
            ps.setString(5, rankTitles.get(MemberRank.MEMBER_LOW));
            ps.setInt(6, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error while saving alliance titles" + e);
        }
    }

    public void broadcast(GamePacket packet) {
        for (int i = 0; i < 5; i++) {
            if (guilds.get(i) != null) {
                guilds.get(i).guildMessage(packet);
            }
        }
    }

    public Iterable<String> getTitles() {
        return rankTitles.values();
    }

    public int getId() {
        return id;
    }

    public void setId(int idd) {
        this.id = idd;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public void setNotice(String noticee) {
        this.notice = noticee;
        broadcast(null);

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE alliances SET notice = ? WHERE id = ?");
            ps.setString(1, notice);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error while saving alliance notice" + e);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String namee) {
        this.name = namee;
    }

    public List<Guild> getGuilds() {
        return guilds;
    }

    public void broadcastMessage(GamePacket packet) {
        for (int i = 0; i < 5; i++) {
            if (guilds.get(i) != null) {
                guilds.get(i).broadcast(packet);
            }
        }
    }

    public int getAmountOfGuilds() {
        int a = 0;
        for (int i = 0; i < 5; i++) {
            if (guilds.get(i) != null) {
                a++;
            }
        }
        return a;
    }
}
