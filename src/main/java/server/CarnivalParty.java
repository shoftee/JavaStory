package server;

import org.javastory.client.ChannelCharacter;
import java.util.LinkedList;
import java.util.List;
import server.maps.GameMap;
import tools.MaplePacketCreator;

/**
 * Note for this class : GameCharacter reference must be removed immediately after cpq or upon dc.
 * @author Rob
 */
public class CarnivalParty {

    private List<ChannelCharacter> members = new LinkedList<ChannelCharacter>();
    private ChannelCharacter leader;
    private byte team;
    private short availableCP = 0, totalCP = 0;
    private boolean winner = false;

    public CarnivalParty(final ChannelCharacter owner, final List<ChannelCharacter> members1, final byte team1) {
        leader = owner;
        members = members1;
        team = team1;

        for (final ChannelCharacter chr : members) {
            chr.setCarnivalParty(this);
        }
    }

    public final ChannelCharacter getLeader() {
        return leader;
    }

    public void addCP(ChannelCharacter player, int ammount) {
        totalCP += ammount;
        availableCP += ammount;
        player.addCP(ammount);
    }

    public int getTotalCP() {
        return totalCP;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public void useCP(ChannelCharacter player, int ammount) {
        availableCP -= ammount;
        player.useCP(ammount);
    }

    public List<ChannelCharacter> getMembers() {
        return members;
    }

    public int getTeam() {
        return team;
    }

    public void warp(final GameMap map, final String portalname) {
        for (ChannelCharacter chr : members) {
            chr.changeMap(map, map.getPortal(portalname));
        }
    }

    public void warp(final GameMap map, final int portalid) {
        for (ChannelCharacter chr : members) {
            chr.changeMap(map, map.getPortal(portalid));
        }
    }

    public boolean allInMap(GameMap map) {
        boolean status = true;
        for (ChannelCharacter chr : members) {
            if (chr.getMap() != map) {
                status = false;
            }
        }
        return status;
    }

    public void removeMember(ChannelCharacter chr) {
        members.remove(chr);
        chr.setCarnivalParty(null);
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean status) {
        winner = status;
    }

    public void displayMatchResult() {
        final String effect = winner ? "quest/carnival/win" : "quest/carnival/lose";

        for (final ChannelCharacter chr : members) {
            chr.getClient().write(MaplePacketCreator.showEffect(effect));
        }

    }
}
