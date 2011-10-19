/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.game.maker;

/**
 *
 * @author Tosho
 */
public class RandomRewardEntry {

    public final int Probability;
    public final int ItemId;

    public RandomRewardEntry(int probability, int itemId) {
        this.Probability = probability;
        this.ItemId = itemId;
    }
}
