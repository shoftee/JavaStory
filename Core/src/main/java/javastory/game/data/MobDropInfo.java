package javastory.game.data;

public final class MobDropInfo {

	public final short QuestId;
	public final int ItemId, Chance, Minimum, Maximum;

	public MobDropInfo(final int itemId, final int chance, final int Minimum, final int Maximum, final short questid) {
		this.ItemId = itemId;
		this.Chance = chance;
		this.QuestId = questid;
		this.Minimum = Minimum;
		this.Maximum = Maximum;
	}
}