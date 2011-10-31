package javastory.channel.client;

import java.awt.Point;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javastory.channel.movement.AbsoluteLifeMovement;
import javastory.channel.movement.LifeMovement;
import javastory.channel.movement.LifeMovementFragment;
import javastory.db.Database;
import javastory.game.data.ItemInfoProvider;

public class Pet implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;

	private String name;
	private int foothold = 0, stance = 0, fullness = 100, level = 1, closeness = 0, uniqueId;

	private final int petItemId;
	private Point pos;
	private short inventoryPosition = 0;
	private boolean summoned;

	private Pet(final int itemId) {
		this.petItemId = itemId;
	}

	private Pet(final int petItemId, final int uniqueId, final short inventorypos) {
		this.petItemId = petItemId;
		this.uniqueId = uniqueId;
		this.summoned = false;
		this.inventoryPosition = inventorypos;
	}

	public static Pet loadFromDb(final int itemId, final int petid, final short inventorypos) {
		try {
			final Pet ret = new Pet(itemId, petid, inventorypos);

			final Connection con = Database.getConnection();

			// Get pet details..
			final PreparedStatement ps = con.prepareStatement("SELECT * FROM pets WHERE petid = ?");
			ps.setInt(1, petid);

			final ResultSet rs = ps.executeQuery();
			rs.next();

			ret.setName(rs.getString("name"));
			ret.setCloseness(rs.getInt("closeness"));
			ret.setLevel(rs.getInt("level"));
			ret.setFullness(rs.getInt("fullness"));

			rs.close();
			ps.close();

			return ret;
		} catch (final SQLException ex) {
			Logger.getLogger(Pet.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public final void saveToDb() {
		try {
			final PreparedStatement ps = Database.getConnection().prepareStatement(
				"UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ? WHERE petid = ?");
			ps.setString(1, this.name); // Set name
			ps.setInt(2, this.level); // Set Level
			ps.setInt(3, this.closeness); // Set Closeness
			ps.setInt(4, this.fullness); // Set Fullness
			ps.setInt(5, this.uniqueId); // Set ID
			ps.executeUpdate(); // Execute statement
			ps.close();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static Pet createPet(final int petItemId) {
		int ret;
		try { // Commit to db first
			final PreparedStatement ps = Database.getConnection().prepareStatement("INSERT INTO pets (name, level, closeness, fullness) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, ItemInfoProvider.getInstance().getName(petItemId));
			ps.setInt(2, 1);
			ps.setInt(3, 0);
			ps.setInt(4, 100);
			ps.executeUpdate();

			final ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			ret = rs.getInt(1);
			rs.close();
			ps.close();
		} catch (final SQLException ex) {
			ex.printStackTrace();
			return null;
		}
		final Pet pet = new Pet(petItemId);
		pet.setName(ItemInfoProvider.getInstance().getName(petItemId));
		pet.setLevel(1);
		pet.setCloseness(0);
		pet.setFullness(100);
		pet.setUniqueId(ret);

		return pet;
	}

	public final String getName() {
		return this.name;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final boolean isSummoned() {
		return this.summoned;
	}

	public final void setSummoned(final boolean summoned) {
		this.summoned = summoned;
	}

	public final short getInventoryPosition() {
		return this.inventoryPosition;
	}

	public final void setInventoryPosition(final short inventorypos) {
		this.inventoryPosition = inventorypos;
	}

	public int getUniqueId() {
		return this.uniqueId;
	}

	public void setUniqueId(final int id) {
		this.uniqueId = id;
	}

	public final int getCloseness() {
		return this.closeness;
	}

	public final void setCloseness(final int closeness) {
		this.closeness = closeness;
	}

	public final int getLevel() {
		return this.level;
	}

	public final void setLevel(final int level) {
		this.level = level;
	}

	public final int getFullness() {
		return this.fullness;
	}

	public final void setFullness(final int fullness) {
		this.fullness = fullness;
	}

	public final int getFoothold() {
		return this.foothold;
	}

	public final void setFoothold(final int Fh) {
		this.foothold = Fh;
	}

	public final Point getPosition() {
		return this.pos;
	}

	public final void setPosition(final Point pos) {
		this.pos = pos;
	}

	public final int getStance() {
		return this.stance;
	}

	public final void setStance(final int stance) {
		this.stance = stance;
	}

	public final int getPetItemId() {
		return this.petItemId;
	}

	public final boolean canConsume(final int itemId) {
		final ItemInfoProvider mii = ItemInfoProvider.getInstance();
		for (final int petId : mii.petsCanConsume(itemId)) {
			if (petId == itemId) {
				return true;
			}
		}
		return false;
	}

	public final void updatePosition(final List<LifeMovementFragment> movement) {
		for (final LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					this.setPosition(((LifeMovement) move).getPosition());
				}
				this.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}
