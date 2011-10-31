package javastory.game;

public enum Stat {

    SKIN(0x1),
    FACE(0x2),
    HAIR(0x4),
    LEVEL(0x10),
    JOB(0x20),
    STR(0x40),
    DEX(0x80),
    INT(0x100),
    LUK(0x200),
    HP(0x400),
    MAX_HP(0x800),
    MP(0x1000),
    MAX_MP(0x2000),
    AVAILABLE_AP(0x4000),
    AVAILABLE_SP(0x8000),
    EXP(0x10000),
    FAME(0x20000),
    MESO(0x40000),
    PET(0x180008);
    private final int i;

    private Stat(final int i) {
        this.i = i;
    }

    public int getValue() {
        return this.i;
    }

    public static Stat fromValue(final int value) {
        for (final Stat stat : Stat.values()) {
            if (stat.i == value) {
                return stat;
            }
        }
        return null;
    }
}