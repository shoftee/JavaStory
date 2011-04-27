package client.messages.commands;

import org.javastory.client.ChannelCharacter;
import org.javastory.client.ChannelClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;

public class CharInfoCommand implements Command {
	@Override
	public void execute(ChannelClient c, String[] splittedLine) throws Exception, IllegalCommandSyntaxException {
		final StringBuilder builder = new StringBuilder();
		final ChannelCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splittedLine[1]);
		builder.append(ChannelClient.getLogMessage(other, ""));
		builder.append(" at ");
		builder.append(" x : ");
		builder.append(other.getPosition().x);
		builder.append(" y : ");
		builder.append(other.getPosition().y);
		builder.append(" fh : ");
		builder.append(other.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
		builder.append(" cy : ");
		builder.append(other.getPosition().y);
		builder.append(" rx0 : ");
		builder.append(other.getPosition().x + 50);
		builder.append(" rx1 : ");
		builder.append(other.getPosition().x - 50);
		builder.append(" || HP : ");
		builder.append(other.getStats().getHp());
		builder.append(" /");
		builder.append(other.getStats().getCurrentMaxHp());
		builder.append(" || MP : ");
		builder.append(other.getStats().getMp());
		builder.append(" /");
		builder.append(other.getStats().getCurrentMaxMp());
		builder.append(" || WATK : ");
		builder.append(other.getStats().getTotalWatk());
		builder.append(" || MATK : ");
		builder.append(other.getStats().getTotalMagic());
		builder.append(" || EXP : ");
		builder.append(other.getExp());
		builder.append(" || hasParty : ");
		builder.append(other.getParty() != null);
		builder.append(" || hasTrade: ");
		builder.append(other.getTrade() != null);
		builder.append("] || CASH: [");
		builder.append(other.getClient().getPlayer().getNX());
		builder.append("] || VOTING POINTS: [");
		builder.append(other.getClient().getPlayer().getVPoints());
		builder.append("] || MESO: [");
		builder.append(other.getClient().getPlayer().getMeso());
		builder.append("] || DOJO POINTS: [");
		builder.append(other.getClient().getPlayer().getDojo());
		builder.append("] || remoteAddress: ");
		other.getClient().DebugMessage(builder);
		c.getPlayer().sendNotice(6, builder.toString());
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("charinfo", "charname", "Shows info about the charcter with the given name", 2)
		};
	}
}