package javastory.game.data;

import com.google.common.collect.ImmutableList;

public class RewardSetInfo {
	
	public final int TotalProbability;
	public final ImmutableList<RewardItemInfo> Rewards;
	
	public RewardSetInfo(Iterable<RewardItemInfo> rewards) {
		
		this.Rewards = ImmutableList.copyOf(rewards);
		
		int total = 0;
		for(RewardItemInfo reward : this.Rewards) {
			total += reward.Probability;
		}
		this.TotalProbability = total;
	}
}
