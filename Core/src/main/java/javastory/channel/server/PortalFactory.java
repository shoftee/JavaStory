package javastory.channel.server;

import java.awt.Point;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.anticheat.CheatingOffense;
import javastory.channel.maps.GameMap;
import javastory.scripting.PortalScriptManager;
import javastory.tools.packets.ChannelPackets;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public class PortalFactory {

	private int nextDoorPortal = 0x80;

	public Portal createPortal(final WzData data) {
		GenericPortal portal = new GenericPortal(this.nextDoorPortal, data);

		if (portal.getType() == Portal.DOOR_PORTAL) {
			this.nextDoorPortal++;
		}

		return portal;
	}

	private class GenericPortal implements Portal {

		private String name, target, scriptName;
		private Point position;
		private int targetmap;
		private final int type;
		private int id;

		public GenericPortal(final int currentDoorId, final WzData data) {
			this.type = WzDataTool.getInt("pt", data);
			this.name = WzDataTool.getString("pn", data);

			if (this.type == Portal.DOOR_PORTAL) {
				this.id = currentDoorId;
			} else {
				this.id = Integer.parseInt(this.name);
			}

			this.target = WzDataTool.getString("tn", data);
			this.targetmap = WzDataTool.getInt("tm", data);
			this.position = WzDataTool.getPoint("x", "y", data);

			String script = WzDataTool.getString("script", data, null);
			if (script != null && script.equals("")) {
				script = null;
			}
			this.scriptName = script;

		}

		@Override
		public final int getId() {
			return this.id;
		}

		@Override
		public final String getName() {
			return this.name;
		}

		@Override
		public final Point getPosition() {
			return this.position;
		}

		@Override
		public final String getTarget() {
			return this.target;
		}

		@Override
		public final int getTargetMapId() {
			return this.targetmap;
		}

		@Override
		public final int getType() {
			return this.type;
		}

		@Override
		public final String getScriptName() {
			return this.scriptName;
		}

		@Override
		public final void enterPortal(final ChannelClient c) {
			final ChannelCharacter player = c.getPlayer();

			if (this.getPosition().distanceSq(player.getPosition()) > 22500) {
				player.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
			}
			if (this.getScriptName() != null) {
				final GameMap currentmap = player.getMap();
				try {
					PortalScriptManager.getInstance().executePortalScript(this, c);
					if (player.getMap() == currentmap) { // Character is still on
															// the same map.
						c.write(ChannelPackets.enableActions());
					}
				} catch (final Exception e) {
					c.write(ChannelPackets.enableActions());
					e.printStackTrace();
				}
			} else if (this.getTargetMapId() != 999999999) {
				final GameMap to = ChannelServer.getMapFactory().getMap(this.getTargetMapId());
				player.changeMap(to, to.getPortal(this.getTarget()) == null ? to.getPortal(0) : to.getPortal(this.getTarget()));
			}
		}
	}
}
