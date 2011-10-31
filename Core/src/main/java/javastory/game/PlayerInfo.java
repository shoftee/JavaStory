
package javastory.game;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * 
 * @author shoftee
 */
@SuppressWarnings("serial")
public class PlayerInfo implements Serializable {

	private final int worldId;
	private final int id;
	private final String name;
	private final Gender gender;
	private final byte gmLevel;
	//
	private int hairId;
	private int faceId;
	private int skinColorId;
	//
	private int jobId;
	private int level;
	private int fame;

	public PlayerInfo(final ResultSet rs) throws SQLException {
		this.worldId = rs.getInt("world");
		this.id = rs.getInt("id");
		this.name = rs.getString("name");
		this.gender = Gender.fromNumber(rs.getInt("gender"));
		this.gmLevel = rs.getByte("gm");

		this.hairId = rs.getInt("hair");
		this.faceId = rs.getInt("face");
		this.skinColorId = rs.getInt("skincolor");

		this.jobId = rs.getInt("job");
		this.level = rs.getInt("level");
		this.fame = rs.getInt("fame");
	}

	public int getWorldId() {
		return this.worldId;
	}

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Gender getGender() {
		return this.gender;
	}

	public byte getGmLevel() {
		return this.gmLevel;
	}

	public int getHairId() {
		return this.hairId;
	}

	public void setHairId(final int hairId) {
		this.hairId = hairId;
	}

	public int getFaceId() {
		return this.faceId;
	}

	public void setFaceId(final int faceId) {
		this.faceId = faceId;
	}

	public int getSkinColorId() {
		return this.skinColorId;
	}

	public void setSkinColorId(final int skinColorId) {
		this.skinColorId = skinColorId;
	}

	public int getJobId() {
		return this.jobId;
	}

	public void setJobId(final int jobId) {
		this.jobId = jobId;
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(final int level) {
		this.level = level;
	}

	public int getFame() {
		return this.fame;
	}

	public void setFame(final int fame) {
		this.fame = fame;
	}
}
