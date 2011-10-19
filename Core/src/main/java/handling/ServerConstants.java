package handling;

public class ServerConstants {
    //public static final byte[] Gateway_IP = {(byte)0xCB, (byte)0x74, (byte)0xC4, (byte)0x08}; // Singapore IP

    public static final byte[] Server_IP = {(byte) 0xCB, (byte) 0xBC, (byte) 0xEF, (byte) 0x52}; // Malaysia IP 203.188.239.82

    public static byte CLASS_EXP(final int job) {
        if (job >= 430 && job <= 434) { // Dual Blade
            return 25;
        } else if (job == 900) {
            return 80;
        } else {
            return 0;
        }
    }
    public static final short GAME_VERSION = 102;
}