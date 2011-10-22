package javastory.channel.life;

public final class MobDropInfo {

	public final short QuestId;
	public final int ItemId, Chance, Minimum, Maximum;

	public MobDropInfo(int itemId, int chance, int Minimum, int Maximum, short questid) {
		this.ItemId = itemId;
		this.Chance = chance;
		this.QuestId = questid;
		this.Minimum = Minimum;
		this.Maximum = Maximum;
	}
}