package javastory.game.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javastory.db.Database;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class MobDropInfoProvider {

	private static final MobDropInfoProvider instance = new MobDropInfoProvider();
	private final Map<Integer, List<MobDropInfo>> drops = Maps.newHashMap();
	private final List<MobGlobalDropInfo> globaldrops = Lists.newArrayList();

	private MobDropInfoProvider() {
		this.retrieveGlobal();
	}

	public static MobDropInfoProvider getInstance() {
		return instance;
	}

	public List<MobGlobalDropInfo> getGlobalDrop() {
		return this.globaldrops;
	}

	private void retrieveGlobal() {
		final Connection con = Database.getConnection();

		try (	PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0");
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				final int itemId = rs.getInt("itemid");
				final int chance = rs.getInt("chance");
				final int continent = rs.getInt("continent");
				final byte type = rs.getByte("dropType");
				final int minQuantity = rs.getInt("minimum_quantity");
				final int maxQuantity = rs.getInt("maximum_quantity");
				final short questId = rs.getShort("questid");
				final MobGlobalDropInfo entry = new MobGlobalDropInfo(itemId, chance, continent, type, minQuantity, maxQuantity, questId);
				this.globaldrops.add(entry);
			}
		} catch (final SQLException e) {
			System.err.println("Error retrieving drop" + e);
		}
	}

	public List<MobDropInfo> retrieveDrop(final int monsterId) {
		if (this.drops.containsKey(monsterId)) {
			return this.drops.get(monsterId);
		}

		final Connection con = Database.getConnection();

		final List<MobDropInfo> ret = Lists.newLinkedList();
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data WHERE dropperid = ?")) {
			ps.setInt(1, monsterId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final int itemId = rs.getInt("itemid");
					final int chance = rs.getInt("chance");
					final int minQuantity = rs.getInt("minimum_quantity");
					final int maxQuantity = rs.getInt("maximum_quantity");
					final short questId = rs.getShort("questid");
					final MobDropInfo entry = new MobDropInfo(itemId, chance, minQuantity, maxQuantity, questId);
					ret.add(entry);
				}
			}
		} catch (final SQLException e) {
			return ret;
		}
		this.drops.put(monsterId, ret);
		return ret;
	}

	public void clearDrops() {
		this.drops.clear();
		this.globaldrops.clear();
		this.retrieveGlobal();
	}
}