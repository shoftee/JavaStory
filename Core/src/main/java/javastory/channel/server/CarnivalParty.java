package javastory.channel.server;

import java.util.LinkedList;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.maps.GameMap;
import javastory.tools.packets.ChannelPackets;

/**
 * Note for this class : GameCharacter reference must be removed immediately
 * after cpq or upon dc.
 * 
 * @author Rob
 */
public class CarnivalParty {

	private List<ChannelCharacter> members = new LinkedList<ChannelCharacter>();
	private final ChannelCharacter leader;
	private final byte team;
	private short availableCP = 0, totalCP = 0;
	private boolean winner = false;

	public CarnivalParty(final ChannelCharacter owner, final List<ChannelCharacter> members1, final byte team1) {
		this.leader = owner;
		this.members = members1;
		this.team = team1;

		for (final ChannelCharacter chr : this.members) {
			chr.setCarnivalParty(this);
		}
	}

	public final ChannelCharacter getLeader() {
		return this.leader;
	}

	public void addCP(final ChannelCharacter player, final int ammount) {
		this.totalCP += ammount;
		this.availableCP += ammount;
		player.addCP(ammount);
	}

	public int getTotalCP() {
		return this.totalCP;
	}

	public int getAvailableCP() {
		return this.availableCP;
	}

	public void useCP(final ChannelCharacter player, final int ammount) {
		this.availableCP -= ammount;
		player.useCP(ammount);
	}

	public List<ChannelCharacter> getMembers() {
		return this.members;
	}

	public int getTeam() {
		return this.team;
	}

	public void warp(final GameMap map, final String portalname) {
		for (final ChannelCharacter chr : this.members) {
			chr.changeMap(map, map.getPortal(portalname));
		}
	}

	public void warp(final GameMap map, final int portalid) {
		for (final ChannelCharacter chr : this.members) {
			chr.changeMap(map, map.getPortal(portalid));
		}
	}

	public boolean allInMap(final GameMap map) {
		boolean status = true;
		for (final ChannelCharacter chr : this.members) {
			if (chr.getMap() != map) {
				status = false;
			}
		}
		return status;
	}

	public void removeMember(final ChannelCharacter chr) {
		this.members.remove(chr);
		chr.setCarnivalParty(null);
	}

	public boolean isWinner() {
		return this.winner;
	}

	public void setWinner(final boolean status) {
		this.winner = status;
	}

	public void displayMatchResult() {
		final String effect = this.winner ? "quest/carnival/win" : "quest/carnival/lose";

		for (final ChannelCharacter chr : this.members) {
			chr.getClient().write(ChannelPackets.showEffect(effect));
		}

	}
}
