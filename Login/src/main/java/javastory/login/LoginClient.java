/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import javastory.db.DatabaseConnection;
import javastory.channel.client.MemberRank;
import javastory.client.GameCharacterUtil;
import javastory.client.GameClient;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.client.LoginCrypto;
import javastory.cryptography.AesTransform;
import javastory.game.Gender;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.FiletimeUtil;
import javastory.tools.packets.CommonPackets;

import com.google.common.collect.Lists;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author shoftee
 */
public final class LoginClient extends GameClient {

    private static final int LOGIN_ATTEMPTS = 5;
    //
    private String loginPassword, loginPasswordSalt;
    private String characterPassword, characterPasswordSalt;
    private Calendar temporaryBan = null;
    private boolean isGm;
    private byte temporaryBanReason = 1;
    private Gender gender;
    private int characterSlots = 0;
    //
    private int loginAttempt = 0;
    private List<Integer> allowedChar = Lists.newLinkedList();

    public LoginClient(AesTransform clientCrypto, AesTransform serverCrypto, IoSession session) {
        super(clientCrypto, serverCrypto, session);
        loggedIn = false;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
            ps.setString(1, super.getSessionIP());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    ret = true;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error checking ip bans" + ex);
        }
        return ret;
    }

    public final boolean deleteCharacter(final int characterId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT id, level, job, guildid, guildrank, name FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, characterId);
            ps.setInt(2, super.getAccountId());
            rs = ps.executeQuery();
            if (!rs.next()) {
                return false;
            }
            if (rs.getInt("guildid") > 0) {
                // is in a guild when deleted
                if (MemberRank.fromNumber(rs.getInt("guildrank")).equals(MemberRank.MASTER)) {
                    return false;
                }
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("DELETE FROM characters WHERE id = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("DELETE FROM hiredmerch WHERE characterid = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("DELETE FROM mountdata WHERE characterid = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("DELETE FROM monsterbook WHERE charid = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            ps.close();

            return true;
        } catch (final SQLException ex) {
            System.err.println("DeleteChar error: " + ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                System.err.println("Error while closing SQL connection: " + ex);
            }
        }
        return false;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        if (rs.getLong("tempban") == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return temporaryBan;
    }

    public byte getTempBanReason() {
        return temporaryBanReason;
    }

    public AuthReplyCode authenticate(String username, String inputPassword) {
        AuthReplyCode replyCode = AuthReplyCode.NOT_REGISTERED;
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `accounts` WHERE `name` = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int banned = rs.getInt("banned");
                    if (banned > 0) {
                        replyCode = AuthReplyCode.DELETED_OR_BLOCKED;
                        return replyCode;
                    } else if (banned == -1) {
                        unban();
                    }
                    super.setAccountId(rs.getInt("id"));
                    super.setAccountName(rs.getString("name"));

                    loginPassword = rs.getString("password");
                    loginPasswordSalt = rs.getString("salt");

                    characterPassword = rs.getString("char_password");
                    characterPasswordSalt = rs.getString("char_salt");

                    isGm = rs.getInt("gm") > 0;
                    temporaryBanReason = rs.getByte("tempban_reason");
                    temporaryBan = getTempBanCalendar(rs);
                    gender = Gender.fromNumber(rs.getByte("gender"));

                    characterSlots = rs.getInt("character_slots");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR" + e);
        }

        if (characterPassword != null && characterPasswordSalt != null) {
            characterPassword = LoginCrypto.getPadding(characterPassword);
        }

        if (!LoginCrypto.checkSaltedSha512Hash(loginPassword, inputPassword, loginPasswordSalt)) {
            replyCode = AuthReplyCode.WRONG_PASSWORD;
        }

        loggedIn = logOn();
        if (loggedIn) {
            replyCode = AuthReplyCode.SUCCESS;
        } else {
            replyCode = AuthReplyCode.ALREADY_LOGGED_IN;
        }

        return replyCode;
    }

    private boolean logOn() {
        boolean success = false;
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE `accounts` "
                        + "SET `loggedin` = ?, `session_ip` = ?, `lastlogin` = CURRENT_TIMESTAMP() "
                        + "WHERE `id` = ? AND `loggedin` = ?")) {
            ps.setBoolean(1, true);
            ps.setBoolean(4, false);
            ps.setString(2, super.getSessionIP());
            ps.setInt(3, getAccountId());
            int updatedRows = ps.executeUpdate();
            success = updatedRows == 1;
        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
        }
        return success;
    }

    public final Gender getGender() {
        return gender;
    }

    public final void setGender(final Gender gender) {
        this.gender = gender;
    }

    public final boolean isGm() {
        return isGm;
    }

    private boolean checkCharacterPassword(String password) {
        boolean allow = false;
        if (LoginCrypto.checkSaltedSha512Hash(characterPassword, password, characterPasswordSalt)) {
            allow = true;
        }
        return allow;
    }

    @Override
    public void disconnect(boolean immediately) {
        super.getSession().close(immediately);
    }

    private void allowNewCharacter(final int characterId) {
        allowedChar.add(characterId);
    }

    private boolean isCharacterAuthorized(final int characterId) {
        return allowedChar.contains(characterId);
    }

    private List<LoginCharacter> loadCharacters(final int serverId) { // TODO make this less costly zZz
        final List<LoginCharacter> list = Lists.newArrayList(
                LoginCharacter.loadCharacters(super.getAccountId(), serverId));
        for (final LoginCharacter character : list) {
            allowedChar.add(character.getId());
        }
        return list;
    }

    public final String getCharacterPassword() {
        return characterPassword;
    }

    private boolean canAttemptAgain() {
        return this.loginAttempt++ <= LOGIN_ATTEMPTS;
    }

    public void handleLogin(final PacketReader reader) throws PacketFormatException {
        final String login = reader.readLengthPrefixedString();
        final String password = LoginCrypto.decryptRSA(reader.readLengthPrefixedString());
        final boolean ipBan = this.hasBannedIP();
        AuthReplyCode code = this.authenticate(login, password);
        final Calendar tempbannedTill = this.getTempBanCalendar();
        if (code == AuthReplyCode.SUCCESS && (ipBan)) {
            code = AuthReplyCode.DELETED_OR_BLOCKED;
        }
        if (code != AuthReplyCode.SUCCESS) {
            if (canAttemptAgain()) {
                this.write(LoginPacket.getLoginFailed(code));
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (canAttemptAgain()) {
                this.write(LoginPacket.getTempBan(FiletimeUtil.getFiletime(tempbannedTill.getTimeInMillis()), this.getTempBanReason()));
            }
        } else {
            this.loginAttempt = 0;
            LoginWorker.registerClient(this);
        }
    }

    public void handleWithoutSecondPassword(final PacketReader reader) throws PacketFormatException {
        reader.skip(1);
        final int characterId = reader.readInt();
        if (!isCharacterAuthorized(characterId)) {
            this.disconnect(true);
            return;
        }

        final String password = this.characterPassword;
        if (reader.remaining() > 0) {
            if (password != null) {
                // Hack
                this.disconnect(true);
                return;
            }

            final String input = reader.readLengthPrefixedString();
            if (input.length() >= 4 && input.length() <= 16) {
                this.updateCharacterPassword(input);
            } else {
                this.write(LoginPacket.characterPasswordError((byte) 0x14));
                return;
            }
        } else if (!canAttemptAgain() || password != null) {
            this.disconnect();
            return;
        }
        // TODO: transfer() method
        if (this.getIdleTask() != null) {
            this.getIdleTask().cancel(true);
        }
        final int port = LoginServer.getInstance().getChannelServerPort(this.getChannelId());
        this.write(CommonPackets.getServerIP(port, characterId));
    }

    private void updateCharacterPassword(String newPassword) {
        final Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `char_password` = ?, `char_salt` = ? WHERE id = ?")) {
            final String newSalt = LoginCrypto.makeSalt();
            ps.setString(1, LoginCrypto.padWithRandom(LoginCrypto.makeSaltedSha512Hash(newPassword, newSalt)));
            ps.setString(2, newSalt);
            ps.setInt(3, super.getAccountId());
            ps.executeUpdate();
            this.characterPassword = newPassword;
            this.characterPasswordSalt = newSalt;
        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
        }
    }

    public void handleWithSecondPassword(final PacketReader reader) throws PacketFormatException {
        final String password = reader.readLengthPrefixedString();
        final int characterId = reader.readInt();
        if (!canAttemptAgain() || this.getCharacterPassword() == null
                || !isCharacterAuthorized(characterId)) {
            this.disconnect();
            return;
        }
        if (checkCharacterPassword(password)) {
            // TODO: transfer() method
            if (getIdleTask() != null) {
                getIdleTask().cancel(true);
            }
            final int port = LoginServer.getInstance().getChannelServerPort(this.getChannelId());
            this.write(CommonPackets.getServerIP(port, characterId));
        } else {
            this.write(LoginPacket.characterPasswordError((byte) 0x14));
        }
    }

    public void handleWorldListRequest() {
        final LoginServer ls = LoginServer.getInstance();
        this.write(LoginPacket.getWorldList(2, "Cassiopeia", ls.getChannels()));
        this.write(LoginPacket.getEndOfWorldList());
    }

    public void handleServerStatusRequest() {
        // TODO: Get number of connected clients from chosen channel.
        // (this packet is for the channel, not the world, nib who wrote this.
        this.write(LoginPacket.getWorldStatus(0));
    }

    public void handleCharacterListRequest(final PacketReader reader) throws PacketFormatException {
        final int server = reader.readByte();
        final int channel = reader.readByte() + 1;
        this.setWorldId(server);
        System.out.println(":: Client is connecting to server " + server
                + " channel " + channel + " ::");
        this.setChannelId(channel);
        final List<LoginCharacter> chars = this.loadCharacters(server);
        if (chars != null) {
            this.write(LoginPacket.getCharacterList(getCharacterPassword()
                    != null, chars, characterSlots));
        } else {
            this.disconnect();
        }
    }

    public void handleCharacterNameCheck(final PacketReader reader) throws PacketFormatException {
        final String name = reader.readLengthPrefixedString();
        final boolean isValid = GameCharacterUtil.validateCharacterName(name);
        final boolean isAvailable = GameCharacterUtil.isAvailableName(name);
        final boolean isAllowed = LoginInfoProvider.getInstance().isAllowedName(name);
        final boolean conditions = isValid && isAvailable && isAllowed;
        this.write(LoginPacket.charNameResponse(name, conditions));
    }

    public void handleCreateCharacter(final PacketReader reader) throws PacketFormatException {
        final String name = reader.readLengthPrefixedString();
        final int jobCategory = reader.readInt();
        final short db = reader.readShort();
        final int faceId = reader.readInt();
        final int hairId = reader.readInt();
        final int hairColor = reader.readInt();
        final int skinColorId = reader.readInt();
        final int top = reader.readInt();
        final int bottom = reader.readInt();
        final int shoes = reader.readInt();
        final int weapon = reader.readInt();

        LoginCharacter newchar = LoginCharacter.getDefault(jobCategory);
        newchar.setName(name);
        newchar.setWorldId(this.getWorldId());
        newchar.setFaceId(faceId);
        newchar.setHairId(hairId + hairColor);
        newchar.setGender(gender);
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

        final boolean isValid = GameCharacterUtil.validateCharacterName(name);
        final boolean isAvailable = GameCharacterUtil.isAvailableName(name);
        final boolean isAllowed = li.isAllowedName(name);

        if (isValid && isAvailable && isAllowed) {
            final boolean isDualBlader = jobCategory == 1 && db > 0;
            LoginCharacter.saveNewCharacterToDb(newchar, jobCategory, isDualBlader);
            this.allowNewCharacter(newchar.getId());
            this.write(LoginPacket.addNewCharEntry(newchar, true));
        } else {
            this.write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public void handleDeleteCharacter(final PacketReader reader) throws PacketFormatException {
        String password = null;
        final byte withPassword = reader.readByte();
        if (withPassword > 0) {
            password = reader.readLengthPrefixedString();
        }
        // read passport string
        reader.readLengthPrefixedString();
        
        final int characterId = reader.readInt();
        if (!isCharacterAuthorized(characterId)) {
            disconnect(true);
            return;
        }
        byte state = 0;
        if (characterPassword != null) {
            if (password == null) {
                disconnect(true);
                return;
            } else {
                if (!checkCharacterPassword(password)) {
                    state = 12;
                }
            }
        }
        if (state == 0) {
            if (!deleteCharacter(characterId)) {
                state = 1;
            }
        }
        this.write(LoginPacket.deleteCharResponse(characterId, state));
    }
}
