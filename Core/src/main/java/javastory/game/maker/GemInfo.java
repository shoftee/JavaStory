package javastory.game.maker;

public class GemInfo {

	private int reqLevel, reqMakerLevel;
	private int cost, quantity;
	private RandomRewardFactory rewards;
	private ItemRecipe recipe;

	public GemInfo(RandomRewardFactory rewards, ItemRecipe recipe, int cost, int reqLevel, int reqMakerLevel, int quantity) {
		this.rewards = rewards;
		this.recipe = recipe;

		this.cost = cost;
		this.reqLevel = reqLevel;
		this.reqMakerLevel = reqMakerLevel;
		this.quantity = quantity;
	}

	public int getManufacturedQuantity() {
		return quantity;
	}

	public ItemRecipe getRecipe() {
		return recipe;
	}

	public int getRequiredLevel() {
		return reqLevel;
	}

	public int getRequiredSkillLevel() {
		return reqMakerLevel;
	}

	public int getCost() {
		return cost;
	}

	public int chooseRandomReward() {
		return rewards.getRandomItem();
	}
}