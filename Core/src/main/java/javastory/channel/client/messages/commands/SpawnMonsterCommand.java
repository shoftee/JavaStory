package javastory.channel.client.messages.commands;

import static javastory.channel.client.messages.CommandProcessor.getNamedDoubleArg;
import static javastory.channel.client.messages.CommandProcessor.getNamedIntArg;
import static javastory.channel.client.messages.CommandProcessor.getOptionalIntArg;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import server.life.OverrideMonsterStats;

public class SpawnMonsterCommand implements Command {
	@Override
	public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		final int mid = Integer.parseInt(splitted[1]);
		final int num = Math.min(getOptionalIntArg(splitted, 2, 1), 50);
		Integer hp = getNamedIntArg(splitted, 1, "hp");
		Integer exp = getNamedIntArg(splitted, 1, "exp");
		Double php = getNamedDoubleArg(splitted, 1, "php");
		Double pexp = getNamedDoubleArg(splitted, 1, "pexp");
		final Monster onemob = LifeFactory.getMonster(mid);
		int newhp = 0, newexp = 0;

		if (hp != null) {
			newhp = hp.intValue();
		} else if (php != null) {
			newhp = (int) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
		} else {
			newhp = onemob.getMobMaxHp();
		}
		if (exp != null) {
			newexp = exp.intValue();
		} else if (pexp != null) {
			newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
		} else {
			newexp = onemob.getMobExp();
		}
		if (newhp < 1) {
			newhp = 1;
		}

		if (c.getPlayer().getGmLevel() <= 5) { // bad animal
			if (mid == 8810018 || mid == 5100001 || mid == 5130106 || mid == 8190001 || mid == 9001009 || mid == 9300256 || mid == 9300257 || mid == 9300280 || mid == 9300281 || mid == 9300282 || mid == 9300283 || mid == 9300284) {
				c.getPlayer().sendNotice(6, "This monster is blocked.");
				return;
			}
		}
		final OverrideMonsterStats overrideStats = new OverrideMonsterStats();
		overrideStats.setOHp(newhp);
		overrideStats.setOExp(newexp);
		overrideStats.setOMp(onemob.getMobMaxMp());
		for (int i = 0; i < num; i++) {
			Monster mob = LifeFactory.getMonster(mid);
			mob.setHp(newhp);
			mob.setOverrideStats(overrideStats);
			c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("spawn", "monsterid [hp newHp] [exp newExp] [php procentual Hp] [pexp procentual Exp]", "Spawns the monster with the given id", 3)
		};
	}
}