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
	public final int ItemId;
    public final int Quantity;
    
    public ItemRecipeEntry(int itemId, int quantity) {
        this.ItemId = itemId;
        this.Quantity = quantity;
    }
}
