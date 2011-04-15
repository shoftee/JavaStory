package handling.login.handler;

import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

import client.IItem;
import client.Item;
import client.LoginCrypto;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MapleQuestStatus;
import handling.login.LoginInformationProvider;
import org.javastory.server.login.LoginServer;
import handling.login.LoginWorker;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;
import tools.KoreanDateUtil;
import tools.data.input.SeekableLittleEndianAccessor;

public class CharLoginHandler {

    private static boolean loginFailCount(final MapleClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    public static void handleLogin(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final String login = slea.readMapleAsciiString();
        final String pwd = LoginCrypto.decryptRSA(slea.readMapleAsciiString());
        c.setAccountName(login);
        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = false; // MSEA doesn't sent mac
        int loginok = c.login(login, pwd, ipBan || macBan);
        final Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginok == 0 && (ipBan || macBan)) {
            loginok = 3;
            if (macBan) {
                MapleCharacter.ban(c.getSession().getRemoteAddress().toString().split(":")[0], "Enforcing account ban, account " + login, false);
            }
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.getSession().write(LoginPacket.getTempBan(KoreanDateUtil.getTempBanTimestamp(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    }

    public static void handleWorldListRequest(final MapleClient c) {
        final LoginServer ls = LoginServer.getInstance();
        c.getSession().write(LoginPacket.getWorldList(2, "Cassiopeia", ls.getChannels()));
        c.getSession().write(LoginPacket.getEndOfWorldList());
    }

    public static void handleWorldStatusRequest(final MapleClient c) {
        int numPlayer = 0;
        for (ChannelServer cserv : ChannelManager.getAllInstances()) {
            numPlayer += cserv.getPlayerStorage().getConnectedClients();
        }
        final int userLimit = LoginServer.getInstance().getUserLimit();
        if (numPlayer >= userLimit) {
            c.getSession().write(LoginPacket.getWorldStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.getSession().write(LoginPacket.getWorldStatus(1));
        } else {
            c.getSession().write(LoginPacket.getWorldStatus(0));
        }
    }

    public static void handleCharacterListRequest(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int server = slea.readByte();
        final int channel = slea.readByte() + 1;
        c.setWorld(server);
        System.out.println(":: Client is connecting to server " + server + " channel " + channel + " ::");
        c.setChannel(channel);
        final List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null) {
            c.getSession().write(LoginPacket.getCharList(c.getSecondPassword() != null, chars));
        } else {
            c.getSession().close(false);
        }
    }

    public static void handleCharacterNameCheck(final String name, final MapleClient c) {
        c.getSession().write(LoginPacket.charNameResponse(name,
                                                          !MapleCharacterUtil.canCreateChar(name) || LoginInformationProvider.getInstance().isForbiddenName(name)));
    }

    public static void handleCreateCharacter(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final String name = slea.readMapleAsciiString();
        final int JobType = slea.readInt();
        final short db = slea.readShort();
        final int face = slea.readInt();
        final int hair = slea.readInt();
        final int hairColor = slea.readInt();
        final int skinColor = slea.readInt();
        final int top = slea.readInt();
        final int bottom = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();
        final byte gender = c.getGender();

        MapleCharacter newchar = MapleCharacter.getDefault(c, JobType);
        newchar.setWorld(c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair + hairColor);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skinColor);
        MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        final LoginInformationProvider li = LoginInformationProvider.getInstance();
        IItem item = li.getEquipById(top);
        item.setPosition((byte) -5);
        equip.addFromDB(item);
        item = li.getEquipById(bottom);
        item.setPosition((byte) -6);
        equip.addFromDB(item);
        item = li.getEquipById(shoes);
        item.setPosition((byte) -7);
        equip.addFromDB(item);
        item = li.getEquipById(weapon);
        item.setPosition((byte) -11);
        equip.addFromDB(item);

        if (MapleCharacterUtil.canCreateChar(name) && !li.isForbiddenName(name)) {
            MapleCharacter.saveNewCharToDB(newchar, JobType, JobType == 1 && db > 0);
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static void handleDeleteCharacter(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        String Secondpw_Client = null;
        if (slea.readByte() > 0) {
            Secondpw_Client = slea.readMapleAsciiString();
        }
        final String AS13Digit = slea.readMapleAsciiString();
        final int Character_ID = slea.readInt();
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

    public static void Character_WithoutSecondPassword(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        slea.skip(1);
        final int charId = slea.readInt();
        final String currentpw = c.getSecondPassword();
        if (slea.available() != 0) {
            if (currentpw != null) { // Hack
                c.getSession().close(true);
                return;
            }
            final String setpassword = slea.readMapleAsciiString();
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
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        c.getSession().write(MaplePacketCreator.getServerIP(Integer.parseInt(LoginServer.getInstance().getIP(c.getChannelId()).split(":")[1]), charId));
    }

    public static void Character_WithSecondPassword(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final String password = slea.readMapleAsciiString();
        final int charId = slea.readInt();
        if (loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId)) {
            c.getSession().close(false);
            return;
        }
        if (c.CheckSecondPassword(password)) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
            c.getSession().write(MaplePacketCreator.getServerIP(Integer.parseInt(LoginServer.getInstance().getIP(c.getChannelId()).split(":")[1]), charId));
        } else {
            c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
        }
    }
}