package javastory.game.maker;

import java.util.ArrayList;
import java.util.List;

public class MakerItemInfo {

	public final int ReqLevel;
	public final int Cost, Quantity, Stimulator;
	public final byte TUC, ReqMakerLevel;
	private ItemRecipe recipe;
	private List<Integer> reqEquips = new ArrayList<Integer>();

	public MakerItemInfo(ItemRecipe recipe, int cost, int reqLevel, byte reqMakerLevel, int quantity, byte tuc, int stimulator) {
		this.recipe = recipe;

		this.Cost = cost;
		this.TUC = tuc;
		this.ReqLevel = reqLevel;
		this.ReqMakerLevel = reqMakerLevel;
		this.Quantity = quantity;
		this.Stimulator = stimulator;
	}

	public ItemRecipe getRecipe() {
		return this.recipe;
	}

	public List<Integer> getReqEquips() {
		return reqEquips;
	}
}