/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.client;

import client.Inventory;
import client.InventoryType;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Tosho
 */
public class MultiInventory implements Iterable<Inventory> {
    private Map<InventoryType, Inventory> tabs;
    
    public MultiInventory() {
        this.tabs = Maps.newEnumMap(InventoryType.class);
    }
    
    public Inventory get(InventoryType type) {
        Inventory inventory = this.tabs.get(type);
        if (inventory == null) {
            inventory = new Inventory(type);
            this.tabs.put(type, inventory);
        }
        return inventory;
    }

    public Iterator<Inventory> iterator() {
        return this.tabs.values().iterator();
    }
}
