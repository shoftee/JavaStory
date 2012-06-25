package javastory.channel;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javastory.channel.client.SkillMacro;
import javastory.tools.packets.ChannelPackets;

public class SkillMacroSet implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5584030461689882224L;
	
	private final SkillMacro[] items;

	public SkillMacroSet() {
		this.items = new SkillMacro[5];
	}

	public SkillMacroSet(final ResultSet rs) throws SQLException {
		final SkillMacro[] loadedItems = new SkillMacro[5];
		while (rs.next()) {
			final int skillId1 = rs.getInt("skill1");
			final int skillId2 = rs.getInt("skill2");
			final int skillId3 = rs.getInt("skill3");
			final String name = rs.getString("name");
			final int shout = rs.getInt("shout");
			final int position = rs.getInt("position");

			final SkillMacro macro = new SkillMacro(skillId1, skillId2, skillId3, name, shout, position);
			loadedItems[position] = macro;
		}

		this.items = loadedItems;
	}

	public void send(final ChannelClient client) {
		for (int i = 0; i < 5; i++) {
			if (this.items[i] != null) {
				client.write(ChannelPackets.getMacros(this.items));
				break;
			}
		}
	}

	public void update(final int position, final SkillMacro newMacro) {
		this.items[position] = newMacro;
	}
	
	public void saveToDb(final int characterId, final Connection connection) throws SQLException {
		try (final PreparedStatement ps = getInsertSkillMacros(characterId, connection)) {
			for (int i = 0; i < 5; i++) {
				final SkillMacro macro = this.items[i];
				if (macro == null) {
					continue;
				}

				ps.setInt(2, macro.getSkill1());
				ps.setInt(3, macro.getSkill2());
				ps.setInt(4, macro.getSkill3());
				ps.setString(5, macro.getName());
				ps.setInt(6, macro.getShout());
				ps.setInt(7, i);
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSkillMacros(final int characterId, final Connection connection) throws SQLException {
		final String sql = "INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, characterId);
		return ps;
	}
}
