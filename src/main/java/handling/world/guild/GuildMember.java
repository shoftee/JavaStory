package handling.world.guild;

import client.GameCharacter;
import java.io.Serializable;
import org.javastory.client.MemberRank;

public class GuildMember implements Serializable {

    public static final long serialVersionUID = 2058609046116597760L;
    private byte channelId;
    private short level;
    private int id, jobId, guildId;
    private MemberRank guildRank;
    private boolean isOnline;
    private String name;

    // either read from active character...
    // if it's online
    public GuildMember(final GameCharacter c) {
        name = c.getName();
        level = (short) c.getLevel();
        id = c.getId();
        channelId = (byte) c.getClient().getChannelId();
        jobId = c.getJob();
        guildRank = c.getGuildRank();
        guildId = c.getGuildId();
        isOnline = true;
    }

    // or we could just read from the database
    public GuildMember(final int id, final short lv, final String name, final byte channel, final int job, final MemberRank rank, final int gid, final boolean online) {
        this.level = lv;
        this.id = id;
        this.name = name;
        if (online) {
            this.channelId = channel;
        }
        this.jobId = job;
        this.isOnline = online;
        this.guildRank = rank;
        this.guildId = gid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(short l) {
        level = l;
    }

    public int getId() {
        return id;
    }

    public void setChannel(byte ch) {
        channelId = ch;
    }

    public int getChannel() {
        return channelId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int job) {
        jobId = job;
    }

    public int getGuildId() {
        return guildId;
    }

    public void setGuildId(int gid) {
        guildId = gid;
    }

    public void setGuildRank(MemberRank rank) {
        guildRank = rank;
    }

    public MemberRank getRank() {
        return guildRank;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getName() {
        return name;
    }

    public void setOnline(boolean f) {
        isOnline = f;
    }
}