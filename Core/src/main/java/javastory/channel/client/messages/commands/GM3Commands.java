package javastory.channel.client.messages.commands;

import static javastory.channel.client.messages.CommandProcessor.getOptionalIntArg;
import handling.GamePacket;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.ISkill;
import javastory.channel.client.SkillFactory;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObject;
import javastory.channel.server.ShopFactory;
import javastory.server.ChannelServer;
import server.maps.GameMapObjectType;
import tools.MaplePacketCreator;
import tools.StringUtil;
import client.Stat;

public class GM3Commands implements Command {

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        final ChannelCharacter player = c.getPlayer();
        switch (splitted[0]) {
            case "-online":
                player.sendNotice(6, "Characters connected to channel " + c.getChannelId() + ":");
                player.sendNotice(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
                break;
            case "-say":
                if (splitted.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    sb.append(player.getName());
                    sb.append("] ");
                    sb.append(StringUtil.joinStringFrom(splitted, 1));
                    GamePacket packet = MaplePacketCreator.serverNotice(6, sb.toString());
                    try {
                        c.getChannelServer().getWorldInterface().broadcastMessage(packet.getBytes());
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                } else {
                    player.sendNotice(6, "Syntax: !say <message>");
                }
                break;
            case "-song":
                player.getMap().broadcastMessage(MaplePacketCreator.musicChange(splitted[1]));
                break;
            case "-level":
                player.setLevel(Short.parseShort(splitted[1]));
                player.levelUp();
                if (player.getExp() < 0) {
                    player.gainExp(-player.getExp(), false, false, true);
                }
                break;
            case "-shop":
                ShopFactory shop = ShopFactory.getInstance();
                int shopId = Integer.parseInt(splitted[1]);
                if (shop.getShop(shopId) != null) {
                    shop.getShop(shopId).sendShop(c);
                }
                break;
            case "-whereami":
                player.sendNotice(5, "You are on map " + player.getMap().getId());
                break;
            case "-job":
                player.changeJob(Integer.parseInt(splitted[1]));
                break;
            case "-ap":
                player.setRemainingAp(getOptionalIntArg(splitted, 1, 1));
                player.updateSingleStat(Stat.AVAILABLE_AP, player.getRemainingAp());
                break;
            case "-sp":
                player.setRemainingSp(getOptionalIntArg(splitted, 1, 1));
                player.updateSingleStat(Stat.AVAILABLE_SP, player.getRemainingSp());
                break;
            case "-skill":
                ISkill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
                byte level = (byte) getOptionalIntArg(splitted, 2, 1);
                byte masterlevel = (byte) getOptionalIntArg(splitted, 3, 1);
                if (level > skill.getMaxLevel()) {
                    level = skill.getMaxLevel();
                }
                player.changeSkillLevel(skill, level, masterlevel);
                break;
            case "-heal":
                player.getStats().setHp(player.getStats().getMaxHp());
                player.getStats().setMp(player.getStats().getMaxMp());
                player.updateSingleStat(Stat.HP, player.getStats().getMaxHp());
                player.updateSingleStat(Stat.MP, player.getStats().getMaxMp());
                break;
            case "-cleardrops":
                GameMap map = player.getMap();
                List<GameMapObject> items = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM));
                for (GameMapObject i : items) {
                    map.removeMapObject(i);
                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
                }
                player.sendNotice(6, "You have destroyed " + items.size() + " items on the ground.");
                break;
            case "-servermessage":
                String outputMessage = StringUtil.joinStringFrom(splitted, 1);
                cserv.setServerMessage(outputMessage);
                break;
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("online", "", "", 3),
                    new CommandDefinition("say", "", "", 3),
                    new CommandDefinition("song", "", "", 3),
                    new CommandDefinition("level", "", "", 3),
                    new CommandDefinition("shop", "", "", 3),
                    new CommandDefinition("whereami", "", "", 3),
                    new CommandDefinition("job", "", "", 3),
                    new CommandDefinition("ap", "", "", 3),
                    new CommandDefinition("sp", "", "", 3),
                    new CommandDefinition("skill", "", "", 3),
                    new CommandDefinition("heal", "", "", 3),
                    new CommandDefinition("cleardrops", "", "", 3),
                    new CommandDefinition("servermessage", "<new message>", "Changes the servermessage to the new message", 3)
                };
    }
}