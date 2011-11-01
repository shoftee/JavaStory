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
	private byte value;

	private ItemType(final int value) {
		this.value = (byte) value;
	}

	public byte asNumber() {
		return this.value;
	}

	public static ItemType fromNumber(final int value) {
		switch (value) {
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
