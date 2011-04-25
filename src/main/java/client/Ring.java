package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import database.DatabaseConnection;
import java.sql.Statement;
import server.InventoryManipulator;
import tools.MaplePacketCreator;

public class Ring implements Comparable<Ring> {

    private int ringId;
    private int partnerRingId;
    private int partnerId;
    private int itemId;
    private String partnerName;
    private boolean equipped;

    private Ring(int ringId, int partnerRingId, int partnerId, int itemid, String partnername) {
        this.ringId = ringId;
        this.partnerRingId = partnerRingId;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnername;
    }

    public static Ring loadFromDb(int ringId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?");
            ps.setInt(1, ringId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            Ring ret = new Ring(ringId, rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"));

            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            return null;
        }
    }

    public static int createRing(int itemid, final GameCharacter partner1, final GameCharacter partner2) {
        try {
            if (partner1 == null) {
                return -2; // Partner Number 1 is not on the same channel.
            } else if (partner2 == null) {
                return -1; // Partner Number 2 is not on the same channel.
            } else if (checkRingDB(partner1) || checkRingDB(partner2)) {
                return 0; // Error or Already have ring.
            }

            int[] ringID = new int[2];
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, itemid);
            ps.setInt(2, partner2.getId());
            ps.setString(3, partner2.getName());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            ringID[0] = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, itemid);
            ps.setInt(2, ringID[0]);
            ps.setInt(3, partner1.getId());
            ps.setString(4, partner1.getName());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            ringID[1] = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("UPDATE rings SET partnerRingId = ? WHERE id = ?");
            ps.setInt(1, ringID[1]);
            ps.setInt(2, ringID[0]);
            ps.executeUpdate();
            ps.close();

            InventoryManipulator.addRing(partner1, itemid, ringID[0]);
            InventoryManipulator.addRing(partner2, itemid, ringID[1]);

	    partner1.getClient().write(MaplePacketCreator.getCharInfo(partner1));
	    partner1.getMap().removePlayer(partner1);
	    partner1.getMap().addPlayer(partner1);
	    partner2.getClient().write(MaplePacketCreator.getCharInfo(partner2));
	    partner2.getMap().removePlayer(partner2);
	    partner2.getMap().addPlayer(partner2);

            partner1.sendNotice(5, "Please log off and log back in if the rings do not work.");
            partner2.sendNotice(5, "Please log off and log back in if the rings do not work.");
            return 1;
        } catch (SQLException ex) {
            return 0;
        }
    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return partnerRingId;
    }

    public int getPartnerCharacterId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Ring) {
            if (((Ring) o).getRingId() == getRingId()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    @Override
    public int compareTo(Ring other) {
        if (ringId < other.getRingId()) {
            return -1;
        } else if (ringId == other.getRingId()) {
            return 0;
        } else {
            return 1;
        }
    }

    public static boolean checkRingDB(GameCharacter player) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException pie) {
            return true;
        }
    }

    public static void removeRingFromDb(GameCharacter player) {
        try {
            Connection con = DatabaseConnection.getConnection();
            int otherId;
            PreparedStatement ps = con.prepareStatement("SELECT partnerRingId FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();
            rs.next();
            otherId = rs.getInt("partnerRingId");
            rs.close();
            ps = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ps.executeUpdate();
            ps = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, otherId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sex) {
        }
    }
}