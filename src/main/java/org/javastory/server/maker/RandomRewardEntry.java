/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server.maker;

/**
 *
 * @author Tosho
 */
public class RandomRewardEntry {

    private int probability;
    private int itemId;

    public RandomRewardEntry(int probability, int itemId) {
        this.probability = probability;
        this.itemId = itemId;
    }

    public int getProbability() {
        return probability;
    }

    public int getItemId() {
        return itemId;
    }
}
