package javastory.game.data;

/**
 * 
 * @author shoftee
 */
public class RandomRewardEntry {

	public final int Probability;
	public final int ItemId;

	public RandomRewardEntry(final int probability, final int itemId) {
		this.Probability = probability;
		this.ItemId = itemId;
	}
}
