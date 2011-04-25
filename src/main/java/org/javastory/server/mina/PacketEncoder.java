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
package org.javastory.server.mina;

import client.GameClient;
import handling.GamePacket;
import org.javastory.cryptography.AesTransform;
import org.javastory.cryptography.CustomEncryption;

import java.util.concurrent.locks.Lock;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class PacketEncoder implements ProtocolEncoder {

    @Override
    public void encode(final IoSession session, final Object message, final ProtocolEncoderOutput out) throws Exception {
	final GameClient client = 
                (GameClient) session.getAttribute(GameClient.CLIENT_KEY);

	if (client != null) {
	    final Lock mutex = client.getLock();

	    mutex.lock();
	    try {
		final AesTransform serverCrypto = client.getServerCrypto();

		final byte[] packet = ((GamePacket) message).getBytes();
		final byte[] packetCopy = new byte[packet.length];
		System.arraycopy(packet, 0, packetCopy, 0, packet.length); 

		final byte[] raw = new byte[packetCopy.length + 4]; 
		final byte[] header = 
                        serverCrypto.constructHeader(packetCopy.length);

		CustomEncryption.encrypt(packetCopy); 
		serverCrypto.transform(packetCopy); 

		System.arraycopy(header, 0, raw, 0, 4); 
		System.arraycopy(packetCopy, 0, raw, 4, packetCopy.length);
		out.write(IoBuffer.wrap(raw));
	    } finally {
		mutex.unlock();
	    }
	} else { 
	    out.write(IoBuffer.wrap(((GamePacket) message).getBytes()));
	}
    }

    @Override
    public void dispose(IoSession session) throws Exception {
	// nothing to do
    }
}
