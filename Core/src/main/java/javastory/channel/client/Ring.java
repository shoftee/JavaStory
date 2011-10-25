package javastory.channel.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

	public static Ring loadFromDb(int ringId) {
		Connection c = Database.getConnection();
		try (	PreparedStatement ps = getRingById(ringId, c);
				ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				final int partnerRingId = rs.getInt("partnerRingId");
				final int partnerCharacterId = rs.getInt("partnerChrId");
				final int ringItemId = rs.getInt("itemid");
				final String partnerName = rs.getString("partnerName");
				return new Ring(ringId, partnerRingId, partnerCharacterId, ringItemId, partnerName);
			}
		} catch (SQLException ex) {
			System.err.println("Could not load ring from database: " + ex);
		}
		return null;
	}

	private static PreparedStatement getRingById(int ringId, Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?");
		ps.setInt(1, ringId);
		return ps;
	}

	public static int createRing(int itemid, final int partner1Id, final String partner1Name, final int partner2Id, final String partner2Name) {
		// TODO: Check if item ID is actually a ring.

		if (checkRingDB(partner1Id) || checkRingDB(partner2Id)) {
			return 0;
		}

		int[] ringID = new int[2];
		Connection con = Database.getConnection();

		try {
			PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, itemid);
			ps.setInt(2, partner2Id);
			ps.setString(3, partner2Name);
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
			ps.setInt(3, partner1Id);
			ps.setString(4, partner1Name);
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

			return 1;
		} catch (SQLException ex) {
			return 0;
		}

		// TODO: Make these work!
		//		InventoryManipulator.addRing(partner1, itemid, ringID[0]);
		//		InventoryManipulator.addRing(partner2, itemid, ringID[1]);
		// TODO: resend character info and respawn both.
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

	public static boolean checkRingDB(int characterId) {
		Connection con = Database.getConnection();
		try (	PreparedStatement ps = getSelectRingIdByPartnerId(characterId, con);
				ResultSet rs = ps.executeQuery()) {
			return rs.next();
		} catch (SQLException ex) {
			// TS NOTE: This used to return true. Very wrong. Failure is appropriate for this case.
			return false;
		}
	}

	private static PreparedStatement getSelectRingIdByPartnerId(int characterId, Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT id FROM rings WHERE partnerChrId = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	public static void removeRingFromDb(int characterId) {
		int otherId;
		Connection con = Database.getConnection();
		try (	PreparedStatement ps = getSelectRingByPartnerId(characterId, con);
				ResultSet rs = ps.executeQuery()) {
			otherId = rs.getInt("partnerRingId");

			removeRing(characterId, con);
			removeRing(otherId, con);
		} catch (SQLException sex) {
			return;
		}
	}

	private static void removeRing(int characterId, Connection con) throws SQLException {
		try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM rings WHERE partnerChrId = ?")) {
			ps2.setInt(1, characterId);
			ps2.executeUpdate();
		}
	}

	private static PreparedStatement getSelectRingByPartnerId(int characterId, Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT partnerRingId FROM rings WHERE partnerChrId = ?");
		ps.setInt(1, characterId);
		return ps;
	}
}