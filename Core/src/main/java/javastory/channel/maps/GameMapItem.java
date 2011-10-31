package javastory.channel.maps;

import java.awt.Point;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.game.Item;
import javastory.tools.packets.ChannelPackets;

public class GameMapItem extends AbstractGameMapObject {

	protected Item item;
	protected GameMapObject dropper;
	protected int character_ownerid, meso, questid = -1;
	protected byte type;
	protected boolean pickedUp = false, playerDrop;

	public GameMapItem(final Item item, final Point position, final GameMapObject dropper, final ChannelCharacter owner, final byte type, final boolean playerDrop) {
		this.setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public GameMapItem(final Item item, final Point position, final GameMapObject dropper, final ChannelCharacter owner, final byte type, final boolean playerDrop, final int questid) {
		this.setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
		this.questid = questid;
	}

	public GameMapItem(final int meso, final Point position, final GameMapObject dropper, final ChannelCharacter owner, final byte type, final boolean playerDrop) {
		this.setPosition(position);
		this.item = null;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = meso;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public final Item getItem() {
		return this.item;
	}

	public final int getQuest() {
		return this.questid;
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
		return this.character_ownerid;
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

	public byte getDropType() {
		return this.type;
	}

	@Override
	public final GameMapObjectType getType() {
		return GameMapObjectType.ITEM;
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		if (this.questid <= 0 || client.getPlayer().getQuestCompletionStatus(this.questid) == 1) {
			client.write(ChannelPackets.dropItemFromMapObject(this, null, this.getPosition(), (byte) 2));
		}
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.removeItemFromMap(this.getObjectId(), 1, 0));
	}
}