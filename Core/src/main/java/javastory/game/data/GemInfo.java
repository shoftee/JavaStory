package javastory.game.data;

public class GemInfo {

	private final int reqLevel, reqMakerLevel;
	private final int cost, quantity;
	private final RandomRewardFactory rewards;
	private final ItemRecipe recipe;

	public GemInfo(final RandomRewardFactory rewards, final ItemRecipe recipe, final int cost, final int reqLevel, final int reqMakerLevel, final int quantity) {
		this.rewards = rewards;
		this.recipe = recipe;

		this.cost = cost;
		this.reqLevel = reqLevel;
		this.reqMakerLevel = reqMakerLevel;
		this.quantity = quantity;
	}

	public int getManufacturedQuantity() {
		return this.quantity;
	}

	public ItemRecipe getRecipe() {
		return this.recipe;
	}

	public int getRequiredLevel() {
		return this.reqLevel;
	}

	public int getRequiredSkillLevel() {
		return this.reqMakerLevel;
	}

	public int getCost() {
		return this.cost;
	}

	public int chooseRandomReward() {
		return this.rewards.getRandomItem();
	}
}