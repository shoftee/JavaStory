/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.channel.client;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import javastory.game.Inventory;
import javastory.game.InventoryType;

import com.google.common.collect.Maps;

/**
 * 
 * @author shoftee
 */
public class MultiInventory implements Iterable<Inventory>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6648952265986974408L;
	
	private final Map<InventoryType, Inventory> inventories;

	public MultiInventory() {
		this.inventories = Maps.newEnumMap(InventoryType.class);
	}

	public Inventory get(final InventoryType type) {
		Inventory inventory = this.inventories.get(type);
		if (inventory == null) {
			inventory = addNewInventory(type);
		}
		return inventory;
	}
	
	private Inventory addNewInventory(InventoryType type) {
		final Inventory inventory = new Inventory(type);
		this.inventories.put(type, inventory);
		return inventory;
	}
	
	@Override
	public Iterator<Inventory> iterator() {
		return this.inventories.values().iterator();
	}
}
