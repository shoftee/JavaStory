package javastory.channel;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javastory.channel.server.Trade;
import javastory.channel.shops.PlayerShop;
import javastory.client.GameClient;
import javastory.cryptography.AesTransform;
import javastory.rmi.WorldChannelInterface;
import javastory.scripting.NpcScriptManager;
import javastory.tools.LogUtil;
import javastory.world.core.PartyOperation;

import javax.script.ScriptEngine;

import org.apache.mina.core.session.IoSession;

public final class ChannelClient extends GameClient {

	private ChannelCharacter player;
	private boolean transition = false;
	private Map<String, ScriptEngine> engines = new HashMap<>();

	public ChannelClient(AesTransform clientCrypto, AesTransform serverCrypto, IoSession session) {
		super(clientCrypto, serverCrypto, session);
	}

	public ChannelCharacter getPlayer() {
		return player;
	}

	public void setPlayer(ChannelCharacter player) {
		this.player = player;
	}

	public final void removalTask() {
		try {
			if (!player.getAllBuffs().isEmpty()) {
				player.clearAllBuffEffects();
			}
			if (!player.getAllDiseases().isEmpty()) {
				player.cancelAllDebuffs();
			}
			if (player.getTrade() != null) {
				Trade.cancelTrade(player.getTrade());
			}
			NpcScriptManager.getInstance().dispose(this);

			if (player.getEventInstance() != null) {
				player.getEventInstance().playerDisconnected(player);
			}
			player.getCheatTracker().dispose();
			if (player.getMap() != null) {
				player.getMap().removePlayer(player);
			}

			final PlayerShop shop = player.getPlayerShop();
			if (shop != null) {
				shop.removeVisitor(player);
				if (shop.isOwner(player)) {
					shop.setOpen(true);
				}
			}
		} catch (final Throwable e) {
			LogUtil.outputFileError(LogUtil.Acc_Stuck, e);
		}
	}

	public final void getDebugMessage(final StringBuilder sb) {
		sb.append(getSession().getRemoteAddress());
		sb.append(" Connected: ");
		sb.append(getSession().isConnected());
		sb.append(" Closing: ");
		sb.append(getSession().isClosing());
		sb.append(" ClientKeySet: ");
		sb.append(getSession().getAttribute(GameClient.CLIENT_KEY) != null);
		sb.append(" has char: ");
		sb.append(getPlayer() != null);
	}

	public final void setScriptEngine(final String name, final ScriptEngine e) {
		engines.put(name, e);
	}

	public final ScriptEngine getScriptEngine(final String name) {
		return engines.get(name);
	}

	public final void removeScriptEngine(final String name) {
		engines.remove(name);
	}

	@Override
	public final void disconnect(boolean force) {
		if (!force) {
			this.removalTask();
			this.player.saveToDb(true);
			final ChannelServer channel = ChannelServer.getInstance();
			try {
				final WorldChannelInterface world = ChannelServer.getWorldInterface();
				if (this.player.getMessenger() != null) {
					world.leaveMessenger(this.player.getMessenger().getId(), new MessengerMember(this.player));
					this.player.setMessenger(null);
				}
				PartyMember partyMember = this.player.getPartyMembership();
				if (partyMember != null) {
					partyMember.setOnline(false);
					world.updateParty(partyMember.getPartyId(), PartyOperation.LOG_ONOFF, partyMember);
				}
				if (!this.transition) {
					world.loggedOff(this.player.getName(), this.player.getId(), super.getChannelId(),
						this.player.getBuddyList().getBuddyIds());
				} else {
					// Change channel
					world.loggedOn(this.player.getName(), this.player.getId(), super.getChannelId(),
						this.player.getBuddyList().getBuddyIds());
				}
				if (this.player.getGuildId() > 0) {
					world.setGuildMemberOnline(this.player.getGuildMembership(), false, -1);
				}
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
				this.player.setMessenger(null);
			} catch (final Exception e) {
				System.err.println(getLogMessage(this, "ERROR") + e);
			} finally {
				if (channel != null) {
					ChannelServer.removePlayer(this.player);
				}
				this.player = null;
			}
		}
		super.getSession().close(force);
	}

	public static String getLogMessage(final ChannelClient cfor, final String message) {
		return getLogMessage(cfor, message, new Object[0]);
	}

	public static String getLogMessage(final ChannelClient cfor, final String message, final Object... parms) {
		final StringBuilder builder = new StringBuilder();
		if (cfor != null) {
			final ChannelCharacter player = cfor.getPlayer();
			if (player != null) {
				builder.append("<");
				builder.append(player.getName().toUpperCase());
				builder.append(" (cid: ");
				builder.append(player.getId());
				builder.append(")> ");
			}
			if (cfor.getAccountName() != null) {
				builder.append("(Account: ");
				builder.append(cfor.getAccountName());
				builder.append(") ");
			}
		}
		builder.append(message);
		int start;
		for (final Object parm : parms) {
			start = builder.indexOf("{}");
			builder.replace(start, start + 2, parm.toString());
		}
		return builder.toString();
	}

	public static String getLogMessage(final ChannelCharacter cfor, final String message) {
		return getLogMessage(cfor == null ? null : cfor.getClient(), message);
	}

	public static String getLogMessage(final ChannelCharacter cfor, final String message, final Object... parms) {
		return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
	}

}
