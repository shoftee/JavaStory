package javastory.game.data;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.game.GameConstants;
import javastory.tools.Randomizer;

public class RandomRewards {

	private final static RandomRewards instance = new RandomRewards();
	private List<Integer> compiledGold = null;
	private List<Integer> compiledSilver = null;
	private List<Integer> compiledFishing = null;

	public static RandomRewards getInstance() {
		return instance;
	}

	protected RandomRewards() {
		System.out.println(":: Loading RandomRewards ::");
		// Gold Box
		List<Integer> returnArray = Lists.newArrayList();

		this.processRewards(returnArray, GameConstants.goldrewards);

		this.compiledGold = returnArray;

		// Silver Box
		returnArray = Lists.newArrayList();

		this.processRewards(returnArray, GameConstants.silverrewards);

		this.compiledSilver = returnArray;

		// Fishing Rewards
		returnArray = Lists.newArrayList();

		this.processRewards(returnArray, GameConstants.fishingReward);

		this.compiledFishing = returnArray;
	}

	private void processRewards(final List<Integer> returnArray, final int[] list) {
		int lastitem = 0;
		for (int i = 0; i < list.length; i++) {
			if (i % 2 == 0) { // Even
				lastitem = list[i];
			} else { // Odd
				for (int j = 0; j < list[i]; j++) {
					returnArray.add(lastitem);
				}
			}
		}
		Collections.shuffle(returnArray);
	}

	public final int getGoldBoxReward() {
		return this.compiledGold.get(Randomizer.nextInt(this.compiledGold.size()));
	}

	public final int getSilverBoxReward() {
		return this.compiledSilver.get(Randomizer.nextInt(this.compiledSilver.size()));
	}

	public final int getFishingReward() {
		return this.compiledFishing.get(Randomizer.nextInt(this.compiledFishing.size()));
	}
}
