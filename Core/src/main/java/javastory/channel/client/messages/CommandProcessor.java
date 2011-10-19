package javastory.channel.client.messages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.commands.CharInfoCommand;
import javastory.channel.client.messages.commands.CheaterHuntingCommands;
import javastory.channel.client.messages.commands.ConnectedCommand;
import javastory.channel.client.messages.commands.GM0Command;
import javastory.channel.client.messages.commands.GM3Commands;
import javastory.channel.client.messages.commands.GM4Commands;
import javastory.channel.client.messages.commands.GM5Commands;
import javastory.channel.client.messages.commands.GoToCommands;
import javastory.channel.client.messages.commands.HelpCommand;
import javastory.channel.client.messages.commands.MonsterInfoCommands;
import javastory.channel.client.messages.commands.NoticeCommand;
import javastory.channel.client.messages.commands.NpcSpawningCommands;
import javastory.channel.client.messages.commands.SearchCommands;
import javastory.channel.client.messages.commands.ShutdownCommands;
import javastory.channel.client.messages.commands.SpawnMonsterCommand;
import javastory.channel.client.messages.commands.TestCommands;
import javastory.channel.client.messages.commands.WarpCommands;
import javastory.server.TimerManager;
import javastory.tools.LogUtil;
import javastory.tools.Pair;
import javastory.tools.StringUtil;

public final class CommandProcessor {

    private static final List<Pair<String, String>> gmlog = new LinkedList<>();
    private final Map<String, DefinitionCommandPair> commands = new LinkedHashMap<>();
    private static CommandProcessor instance = new CommandProcessor();
    private static Runnable persister;
    private static final Lock rl = new ReentrantLock();

    public static CommandProcessor getInstance() {
        return instance;
    }

    private CommandProcessor() {
        persister = new PersistingTask();
        TimerManager.getInstance().register(persister, 1800000); // 30 min once
        registerCommand(new HelpCommand()); // register the helpcommand first so it appears first in the list (LinkedHashMap)
        registerCommand(new CharInfoCommand());
        registerCommand(new CheaterHuntingCommands());
        registerCommand(new ConnectedCommand());
        registerCommand(new GM0Command());
        registerCommand(new GM3Commands());
        registerCommand(new GM4Commands());
        registerCommand(new GM5Commands());
        registerCommand(new GoToCommands());
        registerCommand(new MonsterInfoCommands());
        registerCommand(new NoticeCommand());
        registerCommand(new NpcSpawningCommands());
        registerCommand(new SearchCommands());
        registerCommand(new ShutdownCommands());
        registerCommand(new SpawnMonsterCommand());
        registerCommand(new TestCommands());
        registerCommand(new WarpCommands());
    }

    public static class PersistingTask implements Runnable {

        @Override
        public void run() {
            final StringBuilder sb = new StringBuilder();
            rl.lock();
            try {
                final String time = LogUtil.CurrentReadable_Time();
                for (Pair<String, String> logentry : gmlog) {
                    sb.append("NAME : ");
                    sb.append(logentry.getLeft());
                    sb.append(", COMMAND : ");
                    sb.append(logentry.getRight());
                    sb.append(", TIME : ");
                    sb.append(time);
                    sb.append("\n");
                }
                gmlog.clear();
            } finally {
                rl.unlock();
            }
            LogUtil.log(LogUtil.GMCommand_Log, sb.toString());
        }
    }

    private void registerCommand(Command command) {
        for (CommandDefinition def : command.getDefinition()) {
            commands.put(def.getCommand(), new DefinitionCommandPair(command, def));
        }
    }

    public static String joinAfterString(String splitted[], String str) {
        for (int i = 1; i < splitted.length; i++) {
            if (splitted[i].equalsIgnoreCase(str) && i + 1 < splitted.length) {
                return StringUtil.joinStringFrom(splitted, i + 1);
            }
        }
        return null;
    }

    public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    public static String getNamedArg(String splitted[], int startpos, String name) {
        for (int i = startpos; i < splitted.length; i++) {
            if (splitted[i].equalsIgnoreCase(name) && i + 1 < splitted.length) {
                return splitted[i + 1];
            }
        }
        return null;
    }

    public static Integer getNamedIntArg(String splitted[], int startpos, String name) {
        String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
            try {
                return Integer.parseInt(arg);
            } catch (NumberFormatException nfe) {
                // swallow - we don't really care
            }
        }
        return null;
    }

    public static int getNamedIntArg(String splitted[], int startpos, String name, int def) {
        Integer ret = getNamedIntArg(splitted, startpos, name);
        if (ret == null) {
            return def;
        }
        return ret.intValue();
    }

    public static Double getNamedDoubleArg(String splitted[], int startpos, String name) {
        String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException nfe) {
                // swallow - we don't really care
            }
        }
        return null;
    }

    public boolean processCommand(ChannelClient c, String line) {
        return instance.processCommandInternal(c, line);
    }

    public static void forcePersisting() {
        persister.run();
    }

    public void dropHelp(ChannelCharacter chr, int page) {
        List<DefinitionCommandPair> allCommands = new ArrayList<>(commands.values());
        int startEntry = (page - 1) * 20;
        chr.sendNotice(6, "Command Help Page: --------" + page + "---------");
        for (int i = startEntry; i < startEntry + 20 && i < allCommands.size(); i++) {
            CommandDefinition commandDefinition = allCommands.get(i).getDefinition();
            if (chr.hasGmLevel(commandDefinition.getRequiredLevel())) {
                chr.sendNotice(6, commandDefinition.getCommand() + " "
                        + commandDefinition.getParameterDescription() + ": "
                        + commandDefinition.getHelp());
            }
        }
    }

    private boolean processCommandInternal(ChannelClient c, String line) {
        if (line.charAt(0) == '-' || line.charAt(0) == '@') {
            String[] splitted = line.split(" ");
            if (splitted.length > 0 && splitted[0].length() > 1) {
                DefinitionCommandPair definitionCommandPair = commands.get(splitted[0].substring(1));
                final ChannelCharacter player = c.getPlayer();
                if (definitionCommandPair != null && player.getGmLevel()
                        >= definitionCommandPair.getDefinition().getRequiredLevel()) {
                    try {
                        definitionCommandPair.getCommand().execute(c, splitted);
                    } catch (IllegalCommandSyntaxException e) {
                        player.sendNotice(6, "IllegalCommandSyntaxException:"
                                + e.getMessage());
                        return true;
                    } catch (Exception e) {
                        player.sendNotice(6, "An error occured: "
                                + e.getClass().getName() + " " + e.getMessage());
                        return true;
                    }
                    if (player.getGmLevel() > 0) {
                        rl.lock();
                        try {
                            gmlog.add(new Pair<String, String>(player.getName(), line));
                        } finally {
                            rl.unlock();
                        }
                    }
                    return true;
                } else {
                    player.sendNotice(6, "Command " + splitted[0]
                            + " does not exist or you do not have the required priviledges.");
                    return true;
                }
            }
        }
        return false;
    }
}

class DefinitionCommandPair {

    private Command command;
    private CommandDefinition definition;

    public DefinitionCommandPair(Command command, CommandDefinition definition) {
        super();
        this.command = command;
        this.definition = definition;
    }

    public Command getCommand() {
        return command;
    }

    public CommandDefinition getDefinition() {
        return definition;
    }
}