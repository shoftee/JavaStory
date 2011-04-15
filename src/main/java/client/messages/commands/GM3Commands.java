package client.messages.commands;

import java.rmi.RemoteException;

import static client.messages.CommandProcessor.getOptionalIntArg;
import client.ISkill;
import client.MapleClient;
import client.MapleStat;
import client.SkillFactory;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import handling.MaplePacket;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.MapleShopFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.StringUtil;

public class GM3Commands implements Command {

    @Override
    public void execute(MapleClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equals("-online")) {
            c.getPlayer().dropMessage(6, "Characters connected to channel " + c.getChannelId() + ":");
            c.getPlayer().dropMessage(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
        } else if (splitted[0].equals("-say")) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                MaplePacket packet = MaplePacketCreator.serverNotice(6, sb.toString());
                try {
                    ChannelManager.getInstance(c.getChannelId()).getWorldInterface().broadcastMessage(packet.getBytes());
                } catch (RemoteException e) {
                    c.getChannelServer().pingWorld();
                }
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !say <message>");
            }
        } else if (splitted[0].equals("-song")) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(splitted[1]));
        } else if (splitted[0].equals("-level")) {
            c.getPlayer().setLevel(Short.parseShort(splitted[1]));
            c.getPlayer().levelUp();
            if (c.getPlayer().getExp() < 0) {
                c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
            }
        } else if (splitted[0].equals("-shop")) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            }
        } else if (splitted[0].equals("-whereami")) {
            c.getPlayer().dropMessage(5, "You are on map " + c.getPlayer().getMap().getId());
        } else if (splitted[0].equals("-job")) {
            c.getPlayer().changeJob(Integer.parseInt(splitted[1]));
        } else if (splitted[0].equals("-ap")) {
            c.getPlayer().setRemainingAp(getOptionalIntArg(splitted, 1, 1));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
        } else if (splitted[0].equals("-sp")) {
            c.getPlayer().setRemainingSp(getOptionalIntArg(splitted, 1, 1));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLESP, c.getPlayer().getRemainingSp());
        } else if (splitted[0].equals("-skill")) {
            ISkill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
            byte level = (byte) getOptionalIntArg(splitted, 2, 1);
            byte masterlevel = (byte) getOptionalIntArg(splitted, 3, 1);
            if (level > skill.getMaxLevel()) {
                level = skill.getMaxLevel();
            }
            c.getPlayer().changeSkillLevel(skill, level, masterlevel);
        } else if (splitted[0].equals("-heal")) {
            c.getPlayer().getStat().setHp(c.getPlayer().getStat().getMaxHp());
            c.getPlayer().getStat().setMp(c.getPlayer().getStat().getMaxMp());
            c.getPlayer().updateSingleStat(MapleStat.HP, c.getPlayer().getStat().getMaxHp());
            c.getPlayer().updateSingleStat(MapleStat.MP, c.getPlayer().getStat().getMaxMp());
        } else if (splitted[0].equals("-cleardrops")) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> items = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
            for (MapleMapObject i : items) {
                map.removeMapObject(i);
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, c.getPlayer().getId()));
            }
            c.getPlayer().dropMessage(6, "You have destroyed " + items.size() + " items on the ground.");
        } else if (splitted[0].equals("-servermessage")) {
            Collection<ChannelServer> cservs = ChannelManager.getAllInstances();
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            cserv.setServerMessage(outputMessage);
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