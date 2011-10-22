/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.channel.handling;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javastory.channel.maps.AnimatedGameMapObject;
import javastory.channel.movement.AbsoluteLifeMovement;
import javastory.channel.movement.AranMovement;
import javastory.channel.movement.BounceMovement;
import javastory.channel.movement.ChairMovement;
import javastory.channel.movement.ChangeEquipSpecialAwesome;
import javastory.channel.movement.FlashMovement;
import javastory.channel.movement.JumpDownMovement;
import javastory.channel.movement.LifeMovement;
import javastory.channel.movement.LifeMovementFragment;
import javastory.channel.movement.RelativeLifeMovement;
import javastory.channel.movement.TeleportMovement;
import javastory.channel.movement.UnknownMovement;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;

public class MovementParse {

	public static List<LifeMovementFragment> parseMovement(final PacketReader lea) throws PacketFormatException {
		final List<LifeMovementFragment> res = new ArrayList<>();
		final byte numCommands = lea.readByte();

		// TODO: Extract these into methods.
		for (byte i = 0; i < numCommands; i++) {
			final byte command = lea.readByte();
			switch (command) {
			case -1: // Bounce movement?
			{
				final Point position = lea.readVector();

				final short unk = lea.readShort();
				final short fh = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final BounceMovement bm = new BounceMovement(command, position, duration, newstate);
				bm.setFH(fh);
				bm.setUnk(unk);
				res.add(bm);
				break;
			}
			case 0: // normal move
			case 5:
			case 14:
			case 17: // Float
			{
				final Point position = lea.readVector();

				final Point wobble = lea.readVector();

				final short unk = lea.readShort();

				final Point offset = lea.readVector();

				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, position, duration, newstate);
				alm.setUnk(unk);
				alm.setPixelsPerSecond(wobble);
				alm.setOffset(offset);
				res.add(alm);
				break;
			}
			case 1:
			case 2:
			case 13: // Shot-jump-back thing
			case 18:
			case 16: { // Float
				final Point move = lea.readVector();

				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final RelativeLifeMovement rlm = new RelativeLifeMovement(command, move, duration, newstate);
				res.add(rlm);
				break;
			}
			case 3:
			case 4: // tele... -.-
			case 6: // assaulter
			case 8: // assassinate
			case 11: // rush ?
			{

				final Point position = lea.readVector();

				final Point wobble = lea.readVector();

				final byte newstate = lea.readByte();
				final TeleportMovement tm = new TeleportMovement(command, position, newstate);
				tm.setPixelsPerSecond(wobble);
				res.add(tm);
				break;
			}
			case 9: {// change equip ???
				res.add(new ChangeEquipSpecialAwesome(command, lea.readByte()));
				break;
			}
			case 7: // same structure
			case 10: // chair ???
			{
				final Point position = lea.readVector();
				final short unk = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final ChairMovement cm = new ChairMovement(command, position, duration, newstate);
				cm.setUnk(unk);
				res.add(cm);
				break;
			}
			case 23: // ?
			case 24: // ?
			case 25: // ?
			case 26: // ?
			case 27: // ? <- has no offsets
			case 19:
			case 15:
			case 21: // Aran Combat Step
			case 22: {
				final byte newstate = lea.readByte();
				final short unk = lea.readShort();
				final AranMovement am = new AranMovement(command, new Point(0, 0), unk, newstate);

				res.add(am);
				break;
			}
			case 12: { // Jump Down
				final Point position = lea.readVector();

				final Point wobble = lea.readVector();

				final short unk = lea.readShort();
				final short fh = lea.readShort();

				final Point offset = lea.readVector();

				final byte newstate = lea.readByte();
				final short duration = lea.readShort();

				final JumpDownMovement jdm = new JumpDownMovement(command, position, duration, newstate);

				jdm.setUnk(unk);
				jdm.setPixelsPerSecond(wobble);
				jdm.setOffset(offset);
				jdm.setFH(fh);

				res.add(jdm);
				break;
			}
			case 30: { // ?... 00 00 7A 03 0F 02 64 01 00 00 0F 00 04 5A 00
				final short unk = lea.readShort(); // always 0?
				final Point position = lea.readVector(); // not position?

				final Point wobble = lea.readVector();

				final short fh = lea.readShort();
				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final UnknownMovement um = new UnknownMovement(command, position, duration, newstate);
				um.setUnk(unk);
				um.setPixelsPerSecond(wobble);
				um.setFH(fh);

				res.add(um);
				break;
			}
			case 20: { // fj
				final Point position = lea.readVector();
				final Point wobble = lea.readVector();

				final byte newstate = lea.readByte();
				final short duration = lea.readShort();
				final FlashMovement um = new FlashMovement(command, position, duration, newstate);
				um.setPixelsPerSecond(wobble);
				res.add(um);
				break;
			}
			default:
//		    System.out.println("Remaining : "+(numCommands - res.size())+" New type of movement ID : "+command+", packet : " + lea.toString());
				return null;
			}
		}
		if (numCommands != res.size()) {
//	    System.out.println("error in movement");
			return null; // Probably hack
		}
		return res;
	}

	public static final void updatePosition(final List<LifeMovementFragment> movement, final AnimatedGameMapObject target, final int yoffset) {
		for (final LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					Point position = ((LifeMovement) move).getPosition();
					position.y += yoffset;
					target.setPosition(position);
				}
				target.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}