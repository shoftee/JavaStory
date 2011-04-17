package client;

public enum InventoryType {

    UNDEFINED(0),
    EQUIP(1),
    USE(2),
    SETUP(3),
    ETC(4),
    CASH(5),
    EQUIPPED(-1);
    final byte type;

    private InventoryType(int type) {
        this.type = (byte) type;
    }

    public byte getType() {
        return type;
    }

    public short getBitfieldEncoding() {
        return (short) (2 << type);
    }

    public static InventoryType getByType(byte type) {
        for (InventoryType l : InventoryType.values()) {
            if (l.getType() == type) {
                return l;
            }
        }
        return null;
    }

    public static InventoryType getByWZName(String name) {
        if (name.equals("Install")) {
            return SETUP;
        } else if (name.equals("Consume")) {
            return USE;
        } else if (name.equals("Etc")) {
            return ETC;
        } else if (name.equals("Cash")) {
            return CASH;
        } else if (name.equals("Pet")) {
            return CASH;
        }
        return UNDEFINED;
    }
}