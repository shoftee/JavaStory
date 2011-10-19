package javastory.channel.client.messages.commands;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.client.GameClient;

public class CharInfoCommand implements Command {

    @Override
    public void execute(ChannelClient c, String[] splittedLine) throws Exception, IllegalCommandSyntaxException {
        final StringBuilder builder = new StringBuilder();
        final ChannelCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splittedLine[1]);
        builder.append(GameClient.getLogMessage(other, ""));
        builder.append(" at ");
        builder.append(" x : ");
        builder.append(other.getPosition().x);
        builder.append(" y : ");
        builder.append(other.getPosition().y);
        builder.append(" fh : ");
        
        final ChannelCharacter player = c.getPlayer();
        builder.append(other.getMap().getFootholds().findBelow(player.getPosition()).getId());
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
        builder.append(other.getPartyMembership() != null);
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
        player.sendNotice(6, builder.toString());
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("charinfo", "charname", "Shows info about the charcter with the given name", 2)
                };
    }
}