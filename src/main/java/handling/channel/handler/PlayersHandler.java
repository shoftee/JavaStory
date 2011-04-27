/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.IItem;
import org.javastory.client.ChannelCharacter;
import org.javastory.client.ChannelClient;
import client.Stat;
import client.anticheat.CheatingOffense;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import org.javastory.server.FameLog;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.maps.Door;
import server.maps.GameMapObject;
import server.maps.Reactor;
import tools.FameResponse;
import tools.MaplePacketCreator;

public final class PlayersHandler {

    private PlayersHandler() {
    }

    public static void handleNoteAction(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
        final byte type = reader.readByte();

        switch (type) {
            case 1:
                final byte num = reader.readByte();
                reader.skip(2);

                for (int i = 0; i < num; i++) {
                    final int id = reader.readInt();
                    reader.skip(1);
                    chr.deleteNote(id);
                }
                break;
            default:
                System.out.println("Unhandled note action, " + type + "");
        }
    }

    public static void handleGiveFame(final PacketReader reader, final ChannelClient c, final ChannelCharacter famer) throws PacketFormatException {
        final int receiverId = reader.readInt();
        final boolean isIncrease = reader.readBoolean();

        final int change =
                isIncrease ? 1 : -1;
        final ChannelCharacter receiver = (ChannelCharacter) famer.getMap().getMapObject(receiverId);

        if (receiver == famer) {
            famer.getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF);
            return;
        }

        if (famer.getLevel() < 15) {
            famer.getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15);
            return;
        }

        FameResponse responseCode = FameResponse.SUCCESS;
        if (FameLog.hasFamedRecently(famer.getId(), receiverId)) {
            responseCode = FameResponse.ONCE_A_MONTH;
        } else if (famer.hasFamedToday()) {
            responseCode = FameResponse.ONCE_A_DAY;
        }

        if (responseCode != FameResponse.SUCCESS) {
            c.write(MaplePacketCreator.giveFameErrorResponse(responseCode));
            return;
        }

        if (Math.abs(receiver.getFame() + change) <= 30000) {
            receiver.addFame(change);
            receiver.updateSingleStat(Stat.FAME, receiver.getFame());

            long timestamp = FameLog.addEntry(famer.getId(), receiverId);
            famer.setLastFameTime(timestamp);

            c.write(MaplePacketCreator.giveFameResponse(isIncrease, receiver.getName(), receiver.getFame()));
            receiver.getClient().write(MaplePacketCreator.receiveFame(isIncrease, famer.getName()));
        }
    }

    public static void handleUseDoor(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
        final int oid = reader.readInt();
        final boolean mode = reader.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town

        for (GameMapObject obj : chr.getMap().getAllDoor()) {
            final Door door = (Door) obj;
            if (door.getOwner().getId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    public static void handleTransformPlayer(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
        // D9 A4 FD 00
        // 11 00
        // A0 C0 21 00
        // 07 00 64 66 62 64 66 62 64
        reader.skip(4); // Timestamp
        final byte slot = (byte) reader.readShort();
        final int itemId = reader.readInt();
        final String target = reader.readLengthPrefixedString().toLowerCase();

        final IItem toUse = c.getPlayer().getUseInventory().getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() !=
                itemId) {
            c.write(MaplePacketCreator.enableActions());
            return;
        }
        switch (itemId) {
            case 2212000:
                for (final ChannelCharacter search_chr : c.getPlayer().getMap().getCharacters()) {
                    if (search_chr.getName().toLowerCase().equals(target)) {
                        ItemInfoProvider.getInstance().getItemEffect(2210023).applyTo(search_chr);
                        search_chr.sendNotice(6, chr.getName() +
                                " has played a prank on you!");
                        InventoryManipulator.removeFromSlot(c, chr.getUseInventory(), slot, (short) 1, false);
                    }
                }
                break;
        }
    }

    public static void handleHitReactor(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
        final int oid = reader.readInt();
        final int charPos = reader.readInt();
        final short stance = reader.readShort();
        final Reactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(charPos, stance, c);
    }
}
