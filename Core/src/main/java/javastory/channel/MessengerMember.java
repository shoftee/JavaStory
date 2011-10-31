package javastory.channel;

import java.io.Serializable;

public class MessengerMember implements Serializable {

	private static final long serialVersionUID = 6215463252132450750L;
	private final String name;
	private final int characterId;
	private final int channelId;
	private boolean isOnline;
	private int position;

	public MessengerMember(final ChannelCharacter character) {
		this.name = character.getName();
		this.channelId = character.getClient().getChannelId();
		this.characterId = character.getId();
		this.isOnline = true;
		this.position = 0;
	}

	public MessengerMember(final ChannelCharacter character, final int position) {
		this.name = character.getName();
		this.channelId = character.getClient().getChannelId();
		this.characterId = character.getId();
		this.isOnline = true;
		this.position = position;
	}

	public int getChannel() {
		return this.channelId;
	}

	public boolean isOnline() {
		return this.isOnline;
	}

	public void setOnline(final boolean online) {
		this.isOnline = online;
	}

	public String getName() {
		return this.name;
	}

	public int getId() {
		return this.characterId;
	}

	public int getPosition() {
		return this.position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19 * hash + this.characterId;
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
		final MessengerMember other = (MessengerMember) obj;
		return this.characterId == other.characterId;
	}
}