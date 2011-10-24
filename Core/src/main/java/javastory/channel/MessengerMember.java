package javastory.channel;

import java.io.Serializable;

public class MessengerMember implements Serializable {

	private static final long serialVersionUID = 6215463252132450750L;
	private String name;
	private int characterId;
	private int channelId;
	private boolean isOnline;
	private int position;

	public MessengerMember(ChannelCharacter character) {
		this.name = character.getName();
		this.channelId = character.getClient().getChannelId();
		this.characterId = character.getId();
		this.isOnline = true;
		this.position = 0;
	}

	public MessengerMember(ChannelCharacter character, int position) {
		this.name = character.getName();
		this.channelId = character.getClient().getChannelId();
		this.characterId = character.getId();
		this.isOnline = true;
		this.position = position;
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

	public String getName() {
		return name;
	}

	public int getId() {
		return characterId;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19 * hash + this.characterId;
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
		final MessengerMember other = (MessengerMember) obj;
		return this.characterId == other.characterId;
	}
}