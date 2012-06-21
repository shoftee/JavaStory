package javastory.channel.server;

import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.server.TimerManager;

public class Squad {

	private ChannelCharacter leader;
	private final List<ChannelCharacter> members = Lists.newLinkedList();
	private final List<ChannelCharacter> bannedMembers = Lists.newLinkedList();
	private final String type;
	private byte status = 0;

	public Squad(final String type, final ChannelCharacter leader, final int expiration) {
		this.leader = leader;
		this.members.add(leader);
		this.type = type;
		this.status = 1;

		this.scheduleRemoval(expiration);
	}

	private void scheduleRemoval(final int time) {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				Squad.this.members.clear();
				Squad.this.bannedMembers.clear();
				Squad.this.leader = null;
				ChannelServer.getInstance().removeMapleSquad(Squad.this.type);
			}
		}, time);
	}

	public ChannelCharacter getLeader() {
		return this.leader;
	}

	public boolean containsMember(final ChannelCharacter member) {
		final int id = member.getId();
		for (final ChannelCharacter mmbr : this.members) {
			if (id == mmbr.getId()) {
				return true;
			}
		}
		return false;
	}

	public List<ChannelCharacter> getMembers() {
		return this.members;
	}

	public int getSquadSize() {
		return this.members.size();
	}

	public boolean isBanned(final ChannelCharacter member) {
		return this.bannedMembers.contains(member);
	}

	public int addMember(final ChannelCharacter member, final boolean join) {
		if (join) {
			if (!this.members.contains(member)) {
				if (this.members.size() <= 30) {
					this.members.add(member);
					this.getLeader().sendNotice(5, member.getName() + " has joined the fight!");
					return 1;
				}
				return 2;
			}
			return -1;
		} else {
			if (this.members.contains(member)) {
				this.members.remove(member);
				this.getLeader().sendNotice(5, member.getName() + " have withdrawed from the fight.");
				return 1;
			}
			return -1;
		}
	}

	public void acceptMember(final int pos) {
		final ChannelCharacter toadd = this.bannedMembers.get(pos);
		if (toadd != null) {
			this.members.add(toadd);
			this.bannedMembers.remove(toadd);

			toadd.sendNotice(5, this.leader.getName() + " has decided to add you back to the squad.");
		}
	}

	public void banMember(final int pos) {
		final ChannelCharacter toban = this.members.get(pos);
		if (toban == this.leader) {
			return;
		}
		if (toban != null) {
			this.members.remove(toban);
			this.bannedMembers.add(toban);

			toban.sendNotice(5, this.leader.getName() + " has removed you from the squad.");
		}
	}

	public void setStatus(final byte status) {
		this.status = status;
	}

	public int getStatus() {
		return this.status;
	}

	public int getBannedMemberSize() {
		return this.bannedMembers.size();
	}

	public String getSquadMemberString(final byte type) {
		switch (type) {
		case 0: {
			final StringBuilder sb = new StringBuilder("Squad members : ");
			sb.append("#b").append(this.members.size()).append(" #k ").append("List of participants : \n\r ");
			int i = 0;
			for (final ChannelCharacter chr : this.members) {
				i++;
				sb.append(i).append(" : ").append(chr.getName()).append(" ");
				if (chr == this.leader) {
					sb.append("(Leader of the squad)");
				}
				sb.append(" \n\r ");
			}
			while (i < 30) {
				i++;
				sb.append(i).append(" : ").append(" \n\r ");
			}
			return sb.toString();
		}
		case 1: {
			final StringBuilder sb = new StringBuilder("Squad members : ");
			sb.append("#b").append(this.members.size()).append(" #n ").append("List of participants : \n\r ");
			int i = 0, selection = 0;
			for (final ChannelCharacter chr : this.members) {
				i++;
				sb.append("#b#L").append(selection).append("#");
				selection++;
				sb.append(i).append(" : ").append(chr.getName()).append(" ");
				if (chr == this.leader) {
					sb.append("(Leader of the squad)");
				}
				sb.append("#l").append(" \n\r ");
			}
			while (i < 30) {
				i++;
				sb.append(i).append(" : ").append(" \n\r ");
			}
			return sb.toString();
		}
		case 2: {
			final StringBuilder sb = new StringBuilder("Squad members : ");
			sb.append("#b").append(this.members.size()).append(" #n ").append("List of participants : \n\r ");
			int i = 0, selection = 0;
			for (final ChannelCharacter chr : this.bannedMembers) {
				i++;
				sb.append("#b#L").append(selection).append("#");
				selection++;
				sb.append(i).append(" : ").append(chr.getName()).append(" ");
				if (chr == this.leader) {
					sb.append("(Leader of the squad)");
				}
				sb.append("#l").append(" \n\r ");
			}
			while (i < 30) {
				i++;
				sb.append(i).append(" : ").append(" \n\r ");
			}
			return sb.toString();
		}
		}
		return null;
	}
}
