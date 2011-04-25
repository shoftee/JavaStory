/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

/**
 *
 * @author Tosho
 */
public enum ItemType {

    EQUIP(1),
    NORMAL_ITEM(2);
    private byte type;

    private ItemType(int typeByte) {
        this.type = (byte) typeByte;
    }

    public byte asByte() {
        return this.type;
    }

    public static ItemType fromByte(int type) {
        for (ItemType itemType : ItemType.values()) {
            if (itemType.asByte() == type) {
                return itemType;
            }
        }
        return null;
    }
}
