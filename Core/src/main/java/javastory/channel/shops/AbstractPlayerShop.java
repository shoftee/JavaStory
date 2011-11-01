package javastory.channel.shops;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.maps.AbstractGameMapObject;
import javastory.channel.packet.PlayerShopPacket;
import javastory.db.Database;
import javastory.game.Equip;
import javastory.game.Item;
import javastory.game.ItemType;
import javastory.io.GamePacket;

public abstract class AbstractPlayerShop extends AbstractGameMapObject implements PlayerShop {

	private boolean open;
	private final String ownerName, des;
	private final int ownerId, owneraccount, itemId;
	private final AtomicInteger meso = new AtomicInteger(0);
	protected WeakReference[] visitors = new WeakReference[3];
	protected List<PlayerShopItem> items = Lists.newLinkedList();

	public AbstractPlayerShop(final ChannelCharacter owner, final int itemId, final String desc) {
		this.setPosition(owner.getPosition());
		this.ownerName = owner.getName();
		this.ownerId = owner.getId();
		this.owneraccount = owner.getAccountId();
		this.itemId = itemId;
		this.des = desc;
		this.open = false;
	}

	@Override
	public void broadcastToVisitors(final GamePacket packet) {
		this.broadcastToVisitors(packet, true);
	}

	public void broadcastToVisitors(final GamePacket packet, final boolean owner) {
		ChannelCharacter visitor;
		for (int i = 0; i < 3; i++) {
			visitor = (ChannelCharacter) this.visitors[i].get();
			if (visitor != null) {
				visitor.getClient().write(packet);
			}
		}
		if (this.getShopType() == 2 && owner) {
			((GenericPlayerStore) this).getMCOwner().getClient().write(packet);
		}
	}

	@Override
	public int getMeso() {
		return this.meso.get();
	}

	@Override
	public void setMeso(final int meso) {
		this.meso.set(meso);
	}

	public void removeVisitors() {
		ChannelCharacter chr = (ChannelCharacter) this.visitors[0].get();
		if (chr != null) {
			this.removeVisitor(chr);
		}
		chr = (ChannelCharacter) this.visitors[1].get();
		if (chr != null) {
			this.removeVisitor(chr);
		}
		chr = (ChannelCharacter) this.visitors[2].get();
		if (chr != null) {
			this.removeVisitor(chr);
		}
	}

	@Override
	public void setOpen(final boolean open) {
		this.open = open;
	}

	@Override
	public boolean isOpen() {
		return this.open;
	}

