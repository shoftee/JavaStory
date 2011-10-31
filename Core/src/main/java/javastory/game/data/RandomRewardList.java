package javastory.game.data;

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

	private final List<RandomRewardEntry> entries;

	public RandomRewardList() {
		this.entries = Lists.newLinkedList();
	}

	public void addEntry(final int probability, final int itemId) {
		this.entries.add(new RandomRewardEntry(probability, itemId));
	}

	public void addEntry(final RandomRewardEntry entry) {
		this.entries.add(entry);
	}

	public RandomRewardFactory build() {
		return new ConcreteRandomRewardFactory(this.entries);
	}

	static class ConcreteRandomRewardFactory implements RandomRewardFactory {

		private final List<RandomRewardEntry> entries;
		private int total;

		public ConcreteRandomRewardFactory(final List<RandomRewardEntry> list) {

			final ArrayList<RandomRewardEntry> shuffledList = Lists.newArrayList(list);
			Collections.shuffle(shuffledList);

			this.entries = Lists.newArrayListWithExpectedSize(shuffledList.size());

			for (final RandomRewardEntry entry : shuffledList) {
				this.entries.add(entry);
				this.total += entry.Probability;
			}
		}

		@Override
		public int getRandomItem() {
			int factor = Randomizer.nextInt(this.total);
			int itemId = -1;
			for (final RandomRewardEntry entry : this.entries) {
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
