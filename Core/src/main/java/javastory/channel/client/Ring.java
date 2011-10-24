package javastory.channel.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javastory.channel.ChannelCharacter;
import javastory.channel.server.InventoryManipulator;
import javastory.db.Database;

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

	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.
	public static Ring loadFromDb(int ringId) {
		Connection con = Database.getConnection();
		Ring ret = null;
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?")) {
			ps.setInt(1, ringId);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				ret = new Ring(ringId, rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"));
			}
		} catch (SQLException ex) {
			System.err.println("Could not load ring from database: " + ex);
		}
		return ret;
	}

	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.
	public static int createRing(int itemid, final ChannelCharacter partner1, final ChannelCharacter partner2) {
		try {
			if (partner1 == null) {
				return -2; // Partner Number 1 is not on the same channel.
			} else if (partner2 == null) {
				return -1; // Partner Number 2 is not on the same channel.
			} else if (checkRingDB(partner1.getId()) || checkRingDB(partner2.getId())) {
				return 0; // Error or Already have ring.
			}

			int[] ringID = new int[2];
			Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, itemid);
			ps.setInt(2, partner2.getId());
			ps.setString(3, partner2.getName());
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			ringID[0] = rs.getInt(1);
			rs.close();
			ps.close();

			ps = con.prepareStatement("INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
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

			// TODO: resend character info and respawn both.
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

	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.
	public static boolean checkRingDB(int characterId) {
		try {
			Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id FROM rings WHERE partnerChrId = ?");
			ps.setInt(1, characterId);
			ResultSet rs = ps.executeQuery();
			return rs.next();
		} catch (SQLException pie) {
			return true;
		}
	}

	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.
	public static void removeRingFromDb(int characterId) {
		try {
			Connection con = Database.getConnection();
			int otherId;
			PreparedStatement ps = con.prepareStatement("SELECT partnerRingId FROM rings WHERE partnerChrId = ?");
			ps.setInt(1, characterId);
			ResultSet rs = ps.executeQuery();
			rs.next();
			otherId = rs.getInt("partnerRingId");
			rs.close();
			ps = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?");
			ps.setInt(1, characterId);
			ps.executeUpdate();
			ps = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?");
			ps.setInt(1, otherId);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException sex) {
		}
	}
}