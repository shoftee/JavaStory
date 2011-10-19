package javastory.channel.client.messages.commands;

import java.util.HashMap;
import javastory.channel.ChannelCharacter;

import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.server.Portal;
import javastory.channel.maps.GameMap;
import javastory.channel.ChannelClient;

public class GoToCommands implements Command {

    private static final HashMap<String, Integer> gotomaps = new HashMap<>();

    public GoToCommands() {
        gotomaps.put("gmmap", 180000000);
        gotomaps.put("southperry", 60000);
        gotomaps.put("amherst", 1010000);
        gotomaps.put("henesys", 100000000);
        gotomaps.put("ellinia", 101000000);
        gotomaps.put("perion", 102000000);
        gotomaps.put("kerning", 103000000);
        gotomaps.put("lithharbour", 104000000);
        gotomaps.put("sleepywood", 105040300);
        gotomaps.put("florina", 110000000);
        gotomaps.put("orbis", 200000000);
        gotomaps.put("happyville", 209000000);
        gotomaps.put("elnath", 211000000);
        gotomaps.put("ludibrium", 220000000);
        gotomaps.put("aquaroad", 230000000);
        gotomaps.put("leafre", 240000000);
        gotomaps.put("mulung", 250000000);
        gotomaps.put("herbtown", 251000000);
        gotomaps.put("omegasector", 221000000);
        gotomaps.put("koreanfolktown", 222000000);
        gotomaps.put("newleafcity", 600000000);
        gotomaps.put("sharenian", 990000000);
        gotomaps.put("pianus", 230040420);
        gotomaps.put("horntail", 240060200);
        gotomaps.put("mushmom", 100000005);
        gotomaps.put("griffey", 240020101);
        gotomaps.put("manon", 240020401);
        gotomaps.put("zakum", 280030000);
        gotomaps.put("papulatus", 220080001);
        gotomaps.put("showatown", 801000000);
        gotomaps.put("zipangu", 800000000);
        gotomaps.put("ariant", 260000100);
        gotomaps.put("nautilus", 120000000);
        gotomaps.put("boatquay", 541000000);
        gotomaps.put("malaysia", 550000000);
        gotomaps.put("taiwan", 740000000);
        gotomaps.put("thailand", 500000000);
        gotomaps.put("erev", 130000000);
        gotomaps.put("ellinforest", 300000000);
        gotomaps.put("kampung", 551000000);
        gotomaps.put("singapore", 540000000);
        gotomaps.put("amoria", 680000000);
        gotomaps.put("timetemple", 270000000);
        gotomaps.put("pinkbean", 270050100);
        gotomaps.put("peachblossom", 700000000);
        gotomaps.put("fm", 910000000);
    }

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        final ChannelCharacter player = c.getPlayer();
        if (splitted.length < 2) {
            player.sendNotice(6, "Syntax: !goto <mapname>");
        } else {
            if (gotomaps.containsKey(splitted[1])) {
                GameMap target = c.getChannelServer().getMapFactory(c.getWorldId()).getMap(gotomaps.get(splitted[1]));
                Portal targetPortal = target.getPortal(0);
                player.changeMap(target, targetPortal);
            } else {
                if (splitted[1].equals("locations")) {
                    player.sendNotice(6, "Use !goto <location>. Locations are as follows:");
                    StringBuilder sb = new StringBuilder();
                    for (String s : gotomaps.keySet()) {
                        sb.append(s).append(", ");
                    }
                    player.sendNotice(6, sb.substring(0, sb.length() - 2));
                } else {
                    player.sendNotice(6, "Invalid command syntax - Use !goto <location>. For a list of locations, use !goto locations.");
                }
            }
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("goto", "?", "go <town/map name>", 2)
                };
    }
}