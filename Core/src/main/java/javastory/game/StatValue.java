/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.game;


/**
 * 
 * @author shoftee
 */
public class StatValue {
	public Stat stat;
	public int value;

	public StatValue(final Stat stat, final int value) {
		this.stat = stat;
		this.value = value;
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
		final StatValue other = (StatValue) obj;
		return this.stat == other.stat && this.value == other.value;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 43 * hash + (this.stat != null ? this.stat.hashCode() : 0);
		hash = 43 * hash + this.value;
		return hash;
	}
}
