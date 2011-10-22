package javastory.game;

public enum LifeType {
	NPC('n'), MONSTER('m');

	private char typeChar;

	private LifeType(char c) {
		this.typeChar = c;
	}

	public char toChar() {
		return this.typeChar;
	}

	public static LifeType fromChar(char c) {
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
