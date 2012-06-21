package javastory.channel.server;

import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.game.GameConstants;
import javastory.game.Item;
import javastory.game.ItemFlag;
import javastory.tools.packets.ChannelPackets;

public final class Trade {

	private Trade partner = null;
	private final List<Item> items = Lists.newLinkedList();
	private List<Item> exchangeItems;
	private int meso = 0, exchangeMeso = 0;
	private boolean locked = false;
	private final ChannelCharacter chr;
	private final byte tradingslot;

	public Trade(final byte tradingslot, final ChannelCharacter chr) {
		this.tradingslot = tradingslot;
		this.chr = chr;
	}

	public void CompleteTrade() {
		for (final Item item : this.exchangeItems) {
			final byte flag = item.getFlag();

			if (ItemFlag.KARMA_EQ.check(flag)) {
				item.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
			} else if (ItemFlag.KARMA_USE.check(flag)) {
				item.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
			}
			InventoryManipulator.addFromDrop(this.chr.getClient(), item, false);
		}
		if (this.exchangeMeso > 0) {
			this.chr.gainMeso(this.exchangeMeso - GameConstants.getTaxAmount(this.exchangeMeso), false, true, false);
		}
		this.exchangeMeso = 0;
		this.exchangeItems.clear();

		this.chr.getClient().write(ChannelPackets.TradeMessage(this.tradingslot, (byte) 0x07));
	}

	public void cancel() {
		for (final Item item : this.items) {
			InventoryManipulator.addFromDrop(this.chr.getClient(), item, false);
		}
		if (this.meso > 0) {
			this.chr.gainMeso(this.meso, false, true, false);
		}
		this.meso = 0;
		if (this.items != null) { // just to be on the safe side...
			this.items.clear();
		}
		this.chr.getClient().write(ChannelPackets.getTradeCancel(this.tradingslot));
	}

	public boolean isLocked() {
		return this.locked;
	}

	public void setMeso(final int meso) {
		if (this.locked || this.partner == null || meso <= 0 || this.meso + meso <= 0) {
			return;
		}
		if (this.chr.getMeso() >= meso) {
			this.chr.gainMeso(-meso, false, true, false);
			this.meso += meso;
			this.chr.getClient().write(ChannelPackets.getTradeMesoSet((byte) 0, this.meso));
			if (this.partner != null) {
				this.partner.getChr().getClient().write(ChannelPackets.getTradeMesoSet((byte) 1, this.meso));
			}
		}
	}

	public void addItem(final Item item) {
		if (this.locked || this.partner == null) {
			return;
		}
		this.items.add(item);
		this.chr.getClient().write(ChannelPackets.getTradeItemAdd((byte) 0, item));
		if (this.partner != null) {
			this.partner.getChr().getClient().write(ChannelPackets.getTradeItemAdd((byte) 1, item));
		}
	}

	public void chat(final String message) {
		this.chr.getClient().write(ChannelPackets.getPlayerShopChat(this.chr, message, true));
		if (this.partner != null) {
			this.partner.getChr().getClient().write(ChannelPackets.getPlayerShopChat(this.chr, message, false));
		}
	}

	public Trade getPartner() {
		return this.partner;
	}

	public void setPartner(final Trade partner) {
		if (this.locked) {
			return;
		}
		this.partner = partner;
	}

	public ChannelCharacter getChr() {
		return this.chr;
	}

	private boolean check() {
		if (this.chr.getMeso() + this.exchangeMeso < 0) {
			return false;
		}
		byte eq = 0, use = 0, setup = 0, etc = 0;
		for (final Item item : this.exchangeItems) {
			switch (GameConstants.getInventoryType(item.getItemId())) {
			case EQUIP:
				eq++;
				break;
			case USE:
				use++;
				break;
			case SETUP:
				setup++;
				break;
			case ETC:
				etc++;
				break;
			case CASH: // Not allowed, probably hacking
				return false;
			}
		}
		if (this.chr.getEquipInventory().getNumFreeSlot() <= eq || this.chr.getUseInventory().getNumFreeSlot() <= use
			|| this.chr.getSetupInventory().getNumFreeSlot() <= setup || this.chr.getEtcInventory().getNumFreeSlot() <= etc) {
			return false;
		}
		return true;
	}

