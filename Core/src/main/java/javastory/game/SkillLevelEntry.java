/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.game;

/**
 * 
 * @author shoftee
 */
public class SkillLevelEntry {

	public int skill;
	public int level;

	public SkillLevelEntry(final int skill, final int level) {
		this.skill = skill;
		this.level = level;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() == obj.getClass()) {
			return false;
		}
		final SkillLevelEntry other = (SkillLevelEntry) obj;
		return this.skill == other.skill && this.level == other.level;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 17 * hash + this.skill;
		hash = 17 * hash + this.level;
		return hash;
	}
}
