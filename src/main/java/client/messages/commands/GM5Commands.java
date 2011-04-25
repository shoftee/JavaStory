package client.messages.commands;

import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.text.DateFormat;

import client.Equip;
import client.GameConstants;
import client.IItem;
import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import client.anticheat.CheatingOffense;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import handling.ServerPacketOpcode;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import org.javastory.server.Bans;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.Portal;
import server.ShopFactory;
import server.life.MonsterInfoProvider;
import server.maps.GameMap;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.maps.Reactor;
import server.maps.ReactorFactory;
import server.maps.ReactorStats;
import server.quest.Quest;
import tools.ArrayMap;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

public class GM5Commands implements Command {

    @Override
    public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        final GameCharacter chr = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        if (splitted[0].equalsIgnoreCase("-proitem")) {
            if (splitted.length == 3) {
                int itemid;
                short multiply;
                try {
                    itemid = Integer.parseInt(splitted[1]);
                    multiply = Short.parseShort(splitted[2]);
                } catch (NumberFormatException asd) {
                    return;
                }
                ItemInfoProvider ii = ItemInfoProvider.getInstance();
                IItem item = ii.getEquipById(itemid);
                InventoryType type = GameConstants.getInventoryType(itemid);
                if (type.equals(InventoryType.EQUIP)) {
                    InventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, multiply), true);
                } else {
                    c.getPlayer().dropMessage(6, "Make sure it's an equippable item.");
                }
            } else {
                c.getPlayer().dropMessage(6, "Invalid syntax.(!proitem (Item ID) (Stat) Example: !proitem 9999999 32767");
            }
        } else if (splitted[0].equals("-mutecall")) {
            c.getPlayer().setCallGM(!c.getPlayer().isCallGM());
            c.getPlayer().dropMessage(6, "GM Messages set to " + c.getPlayer().isCallGM());
        } else if (splitted[0].equals("-clearinv")) {
            Map<Pair<Short, Short>, InventoryType> eqs = new ArrayMap<Pair<Short, Short>, InventoryType>();
            if (splitted[1].equals("all")) {
                for (InventoryType type : InventoryType.values()) {
                    for (IItem item : c.getPlayer().getInventoryType(type)) {
                        eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), type);
                    }
                }
            } else if (splitted[1].equals("eqp")) {
                for (IItem item : c.getPlayer().getInventoryType(InventoryType.EQUIPPED)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), InventoryType.EQUIPPED);
                }
            } else if (splitted[1].equals("eq")) {
                for (IItem item : c.getPlayer().getInventoryType(InventoryType.EQUIP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), InventoryType.EQUIP);
                }
            } else if (splitted[1].equals("u")) {
                for (IItem item : c.getPlayer().getInventoryType(InventoryType.USE)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), InventoryType.USE);
                }
            } else if (splitted[1].equals("s")) {
                for (IItem item : c.getPlayer().getInventoryType(InventoryType.SETUP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), InventoryType.SETUP);
                }
            } else if (splitted[1].equals("e")) {
                for (IItem item : c.getPlayer().getInventoryType(InventoryType.ETC)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), InventoryType.ETC);
                }
            } else if (splitted[1].equals("c")) {
                for (IItem item : c.getPlayer().getInventoryType(InventoryType.CASH)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), InventoryType.CASH);
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }
            for (Entry<Pair<Short, Short>, InventoryType> eq : eqs.entrySet()) {
                InventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
        } else if (splitted[0].equals("-ban")) {
            if (splitted.length < 3) {
                return;
            }
            final StringBuilder sb = new StringBuilder(c.getPlayer().getName());
            sb.append(" banned ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
            if (chr != null) {
                sb.append(" (IP: ").append(chr.getClient().getSessionIP()).append(")");
                if (chr.ban(sb.toString(), false, false)) {
                    c.getPlayer().dropMessage(6, "Successfully banned.");
                } else {
                    c.getPlayer().dropMessage(6, "Failed to ban.");
                }
            } else {
                if (Bans.banBySessionIP(splitted[1], sb.toString())) {
                    sb.append(" (IP: ").append(chr.getClient().getSessionIP()).append(")");
                } else {
                    c.getPlayer().dropMessage(6, "Failed to ban " + splitted[1]);
                }
            }
        } else if (splitted[0].equals("-tempban")) {
            final GameCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            final int reason = Integer.parseInt(splitted[2]);
            final int numDay = Integer.parseInt(splitted[3]);
            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            final DateFormat df = DateFormat.getInstance();
            if (victim == null) {
                c.getPlayer().dropMessage(6, "Unable to find character");
                return;
            }
            victim.temporaryBan("Temp banned by : " + c.getPlayer().getName() + "", cal, reason, true);
            c.getPlayer().dropMessage(6, "The character " + splitted[1] + " has been successfully tempbanned till " + df.format(cal.getTime()));
        } else if (splitted[0].equals("-unban")) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "!unban <Character name>");
            } else {
                final byte result = c.unban(splitted[1]);
                if (result == -1) {
                    c.getPlayer().dropMessage(6, "No character found with that name.");
                } else if (result == -2) {
                    c.getPlayer().dropMessage(6, "Error occured while unbanning, please try again later.");
                } else {
                    c.getPlayer().dropMessage(6, "Character successfully unbanned.");
                }
            }
        } else if (splitted[0].equals("-dc")) {
            int level = 0;
            GameCharacter victim;
            if (splitted[1].charAt(0) == '-') {
                level = StringUtil.countCharacters(splitted[1], 'f');
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
            } else {
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            }
            if (level < 2) {
                victim.getClient().disconnect();
                if (level >= 1) {
                    victim.getClient().disconnect();
                }
            } else {
                c.getPlayer().dropMessage(6, "Please use dc -f instead.");
            }
        } else if (splitted[0].equals("-resetquest")) {
            Quest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
        } else if (splitted[0].equals("-nearestPortal")) {
            final Portal portal = chr.getMap().findClosestSpawnpoint(chr.getPosition());
            c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
        } else if (splitted[0].equals("-spawndebug")) {
            c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
        } else if (splitted[0].equals("-threads")) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                    c.getPlayer().dropMessage(6, i + ": " + tstring);
                }
            }
        } else if (splitted[0].equals("-showtrace")) {
            if (splitted.length < 2) {
                throw new IllegalCommandSyntaxException(2);
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(6, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(6, elem.toString());
            }
        } else if (splitted[0].equals("-fakerelog")) {
            c.write(MaplePacketCreator.getCharInfo(chr));
            chr.getMap().removePlayer(chr);
            chr.getMap().addPlayer(chr);
        } else if (splitted[0].equals("-toggleoffense")) {
            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                c.getPlayer().dropMessage(6, "Offense " + splitted[1] + " not found");
            }
        } else if (splitted[0].equals("-tdrops")) {
            chr.getMap().toggleDrops();
        } else if (splitted[0].equals("-tmegaphone")) {
            try {
                c.getChannelServer().getWorldInterface().toggleMegaphoneMuteState();
            } catch (RemoteException e) {
                c.getChannelServer().pingWorld();
            }
            c.getPlayer().dropMessage(6, "Megaphone state : " + (c.getChannelServer().getMegaphoneMuteState() ? "Enabled" : "Disabled"));
        } else if (splitted[0].equalsIgnoreCase("!sreactor")) {
            ReactorStats reactorSt = ReactorFactory.getReactor(Integer.parseInt(splitted[1]));
            Reactor reactor = new Reactor(reactorSt, Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            reactor.setPosition(c.getPlayer().getPosition());
            c.getPlayer().getMap().spawnReactor(reactor);
        } else if (splitted[0].equals("-hreactor")) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
        } else if (splitted[0].equals("-lreactor")) {
            GameMap map = c.getPlayer().getMap();
            List<GameMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
            for (GameMapObject reactorL : reactors) {
                Reactor reactor2l = (Reactor) reactorL;
                c.getPlayer().dropMessage(6, "Reactor: oID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getReactorId() + " Position: " + reactor2l.getPosition().toString() + " State: " + reactor2l.getState());
            }
        } else if (splitted[0].equals("-dreactor")) {
            GameMap map = c.getPlayer().getMap();
            List<GameMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
            if (splitted[1].equals("all")) {
                for (GameMapObject reactorL : reactors) {
                    Reactor reactor2l = (Reactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
        } else if (splitted[0].equals("-resetreactor")) {
            c.getPlayer().getMap().resetReactors();
        } else if (splitted[0].equals("-setreactor")) {
            c.getPlayer().getMap().setReactorState();
        } else if (splitted[0].equals("-removedrops")) {
            List<GameMapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM));
            for (GameMapObject i : items) {
                c.getPlayer().getMap().removeMapObject(i);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, 0), i.getPosition());
            }
        } else if (splitted[0].equals("-exprate")) {
            if (splitted.length > 1) {
                final byte rate = Byte.parseByte(splitted[1]);
                c.getChannelServer().setExpRate(rate);
                c.getPlayer().dropMessage(6, "Exprate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !exprate <number>");
            }
        } else if (splitted[0].equals("-droprate")) {
            if (splitted.length > 1) {
                final byte rate = Byte.parseByte(splitted[1]);
                c.getChannelServer().setDropRate(rate);
                c.getPlayer().dropMessage(6, "Drop Rate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !droprate <number>");
            }
        } else if (splitted[0].equals("-dcall")) {
            c.getChannelServer().getPlayerStorage().disconnectAll();
        } else if (splitted[0].equals("-reloadops")) {
            ServerPacketOpcode.reloadValues();
        } else if (splitted[0].equals("-reloaddrops")) {
            MonsterInfoProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
        } else if (splitted[0].equals("-reloadportal")) {
            PortalScriptManager.getInstance().clearScripts();
        } else if (splitted[0].equals("-clearshops")) {
            ShopFactory.getInstance().clear();
        } else if (splitted[0].equals("-clearevents")) {
            for (ChannelServer instance : ChannelManager.getAllInstances()) {
                instance.reloadEvents();
            }
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("proitem", "", "", 5),
                    new CommandDefinition("mutecall", "", "", 5),
                    new CommandDefinition("clearinv", "", "", 5),
                    new CommandDefinition("ban", "charname reason", "Permanently ip, mac and accountbans the given character", 5),
                    new CommandDefinition("tempban", "<name> <reason> <numDay>", "Tempbans the given account", 5),
                    new CommandDefinition("dc", "[-f] name", "Disconnects player matching name provided. Use -f only if player is persistent!", 5),
                    new CommandDefinition("dcall", "", "Disconnects every players", 5),
                    new CommandDefinition("removedrops", "", "", 5),
                    new CommandDefinition("resetquest", "", "", 5),
                    new CommandDefinition("nearestPortal", "", "", 5),
                    new CommandDefinition("spawndebug", "", "", 5),
                    new CommandDefinition("tmegaphone", "", "", 5),
                    new CommandDefinition("threads", "", "", 5),
                    new CommandDefinition("showtrace", "", "", 5),
                    new CommandDefinition("toggleoffense", "", "", 5),
                    new CommandDefinition("fakerelog", "", "", 5),
                    new CommandDefinition("tdrops", "", "", 5),
                    new CommandDefinition("sreactor", "[id]", "Spawn a Reactor", 5),
                    new CommandDefinition("hreactor", "[object ID]", "Hit reactor", 5),
                    new CommandDefinition("resetreactor", "", "Resets all reactors", 5),
                    new CommandDefinition("lreactor", "", "List reactors", 5),
                    new CommandDefinition("dreactor", "", "Remove a Reactor", 5),
                    new CommandDefinition("setreactor", "", "Set reactor state", 5),
                    new CommandDefinition("exprate", "rate", "Changes the exp rate", 5),
                    new CommandDefinition("droprate", "rate", "Changes the drop rate", 5),
                    new CommandDefinition("reloadops", "", "", 5),
                    new CommandDefinition("reloadportal", "", "", 5),
                    new CommandDefinition("reloaddrops", "", "", 5),
                    new CommandDefinition("clearshops", "", "", 5),
                    new CommandDefinition("clearevents", "", "", 5)
                };
    }
}