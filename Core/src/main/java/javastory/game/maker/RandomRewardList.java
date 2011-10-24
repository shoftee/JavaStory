/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.game.maker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javastory.tools.Randomizer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 *
 * @author shoftee
 */
public class RandomRewardList {

    private List<RandomRewardEntry> entries;

    public RandomRewardList() {
        entries = Lists.newLinkedList();
    }

    public void addEntry(int probability, int itemId) {
        entries.add(new RandomRewardEntry(probability, itemId));
    }

    public void addEntry(RandomRewardEntry entry) {
        entries.add(entry);
    }

    public RandomRewardFactory build() {
        return new ConcreteRandomRewardFactory(entries);
    }

    static class ConcreteRandomRewardFactory implements RandomRewardFactory {

        private List<RandomRewardEntry> entries;
        private int total;
        
        public ConcreteRandomRewardFactory(List<RandomRewardEntry> list) {
        	
            ArrayList<RandomRewardEntry> shuffledList = Lists.newArrayList(list);
            Collections.shuffle(shuffledList);
            
            this.entries = Lists.newArrayListWithExpectedSize(shuffledList.size());
            
            for(RandomRewardEntry entry : shuffledList) {
                this.entries.add(entry);
                this.total += entry.Probability;
            }
        }       
        
        @Override
		public int getRandomItem() {
            int factor = Randomizer.nextInt(total);
            int itemId = -1;
            for(RandomRewardEntry entry : entries) {
                factor -= entry.Probability;
                if (factor <= 0) {
                    itemId = entry.ItemId;
                    break;
                }
            }
            Preconditions.checkState(itemId != -1);
            return itemId;
        }
    }
}