	public boolean saveItems() {
		final Connection con = Database.getConnection();
		try {
			final PreparedStatement ps = con.prepareStatement("INSERT INTO hiredmerch (characterid, accountid, Mesos, time) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, this.ownerId);
			ps.setInt(2, this.owneraccount);
			ps.setInt(3, this.meso.get());
			ps.setLong(4, System.currentTimeMillis());

			ps.executeUpdate();

			final ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			final int packageid = rs.getInt(1);
			rs.close();
			ps.close();

			PreparedStatement ps2;

			for (final PlayerShopItem pItems : this.items) {
				if (pItems.bundles <= 0) {
					continue;
				}
				final Item item = pItems.item;

				if (item.getType() == ItemType.EQUIP) { // equips
					ps2 = con
						.prepareStatement("INSERT INTO hiredmerchitems (PackageId, itemid, quantity, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, owner, GM_Log, flag, expiredate, ViciousHammer, itemLevel, itemEXP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					final Equip eq = (Equip) item;
					ps2.setInt(1, packageid);
					ps2.setInt(2, eq.getItemId());
					ps2.setInt(3, 1); // Quantity
					ps2.setInt(4, eq.getUpgradeSlots());
					ps2.setInt(5, eq.getLevel());
					ps2.setInt(6, eq.getStr());
					ps2.setInt(7, eq.getDex());
					ps2.setInt(8, eq.getInt());
					ps2.setInt(9, eq.getLuk());
					ps2.setInt(10, eq.getHp());
					ps2.setInt(11, eq.getMp());
					ps2.setInt(12, eq.getWatk());
					ps2.setInt(13, eq.getMatk());
					ps2.setInt(14, eq.getWdef());
					ps2.setInt(15, eq.getMdef());
					ps2.setInt(16, eq.getAcc());
					ps2.setInt(17, eq.getAvoid());
					ps2.setInt(18, eq.getHands());
					ps2.setInt(19, eq.getSpeed());
					ps2.setInt(20, eq.getJump());
					ps2.setString(21, eq.getOwner());
					ps2.setString(22, eq.getGMLog());
					ps2.setInt(23, eq.getFlag());
					ps2.setLong(24, eq.getExpiration());
					ps2.setInt(25, eq.getViciousHammer());
					ps2.setInt(26, eq.getItemLevel());
					ps2.setInt(27, eq.getItemEXP());
				} else {
					ps2 = con
						.prepareStatement("INSERT INTO hiredmerchitems (PackageId, itemid, quantity, owner, GM_Log, flag, expiredate) VALUES (?, ?, ?, ?, ?, ?, ?)");
					ps2.setInt(1, packageid);
					ps2.setInt(2, item.getItemId());
					ps2.setInt(3, pItems.bundles * item.getQuantity());
					ps2.setString(4, item.getOwner());
					ps2.setString(5, item.getGMLog());
					ps2.setInt(6, item.getFlag());
					ps2.setLong(7, item.getExpiration());
				}
				ps2.execute();
				ps2.close();
			}
			return true;
		} catch (final SQLException se) {
			se.printStackTrace();
		}
		return false;
	}

	public ChannelCharacter getVisitor(final int num) {
		if (1 <= num && num <= 3) {
			return (ChannelCharacter) this.visitors[num - 1].get();
		}
		return null;
	}

	@Override
	public void addVisitor(final ChannelCharacter visitor) {
		final int i = this.getFreeSlot();
		if (i > -1) {
			this.broadcastToVisitors(PlayerShopPacket.shopVisitorAdd(visitor, i));
			this.visitors[i - 1] = new WeakReference<ChannelCharacter>(visitor);

			if (i == 3) {
				if (this.getShopType() == 1) {
					final HiredMerchantStore hiredMerchant = (HiredMerchantStore) this;
					hiredMerchant.getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant(hiredMerchant));
				} else {
					final ChannelCharacter owner = ((GenericPlayerStore) this).getMCOwner();
					owner.getMap().broadcastMessage(PlayerShopPacket.sendPlayerShopBox(owner));
				}
			}
		}
	}

	@Override
	public void removeVisitor(final ChannelCharacter visitor) {
		final byte slot = this.getVisitorSlot(visitor);
		final boolean shouldUpdate = this.getFreeSlot() == -1;
		if (slot != -1) {
			this.broadcastToVisitors(PlayerShopPacket.shopVisitorLeave(slot));
			this.visitors[slot - 1] = new WeakReference<ChannelCharacter>(null);

			if (shouldUpdate) {
				if (this.getShopType() == 1) {
					final HiredMerchantStore hiredMerchant = (HiredMerchantStore) this;
					hiredMerchant.getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant(hiredMerchant));
				} else {
					final ChannelCharacter owner = ((GenericPlayerStore) this).getMCOwner();
					owner.getMap().broadcastMessage(PlayerShopPacket.sendPlayerShopBox(owner));
				}
			}
		}
	}

	@Override
	public byte getVisitorSlot(final ChannelCharacter visitor) {
		ChannelCharacter chr;
		for (byte i = 1; i <= 3; i++) {
			chr = (ChannelCharacter) this.visitors[i - 1].get();
			if (chr == visitor) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void removeAllVisitors(final int error, final int type) {
		for (int i = 1; i <= 3; i++) {
			final ChannelCharacter visitor = this.getVisitor(i);
			if (visitor != null) {
				if (type != -1) {
					visitor.getClient().write(PlayerShopPacket.shopErrorMessage(error, type));
				}
				visitor.setPlayerShop(null);
				this.visitors[i - 1] = new WeakReference<>(null);
			}
		}
	}

	@Override
	public String getOwnerName() {
		return this.ownerName;
	}

	@Override
	public int getOwnerId() {
		return this.ownerId;
	}

	@Override
	public int getOwnerAccountId() {
		return this.owneraccount;
	}

	@Override
	public String getDescription() {
		if (this.des == null) {
			return "";
		}
		return this.des;
	}

	@Override
	public List<ChannelCharacter> getVisitors() {
		final List<ChannelCharacter> chars = Lists.newLinkedList();
		ChannelCharacter visitor;
		for (int i = 1; i <= 3; i++) {
			visitor = (ChannelCharacter) this.visitors[i - 1].get();
			if (visitor != null) {
				chars.add(visitor);
			}
		}
		return chars;
	}

	@Override
	public List<PlayerShopItem> getItems() {
		return this.items;
	}

	@Override
	public void addItem(final PlayerShopItem item) {
		this.items.add(item);
	}

	@Override
	public boolean removeItem(final int item) {
		return false;
	}

	@Override
	public void removeFromSlot(final int slot) {
		this.items.remove(slot);
	}

	@Override
	public byte getFreeSlot() {
		for (byte i = 1; i <= 3; i++) {
			if (this.visitors[i].get() == null) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getItemId() {
		return this.itemId;
	}

	@Override
	public boolean isOwner(final ChannelCharacter chr) {
		return chr.getId() == this.ownerId && chr.getName().equals(this.ownerName);
	}
}
