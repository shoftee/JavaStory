package handling.login.handler;

import java.util.List;
import java.util.Calendar;

import client.IItem;
import client.LoginCrypto;
import client.GameCharacterUtil;
import client.Inventory;
import handling.login.LoginInfoProvider;
import org.javastory.io.PacketFormatException;
import org.javastory.server.login.LoginServer;
import handling.login.LoginWorker;
import org.javastory.client.LoginCharacter;
import org.javastory.client.LoginClient;
import org.javastory.io.PacketReader;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import org.javastory.server.login.AuthReplyCode;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;
import tools.FiletimeUtil;

public class CharLoginHandler {

    private static boolean loginFailCount(final LoginClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    public static void handleLogin(final PacketReader reader, final LoginClient c) throws PacketFormatException {
        final String login = reader.readLengthPrefixedString();
        final String pwd = LoginCrypto.decryptRSA(reader.readLengthPrefixedString());
        c.setAccountName(login);
        final boolean ipBan = c.hasBannedIP();
        AuthReplyCode code = c.authenticate(login, pwd, ipBan);
        final Calendar tempbannedTill = c.getTempBanCalendar();
        if (code == AuthReplyCode.SUCCESS && (ipBan)) {
            code = AuthReplyCode.DELETED_OR_BLOCKED;
        }
        if (code != AuthReplyCode.SUCCESS) {
            if (!loginFailCount(c)) {
                c.write(LoginPacket.getLoginFailed(code));
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.write(LoginPacket.getTempBan(FiletimeUtil.getFiletime(tempbannedTill.getTimeInMillis()), c.getTempBanReason()));
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    }

    public static void handleWorldListRequest(final LoginClient c) {
        final LoginServer ls = LoginServer.getInstance();
        c.write(LoginPacket.getWorldList(2, "Cassiopeia", ls.getChannels()));
        c.write(LoginPacket.getEndOfWorldList());
    }

    public static void handleWorldStatusRequest(final LoginClient c) {
        int numPlayer = 0;
        for (ChannelServer cserv : ChannelManager.getAllInstances()) {
            numPlayer += cserv.getPlayerStorage().getConnectedClients();
        }
        final int userLimit = LoginServer.USER_LIMIT;
        if (numPlayer >= userLimit) {
            c.write(LoginPacket.getWorldStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.write(LoginPacket.getWorldStatus(1));
        } else {
            c.write(LoginPacket.getWorldStatus(0));
        }
    }

    public static void handleCharacterListRequest(final PacketReader reader, final LoginClient c) throws PacketFormatException {
        final int server = reader.readByte();
        final int channel = reader.readByte() + 1;
        c.setWorldId(server);
        System.out.println(":: Client is connecting to server " + server +
                " channel " + channel + " ::");
        c.setChannelId(channel);
        final List<LoginCharacter> chars = c.loadCharacters(server);
        if (chars != null) {
            // TODO: max characters should be set in account, 
            // not in LoginServer config, wtf.
            c.write(LoginPacket.getCharacterList(c.getCharacterPassword() !=
                    null, chars, 3));
        } else {
            c.disconnect();
        }
    }

    public static void handleCharacterNameCheck(final String name, final LoginClient c) {
        c.write(LoginPacket.charNameResponse(name,
                                             !GameCharacterUtil.canCreateChar(name) ||
                LoginInfoProvider.getInstance().isForbiddenName(name)));
    }

    public static void handleCreateCharacter(final PacketReader reader, final LoginClient c) throws PacketFormatException {
        final String name = reader.readLengthPrefixedString();
        final int jobCategory = reader.readInt();
        final short db = reader.readShort();
        final int face = reader.readInt();
        final int hair = reader.readInt();
        final int hairColor = reader.readInt();
        final int skinColorId = reader.readInt();
        final int top = reader.readInt();
        final int bottom = reader.readInt();
        final int shoes = reader.readInt();
        final int weapon = reader.readInt();
        final byte gender = c.getGender();

        LoginCharacter newchar = LoginCharacter.getDefault(jobCategory);
        newchar.setWorldId(c.getWorldId());
        newchar.setFaceId(face);
        newchar.setHairId(hair + hairColor);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColorId(skinColorId);
        Inventory equip = newchar.getEquippedItemsInventory();
        final LoginInfoProvider li = LoginInfoProvider.getInstance();
        IItem item = li.getEquipById(top);
        item.setPosition((byte) -5);
        equip.addFromDb(item);
        item = li.getEquipById(bottom);
        item.setPosition((byte) -6);
        equip.addFromDb(item);
        item = li.getEquipById(shoes);
        item.setPosition((byte) -7);
        equip.addFromDb(item);
        item = li.getEquipById(weapon);
        item.setPosition((byte) -11);
        equip.addFromDb(item);

        if (GameCharacterUtil.canCreateChar(name) && !li.isForbiddenName(name)) {
            final boolean isDualBlader = jobCategory == 1 && db > 0;
            LoginCharacter.saveNewCharacterToDb(newchar, jobCategory, isDualBlader);
            c.createdChar(newchar.getId());
            c.write(LoginPacket.addNewCharEntry(newchar, true));
        } else {
            c.write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static void handleDeleteCharacter(final PacketReader reader, final LoginClient c) throws PacketFormatException {
        String characterPassword = null;
        if (reader.readByte() > 0) {
            characterPassword = reader.readLengthPrefixedString();
        }
        final String passport = reader.readLengthPrefixedString();
        final int characterId = reader.readInt();
        if (!c.login_Auth(characterId)) {
            c.disconnect(true);
            return;
        }
        byte state = 0;
        if (c.getCharacterPassword() != null) {
            if (characterPassword == null) {
                c.disconnect(true);
                return;
            } else {
                if (!c.checkCharacterPassword(characterPassword)) {
                    state = 12;
                }
            }
        }
        if (state == 0) {
            if (!c.deleteCharacter(characterId)) {
                state = 1;
            }
        }
        c.write(LoginPacket.deleteCharResponse(characterId, state));
    }

    public static void handleWithoutSecondPassword(final PacketReader reader, final LoginClient c) throws PacketFormatException {
        reader.skip(1);
        final int charId = reader.readInt();
        final String currentpw = c.getCharacterPassword();
        if (reader.remaining() != 0) {
            if (currentpw != null) {
                // Hack
                c.disconnect(true);
                return;
            }
            final String password = reader.readLengthPrefixedString();
            if (password.length() >= 4 && password.length() <= 16) {
                c.setCharacterPassword(password);
                c.updateSecondPassword();
                if (!c.login_Auth(charId)) {
                    c.disconnect(true);
                    return;
                }
            } else {
                c.write(LoginPacket.characterPasswordError((byte) 0x14));
                return;
            }
        } else if (loginFailCount(c) || currentpw != null ||
                !c.login_Auth(charId)) {
            c.disconnect();
            return;
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        // TODO: transfer() method
        //c.updateAccountLoginState(LoginClient.LOGIN_SERVER_TRANSITION, c.getSessionIP());
        c.write(MaplePacketCreator.getServerIP(LoginServer.getInstance().getChannelServerPort(c.getChannelId()), charId));
    }

    public static void handleWithSecondPassword(final PacketReader reader, final LoginClient c) throws PacketFormatException {
        final String password = reader.readLengthPrefixedString();
        final int charId = reader.readInt();
        if (loginFailCount(c) || c.getCharacterPassword() == null ||
                !c.login_Auth(charId)) {
            c.disconnect();
            return;
        }
        if (c.checkCharacterPassword(password)) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            // TODO: transfer() method
            //c.updateAccountLoginState(ChannelClient.LOGIN_SERVER_TRANSITION, c.getSessionIP());
            c.write(MaplePacketCreator.getServerIP(LoginServer.getInstance().getChannelServerPort(c.getChannelId()), charId));
        } else {
            c.write(LoginPacket.characterPasswordError((byte) 0x14));
        }
    }
}