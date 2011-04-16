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
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleStat;
import client.anticheat.CheatingOffense;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleDoor;
import server.maps.MapleMapObject;
import server.maps.MapleReactor;
import tools.MaplePacketCreator;

public final class PlayersHandler {

    private PlayersHandler() {
    }

    public static void handleNoteAction(final PacketReader reader, final MapleCharacter chr) throws PacketFormatException {
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

    public static void handleGiveFame(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        final int who = reader.readInt();
        final int mode = reader.readByte();

        final int famechange = mode == 0 ? -1 : 1;
        final MapleCharacter target = (MapleCharacter) chr.getMap().getMapObject(who);

        if (target == chr) { // faming self
            chr.getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF);
            return;
        } else if (chr.getLevel() < 15) {
            chr.getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15);
            return;
        }
        switch (chr.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame() + famechange) <= 30000) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.FAME, target.getFame());
                }
                if (!chr.isGM()) {
                    chr.hasGivenFame(target);
                }
                c.getSession().write(MaplePacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
                target.getClient().getSession().write(MaplePacketCreator.receiveFame(mode, chr.getName()));
                break;
            case NOT_TODAY:
                c.getSession().write(MaplePacketCreator.giveFameErrorResponse(3));
                break;
            case NOT_THIS_MONTH:
                c.getSession().write(MaplePacketCreator.giveFameErrorResponse(4));
                break;
        }
    }

    public static void handleUseDoor(final PacketReader reader, final MapleCharacter chr) throws PacketFormatException {
        final int oid = reader.readInt();
        final boolean mode = reader.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town

        for (MapleMapObject obj : chr.getMap().getAllDoor()) {
            final MapleDoor door = (MapleDoor) obj;
            if (door.getOwner().getId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    public static void handleTransformPlayer(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        // D9 A4 FD 00
        // 11 00
        // A0 C0 21 00
        // 07 00 64 66 62 64 66 62 64
        reader.skip(4); // Timestamp
        final byte slot = (byte) reader.readShort();
        final int itemId = reader.readInt();
        final String target = reader.readLengthPrefixedString().toLowerCase();

        final IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        switch (itemId) {
            case 2212000:
                for (final MapleCharacter search_chr : c.getPlayer().getMap().getCharacters()) {
                    if (search_chr.getName().toLowerCase().equals(target)) {
                        MapleItemInformationProvider.getInstance().getItemEffect(2210023).applyTo(search_chr);
                        search_chr.dropMessage(6, chr.getName() + " has played a prank on you!");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    }
                }
                break;
        }
    }

    public static void handleHitReactor(final PacketReader reader, final MapleClient c) throws PacketFormatException {
        final int oid = reader.readInt();
        final int charPos = reader.readInt();
        final short stance = reader.readShort();
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(charPos, stance, c);
    }
}
