/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.game;

/**
 * 
 * @author shoftee
 */
public enum ItemType {

	EQUIP(1),
	NORMAL_ITEM(2),
	PET(3);
	private byte type;

	private ItemType(int typeByte) {
		this.type = (byte) typeByte;
	}

	public byte asByte() {
		return this.type;
	}

	public static ItemType fromByte(int type) {
		switch (type) {
		case 1:
			return EQUIP;
		case 2:
			return NORMAL_ITEM;
		case 3:
			return PET;
		default:
			return null;
		}
	}
}
