/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server.maker;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import javastory.tools.Randomizer;

/**
 *
 * @author Tosho
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

    class ConcreteRandomRewardFactory implements RandomRewardFactory {

        private List<RandomRewardEntry> entries;
        private int total;
        
        public ConcreteRandomRewardFactory(List<RandomRewardEntry> list) {
            this.entries = Lists.newArrayList();
            for(RandomRewardEntry entry : entries) {
                this.entries.add(entry);
                this.total += entry.getProbability();
            }
        }       
        
        @Override
		public int getRandomItem() {
            int factor = Randomizer.nextInt(total);
            int itemId = -1;
            for(RandomRewardEntry entry : entries) {
                factor -= entry.getProbability();
                if (factor <= 0) {
                    itemId = entry.getItemId();
                    break;
                }
            }
            Preconditions.checkState(itemId != -1);
            return itemId;
        }
    }
}
