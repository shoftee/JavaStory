package server;

import java.util.LinkedList;
import java.util.List;

import client.GameCharacter;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;

public class Squad {

    private GameCharacter leader;
    private List<GameCharacter> members = new LinkedList<GameCharacter>();
    private List<GameCharacter> bannedMembers = new LinkedList<GameCharacter>();
    private int ch;
    private String type;
    private byte status = 0;

    public Squad(final int ch, final String type, final GameCharacter leader, final int expiration) {
	this.leader = leader;
	this.members.add(leader);
	this.ch = ch;
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
		ChannelManager.getInstance(ch).removeMapleSquad(type);
	    }
	}, time);
    }

    public GameCharacter getLeader() {
	return leader;
    }

    public boolean containsMember(GameCharacter member) {
	final int id = member.getId();
	for (GameCharacter mmbr : members) {
	    if (id == mmbr.getId()) {
		return true;
	    }
	}
	return false;
    }

    public List<GameCharacter> getMembers() {
	return members;
    }

    public int getSquadSize() {
	return members.size();
    }

    public boolean isBanned(GameCharacter member) {
	return bannedMembers.contains(member);
    }

    public int addMember(GameCharacter member, boolean join) {
	if (join) {
	    if (!members.contains(member)) {
		if (members.size() <= 30) {
		    members.add(member);
		    getLeader().dropMessage(5, member.getName() + " has joined the fight!");
		    return 1;
		}
		return 2;
	    }
	    return -1;
	} else {
	    if (members.contains(member)) {
		members.remove(member);
		getLeader().dropMessage(5, member.getName() + " have withdrawed from the fight.");
		return 1;
	    }
	    return -1;
	}
    }

    public void acceptMember(int pos) {
	final GameCharacter toadd = bannedMembers.get(pos);
	if (toadd != null) {
	    members.add(toadd);
	    bannedMembers.remove(toadd);

	    toadd.dropMessage(5, leader.getName() + " has decided to add you back to the squad.");
	}
    }

    public void banMember(int pos) {
	final GameCharacter toban = members.get(pos);
	if (toban == leader) {
	    return;
	}
	if (toban != null) {
	    members.remove(toban);
	    bannedMembers.add(toban);

	    toban.dropMessage(5, leader.getName() + " has removed you from the squad.");
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
		for (GameCharacter chr : members) {
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
		for (GameCharacter chr : members) {
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
		for (GameCharacter chr : bannedMembers) {
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
