package javastory.game;

public enum LifeType {
	NPC('n'), MONSTER('m');

	private char typeChar;

	private LifeType(final char c) {
		this.typeChar = c;
	}

	public char toChar() {
		return this.typeChar;
	}

	public static LifeType fromChar(final char c) {
		switch (c) {
		case 'n':
			return NPC;
		case 'm':
			return MONSTER;
		default:
			return null;
		}
	}
}
