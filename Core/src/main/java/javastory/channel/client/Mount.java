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
    private int itemId, skillId, fatigue, exp, level;
    private transient boolean changed = false;
    private transient WeakReference<ChannelCharacter> owner;

    public Mount(ChannelCharacter owner, int id, int skillid, int fatigue, int level, int exp) {
        this.itemId = id;
        this.skillId = skillid;
        this.fatigue = fatigue;
        this.level = level;
        this.exp = exp;
        this.owner = new WeakReference<>(owner);
    }

    public void saveMount(final int characterId) throws SQLException {
        if (!changed) {
            return;
        }
        Connection con = Database.getConnection();
        try (PreparedStatement ps = con.prepareStatement("UPDATE mountdata set `Level` = ?, `Exp` = ?, `Fatigue` = ? WHERE characterid = ?")) {
            ps.setInt(1, level);
            ps.setInt(2, exp);
            ps.setInt(3, fatigue);
            ps.setInt(4, characterId);
        }
    }

    public int getId() {
        switch (itemId) {
            case 1902000:
            case 1902001:
            case 1902002:
                return itemId - 1901999;
            case 1902005:
            case 1902006:
            case 1902007:
                return itemId - 1902004;
            case 1902015:
            case 1902016:
            case 1902017:
            case 1902018:
                return itemId - 1902014;
            case 1902040:
            case 1902041:
            case 1902042:
                return itemId - 1902039;
            default:
                return 4;
        }
    }

    public int getItemId() {
        return itemId;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getFatigue() {
        return fatigue;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public void setItemId(int c) {
        changed = true;
        this.itemId = c;
    }

    public void setFatigue(int amount) {
        changed = true;
        fatigue += amount;
        if (fatigue < 0) {
            fatigue = 0;
        }
    }

    public void setExp(int c) {
        changed = true;
        this.exp = c;
    }

    public void setLevel(int c) {
        changed = true;
        this.level = c;
    }

    public void increaseFatigue() {
        changed = true;
        this.fatigue++;
        if (fatigue > 100 && owner.get() != null) {
            owner.get().dispelSkill(1004);
        }
        update();
    }

    public void increaseExp() {
        int e;
        if (level >= 1 && level <= 7) {
            e = Randomizer.nextInt(10) + 15;
        } else if (level >= 8 && level <= 15) {
            e = Randomizer.nextInt(13) + 15 / 2;
        } else if (level >= 16 && level <= 24) {
            e = Randomizer.nextInt(23) + 18 / 2;
        } else {
            e = Randomizer.nextInt(28) + 25 / 2;
        }
        setExp(exp + e);
    }

    public void update() {
        final ChannelCharacter chr = owner.get();
        if (chr != null && chr != null) {
//	    cancelSchedule();
            chr.getMap().broadcastMessage(ChannelPackets.updateMount(chr, false));
        }
    }
}
