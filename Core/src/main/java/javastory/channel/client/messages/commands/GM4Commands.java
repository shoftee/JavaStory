package javastory.channel.client.messages.commands;

import static javastory.channel.client.messages.CommandProcessor.getOptionalIntArg;
import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMapObject;
import javastory.channel.packet.MobPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.client.Equip;
import javastory.client.IItem;
import javastory.client.Item;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.server.ChannelServer;
import javastory.server.ItemInfoProvider;

public class GM4Commands implements Command {

	@Override
	public void execute(ChannelClient c, String[] splitted) throws Exception,
			IllegalCommandSyntaxException {
		ChannelServer cserv = c.getChannelServer();
		final ChannelCharacter player = c.getPlayer();
		switch (splitted[0]) {
		case "-item": {
			final int itemId = Integer.parseInt(splitted[1]);
			final short quantity = (short) getOptionalIntArg(splitted, 2, 1);
			if (itemId == 2100106 | itemId == 2100107) {
				player.sendNotice(5, "Item is blocked.");
				return;
			}
			if (GameConstants.isPet(itemId)) {
				player.sendNotice(	5,
									"Please purshase a pet from the cash shop instead.");
			} else {
				IItem item;
				if (GameConstants.getInventoryType(itemId)
				== InventoryType.EQUIP) {
					ItemInfoProvider ii = ItemInfoProvider.getInstance();
					item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
				} else {
					item = new Item(itemId, (byte) 0, quantity, (byte) 0);
				}
				item.setOwner(player.getName());
				item.setGMLog(player.getName());
				InventoryManipulator.addbyItem(c, item);
			}
			break;
		}
		case "-drop": {
			final int itemId = Integer.parseInt(splitted[1]);
			final short quantity = (short) getOptionalIntArg(splitted, 2, 1);
			if (itemId == 2100106 | itemId == 2100107) {
				player.sendNotice(5, "Item is blocked.");
				return;
			}
			if (GameConstants.isPet(itemId)) {
				player.sendNotice(	5,
									"Please purshase a pet from the cash shop instead.");
			} else {
				IItem toDrop;
				if (GameConstants.getInventoryType(itemId)
				== InventoryType.EQUIP) {
					ItemInfoProvider ii = ItemInfoProvider.getInstance();
					toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
				} else {
					toDrop = new Item(itemId, (byte) 0, quantity, (byte) 0);
				}
				toDrop.setGMLog(player.getName());
				player.getMap().spawnItemDrop(player, player, toDrop,
												player.getPosition(), true,
												true);
			}
			break;
		}
		case "-maxmesos":
			player.gainMeso(Integer.MAX_VALUE - player.getMeso(), true);
			break;
		case "-vac":
			if (!player.isHidden()) {
				player.sendNotice(6, "You can only vac monsters while in hide.");
			} else {
				for (final GameMapObject mmo : player.getMap().getAllMonster()) {
					final Monster monster = (Monster) mmo;
					player.getMap()
							.broadcastMessage(	MobPacket
														.moveMonster(	false,
																		-1,
																		0,
																		0,
																		0,
																		0,
																		monster.getObjectId(),
																		monster.getPosition(),
																		player.getLastRes()));
					monster.setPosition(player.getPosition());
				}
			}
			break;
		case "-jobperson":
			cserv.getPlayerStorage().getCharacterByName(splitted[1])
					.changeJob(Integer.parseInt(splitted[1]));
			break;
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("item", "", "", 4),
			new CommandDefinition("drop", "", "", 4),
			new CommandDefinition("maxmesos", "", "", 4),
			new CommandDefinition("vac", "", "", 4),
			new CommandDefinition("jobperson", "", "", 4)
		};
	}
}