/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server.handling;

import handling.ClientPacketOpcode;
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
                loginClient.handleLogin(reader);
                break;
            case SERVERLIST_REQUEST:
                loginClient.handleWorldListRequest();
                break;
            case CHARLIST_REQUEST:
                loginClient.handleCharacterListRequest(reader);
                break;
            case SERVERSTATUS_REQUEST:
                loginClient.handleServerStatusRequest();
                break;
            case CHECK_CHAR_NAME:
                loginClient.handleCharacterNameCheck(reader);
                break;
            case CREATE_CHAR:
                loginClient.handleCreateCharacter(reader);
                break;
            case DELETE_CHAR:
                loginClient.handleDeleteCharacter(reader);
                break;
            case CHAR_SELECT:
                loginClient.handleWithoutSecondPassword(reader);
                break;
            case AUTH_SECOND_PASSWORD:
                loginClient.handleWithSecondPassword(reader);
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
