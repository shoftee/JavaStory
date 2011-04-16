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

package client;

import java.util.Map;
import java.util.HashMap;

import handling.GamePacket;
import handling.ServerPacketOpcode;
import org.javastory.io.PacketBuilder;

public class NPC {

    private short state;
    private byte senddialogue;
    private int npcid;
    private String text;
    private Boolean ready;
    private Map<Byte, String> previous = new HashMap<Byte, String>();
    private Map<Byte, String> pending = new HashMap<Byte, String>();

    protected NPC() {
	resetContents();
    }

    public void addText(String text) {
	this.text += text;
    }

    public void setNPC(final int npc) {
	npcid = npc;
    }

    public int getNPCVar() {
	return npcid;
    }

    public byte getSentDialogue() {
	return senddialogue;
    }

    public void resetContents() {
	npcid = 0;
	state = -1;
	senddialogue = -1;
	text = "";
	ready = false;
    }

    private GamePacket npcPacket(byte senddialogue) {
	this.senddialogue = senddialogue;

	PacketBuilder builder = new PacketBuilder();
	builder.writeAsShort(ServerPacketOpcode.NPC_TALK.getValue());
	builder.writeAsByte(4);
	builder.writeInt(npcid);
	builder.writeByte(senddialogue);
	builder.writeLengthPrefixedString(text);

	text = "";

	return builder.getPacket();
    }

    public void proceedNext() {
	state++;
	waitClient();
	if (state < previous.size()) {
	    // sent here, just an example.
	    // I may not be using stateless NPC script since there is
	    // 500 scripts to convery x_X
	}
    }

    public void messageRecieved() {
	synchronized (ready) {
	    ready.notifyAll();
	}
    }

    private void waitClient() {
	synchronized (ready) {
	    while (!ready) {
		try {
		    ready.wait();
		} catch (InterruptedException e) {
		}
	    }
	    ready = false;
	}
    }
}