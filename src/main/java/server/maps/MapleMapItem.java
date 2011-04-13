package server.maps;

import java.awt.Point;
import client.IItem;
import client.MapleCharacter;
import client.MapleClient;
import tools.MaplePacketCreator;

public class MapleMapItem extends AbstractMapleMapObject {

	protected IItem item;
	protected MapleMapObject dropper;
	protected int character_ownerid, meso, questid = -1;
	protected byte type;
	protected boolean pickedUp = false, playerDrop;

	public MapleMapItem(IItem item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop) {
		setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
	}

	public MapleMapItem(IItem item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop, int questid) {
		setPosition(position);
		this.item = item;
		this.dropper = dropper;
		this.character_ownerid = owner.getId();
		this.meso = 0;
		this.type = type;
		this.playerDrop = playerDrop;
		this.questid = questid;
	}

	public MapleMapItem(int meso, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop) {
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

	public final MapleMapObject getDropper() {
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
	public final MapleMapObjectType getType() {
		return MapleMapObjectType.ITEM;
	}

	@Override
	public void sendSpawnData(final MapleClient client) {
		if (questid <= 0 || client.getPlayer().getQuestStatus(questid) == 1) {
			client.getSession().write(MaplePacketCreator.dropItemFromMapObject(this, null, getPosition(), (byte) 2));
		}
	}

	@Override
	public void sendDestroyData(final MapleClient client) {
		client.getSession().write(MaplePacketCreator.removeItemFromMap(getObjectId(), 1, 0));
	}
}