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
package javastory.io;

import javastory.tools.HexTool;

public class ByteArrayGamePacket implements GamePacket {

    public static final long serialVersionUID = -7997681658570958848L;
    private byte[] data;
    private Runnable onSend;

    public ByteArrayGamePacket(final byte[] data) {
	this.data = data;
    }

    @Override
    public final byte[] getBytes() {
	return data;
    }

    @Override
    public final Runnable getOnSend() {
	return onSend;
    }

    @Override
    public void setOnSend(final Runnable onSend) {
	this.onSend = onSend;
    }

    @Override
    public String toString() {
	return HexTool.toString(data);
    }
}
