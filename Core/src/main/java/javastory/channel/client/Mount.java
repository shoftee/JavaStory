package javastory.channel.client;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javastory.channel.ChannelCharacter;
import javastory.db.Database;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

public class Mount implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;
	private int itemId;
	private final int skillId;
	private int fatigue;
	private int exp;
	private int level;
	private transient boolean changed = false;
	private transient WeakReference<ChannelCharacter> owner;

	public Mount(final ChannelCharacter owner, final int id, final int skillid, final int fatigue, final int level, final int exp) {
		this.itemId = id;
		this.skillId = skillid;
		this.fatigue = fatigue;
		this.level = level;
		this.exp = exp;
		this.owner = new WeakReference<>(owner);
	}

	public void saveMount(final int characterId) throws SQLException {
		if (!this.changed) {
			return;
		}
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE mountdata set `Level` = ?, `Exp` = ?, `Fatigue` = ? WHERE characterid = ?")) {
			ps.setInt(1, this.level);
			ps.setInt(2, this.exp);
			ps.setInt(3, this.fatigue);
			ps.setInt(4, characterId);
		}
	}

	public int getId() {
		switch (this.itemId) {
		case 1902000:
		case 1902001:
		case 1902002:
			return this.itemId - 1901999;
		case 1902005:
		case 1902006:
		case 1902007:
			return this.itemId - 1902004;
		case 1902015:
		case 1902016:
		case 1902017:
		case 1902018:
			return this.itemId - 1902014;
		case 1902040:
		case 1902041:
		case 1902042:
			return this.itemId - 1902039;
		default:
			return 4;
		}
	}

	public int getItemId() {
		return this.itemId;
	}

	public int getSkillId() {
		return this.skillId;
	}

	public int getFatigue() {
		return this.fatigue;
	}

	public int getExp() {
		return this.exp;
	}

	public int getLevel() {
		return this.level;
	}

	public void setItemId(final int c) {
		this.changed = true;
		this.itemId = c;
	}

	public void setFatigue(final int amount) {
		this.changed = true;
		this.fatigue += amount;
		if (this.fatigue < 0) {
			this.fatigue = 0;
		}
	}

	public void setExp(final int c) {
		this.changed = true;
		this.exp = c;
	}

	public void setLevel(final int c) {
		this.changed = true;
		this.level = c;
	}

	public void increaseFatigue() {
		this.changed = true;
		this.fatigue++;
		if (this.fatigue > 100 && this.owner.get() != null) {
			this.owner.get().dispelSkill(1004);
		}
		this.update();
	}

	public void increaseExp() {
		int e;
		if (this.level >= 1 && this.level <= 7) {
			e = Randomizer.nextInt(10) + 15;
		} else if (this.level >= 8 && this.level <= 15) {
			e = Randomizer.nextInt(13) + 15 / 2;
		} else if (this.level >= 16 && this.level <= 24) {
			e = Randomizer.nextInt(23) + 18 / 2;
		} else {
			e = Randomizer.nextInt(28) + 25 / 2;
		}
		this.setExp(this.exp + e);
	}

	public void update() {
		final ChannelCharacter chr = this.owner.get();
		if (chr != null) {
			chr.getMap().broadcastMessage(ChannelPackets.updateMount(chr, false));
		}
	}
}
