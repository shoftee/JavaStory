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
package org.javastory.server.handling;

import org.javastory.client.ChannelClient;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.javastory.cryptography.AesTransform;
import org.javastory.cryptography.CustomEncryption;

import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class PacketDecoder extends CumulativeProtocolDecoder {

    public static final String DECODER_STATE_KEY =
            PacketDecoder.class.getName() + ".STATE";

    public static class DecoderState {

        public int packetLength = -1;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer buffer,
            ProtocolDecoderOutput out) throws Exception {
        final DecoderState decoderState =
                (DecoderState) session.getAttribute(DECODER_STATE_KEY);

        final ChannelClient client =
                (ChannelClient) session.getAttribute(ChannelClient.CLIENT_KEY);

        if (decoderState.packetLength == -1) {
            if (buffer.remaining() < 4) {
                return false;
            }
            
            final byte[] header = new byte[4];
            buffer.get(header);
            if (!client.getClientCrypto().validateHeader(header)) {
                session.close(true);
                return false;
            }
            decoderState.packetLength =
                    AesTransform.getPacketLength(header);
        }

        if (buffer.remaining() < decoderState.packetLength) {
            return false;
        }
        
        final byte decryptedPacket[] = new byte[decoderState.packetLength];
        buffer.get(decryptedPacket, 0, decoderState.packetLength);
        decoderState.packetLength = -1;

        client.getClientCrypto().transform(decryptedPacket);
        CustomEncryption.decrypt(decryptedPacket);

        out.write(decryptedPacket);
        return true;
    }
}
