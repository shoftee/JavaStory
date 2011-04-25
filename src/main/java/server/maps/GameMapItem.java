package server.maps;

import java.awt.Point;
import client.IItem;
import client.GameCharacter;
import client.GameClient;
import tools.MaplePacketCreator;

public class GameMapItem extends AbstractGameMapObject {

	protected IItem item;
	protected GameMapObject dropper;
	protected int character_ownerid, meso, questid = -1;
	protected byte type;
	protected boolean pickedUp = false, playerDrop;

	public GameMapItem(IItem item, Point position, GameMapObject dropper, GameCharacter owner, byte type, boolean playerDrop) {
		setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public GameMapItem(IItem item, Point position, GameMapObject dropper, GameCharacter owner, byte type, boolean playerDrop, int questid) {
		setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
		this.questid = questid;
	}

	public GameMapItem(int meso, Point position, GameMapObject dropper, GameCharacter owner, byte type, boolean playerDrop) {
		setPosition(position);
		this.item = null;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = meso;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public final IItem getItem() {
		return item;
	}

	public final int getQuest() {
		return questid;
	}

	public final int getItemId() {
		if (getMeso() > 0) {
			return meso;
		}
		return item.getItemId();
	}

	public final GameMapObject getDropper() {
		return dropper;
	}

	public final int getOwner() {
		return character_ownerid;
	}
   
	public final int getMeso() {
		return meso;
	}

	public final boolean isPlayerDrop() {
		return playerDrop;
	}

	public final boolean isPickedUp() {
		return pickedUp;
	}

	public void setPickedUp(final boolean pickedUp) {
		this.pickedUp = pickedUp;
	}

	public byte getDropType() {
		return type;
	}

	@Override
	public final GameMapObjectType getType() {
		return GameMapObjectType.ITEM;
	}

	@Override
	public void sendSpawnData(final GameClient client) {
		if (questid <= 0 || client.getPlayer().getQuestStatus(questid) == 1) {
			client.write(MaplePacketCreator.dropItemFromMapObject(this, null, getPosition(), (byte) 2));
		}
	}

	@Override
	public void sendDestroyData(final GameClient client) {
		client.write(MaplePacketCreator.removeItemFromMap(getObjectId(), 1, 0));
	}
}