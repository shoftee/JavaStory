package handling.world;

import java.io.Serializable;

import client.GameCharacter;

public class MessengerCharacter implements Serializable {
	private static final long serialVersionUID = 6215463252132450750L;
	private String name;
	private int id;
	private int channel;
	private boolean online;
	private int position;

	public MessengerCharacter(GameCharacter maplechar) {
		this.name = maplechar.getName();
		this.channel = maplechar.getClient().getChannelId();
		this.id = maplechar.getId();
		this.online = true;
		this.position = 0;
	}

	public MessengerCharacter(GameCharacter maplechar, int position) {
		this.name = maplechar.getName();
		this.channel = maplechar.getClient().getChannelId();
		this.id = maplechar.getId();
		this.online = true;
		this.position = position;
	}

	public MessengerCharacter() {
		this.name = "";
		//default values for everything o.o
	}

	public int getChannel() {
		return channel;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
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
		final MessengerCharacter other = (MessengerCharacter) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}