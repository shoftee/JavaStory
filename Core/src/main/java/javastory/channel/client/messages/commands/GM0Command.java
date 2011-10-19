package javastory.channel.client.messages.commands;

import java.rmi.RemoteException;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.client.ActivePlayerStats;
import javastory.client.GameCharacterUtil;
import javastory.client.Stat;
import javastory.scripting.NpcScriptManager;
import javastory.tools.StringUtil;
import javastory.tools.packets.ChannelPackets;

public class GM0Command implements Command {

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {

        final ChannelCharacter player = c.getPlayer();
        final ActivePlayerStats stat = player.getStats();
        switch (splitted[0]) {
            case "@str":
                int str = Integer.parseInt(splitted[1]);
                if (stat.getStr() + str > player.getMaxStats() || player.getRemainingAp() < str || player.getRemainingAp() < 0 || str < 0) {
                    player.sendNotice(5, "Sorry this cannot be done.");
                } else {
                    stat.setStr(stat.getStr() + str);
                    player.setRemainingAp(player.getRemainingAp() - str);
                    player.updateSingleStat(Stat.AVAILABLE_AP, player.getRemainingAp());
                    player.updateSingleStat(Stat.STR, stat.getStr());
                }
                break;

            case "@dex":
                int dex = Integer.parseInt(splitted[1]);
                if (stat.getDex() + dex > player.getMaxStats() || player.getRemainingAp() < dex || player.getRemainingAp() < 0 || dex < 0) {
                    player.sendNotice(5, "Sorry this cannot be done.");
                } else {
                    stat.setDex(stat.getDex() + dex);
                    player.setRemainingAp(player.getRemainingAp() - dex);
                    player.updateSingleStat(Stat.AVAILABLE_AP, player.getRemainingAp());
                    player.updateSingleStat(Stat.DEX, stat.getDex());
                }
                break;

            case "@int":
                int int_ = Integer.parseInt(splitted[1]);
                if (stat.getInt() + int_ > player.getMaxStats() || player.getRemainingAp() < int_ || player.getRemainingAp() < 0 || int_ < 0) {
                    player.sendNotice(5, "Sorry this cannot be done.");
                } else {
                    stat.setInt(stat.getInt() + int_);
                    player.setRemainingAp(player.getRemainingAp() - int_);
                    player.updateSingleStat(Stat.AVAILABLE_AP, player.getRemainingAp());
                    player.updateSingleStat(Stat.INT, stat.getInt());
                }
                break;

            case "@luk":
                int luk = Integer.parseInt(splitted[1]);
                if (stat.getLuk() + luk > player.getMaxStats() || player.getRemainingAp() < luk || player.getRemainingAp() < 0 || luk < 0) {
                    player.sendNotice(5, "Sorry this cannot be done.");
                } else {
                    stat.setLuk(stat.getLuk() + luk);
                    player.setRemainingAp(player.getRemainingAp() - luk);
                    player.updateSingleStat(Stat.AVAILABLE_AP, player.getRemainingAp());
                    player.updateSingleStat(Stat.LUK, stat.getLuk());
                }
                break;

            case "@gmlist":
                player.sendNotice(5, "Current GM : johnlth93, GMCOnion | User GM : UGCSkyther");
                break;
            case "@togm":
                if (player.getCSPoints(1) < 500) {
                    player.sendNotice(6, "You need 500 a-cash to operate this function.");
                    return;
                }
                player.modifyCSPoints(1, -500, true);
                final StringBuilder msg = new StringBuilder("[togm] ");
                msg.append(player.getName());
                msg.append(" has requested for help : ");
                msg.append(StringUtil.joinStringFrom(splitted, 1));
                try {
                    c.getChannelServer().getWorldInterface().broadcastGMMessage(ChannelPackets.serverNotice(5, msg.toString()).getBytes());
                } catch (RemoteException e) {
                    c.getChannelServer().pingWorld();
                }
                break;
            case "@dispose":
                NpcScriptManager.getInstance().dispose(c);
                break;
            case "@ea":
                c.write(ChannelPackets.enableActions());
                break;
            case "@changesecondpass":
                if (splitted[2].equals(splitted[3])) {
                    if (splitted[2].length() < 4 || splitted[2].length() > 16) {
                        player.sendNotice(5, "Your new password must not be length of below 4 or above 16.");
                    } else {
                        final int output = GameCharacterUtil.Change_SecondPassword(c.getAccountId(), splitted[1], splitted[2]);
                        if (output == -2 || output == -1) {
                            player.sendNotice(1, "An unknown error occured");
                        } else if (output == 0) {
                            player.sendNotice(1, "You do not have a second password set currently, please set one at character selection.");
                        } else if (output == 1) {
                            player.sendNotice(1, "The old password which you have inputted is invalid.");
                        } else if (output == 2) {
                            player.sendNotice(1, "Password changed successfully!");
                        }
                    }
                } else {
                    player.sendNotice(1, "Please confirm your new password again.");
                }
                break;
            case "@help":
                player.sendNotice(5, "Available commands :");
                player.sendNotice(5, "@str, @dex, @int, @luk [space] amount to add");
                player.sendNotice(5, "@gmlist | List out all current GM");
                player.sendNotice(5, "@togm | send a message to online GM, cost 500 cash");
                player.sendNotice(5, "@dispose | Dispose if you are unable to talk to NPC");
                player.sendNotice(5, "@ea | If you are unable to attack");
                player.sendNotice(5, "@changesecondpass | Change second password, @changesecondpass <current Password> <new password> <Confirm new password>");
                break;
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("str", "", "", 0),
                    new CommandDefinition("dex", "", "", 0),
                    new CommandDefinition("int", "", "", 0),
                    new CommandDefinition("luk", "", "", 0),
                    new CommandDefinition("gmlist", "", "", 0),
                    new CommandDefinition("togm", "", "", 0),
                    new CommandDefinition("dispose", "", "", 0),
                    new CommandDefinition("ea", "", "", 0),
                    new CommandDefinition("changesecondpass", "", "", 0),
                    new CommandDefinition("help", "", "", 0)
                };
    }
}