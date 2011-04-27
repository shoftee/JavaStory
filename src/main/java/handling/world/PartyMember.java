package handling.world;

import java.awt.Point;
import java.util.List;
import java.io.Serializable;

import org.javastory.client.ChannelCharacter;
import server.maps.Door;

public class PartyMember implements Serializable {

    private static final long serialVersionUID = 6215463252132450750L;
    private int characterId;
    private String name;
    private int level;
    private int channelId;
    private int jobId;
    private int mapId;
    private int doorTown = 999999999;
    private int doorTarget = 999999999;
    private Point doorPosition = new Point(0, 0);
    private boolean isOnline;

    public PartyMember(ChannelCharacter character) {
        this.characterId = character.getId();
        this.name = character.getName();
        this.channelId = character.getClient().getChannelId();
        this.mapId = character.getMapId();
        this.level = character.getLevel();
        this.jobId = character.getJobId();
        this.isOnline = true;
        final List<Door> doors = character.getDoors();
        if (!doors.isEmpty()) {
            final Door door = doors.get(0);
            this.doorTown = door.getTown().getId();
            this.doorTarget = door.getTarget().getId();
            this.doorPosition = door.getTargetPosition();
        }
    }

    public PartyMember() {
        this.name = "";
        //default values for everything
    }

    public int getLevel() {
        return level;
    }

    public int getChannel() {
        return channelId;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
    }

    public int getMapid() {
        return mapId;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return characterId;
    }

    public int getJobId() {
        return jobId;
    }

    public int getDoorTown() {
        return doorTown;
    }

    public int getDoorTarget() {
        return doorTarget;
    }

    public Point getDoorPosition() {
        return doorPosition;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.characterId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PartyMember other = (PartyMember) obj;
        return this.characterId == other.characterId;
    }
}