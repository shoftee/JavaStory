package javastory.channel;

import javastory.channel.client.Disease;

/**
 * 
 * @author shoftee
 */
public class DiseaseValue {
	private final Disease disease;
	private final int value;

	public DiseaseValue(final Disease disease, final int value) {
		this.disease = disease;
		this.value = value;
	}

	public Disease getDisease() {
		return this.disease;
	}

	public int getValue() {
		return this.value;
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
		final DiseaseValue other = (DiseaseValue) obj;
		return this.disease.equals(other.disease);
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 79 * hash + (this.disease != null ? this.disease.hashCode() : 0);
		return hash;
	}

}
