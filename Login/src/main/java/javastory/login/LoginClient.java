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

import javastory.channel.client.MemberRank;
import javastory.client.GameCharacterUtil;
import javastory.client.GameClient;
import javastory.client.LoginCrypto;
import javastory.cryptography.AesTransform;
import javastory.db.Database;
import javastory.game.Gender;
import javastory.game.IItem;
import javastory.game.Inventory;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.FiletimeUtil;
import javastory.tools.packets.CommonPackets;

import org.apache.mina.core.session.IoSession;

import com.google.common.collect.Lists;

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
    private final List<Integer> allowedChar = Lists.newLinkedList();

    public LoginClient(final AesTransform clientCrypto, final AesTransform serverCrypto, final IoSession session) {
        super(clientCrypto, serverCrypto, session);
        this.loggedIn = false;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        final Connection con = Database.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
            ps.setString(1, super.getSessionIP());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    ret = true;
                }
            }
        } catch (final SQLException ex) {
            System.err.println("Error checking ip bans" + ex);
        }
        return ret;
    }

    public final boolean deleteCharacter(final int characterId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final Connection con = Database.getConnection();
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
            } catch (final SQLException ex) {
                System.err.println("Error while closing SQL connection: " + ex);
            }
        }
        return false;
    }

    private Calendar getTempBanCalendar(final ResultSet rs) throws SQLException {
        final Calendar lTempban = Calendar.getInstance();
        if (rs.getLong("tempban") == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        final Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return this.temporaryBan;
    }

    public byte getTempBanReason() {
        return this.temporaryBanReason;
    }

    public AuthReplyCode authenticate(final String username, final String inputPassword) {
        AuthReplyCode replyCode = AuthReplyCode.NOT_REGISTERED;
        final Connection con = Database.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `accounts` WHERE `name` = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int banned = rs.getInt("banned");
                    if (banned > 0) {
                        replyCode = AuthReplyCode.DELETED_OR_BLOCKED;
                        return replyCode;
                    } else if (banned == -1) {
                        this.unban();
                    }
                    super.setAccountId(rs.getInt("id"));
                    super.setAccountName(rs.getString("name"));

                    this.loginPassword = rs.getString("password");
                    this.loginPasswordSalt = rs.getString("salt");

                    this.characterPassword = rs.getString("char_password");
                    this.characterPasswordSalt = rs.getString("char_salt");

                    this.isGm = rs.getInt("gm") > 0;
                    this.temporaryBanReason = rs.getByte("tempban_reason");
                    this.temporaryBan = this.getTempBanCalendar(rs);
                    this.gender = Gender.fromNumber(rs.getByte("gender"));

                    this.characterSlots = rs.getInt("character_slots");
                }
            }
        } catch (final SQLException e) {
            System.err.println("ERROR" + e);
        }

        if (this.characterPassword != null && this.characterPasswordSalt != null) {
            this.characterPassword = LoginCrypto.getPadding(this.characterPassword);
        }

        if (!LoginCrypto.checkSaltedSha512Hash(this.loginPassword, inputPassword, this.loginPasswordSalt)) {
            replyCode = AuthReplyCode.WRONG_PASSWORD;
        }

        this.loggedIn = this.logOn();
        if (this.loggedIn) {
            replyCode = AuthReplyCode.SUCCESS;
        } else {
            replyCode = AuthReplyCode.ALREADY_LOGGED_IN;
        }

        return replyCode;
    }

    private boolean logOn() {
        boolean success = false;
        final Connection con = Database.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE `accounts` "
                        + "SET `loggedin` = ?, `session_ip` = ?, `lastlogin` = CURRENT_TIMESTAMP() "
                        + "WHERE `id` = ? AND `loggedin` = ?")) {
            ps.setBoolean(1, true);
            ps.setBoolean(4, false);
            ps.setString(2, super.getSessionIP());
            ps.setInt(3, this.getAccountId());
            final int updatedRows = ps.executeUpdate();
            success = updatedRows == 1;
        } catch (final SQLException e) {
            System.err.println("error updating login state" + e);
        }
        return success;
    }

    public final Gender getGender() {
        return this.gender;
    }

    public final void setGender(final Gender gender) {
        this.gender = gender;
    }

    public final boolean isGm() {
        return this.isGm;
    }

    private boolean checkCharacterPassword(final String password) {
        boolean allow = false;
        if (LoginCrypto.checkSaltedSha512Hash(this.characterPassword, password, this.characterPasswordSalt)) {
            allow = true;
        }
        return allow;
    }

    @Override
    public void disconnect(final boolean immediately) {
        super.getSession().close(immediately);
    }

    private void allowNewCharacter(final int characterId) {
        this.allowedChar.add(characterId);
    }

    private boolean isCharacterAuthorized(final int characterId) {
        return this.allowedChar.contains(characterId);
    }

    private List<LoginCharacter> loadCharacters(final int serverId) { // TODO make this less costly zZz
        final List<LoginCharacter> list = Lists.newArrayList(
                LoginCharacter.loadCharacters(super.getAccountId(), serverId));
        for (final LoginCharacter character : list) {
            this.allowedChar.add(character.getId());
        }
        return list;
    }

    public final String getCharacterPassword() {
        return this.characterPassword;
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
        if (code == AuthReplyCode.SUCCESS && ipBan) {
            code = AuthReplyCode.DELETED_OR_BLOCKED;
        }
        if (code != AuthReplyCode.SUCCESS) {
            if (this.canAttemptAgain()) {
                this.write(LoginPacket.getLoginFailed(code));
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (this.canAttemptAgain()) {
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
        if (!this.isCharacterAuthorized(characterId)) {
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
        } else if (!this.canAttemptAgain() || password != null) {
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

    private void updateCharacterPassword(final String newPassword) {
        final Connection con = Database.getConnection();
        try (PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `char_password` = ?, `char_salt` = ? WHERE id = ?")) {
            final String newSalt = LoginCrypto.makeSalt();
            ps.setString(1, LoginCrypto.padWithRandom(LoginCrypto.makeSaltedSha512Hash(newPassword, newSalt)));
            ps.setString(2, newSalt);
            ps.setInt(3, super.getAccountId());
            ps.executeUpdate();
            this.characterPassword = newPassword;
            this.characterPasswordSalt = newSalt;
        } catch (final SQLException e) {
            System.err.println("error updating login state" + e);
        }
    }

    public void handleWithSecondPassword(final PacketReader reader) throws PacketFormatException {
        final String password = reader.readLengthPrefixedString();
        final int characterId = reader.readInt();
        if (!this.canAttemptAgain() || this.getCharacterPassword() == null
                || !this.isCharacterAuthorized(characterId)) {
            this.disconnect();
            return;
        }
        if (this.checkCharacterPassword(password)) {
            // TODO: transfer() method
            if (this.getIdleTask() != null) {
                this.getIdleTask().cancel(true);
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
            this.write(LoginPacket.getCharacterList(this.getCharacterPassword()
                    != null, chars, this.characterSlots));
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

        final LoginCharacter newchar = LoginCharacter.getDefault(jobCategory);
        newchar.setName(name);
        newchar.setWorldId(this.getWorldId());
        newchar.setFaceId(faceId);
        newchar.setHairId(hairId + hairColor);
        newchar.setGender(this.gender);
        newchar.setSkinColorId(skinColorId);
        final Inventory equip = newchar.getEquippedItemsInventory();
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
        if (!this.isCharacterAuthorized(characterId)) {
            this.disconnect(true);
            return;
        }
        byte state = 0;
        if (this.characterPassword != null) {
            if (password == null) {
                this.disconnect(true);
                return;
            } else {
                if (!this.checkCharacterPassword(password)) {
                    state = 12;
                }
            }
        }
        if (state == 0) {
            if (!this.deleteCharacter(characterId)) {
                state = 1;
            }
        }
        this.write(LoginPacket.deleteCharResponse(characterId, state));
    }
}
