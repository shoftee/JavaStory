package javastory.channel.client.messages.commands;

import java.awt.Point;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Npc;
import javastory.channel.maps.GameMapObject;
import tools.MaplePacketCreator;

public class NpcSpawningCommands implements Command {

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        final ChannelCharacter player = c.getPlayer();
        switch (splitted[0]) {
            case "-npc":
                int npcId = Integer.parseInt(splitted[1]);
                Npc npc = LifeFactory.getNPC(npcId);
                if (npc != null && !npc.getName().equals("MISSINGNO")) {
                    npc.setPosition(player.getPosition());
                    npc.setCy(player.getPosition().y);
                    npc.setRx0(player.getPosition().x + 50);
                    npc.setRx1(player.getPosition().x - 50);
                    npc.setFoothold(player.getMap().getFootholds().findBelow(player.getPosition()).getId());
                    npc.setCustom(true);
                    player.getMap().addMapObject(npc);
                    player.getMap().broadcastMessage(MaplePacketCreator.spawnNpc(npc, true));
                } else {
                    player.sendNotice(6, "You have entered an invalid Npc-Id");
                }
                break;
            case "-removenpcs":
                List<GameMapObject> npcs = player.getMap().getAllNPC();
                for (GameMapObject npcmo : npcs) {
                    Npc item = (Npc) npcmo;
                    if (item.isCustom()) {
                        player.getMap().broadcastMessage(MaplePacketCreator.spawnNpc(item, false));
                        player.getMap().removeMapObject(item.getObjectId());
                    }
                }
                break;
            case "-mynpcpos":
                Point pos = player.getPosition();
                player.sendNotice(6, "CY: " + pos.y + " | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50) + " | FH: " + player.getMap().getFootholds().findBelow(pos).getId());
                break;
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("npc", "npcid", "Spawns the npc with the given id at the player position", 5),
                    new CommandDefinition("removenpcs", "", "Removes all custom spawned npcs from the map - requires reentering the map", 5),
                    new CommandDefinition("mynpcpos", "", "Gets the info for making an npc", 5)
                };
    }
}