	public static void completeTrade(final ChannelCharacter c) {
		final Trade local = c.getTrade();
		final Trade partner = local.getPartner();

		if (partner == null || local.locked) {
			return;
		}
		local.locked = true; // Locking the trade
		partner.getChr().getClient().write(ChannelPackets.getTradeConfirmation());

		partner.exchangeItems = local.items; // Copy this to partner's trade since it's alreadt accepted
		partner.exchangeMeso = local.meso; // Copy this to partner's trade since it's alreadt accepted

		if (partner.isLocked()) { // Both locked
			if (!local.check() || !partner.check()) { // Check for full inventories
				// NOTE : IF accepted = other party but inventory is full, the item is lost.
				partner.cancel();
				local.cancel();

				c.getClient().write(ChannelPackets.serverNotice(5, "There is not enough inventory space to complete the trade."));
				partner.getChr().getClient().write(ChannelPackets.serverNotice(5, "There is not enough inventory space to complete the trade."));
			} else {
				local.CompleteTrade();
				partner.CompleteTrade();
			}
			partner.getChr().setTrade(null);
			c.setTrade(null);
		}
	}

	public static void cancelTrade(final Trade Localtrade) {
		Localtrade.cancel();

		final Trade partner = Localtrade.getPartner();
		if (partner != null) {
			partner.cancel();
			partner.getChr().setTrade(null);
		}
		Localtrade.chr.setTrade(null);
	}

	public static void startTrade(final ChannelCharacter c) {
		if (c.getTrade() == null) {
			c.setTrade(new Trade((byte) 0, c));
			c.getClient().write(ChannelPackets.getTradeStart(c.getClient(), c.getTrade(), (byte) 0));
		} else {
			c.getClient().write(ChannelPackets.serverNotice(5, "You are already in a trade"));
		}
	}

	public static void inviteTrade(final ChannelCharacter c1, final ChannelCharacter c2) {
		if (c2.getTrade() == null) {
			c2.setTrade(new Trade((byte) 1, c2));
			c2.getTrade().setPartner(c1.getTrade());
			c1.getTrade().setPartner(c2.getTrade());
			c2.getClient().write(ChannelPackets.getTradeInvite(c1));
		} else {
			c1.getClient().write(ChannelPackets.serverNotice(5, "The other player is already trading with someone else."));
			cancelTrade(c1.getTrade());
		}
	}

	public static void visitTrade(final ChannelCharacter c1, final ChannelCharacter c2) {
		if (c1.getTrade() != null && c1.getTrade().getPartner() == c2.getTrade() && c2.getTrade() != null && c2.getTrade().getPartner() == c1.getTrade()) {
			// We don't need to check for map here as the user is found via MapleMap.getCharacterById_InMap()
			c2.getClient().write(ChannelPackets.getTradePartnerAdd(c1));
			c1.getClient().write(ChannelPackets.getTradeStart(c1.getClient(), c1.getTrade(), (byte) 1));
		} else {
			c1.getClient().write(ChannelPackets.serverNotice(5, "The other player has already closed the trade"));
		}
	}

	public static void declineTrade(final ChannelCharacter c) {
		final Trade trade = c.getTrade();
		if (trade != null) {
			if (trade.getPartner() != null) {
				final ChannelCharacter other = trade.getPartner().getChr();
				other.getTrade().cancel();
				other.setTrade(null);
				other.getClient().write(ChannelPackets.serverNotice(5, c.getName() + " has declined your trade request"));
			}
			trade.cancel();
			c.setTrade(null);
		}
	}
}
