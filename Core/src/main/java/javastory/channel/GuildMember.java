package javastory.channel;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.channel.client.MemberRank;

public class GuildMember implements Serializable {

	public static final long serialVersionUID = 2058609046116597760L;
	private byte channelId;
	private int level;
	private final int characterId;
	private int jobId;
	private int guildId;
	private MemberRank guildRank;
	private boolean isOnline;
	private final String name;

	// either read from active character...
	// if it's online
	public GuildMember(final ChannelCharacter character) {
		this.name = character.getName();
		this.level = character.getLevel();
		this.characterId = character.getId();
		this.channelId = (byte) character.getClient().getChannelId();
		this.jobId = character.getJobId();
		this.guildRank = character.getGuildRank();
		this.guildId = character.getGuildId();
		this.isOnline = true;
	}

	public GuildMember(final ResultSet rs) throws SQLException {
		this.characterId = rs.getInt("id");
		this.level = rs.getShort("level");
		this.name = rs.getString("name");
		this.channelId = (byte) -1;
		this.jobId = rs.getInt("job");
		this.guildRank = MemberRank.fromNumber(rs.getInt("guildrank"));
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(final int level) {
		this.level = level;
	}

	public int getCharacterId() {
		return this.characterId;
	}

	public void setChannel(final byte ch) {
		this.channelId = ch;
	}

	public int getChannel() {
		return this.channelId;
	}

	public int getJobId() {
		return this.jobId;
	}

	public void setJobId(final int job) {
		this.jobId = job;
	}

	public int getGuildId() {
		return this.guildId;
	}

	public void setGuildId(final int gid) {
		this.guildId = gid;
	}

	public void setGuildRank(final MemberRank rank) {
		this.guildRank = rank;
	}

	public MemberRank getRank() {
		return this.guildRank;
	}

	public boolean isOnline() {
		return this.isOnline;
	}

	public String getName() {
		return this.name;
	}

	public void setOnline(final boolean f) {
		this.isOnline = f;
	}
}