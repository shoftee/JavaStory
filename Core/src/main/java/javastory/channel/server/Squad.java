package javastory.channel.server;

import java.util.LinkedList;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.server.TimerManager;

public class Squad {

	private ChannelCharacter leader;
	private List<ChannelCharacter> members = new LinkedList<>();
	private List<ChannelCharacter> bannedMembers = new LinkedList<>();
	private String type;
	private byte status = 0;

	public Squad(final String type, final ChannelCharacter leader, final int expiration) {
		this.leader = leader;
		this.members.add(leader);
		this.type = type;
		this.status = 1;

		scheduleRemoval(expiration);
	}

	private void scheduleRemoval(final int time) {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				members.clear();
				bannedMembers.clear();
				leader = null;
				ChannelServer.getInstance().removeMapleSquad(type);
			}
		}, time);
	}

	public ChannelCharacter getLeader() {
		return leader;
	}

	public boolean containsMember(ChannelCharacter member) {
		final int id = member.getId();
		for (ChannelCharacter mmbr : members) {
			if (id == mmbr.getId()) {
				return true;
			}
		}
		return false;
	}

	public List<ChannelCharacter> getMembers() {
		return members;
	}

	public int getSquadSize() {
		return members.size();
	}

	public boolean isBanned(ChannelCharacter member) {
		return bannedMembers.contains(member);
	}

	public int addMember(ChannelCharacter member, boolean join) {
		if (join) {
			if (!members.contains(member)) {
				if (members.size() <= 30) {
					members.add(member);
					getLeader().sendNotice(5, member.getName() + " has joined the fight!");
					return 1;
				}
				return 2;
			}
			return -1;
		} else {
			if (members.contains(member)) {
				members.remove(member);
				getLeader().sendNotice(5, member.getName() + " have withdrawed from the fight.");
				return 1;
			}
			return -1;
		}
	}

	public void acceptMember(int pos) {
		final ChannelCharacter toadd = bannedMembers.get(pos);
		if (toadd != null) {
			members.add(toadd);
			bannedMembers.remove(toadd);

			toadd.sendNotice(5, leader.getName() + " has decided to add you back to the squad.");
		}
	}

	public void banMember(int pos) {
		final ChannelCharacter toban = members.get(pos);
		if (toban == leader) {
			return;
		}
		if (toban != null) {
			members.remove(toban);
			bannedMembers.add(toban);

			toban.sendNotice(5, leader.getName() + " has removed you from the squad.");
		}
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public int getBannedMemberSize() {
		return bannedMembers.size();
	}

	public String getSquadMemberString(byte type) {
		switch (type) {
		case 0: {
			StringBuilder sb = new StringBuilder("Squad members : ");
			sb.append("#b").append(members.size()).append(" #k ").append("List of participants : \n\r ");
			int i = 0;
			for (ChannelCharacter chr : members) {
				i++;
				sb.append(i).append(" : ").append(chr.getName()).append(" ");
				if (chr == leader) {
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
			StringBuilder sb = new StringBuilder("Squad members : ");
			sb.append("#b").append(members.size()).append(" #n ").append("List of participants : \n\r ");
			int i = 0, selection = 0;
			for (ChannelCharacter chr : members) {
				i++;
				sb.append("#b#L").append(selection).append("#");
				selection++;
				sb.append(i).append(" : ").append(chr.getName()).append(" ");
				if (chr == leader) {
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
			StringBuilder sb = new StringBuilder("Squad members : ");
			sb.append("#b").append(members.size()).append(" #n ").append("List of participants : \n\r ");
			int i = 0, selection = 0;
			for (ChannelCharacter chr : bannedMembers) {
				i++;
				sb.append("#b#L").append(selection).append("#");
				selection++;
				sb.append(i).append(" : ").append(chr.getName()).append(" ");
				if (chr == leader) {
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
