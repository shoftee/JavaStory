package javastory.channel.maps;

import java.awt.Point;

import javastory.channel.ChannelClient;
import javastory.game.Item;
import javastory.tools.packets.ChannelPackets;

public final class GameMapItem extends AbstractGameMapObject {

	private Item item;
	private GameMapObject dropper;
	private int ownerId, meso, questId;
	private ItemDropType type;
	private boolean pickedUp, playerDrop;

	public static GameMapItem meso(final int amount, final Point position, final GameMapObject dropper, final int ownerId, final ItemDropType type,
		final boolean playerDrop) {
		final GameMapItem item = new GameMapItem(position, dropper, ownerId, type, playerDrop);
		item.item = null;
		item.meso = amount;
		return item;
	}

	public static GameMapItem item(final Item item, final Point position, final GameMapObject dropper, final int ownerId, final ItemDropType type,
		final boolean playerDrop) {
		final GameMapItem gameMapItem = new GameMapItem(position, dropper, ownerId, type, playerDrop);
		gameMapItem.item = item;
		gameMapItem.meso = 0;
		return gameMapItem;
	}

	public static GameMapItem questItem(final Item item, final Point position, final GameMapObject dropper, final int ownerId, final ItemDropType type,
		final boolean playerDrop, final int questId) {
		final GameMapItem gameMapItem = GameMapItem.item(item, position, dropper, ownerId, type, playerDrop);
		gameMapItem.questId = questId;
		return gameMapItem;
	}

	private GameMapItem(final Point position, final GameMapObject dropper, final int ownerId, final ItemDropType type, final boolean playerDrop) {
		this.setPosition(position);
		this.dropper = dropper;
		this.ownerId = ownerId;
		this.type = type;
		this.playerDrop = playerDrop;

		this.meso = 0;
		this.questId = -1;
		this.item = null;
		this.pickedUp = false;
	}

	public final Item getItem() {
		return this.item;
	}

	public final int getQuest() {
		return this.questId;
	}

	public final int getItemId() {
		if (this.getMeso() > 0) {
			return this.meso;
		}
		return this.item.getItemId();
	}

	public final GameMapObject getDropper() {
		return this.dropper;
	}

	public final int getOwner() {
		return this.ownerId;
	}

	public final int getMeso() {
		return this.meso;
	}

	public final boolean isPlayerDrop() {
		return this.playerDrop;
	}

	public final boolean isPickedUp() {
		return this.pickedUp;
	}

	public void setPickedUp(final boolean pickedUp) {
		this.pickedUp = pickedUp;
	}

	public ItemDropType getDropType() {
		return this.type;
	}

	@Override
	public final GameMapObjectType getType() {
		return GameMapObjectType.ITEM;
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		if (this.questId <= 0 || client.getPlayer().getQuestCompletionStatus(this.questId) == 1) {
			client.write(ChannelPackets.dropItemFromMapObject(this, null, this.getPosition(), (byte) 2));
		}
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.removeItemFromMap(this.getObjectId(), 1, 0));
	}
}