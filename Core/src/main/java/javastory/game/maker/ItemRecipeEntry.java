package javastory.game.maker;

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
