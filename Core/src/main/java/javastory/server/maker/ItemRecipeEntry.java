/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server.maker;

/**
 *
 * @author Tosho
 */
public class ItemRecipeEntry {
    private int itemId;
    private int quantity;
    
    public ItemRecipeEntry(int itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public int getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
