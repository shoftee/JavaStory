package javastory.channel;

import java.io.Serializable;

public class PartyMember implements Serializable {

	private static final long serialVersionUID = 6215463252132450750L;
	private int partyId;
	private int characterId;
	private final String name;
	private int level;
	private int channelId;
	private int mapId;
	private int jobId;
	private boolean isOnline;
	private boolean isLeader;

	public PartyMember(final int partyId, final ChannelCharacter character) {
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
		return this.level;
	}

	public int getChannel() {
		return this.channelId;
	}

	public boolean isLeader() {
		return this.isLeader;
	}

	public void setLeader(final boolean isLeader) {
		this.isLeader = isLeader;
	}

	public boolean isOnline() {
		return this.isOnline;
	}

	public void setOnline(final boolean online) {
		this.isOnline = online;
	}

	public int getPartyId() {
		return this.partyId;
	}

	public int getMapId() {
		return this.mapId;
	}

	public String getName() {
		return this.name;
	}

	public int getCharacterId() {
		return this.characterId;
	}

	public int getJobId() {
		return this.jobId;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 59 * hash + this.characterId;
		return hash;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final PartyMember other = (PartyMember) obj;
		return this.characterId == other.characterId;
	}
}