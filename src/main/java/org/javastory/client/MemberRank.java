/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.client;

/**
 *
 * @author Tosho
 */
public enum MemberRank {

    MASTER(1),
    JR_MASTER(2),
    MEMBER_HIGH(3),
    MEMBER_MIDDLE(4),
    MEMBER_LOW(5);
    private byte ordinal;

    private MemberRank(int ordinal) {
        this.ordinal = (byte) ordinal;
    }

    public byte asNumber() {
        return ordinal;
    }
    
    public boolean isSuperior(MemberRank other) {
        // Regular members have the same priviledges;
        // Jr. Masters are equal (not superior) to other Jr. Masters.
        return this.isMaster() && this.ordinal < other.ordinal;
    }
    
    public boolean isMaster() {
        return this.equals(MemberRank.JR_MASTER) || this.equals(MemberRank.MASTER);
    }

    public static MemberRank fromNumber(int ordinal) {
        switch (ordinal) {
            case 1:
                return MASTER;
            case 2:
                return JR_MASTER;
            case 3:
                return MEMBER_HIGH;
            case 4:
                return MEMBER_MIDDLE;
            case 5:
                return MEMBER_LOW;
            default:
                return null;
        }
    }
}
