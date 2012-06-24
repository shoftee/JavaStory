package javastory.server;

import java.io.Serializable;

import javastory.channel.client.BuffStat;

/**
 * 
 * @author shoftee
 */
public class BuffStatValue implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7052182724878936305L;
	
	public final BuffStat Stat;
	public final int Value;

	public BuffStatValue(final BuffStat stat, final int value) {
		this.Stat = stat;
		this.Value = value;
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
		return this.Stat == other.Stat && this.Value == other.Value;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 43 * hash + (this.Stat != null ? this.Stat.hashCode() : 0);
		hash = 43 * hash + this.Value;
		return hash;
	}
}
