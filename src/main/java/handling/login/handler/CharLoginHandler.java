package handling.login.handler;

import java.util.List;
import java.util.Calendar;

import client.IItem;
import client.LoginCrypto;
import client.GameClient;
import client.GameCharacter;
import client.GameCharacterUtil;
import client.Inventory;
import client.InventoryType;
import handling.login.LoginInformationProvider;
import org.javastory.io.PacketFormatException;
import org.javastory.server.login.LoginServer;
import handling.login.LoginWorker;
import org.javastory.io.PacketReader;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;
import tools.FiletimeUtil;

public class CharLoginHandler {

    private static boolean loginFailCount(final GameClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    public static void handleLogin(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final String login = reader.readLengthPrefixedString();
        final String pwd = LoginCrypto.decryptRSA(reader.readLengthPrefixedString());
        c.setAccountName(login);
        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = false; // MSEA doesn't sent mac
        int loginok = c.login(login, pwd, ipBan || macBan);
        final Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginok == 0 && (ipBan || macBan)) {
            loginok = 3;
            if (macBan) {
                GameCharacter.ban(c.getSession().getRemoteAddress().toString().split(":")[0], "Enforcing account ban, account " + login, false);
            }
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.getSession().write(LoginPacket.getTempBan(FiletimeUtil.getFiletime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    }

    public static void handleWorldListRequest(final GameClient c) {
        final LoginServer ls = LoginServer.getInstance();
        c.getSession().write(LoginPacket.getWorldList(2, "Cassiopeia", ls.getChannels()));
        c.getSession().write(LoginPacket.getEndOfWorldList());
    }

    public static void handleWorldStatusRequest(final GameClient c) {
        int numPlayer = 0;
        for (ChannelServer cserv : ChannelManager.getAllInstances()) {
            numPlayer += cserv.getPlayerStorage().getConnectedClients();
        }
        final int userLimit = LoginServer.USER_LIMIT;
        if (numPlayer >= userLimit) {
            c.getSession().write(LoginPacket.getWorldStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.getSession().write(LoginPacket.getWorldStatus(1));
        } else {
            c.getSession().write(LoginPacket.getWorldStatus(0));
        }
    }

    public static void handleCharacterListRequest(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final int server = reader.readByte();
        final int channel = reader.readByte() + 1;
        c.setWorld(server);
        System.out.println(":: Client is connecting to server " + server + " channel " + channel + " ::");
        c.setChannel(channel);
        final List<GameCharacter> chars = c.loadCharacters(server);
        if (chars != null) {
            // TODO: max characters should be set in account, 
            // not in LoginServer config, wtf.
            c.getSession().write(LoginPacket.getCharacterList(c.getSecondPassword() != null, chars, 3));
        } else {
            c.getSession().close(false);
        }
    }

    public static void handleCharacterNameCheck(final String name, final GameClient c) {
        c.getSession().write(LoginPacket.charNameResponse(name,
                                                          !GameCharacterUtil.canCreateChar(name) || LoginInformationProvider.getInstance().isForbiddenName(name)));
    }

    public static void handleCreateCharacter(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final String name = reader.readLengthPrefixedString();
        final int JobType = reader.readInt();
        final short db = reader.readShort();
        final int face = reader.readInt();
        final int hair = reader.readInt();
        final int hairColor = reader.readInt();
        final int skinColor = reader.readInt();
        final int top = reader.readInt();
        final int bottom = reader.readInt();
        final int shoes = reader.readInt();
        final int weapon = reader.readInt();
        final byte gender = c.getGender();

        GameCharacter newchar = GameCharacter.getDefault(c, JobType);
        newchar.setWorld(c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair + hairColor);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skinColor);
        Inventory equip = newchar.getInventory(InventoryType.EQUIPPED);
        final LoginInformationProvider li = LoginInformationProvider.getInstance();
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
            GameCharacter.saveNewCharacterToDb(newchar, JobType, JobType == 1 && db > 0);
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static void handleDeleteCharacter(final PacketReader reader, final GameClient c) throws PacketFormatException {
        String Secondpw_Client = null;
        if (reader.readByte() > 0) {
            Secondpw_Client = reader.readLengthPrefixedString();
        }
        final String AS13Digit = reader.readLengthPrefixedString();
        final int Character_ID = reader.readInt();
        if (!c.login_Auth(Character_ID)) {
            c.getSession().close(true);
            return;
        }
        byte state = 0;
        if (c.getSecondPassword() != null) {
            if (Secondpw_Client == null) {
                c.getSession().close(true);
                return;
            } else {
                if (!c.CheckSecondPassword(Secondpw_Client)) {
                    state = 12;
                }
            }
        }
        if (state == 0) {
            if (!c.deleteCharacter(Character_ID)) {
                state = 1;
            }
        }
        c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
    }

    public static void handleWithoutSecondPassword(final PacketReader reader, final GameClient c) throws PacketFormatException {
        reader.skip(1);
        final int charId = reader.readInt();
        final String currentpw = c.getSecondPassword();
        if (reader.remaining() != 0) {
            if (currentpw != null) { // Hack
                c.getSession().close(true);
                return;
            }
            final String setpassword = reader.readLengthPrefixedString();
            if (setpassword.length() >= 4 && setpassword.length() <= 16) {
                c.setSecondPassword(setpassword);
                c.updateSecondPassword();
                if (!c.login_Auth(charId)) {
                    c.getSession().close(true);
                    return;
                }
            } else {
                c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
                return;
            }
        } else if (loginFailCount(c) || currentpw != null || !c.login_Auth(charId)) {
            c.getSession().close(false);
            return;
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        c.updateLoginState(GameClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        c.getSession().write(MaplePacketCreator.getServerIP(Integer.parseInt(LoginServer.getInstance().getIP(c.getChannelId()).split(":")[1]), charId));
    }

    public static void handleWithSecondPassword(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final String password = reader.readLengthPrefixedString();
        final int charId = reader.readInt();
        if (loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId)) {
            c.getSession().close(false);
            return;
        }
        if (c.CheckSecondPassword(password)) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            c.updateLoginState(GameClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
            c.getSession().write(MaplePacketCreator.getServerIP(Integer.parseInt(LoginServer.getInstance().getIP(c.getChannelId()).split(":")[1]), charId));
        } else {
            c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
        }
    }
}