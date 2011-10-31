package javastory.server;

import javastory.channel.client.BuffStat;

/**
 * 
 * @author shoftee
 */
public class BuffStatValue {

	public BuffStat stat;
	public int value;

	public BuffStatValue(final BuffStat stat, final int value) {
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
		final BuffStatValue other = (BuffStatValue) obj;
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
