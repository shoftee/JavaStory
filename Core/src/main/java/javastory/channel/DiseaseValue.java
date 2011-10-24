package javastory.channel;

import javastory.channel.client.Disease;

/**
 * 
 * @author shoftee
 */
public class DiseaseValue {
	private Disease disease;
	private int value;

	public DiseaseValue(Disease disease, int value) {
		this.disease = disease;
		this.value = value;
	}

	public Disease getDisease() {
		return disease;
	}

	public int getValue() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		DiseaseValue other = (DiseaseValue) obj;
		return this.disease.equals(other.disease);
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 79 * hash + (this.disease != null ? this.disease.hashCode() : 0);
		return hash;
	}

}
