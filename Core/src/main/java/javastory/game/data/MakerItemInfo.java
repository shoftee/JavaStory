package javastory.game.data;

import java.util.ArrayList;
import java.util.List;

public class MakerItemInfo {

	public final int ReqLevel;
	public final int Cost, Quantity, Stimulator;
	public final byte TUC, ReqMakerLevel;
	private final ItemRecipe recipe;
	private final List<Integer> reqEquips = new ArrayList<Integer>();

	public MakerItemInfo(final ItemRecipe recipe, final int cost, final int reqLevel, final byte reqMakerLevel, final int quantity, final byte tuc, final int stimulator) {
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
		return this.reqEquips;
	}
}