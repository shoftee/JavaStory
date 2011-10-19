/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.game.Gender;

/**
 *
 * @author shoftee
 */
@SuppressWarnings("serial")
public class PlayerInfo implements Serializable {

    private int worldId;
    private int id;
    private String name;
    private Gender gender;
    private byte gmLevel;
    //
    private int hairId;
    private int faceId;
    private int skinColorId;
    //
    private int jobId;
    private int level;
    private int fame;
    
    public PlayerInfo(ResultSet rs) throws SQLException {
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
        return worldId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public byte getGmLevel() {
        return gmLevel;
    }

    public int getHairId() {
        return hairId;
    }

    public void setHairId(int hairId) {
        this.hairId = hairId;
    }

    public int getFaceId() {
        return faceId;
    }

    public void setFaceId(int faceId) {
        this.faceId = faceId;
    }

    public int getSkinColorId() {
        return skinColorId;
    }

    public void setSkinColorId(int skinColorId) {
        this.skinColorId = skinColorId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getFame() {
        return fame;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }
}
