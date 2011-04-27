/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

/**
 *
 * @author Tosho
 */
public enum ServerMessageType {
    /* 0: [Notice]<br>
     * 1: Popup<br>
     * 2: Megaphone<br>
     * 3: Super Megaphone<br>
     * 4: Scrolling message at top<br>
     * 5: Pink Text<br>
     * 6: Lightblue Text
     * 8: Item megaphone
     * 9: Heart megaphone
     * 10: Skull Super megaphone
     * 11: Green megaphone message?
     * 12: Three line of megaphone text
     * 13: End of file =.="
     * 14: Green Gachapon box
     * 15: Red Gachapon box*/

    NOTICE(0),
    POPUP(1),
    MEGAPHONE(2),
    SUPER_MEGAPHONE(3),
    MARQUEE(4),
    PINK(5),
    LIGHTBLUE(6),
    ITEM_MEGAPHONE(8),
    HEART_MEGAPHONE(9),
    SKULL_MEGAPHONE(10),
    GREEN_MEGAPHONE(11),
    TRIPLE_MEGAPHONE(12),
    GREEN_GACHAPON(14),
    RED_GACHAPON(15);
    //
    private int type;

    private ServerMessageType(int type) {
        this.type = type;
    }

    public int asNumber() {
        return type;
    }
}
