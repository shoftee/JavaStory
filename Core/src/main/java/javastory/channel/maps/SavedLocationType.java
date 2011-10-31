package javastory.channel.maps;

public enum SavedLocationType {

	FREE_MARKET(0), MIRROR_OF_DIMENSION(1), WORLDTOUR(2), FLORINA(3), FISHING(4), RICHIE(5), DONGDONGCHIANG(6), AMORIA(7);

	private int value;

	private SavedLocationType(final int value) {
		this.value = value;
	}

	public int asNumber() {
		return this.value;
	}

	public static SavedLocationType fromNumber(final int value) {
		switch (value) {
		case 0:
			return FREE_MARKET;
		case 1:
			return MIRROR_OF_DIMENSION;
		case 2:
			return WORLDTOUR;
		case 3:
			return FLORINA;
		case 4:
			return FISHING;
		case 5:
			return RICHIE;
		case 6:
			return DONGDONGCHIANG;
		case 7:
			return AMORIA;
		default:
			return null;
		}
	}

	public static SavedLocationType fromString(final String Str) {
		return valueOf(Str);
	}
}