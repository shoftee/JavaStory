package javastory.game.data;

/**
 * 
 * @author shoftee
 */
public class MobAttackId {

	public final int MobId;
	public final int AttackId;

	public MobAttackId(final int id, final int attack) {
		this.MobId = id;
		this.AttackId = attack;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final MobAttackId other = (MobAttackId) obj;
		return this.MobId == other.MobId && this.AttackId == other.AttackId;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + this.MobId;
		hash = 71 * hash + this.AttackId;
		return hash;
	}

}
