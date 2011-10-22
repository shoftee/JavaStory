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
package javastory.channel.client;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javastory.client.GameClient;
import javastory.db.Database;
import javastory.game.GameConstants;
import javastory.game.ItemInfoProvider;
import javastory.io.PacketBuilder;
import javastory.tools.packets.MonsterBookPacket;

import com.google.common.collect.Maps;

public class MonsterBook implements Serializable {

	private static final long serialVersionUID = 7179541993413738569L;
	private boolean changed = false;
	private int specialCardCount = 0, normalCardCount = 0, bookLevel = 1;
	private final Map<Integer, Integer> cards = Maps.newLinkedHashMap();

	public final int getTotalCards() {
		return specialCardCount + normalCardCount;
	}

	public static MonsterBook loadFromDb(final int characterId)
			throws SQLException {
		final Connection connection = Database.getConnection();

		final MonsterBook instance = new MonsterBook();
		try (PreparedStatement ps = connection
				.prepareStatement("SELECT * FROM monsterbook WHERE charid = ? ORDER BY cardid ASC")) {
			ps.setInt(1, characterId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final int cardId = rs.getInt("cardid");
					final int level = rs.getInt("level");

					if (GameConstants.isSpecialCard(cardId)) {
						instance.specialCardCount += level;
					} else {
						instance.normalCardCount += level;
					}
					instance.cards.put(cardId, level);
				}
			}
		}

		instance.calculateLevel();
		return instance;
	}

	public final void saveCards(final int charid) throws SQLException {
		if (!changed || cards.isEmpty()) {
			return;
		}
		final Connection con = Database.getConnection();
		PreparedStatement ps = con
				.prepareStatement("DELETE FROM monsterbook WHERE charid = ?");
		ps.setInt(1, charid);
		ps.execute();
		ps.close();

		boolean first = true;
		final StringBuilder query = new StringBuilder();

		for (final Entry<Integer, Integer> all : cards.entrySet()) {
			if (first) {
				first = false;
				query.append("INSERT INTO monsterbook VALUES (DEFAULT,");
			} else {
				query.append(",(DEFAULT,");
			}
			query.append(charid);
			query.append(",");
			query.append(all.getKey()); // Card ID
			query.append(",");
			query.append(all.getValue()); // Card level
			query.append(")");
		}
		ps = con.prepareStatement(query.toString());
		ps.execute();
		ps.close();
	}

	private void calculateLevel() {
		int Size = normalCardCount + specialCardCount;
		bookLevel = 8;

		for (int i = 0; i < 8; i++) {
			if (Size <= GameConstants.getBookLevel(i)) {
				bookLevel = (i + 1);
				break;
			}
		}
	}

	public final void addCardPacket(final PacketBuilder builder) {
		builder.writeAsShort(cards.size());

		for (Entry<Integer, Integer> all : cards.entrySet()) {
			builder.writeAsShort(GameConstants.getCardShortId(all.getKey())); // Id
			builder.writeAsByte(all.getValue()); // Level
		}
	}

	public final void addCharInfoPacket(final int bookcover,
			final PacketBuilder builder) {
		builder.writeInt(bookLevel);
		builder.writeInt(normalCardCount);
		builder.writeInt(specialCardCount);
		builder.writeInt(normalCardCount + specialCardCount);
		builder.writeInt(ItemInfoProvider.getInstance().getCardMobId(bookcover));
	}

	public final void updateCard(final GameClient c, final int cardid) {
		c.write(MonsterBookPacket.changeCover(cardid));
	}

	public final void addCard(final GameClient c, final int cardid) {
		changed = true;

		for (final Entry<Integer, Integer> all : cards.entrySet()) {
			if (all.getKey() == cardid) {

				if (all.getValue() >= 5) {
					c.write(MonsterBookPacket.addCard(true, cardid, all
							.getValue()));
				} else {
					c.write(MonsterBookPacket.addCard(false, cardid, all
							.getValue()));
					c.write(MonsterBookPacket.showGainCard(cardid));
					all.setValue(all.getValue() + 1);
					calculateLevel();
				}
				return;
			}
		}
		// New card
		cards.put(cardid, 1);
		c.write(MonsterBookPacket.addCard(false, cardid, 1));
		c.write(MonsterBookPacket.showGainCard(cardid));
		calculateLevel();
	}
}