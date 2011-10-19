/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.game;

/**
 *
 * @author Tosho
 */
public enum Gender {
    MALE(0),
    FEMALE(1),
    UNSPECIFIED(2);
    
    private byte type;
    
    private Gender(int type) {
        this.type = (byte) type;
    }
    
    public byte asNumber() {
        return this.type;
    }
    
    public static Gender fromNumber(int type) {
        switch (type) {
            case 0:
                return MALE;
            case 1:
                return FEMALE;
            case 2:
                return UNSPECIFIED;
            default:
                return null;
        }
    }
}
