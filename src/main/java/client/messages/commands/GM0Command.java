package client.messages.commands;

import client.GameCharacterUtil;
import client.GameClient;
import client.Stat;
import client.PlayerStats;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import java.rmi.RemoteException;
import org.javastory.server.channel.ChannelServer;
import scripting.NpcScriptManager;
import tools.MaplePacketCreator;
import tools.StringUtil;

public class GM0Command implements Command {
	@Override
	public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		ChannelServer cserv = c.getChannelServer();
		if (splitted[0].equals("@str")) {
			int str = Integer.parseInt(splitted[1]);
			final PlayerStats stat = c.getPlayer().getStat();
			if (stat.getStr() + str > c.getPlayer().getMaxStats() || c.getPlayer().getRemainingAp() < str || c.getPlayer().getRemainingAp() < 0 || str < 0) {
				c.getPlayer().sendNotice(5, "Sorry this cannot be done.");
			} else {
				stat.setStr(stat.getStr() + str);
				c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - str);
				c.getPlayer().updateSingleStat(Stat.AVAILABLE_AP, c.getPlayer().getRemainingAp());
				c.getPlayer().updateSingleStat(Stat.STR, stat.getStr());
			}
		} else if (splitted[0].equals("@dex")) {
			int dex = Integer.parseInt(splitted[1]);
			final PlayerStats stat = c.getPlayer().getStat();
			if (stat.getDex() + dex > c.getPlayer().getMaxStats() || c.getPlayer().getRemainingAp() < dex || c.getPlayer().getRemainingAp() < 0 || dex < 0) {
				c.getPlayer().sendNotice(5, "Sorry this cannot be done.");
			} else {
				stat.setDex(stat.getDex() + dex);
				c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - dex);
				c.getPlayer().updateSingleStat(Stat.AVAILABLE_AP, c.getPlayer().getRemainingAp());
				c.getPlayer().updateSingleStat(Stat.DEX, stat.getDex());
			}
		} else if (splitted[0].equals("@int")) {
			int int_ = Integer.parseInt(splitted[1]);
			final PlayerStats stat = c.getPlayer().getStat();
			if (stat.getInt() + int_ > c.getPlayer().getMaxStats() || c.getPlayer().getRemainingAp() < int_ || c.getPlayer().getRemainingAp() < 0 || int_ < 0) {
				c.getPlayer().sendNotice(5, "Sorry this cannot be done.");
			} else {
				stat.setInt(stat.getInt() + int_);
				c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - int_);
				c.getPlayer().updateSingleStat(Stat.AVAILABLE_AP, c.getPlayer().getRemainingAp());
				c.getPlayer().updateSingleStat(Stat.INT, stat.getInt());
			}
		} else if (splitted[0].equals("@luk")) {
			int luk = Integer.parseInt(splitted[1]);
			final PlayerStats stat = c.getPlayer().getStat();
			if (stat.getLuk() + luk > c.getPlayer().getMaxStats() || c.getPlayer().getRemainingAp() < luk || c.getPlayer().getRemainingAp() < 0 || luk < 0) {
				c.getPlayer().sendNotice(5, "Sorry this cannot be done.");
			} else {
				stat.setLuk(stat.getLuk() + luk);
				c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - luk);
				c.getPlayer().updateSingleStat(Stat.AVAILABLE_AP, c.getPlayer().getRemainingAp());
				c.getPlayer().updateSingleStat(Stat.LUK, stat.getLuk());
			}
		} else if (splitted[0].equals("@gmlist")) {
			c.getPlayer().sendNotice(5, "Current GM : johnlth93, GMCOnion | User GM : UGCSkyther");
		} /*else if (splitted[0].equals("@mobdebug")) {
		MapleMonster mob;
		for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 250000, Arrays.asList(MapleMapObjectType.MONSTER))) {
		mob = (MapleMonster) monstermo;
		c.getPlayer().dropMessage(6, "Monster " + mob.toString());
		}
		} else if (splitted[0].equals("@dmg")) {
		c.getPlayer().setOnDMG(!c.getPlayer().isOnDMG());
		c.getPlayer().dropMessage(6, "Damage displays set to " + c.getPlayer().isOnDMG());
		} else if (splitted[0].equals("@npc")) {
		NPCScriptManager.getInstance().start(c, 1052013);
		} */else if (splitted[0].equals("@togm")) {
			if (c.getPlayer().getCSPoints(1) < 500) {
				c.getPlayer().sendNotice(6, "You need 500 a-cash to operate this function.");
				return;
			}
			c.getPlayer().modifyCSPoints(1, -500, true);
			final StringBuilder msg = new StringBuilder("[togm] ");
			msg.append(c.getPlayer().getName());
			msg.append(" has requested for help : ");
			msg.append(StringUtil.joinStringFrom(splitted, 1));
			try {
				c.getChannelServer().getWorldInterface().broadcastGMMessage(MaplePacketCreator.serverNotice(5, msg.toString()).getBytes());
			} catch (RemoteException e) {
				c.getChannelServer().pingWorld();
			}
		} else if (splitted[0].equals("@dispose")) {
			NpcScriptManager.getInstance().dispose(c);
		} else if (splitted[0].equals("@ea")) {
			c.write(MaplePacketCreator.enableActions());
		} else if (splitted[0].equals("@changesecondpass")) {
			if (splitted[2].equals(splitted[3])) {
				if (splitted[2].length() < 4 || splitted[2].length() > 16) {
					c.getPlayer().sendNotice(5, "Your new password must not be length of below 4 or above 16.");
				} else {
					final int output = GameCharacterUtil.Change_SecondPassword(c.getAccountId(), splitted[1], splitted[2]);
					if (output == -2 || output == -1) {
						c.getPlayer().sendNotice(1, "An unknown error occured");
					} else if (output == 0) {
						c.getPlayer().sendNotice(1, "You do not have a second password set currently, please set one at character selection.");
					} else if (output == 1) {
						c.getPlayer().sendNotice(1, "The old password which you have inputted is invalid.");
					} else if (output == 2) {
						c.getPlayer().sendNotice(1, "Password changed successfully!");
					}
				}
			} else {
				c.getPlayer().sendNotice(1, "Please confirm your new password again.");
			}
		} else if (splitted[0].equals("@help")) {
			c.getPlayer().sendNotice(5, "Available commands :");
			c.getPlayer().sendNotice(5, "@str, @dex, @int, @luk [space] amount to add");
			c.getPlayer().sendNotice(5, "@gmlist | List out all current GM");
			c.getPlayer().sendNotice(5, "@togm | send a message to online GM, cost 500 cash");
			c.getPlayer().sendNotice(5, "@dispose | Dispose if you are unable to talk to NPC");
			c.getPlayer().sendNotice(5, "@ea | If you are unable to attack");
			c.getPlayer().sendNotice(5, "@changesecondpass | Change second password, @changesecondpass <current Password> <new password> <Confirm new password>");
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