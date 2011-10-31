package javastory.game;

import javastory.client.PacketWritable;
import javastory.io.PacketBuilder;

/**
 * 
 * @author shoftee
 */
public class PlayerStats implements PacketWritable {

	public int STR, DEX, LUK, INT, HP, MaxHP, MP, MaxMP;

	public final int getDex() {
		return this.DEX;
	}

	public final int getHp() {
		return this.HP;
	}

	public final int getInt() {
		return this.INT;
	}

	public final int getLuk() {
		return this.LUK;
	}

	public final int getMaxHp() {
		return this.MaxHP;
	}

	public final int getMaxMp() {
		return this.MaxMP;
	}

	public final int getMp() {
		return this.MP;
	}

	public final int getStr() {
		return this.STR;
	}

	@Override
	public void connectData(final PacketBuilder builder) {
		builder.writeAsShort(this.STR);
		builder.writeAsShort(this.DEX);
		builder.writeAsShort(this.INT);
		builder.writeAsShort(this.LUK);
		builder.writeAsShort(this.HP);
		builder.writeAsShort(this.MaxHP);
		builder.writeAsShort(this.MP);
		builder.writeAsShort(this.MaxMP);
	}
}
