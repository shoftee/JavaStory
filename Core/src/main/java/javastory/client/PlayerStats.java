package javastory.client;

import javastory.io.PacketBuilder;

/**
 * 
 * @author shoftee
 */
public class PlayerStats implements PacketWritable {

	public int STR, DEX, LUK, INT, HP, MaxHP, MP, MaxMP;

	public final int getDex() {
		return DEX;
	}

	public final int getHp() {
		return HP;
	}

	public final int getInt() {
		return INT;
	}

	public final int getLuk() {
		return LUK;
	}

	public final int getMaxHp() {
		return MaxHP;
	}

	public final int getMaxMp() {
		return MaxMP;
	}

	public final int getMp() {
		return MP;
	}

	public final int getStr() {
		return STR;
	}

	@Override
	public void connectData(final PacketBuilder builder) {
		builder.writeAsShort(STR);
		builder.writeAsShort(DEX);
		builder.writeAsShort(INT);
		builder.writeAsShort(LUK);
		builder.writeAsShort(HP);
		builder.writeAsShort(MaxHP);
		builder.writeAsShort(MP);
		builder.writeAsShort(MaxMP);
	}
}
