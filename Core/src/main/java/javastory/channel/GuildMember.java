package javastory.channel;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.Serializable;


import javastory.client.MemberRank;

public class GuildMember implements Serializable {

    public static final long serialVersionUID = 2058609046116597760L;
    private byte channelId;
    private int level;
    private int characterId, jobId, guildId;
    private MemberRank guildRank;
    private boolean isOnline;
    private String name;

    // either read from active character...
    // if it's online
    public GuildMember(final ChannelCharacter character) {
        name = character.getName();
        level = character.getLevel();
        characterId = character.getId();
        channelId = (byte) character.getClient().getChannelId();
        jobId = character.getJobId();
        guildRank = character.getGuildRank();
        guildId = character.getGuildId();
        isOnline = true;
    }

    public GuildMember(ResultSet rs) throws SQLException {
        this.characterId = rs.getInt("id");
        this.level = rs.getShort("level");
        this.name = rs.getString("name");
        this.channelId = (byte) -1;
        this.jobId = rs.getInt("job");
        this.guildRank = MemberRank.fromNumber(rs.getInt("guildrank"));
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCharacterId() {
        return characterId;
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