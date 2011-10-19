package javastory.channel.client.messages.commands;


import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.anticheat.CheatingOffense;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.life.MonsterInfoProvider;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.maps.Reactor;
import javastory.channel.maps.ReactorFactory;
import javastory.channel.maps.ReactorStats;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.Portal;
import javastory.channel.server.ShopFactory;
import javastory.client.Equip;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.scripting.PortalScriptManager;
import javastory.scripting.ReactorScriptManager;
import javastory.server.Bans;
import javastory.server.ChannelServer;
import javastory.server.ItemInfoProvider;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.Pair;
import javastory.tools.StringUtil;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Maps;

public class GM5Commands implements Command {

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        final ChannelCharacter chr = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        final ChannelCharacter player = c.getPlayer();
        final String parameter = splitted[0].toLowerCase();
        switch (parameter) {
            case "-proitem":
                getProItem(splitted, c, player);
                break;
            case "-mutecall":
                player.setCallGM(!player.isCallGM());
                player.sendNotice(6, "GM Messages set to " + player.isCallGM());
                break;
            case "-clearinv":
                clearInventory(splitted, player, c);
                break;
            case "-ban":
                ban(splitted, player, chr);
                break;
            case "-tempban":
                temporaryBan(cserv, splitted, player);
                break;
            case "-unban":
                unban(splitted, player, c);
                break;
            case "-dc":
                disconnect(splitted, cserv, player);
                break;
            case "-resetquest":
                player.getQuestStatus(Integer.parseInt(splitted[1])).forfeit();
                break;
            case "-nearestPortal":
                final Portal portal = chr.getMap().findClosestSpawnpoint(chr.getPosition());
                player.sendNotice(6, portal.getName() + " id: " +
                        portal.getId() + " script: " + portal.getScriptName());
                break;
            case "-spawndebug":
                player.sendNotice(6, player.getMap().spawnDebug());
                break;
            case "-threads":
                listThreads(splitted, player);
                break;
            case "-showtrace":
                showTrace(splitted, player);
                break;
            case "-fakerelog":
                c.write(ChannelPackets.getCharInfo(chr));
                chr.getMap().removePlayer(chr);
                chr.getMap().addPlayer(chr);
                break;
            case "-toggleoffense":
                toggleOffense(splitted, player);
                break;
            case "-tdrops":
                chr.getMap().toggleDrops();
                break;
            case "-tmegaphone":
                toggleMegaphone(c, player);
                break;
            case "!sreactor":
                spawnReactor(splitted, player);
                break;
            case "-hreactor":
                player.getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
                break;
            case "-lreactor":
                listReactors(player);
                break;
            case "-dreactor":
                destroyReactors(player, splitted);
                break;
            case "-resetreactor":
                player.getMap().resetReactors();
                break;
            case "-setreactor":
                player.getMap().setReactorState();
                break;
            case "-removedrops":
                removeDrops(player);
                break;
            case "-exprate":
                setExpRate(splitted, c, player);
                break;
            case "-droprate":
                setDropRate(splitted, c, player);
                break;
            case "-dcall":
                c.getChannelServer().getPlayerStorage().disconnectAll();
                break;
            case "-reloadops":
                ServerPacketOpcode.reloadValues();
                break;
            case "-reloaddrops":
                MonsterInfoProvider.getInstance().clearDrops();
                ReactorScriptManager.getInstance().clearDrops();
                break;
            case "-reloadportal":
                PortalScriptManager.getInstance().clearScripts();
                break;
            case "-clearshops":
                ShopFactory.getInstance().clear();
                break;
        }
    }

    private static void getProItem(String[] splitted, ChannelClient c, final ChannelCharacter player) {
        if (splitted.length == 3) {
            int itemid;
            short multiply;
            try {
                itemid = Integer.parseInt(splitted[1]);
                multiply = Short.parseShort(splitted[2]);
                ItemInfoProvider ii = ItemInfoProvider.getInstance();
                IItem item = ii.getEquipById(itemid);
                InventoryType type = GameConstants.getInventoryType(itemid);
                if (type.equals(InventoryType.EQUIP)) {
                    InventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, multiply), true);
                } else {
                    player.sendNotice(6, "Make sure it's an equippable item.");
                }
            } catch (NumberFormatException asd) {
            }
        } else {
            player.sendNotice(6, "Invalid syntax.(!proitem (Item ID) (Stat) Example: !proitem 9999999 32767");
        }
    }

    private boolean clearInventory(String[] splitted, final ChannelCharacter player, ChannelClient c) {
        Map<Pair<Short, Short>, Inventory> items = Maps.newLinkedHashMap();
        Inventory inventory;
        switch (splitted[1]) {
            case "all":
                for (Inventory inv : player.getInventories()) {
                    for (IItem item : inv) {
                        items.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), inv);
                    }
                }
            case "eqp":
                inventory = player.getEquippedItemsInventory();
                break;
            case "eq":
                inventory = player.getEquipInventory();
                break;
            case "u":
                inventory = player.getUseInventory();
                break;
            case "s":
                inventory = player.getSetupInventory();
                break;
            case "e":
                inventory = player.getEtcInventory();
                break;
            case "c":
                inventory = player.getCashInventory();
                break;
            default:
                player.sendNotice(6, "[all/eqp/eq/u/s/e/c]");
                return true;
        }
        for (IItem item : inventory) {
            items.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), inventory);
        }

        for (Entry<Pair<Short, Short>, Inventory> eq : items.entrySet()) {
            InventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
        }
        return false;
    }

    private boolean ban(String[] splitted, final ChannelCharacter player, final ChannelCharacter chr) {
        if (splitted.length < 3) {
            return true;
        }
        final StringBuilder sb = new StringBuilder(player.getName());
        sb.append(" banned ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
        if (chr != null) {
            sb.append(" (IP: ").append(chr.getClient().getSessionIP()).append(")");
            if (chr.ban(sb.toString(), false)) {
                player.sendNotice(6, "Successfully banned.");
            } else {
                player.sendNotice(6, "Failed to ban.");
            }
        } else {
            if (Bans.banBySessionIP(splitted[1], sb.toString())) {
                sb.append(" (IP: ").append(splitted[1]).append(")");
            } else {
                player.sendNotice(6, "Failed to ban " + splitted[1]);
            }
        }
        return false;
    }

    private boolean temporaryBan(ChannelServer cserv, String[] splitted, final ChannelCharacter player) throws NumberFormatException {
        final ChannelCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        final int reason = Integer.parseInt(splitted[2]);
        final int numDay = Integer.parseInt(splitted[3]);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, numDay);
        final DateFormat df = DateFormat.getInstance();
        if (victim == null) {
            player.sendNotice(6, "Unable to find character");
            return true;
        }
        victim.temporaryBan("Temp banned by : " + player.getName() +
                "", cal, reason);
        player.sendNotice(6, "The character " + splitted[1] +
                " has been successfully tempbanned till " +
                df.format(cal.getTime()));
        return false;
    }

    private void unban(String[] splitted, final ChannelCharacter player, ChannelClient c) {
        if (splitted.length < 1) {
            player.sendNotice(6, "!unban <Character name>");
        } else {
            final byte result = Bans.unban(splitted[1]);
            if (result == -1) {
                player.sendNotice(6, "No character found with that name.");
            } else if (result == -2) {
                player.sendNotice(6, "Error occured while unbanning, please try again later.");
            } else {
                player.sendNotice(6, "Character successfully unbanned.");
            }
        }
    }

    private void disconnect(String[] splitted, ChannelServer cserv, final ChannelCharacter player) {
        int level = 0;
        ChannelCharacter victim;
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
            player.sendNotice(6, "Please use dc -f instead.");
        }
    }

    private void listThreads(String[] splitted, final ChannelCharacter player) {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        String filter = "";
        if (splitted.length > 1) {
            filter = splitted[1];
        }
        for (int i = 0; i < threads.length; i++) {
            String tstring = threads[i].toString();
            if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                player.sendNotice(6, i + ": " + tstring);
            }
        }
    }

    private static void showTrace(String[] splitted, final ChannelCharacter player) throws NumberFormatException, IllegalCommandSyntaxException {
        if (splitted.length < 2) {
            throw new IllegalCommandSyntaxException(2);
        }
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        Thread t = threads[Integer.parseInt(splitted[1])];
        player.sendNotice(6, t.toString() + ":");
        for (StackTraceElement elem : t.getStackTrace()) {
            player.sendNotice(6, elem.toString());
        }
    }

    private static void toggleOffense(String[] splitted, final ChannelCharacter player) {
        try {
            CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
            co.setEnabled(!co.isEnabled());
        } catch (IllegalArgumentException iae) {
            player.sendNotice(6, "Offense " + splitted[1] +
                    " not found");
        }
    }

    private static void toggleMegaphone(ChannelClient c, final ChannelCharacter player) {
        try {
            c.getChannelServer().getWorldInterface().toggleMegaphoneMuteState();
        } catch (RemoteException e) {
            c.getChannelServer().pingWorld();
        }
        player.sendNotice(6, "Megaphone state : " +
                (c.getChannelServer().getMegaphoneMuteState() ? "Enabled" : "Disabled"));
    }

    private static void spawnReactor(String[] splitted, final ChannelCharacter player) throws NumberFormatException {
        ReactorStats reactorSt = ReactorFactory.getReactor(Integer.parseInt(splitted[1]));
        Reactor reactor = new Reactor(reactorSt, Integer.parseInt(splitted[1]));
        reactor.setDelay(-1);
        reactor.setPosition(player.getPosition());
        player.getMap().spawnReactor(reactor);
    }

    private static void listReactors(final ChannelCharacter player) {
        GameMap map = player.getMap();
        List<GameMapObject> reactors = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
        for (GameMapObject reactorL : reactors) {
            Reactor reactor2l = (Reactor) reactorL;
            player.sendNotice(6, "Reactor: oID: " +
                    reactor2l.getObjectId() + " reactorID: " +
                    reactor2l.getReactorId() + " Position: " +
                    reactor2l.getPosition().toString() + " State: " +
                    reactor2l.getState());
        }
    }

    private static void destroyReactors(final ChannelCharacter player, String[] splitted) throws NumberFormatException {
        GameMap map = player.getMap();
        List<GameMapObject> reactors = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
        if (splitted[1].equals("all")) {
            for (GameMapObject reactorL : reactors) {
                Reactor reactor2l = (Reactor) reactorL;
                player.getMap().destroyReactor(reactor2l.getObjectId());
            }
        } else {
            player.getMap().destroyReactor(Integer.parseInt(splitted[1]));
        }
    }

    private static void removeDrops(final ChannelCharacter player) {
        List<GameMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM));
        for (GameMapObject i : items) {
            player.getMap().removeMapObject(i);
            player.getMap().broadcastMessage(ChannelPackets.removeItemFromMap(i.getObjectId(), 0, 0), i.getPosition());
        }
    }

    private static void setExpRate(String[] splitted, ChannelClient c, final ChannelCharacter player) throws NumberFormatException {
        if (splitted.length > 1) {
            final byte rate = Byte.parseByte(splitted[1]);
            c.getChannelServer().setExpRate(rate);
            player.sendNotice(6, "Exprate has been changed to " +
                    rate + "x");
        } else {
            player.sendNotice(6, "Syntax: !exprate <number>");
        }
    }

    private static void setDropRate(String[] splitted, ChannelClient c, final ChannelCharacter player) throws NumberFormatException {
        if (splitted.length > 1) {
            final byte rate = Byte.parseByte(splitted[1]);
            c.getChannelServer().setDropRate(rate);
            player.sendNotice(6, "Drop Rate has been changed to " +
                    rate + "x");
        } else {
            player.sendNotice(6, "Syntax: !droprate <number>");
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
                };
    }
}