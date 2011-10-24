package javastory.channel;

import java.io.Serializable;

public class PartyMember implements Serializable {

	private static final long serialVersionUID = 6215463252132450750L;
	private int partyId;
	private int characterId;
	private String name;
	private int level;
	private int channelId;
	private int mapId;
	private int jobId;
	private boolean isOnline;
	private boolean isLeader;

	public PartyMember(int partyId, ChannelCharacter character) {
		this.partyId = partyId;
		this.characterId = character.getId();
		this.name = character.getName();
		this.channelId = character.getClient().getChannelId();
		this.mapId = character.getMapId();
		this.level = character.getLevel();
		this.jobId = character.getJobId();
		this.isOnline = true;
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

	public boolean isLeader() {
		return isLeader;
	}

	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean online) {
		this.isOnline = online;
	}

	public int getPartyId() {
		return partyId;
	}

	public int getMapId() {
		return mapId;
	}

	public String getName() {
		return name;
	}

	public int getCharacterId() {
		return characterId;
	}

	public int getJobId() {
		return jobId;
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