/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.client;

import client.LoginCrypto;
import com.google.common.collect.Lists;
import database.DatabaseConnection;
import handling.world.guild.GuildMember;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.mina.core.session.IoSession;
import org.javastory.cryptography.AesTransform;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.PlayerStorage;
import org.javastory.server.login.AuthReplyCode;
import org.javastory.server.login.LoginServer;

/**
 *
 * @author Tosho
 */
public final class LoginClient extends GameClient {

    private transient List<Integer> allowedChar = Lists.newLinkedList();
    private transient String characterPassword, characterPasswordSalt;
    private transient Calendar birthday = null, tempban = null;
    private boolean isGm;
    private byte tempBanReason = 1, gender = -1;
    public transient short loginAttempt = 0;

    public LoginClient(AesTransform clientCrypto, AesTransform serverCrypto,
            IoSession session) {
        super(clientCrypto, serverCrypto, session);
        loggedIn = false;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
            ps.setString(1, super.getSessionIP());
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error checking ip bans" + ex);
        }
        return ret;
    }

    public final boolean deleteCharacter(final int characterId) {
        try {
            final Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, characterId);
            ps.setInt(2, super.getAccountId());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                return false;
            }
            if (rs.getInt("guildid") > 0) {
                // is in a guild when deleted
                String characterName = rs.getString("name");
                byte characterChannelId = -1;
                int characterJobId = 0;
                MemberRank characterRank = MemberRank.fromNumber(rs.getInt("guildrank"));
                int characterGuildId = rs.getInt("guildid");
                final GuildMember mgc = new GuildMember(characterId, (short) 0, characterName, characterChannelId, characterJobId, characterRank, characterGuildId, false);
                try {
                    LoginServer.getInstance().getWorldInterface().deleteGuildCharacter(mgc);
                } catch (RemoteException e) {
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
        } catch (final SQLException e) {
            System.err.println("DeleteChar error" + e);
        }
        return false;
    }

    public final void updateSecondPassword() {
        try {
            final Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `char_password` = ?, `char_salt` = ? WHERE id = ?");
            final String newSalt = LoginCrypto.makeSalt();
            ps.setString(1, LoginCrypto.padWithRandom(LoginCrypto.makeSaltedSha512Hash(characterPassword, newSalt)));
            ps.setString(2, newSalt);
            ps.setInt(3, super.getAccountId());
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
        }
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
        return tempban;
    }

    public byte getTempBanReason() {
        return tempBanReason;
    }

    public AuthReplyCode authenticate(String login, String pwd, boolean ipMacBanned) {
        AuthReplyCode replyCode = AuthReplyCode.NOT_REGISTERED;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM `accounts` WHERE `name` = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                final int banned = rs.getInt("banned");
                if (banned > 0) {
                    replyCode = AuthReplyCode.DELETED_OR_BLOCKED;
                    return replyCode;
                } else if (banned == -1) {
                    unban();
                }
                final String passhash = rs.getString("password");
                final String salt = rs.getString("salt");

                super.setAccountId(rs.getInt("id"));
                super.setAccountName(rs.getString("name"));
                characterPassword = rs.getString("char_password");
                characterPasswordSalt = rs.getString("char_salt");
                isGm = rs.getInt("gm") > 0;
                tempBanReason = rs.getByte("tempban_reason");
                tempban = getTempBanCalendar(rs);
                gender = rs.getByte("gender");
                birthday = Calendar.getInstance();
                Date date = rs.getDate("birthday");
                long timestamp = date.getTime();
                if (timestamp > 0) {
                    birthday.setTimeInMillis(timestamp);
                }

                if (characterPassword != null && characterPasswordSalt != null) {
                    characterPassword = LoginCrypto.getPadding(characterPassword);
                }

                if (!LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
                    replyCode = AuthReplyCode.WRONG_PASSWORD;
                }

                ps.close();
                loggedIn = logOn();
                if (loggedIn) {
                    replyCode = AuthReplyCode.SUCCESS;
                } else {
                    replyCode = AuthReplyCode.ALREADY_LOGGED_IN;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR" + e);
        }
        return replyCode;
    }

    public void initiateTransfer(int characterId, int targetChannelId) {
        final PlayerStorage playerStorage = ChannelManager.getInstance(targetChannelId).getPlayerStorage();
        playerStorage.registerSession(characterId, super.getSessionIP());
    }

    private boolean logOn() {
        int rows = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            // Sets loggedin = true if loggedin = false.
            // Query returns 0 (rows updated) if loggedin was already true.
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE `accounts` " +
                    "SET `loggedin` = ?, `session_ip` = ?, `lastlogin` = CURRENT_TIMESTAMP() " +
                    "WHERE `id` = ? AND `loggedin` = ?");
            ps.setBoolean(1, true);
            ps.setBoolean(4, false);
            ps.setString(2, super.getSessionIP());
            ps.setInt(3, getAccountId());
            rows = ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
        }
        return rows == 1;
    }

    public final boolean checkBirthDate(final Calendar date) {
        if (date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) &&
                date.get(Calendar.DAY_OF_MONTH) ==
                birthday.get(Calendar.DAY_OF_MONTH)) {
            return true;
        }
        return false;
    }

    public final byte getGender() {
        return gender;
    }

    public final void setGender(final byte gender) {
        this.gender = gender;
    }

    public final boolean isGm() {
        return isGm;
    }

    public boolean checkCharacterPassword(String password) {
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

    public void createdChar(final int id) {
        allowedChar.add(id);
    }

    public final boolean login_Auth(final int id) {
        return allowedChar.contains(id);
    }

    public final List<LoginCharacter> loadCharacters(final int serverId) { // TODO make this less costly zZz
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

    public final void setCharacterPassword(final String password) {
        this.characterPassword = password;
    }
}
