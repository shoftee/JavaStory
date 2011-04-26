/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server.handling;

import client.ChannelClient;
import handling.ClientPacketOpcode;
import handling.ServerType;
import handling.login.handler.CharLoginHandler;
import org.apache.mina.core.session.IoSession;
import org.javastory.client.GameClient;
import org.javastory.client.LoginClient;
import org.javastory.cryptography.AesTransform;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import tools.packet.LoginPacket;

/**
 *
 * @author Tosho
 */
public class LoginPacketHandler extends PacketHandler {

    public LoginPacketHandler() {
    }

    @Override
    protected void handlePacket(ClientPacketOpcode header, PacketReader reader, GameClient client) throws PacketFormatException {
        LoginClient loginClient = (LoginClient) client;
        switch (header) {
            case PONG:
                client.pongReceived();
                break;
            case STRANGE_DATA:
                // Does nothing for now, HackShield's heartbeat
                break;
            case LOGIN_PASSWORD:
                CharLoginHandler.handleLogin(reader, loginClient);
                break;
            case SERVERLIST_REQUEST:
                CharLoginHandler.handleWorldListRequest(loginClient);
                break;
            case CHARLIST_REQUEST:
                CharLoginHandler.handleCharacterListRequest(reader, loginClient);
                break;
            case SERVERSTATUS_REQUEST:
                CharLoginHandler.handleWorldStatusRequest(loginClient);
                break;
            case CHECK_CHAR_NAME:
                CharLoginHandler.handleCharacterNameCheck(reader.readLengthPrefixedString(), loginClient);
                break;
            case CREATE_CHAR:
                CharLoginHandler.handleCreateCharacter(reader, loginClient);
                break;
            case DELETE_CHAR:
                CharLoginHandler.handleDeleteCharacter(reader, loginClient);
                break;
            case CHAR_SELECT:
                CharLoginHandler.handleWithoutSecondPassword(reader, loginClient);
                break;
            case AUTH_SECOND_PASSWORD:
                CharLoginHandler.handleWithSecondPassword(reader, loginClient);
                break;
            case RSA_KEY: // Fix this somehow
                client.write(LoginPacket.StrangeDATA());
                break;
            default:
                break;
        }
    }

    protected GameClient createClient(final AesTransform clientCrypto, final AesTransform serverCrypto, final IoSession session) {
        final GameClient client = new LoginClient(clientCrypto, serverCrypto, session);
        return client;
    }
}
