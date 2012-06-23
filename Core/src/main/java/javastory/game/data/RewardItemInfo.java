package javastory.game.data;

import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public class RewardItemInfo {
	public final int ItemId;
	public final int Period;

	public final short Probability;
	public final short Quantity;

	public final String Effect;
	public final String WorldMessage;

	public RewardItemInfo(WzData data) {
		this.ItemId = WzDataTool.getInt("item", data, 0);
		this.Period = WzDataTool.getInt("period", data, -1);

		this.Probability = (byte) WzDataTool.getInt("prob", data, 0);
		this.Quantity = (short) WzDataTool.getInt("count", data, 0);

		this.Effect = WzDataTool.getString("Effect", data, "");
		this.WorldMessage = WzDataTool.getString("worldMsg", data, null);
	}

}
