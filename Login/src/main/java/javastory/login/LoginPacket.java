package javastory.login;

import java.util.List;
import java.util.Map;

import handling.GamePacket;
import handling.ServerPacketOpcode;
import javastory.server.LoginChannelInfo;
import javastory.io.PacketBuilder;
import javastory.server.login.AuthReplyCode;
import tools.HexTool;
import tools.packet.GameCharacterPacket;

public final class LoginPacket {


    public static GamePacket loginCertificate() {
        final PacketBuilder builder = new PacketBuilder(16);

        builder.writeAsShort(0x12);
        builder.writeLengthPrefixedString("30819F300D06092A864886F70D010101050003818D0030818902818100994F4E66B003A7843C944E67BE4375203DAA203C676908E59839C9BADE95F53E848AAFE61DB9C09E80F48675CA2696F4E897B7F18CCB6398D221C4EC5823D11CA1FB9764A78F84711B8B6FCA9F01B171A51EC66C02CDA9308887CEE8E59C4FF0B146BF71F697EB11EDCEBFCE02FB0101A7076A3FEB64F6F6022C8417EB6B87270203010001");

        return builder.getPacket();
    }

    public static GamePacket getLoginFailed(final AuthReplyCode replyCode) {
        final PacketBuilder builder = new PacketBuilder(16);

        /* 3: ID deleted or blocked
         * 4: Incorrect password
         * 5: Not a registered id
         * 6: System error
         * 7: Already logged in
         * 8: System error
         * 9: System error
         * 10: Cannot process so many connections
         * 11: Only users older than 20 can use this channel
         * 13: Unable to log on as master at this ip
         * 14: Wrong gateway or personal info and weird korean button
         * 15: Processing request with that korean button!
         * 16: Please verify your account through email...
         * 17: Wrong gateway or personal info
         * 21: Please verify your account through email...
         * 23: License agreement
         * 25: Maple Europe notice
         * 27: Some weird full client notice, probably for trial versions
         */

        builder.writeAsShort(ServerPacketOpcode.LOGIN_STATUS.getValue());
        builder.writeInt(replyCode.asNumber());
        builder.writeAsShort(0);

        return builder.getPacket();
    }

    public static GamePacket getPermBan(final byte reason) {
        final PacketBuilder builder = new PacketBuilder(16);

        builder.writeAsShort(ServerPacketOpcode.LOGIN_STATUS.getValue());
        builder.writeAsShort(2); // Account is banned
        builder.writeAsByte(0);
        builder.writeByte(reason);
        builder.writeBytes(HexTool.getByteArrayFromHexString("01 01 01 01 00"));

        return builder.getPacket();
    }

    public static GamePacket getTempBan(final long timestampTill, final byte reason) {
        final PacketBuilder builder = new PacketBuilder(17);

        builder.writeAsShort(ServerPacketOpcode.LOGIN_STATUS.getValue());
        builder.writeAsByte(2);
        builder.writeBytes(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
        builder.writeByte(reason);
        builder.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.

        return builder.getPacket();
    }

    public static GamePacket getAuthSuccessRequest(final LoginClient client) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.LOGIN_STATUS.getValue());
        builder.writeAsByte(0);
        builder.writeInt(client.getAccountId());
        builder.writeByte(client.getGender().asNumber());
        builder.writeAsByte(client.isGm() ? 1 : 0); // Admin byte
        builder.writeAsByte(0);
        builder.writeLengthPrefixedString("T13333333337W");
        builder.writeZeroBytes(12);
        /*00 00
        00
        9C 7E 0F 00
        00
        00 00
        0D 00 54 30 36 31 31 30 35 30 34 33 31 38 4B
        03 00 00 00
        00 00 00 00
        00 00 00 00
        ...?~.......T06110504318K............*/
        return builder.getPacket();
    }

    public static GamePacket deleteCharResponse(final int cid, final int state) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        builder.writeInt(cid);
        builder.writeAsByte(state);

        return builder.getPacket();
    }

    public static GamePacket characterPasswordError(final byte mode) {
        final PacketBuilder builder = new PacketBuilder(3);

        builder.writeAsShort(ServerPacketOpcode.SECONDPW_ERROR.getValue());
        builder.writeByte(mode);

        return builder.getPacket();
    }

    public static GamePacket getWorldList(
            final int worldId,
            final String worldName,
            final Map<Integer, LoginChannelInfo> channels) {

        final PacketBuilder builder = new PacketBuilder();
        builder.writeAsShort(ServerPacketOpcode.SERVERLIST.getValue());
        builder.writeAsByte(worldId);
        builder.writeLengthPrefixedString(worldName);
        
        // TODO, fairly sure this flag is world-specific, not loginserver stuff.
        builder.writeAsByte(0);
        
        // TODO: another world-specific thing.
        builder.writeLengthPrefixedString("");
        builder.writeAsByte(0x64);
        builder.writeAsByte(0);
        builder.writeAsByte(0x64);
        builder.writeAsByte(0);
        int count = channels.size();
        builder.writeAsByte(count);
        for (LoginChannelInfo info : channels.values()) {
            builder.writeLengthPrefixedString(info.getName());
            builder.writeInt(info.getLoad());
            builder.writeAsByte(worldId);
            builder.writeAsShort(info.getId());
        }
        builder.writeAsShort(0);

        return builder.getPacket();
    }

    public static GamePacket getEndOfWorldList() {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SERVERLIST.getValue());
        builder.writeAsByte(0xFF);

        return builder.getPacket();
    }

    public static GamePacket getWorldStatus(final int status) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.SERVERSTATUS.getValue());
        builder.writeAsShort(status);

        return builder.getPacket();
    }

    public static GamePacket getCharacterList(final boolean secondpw, final List<LoginCharacter> chars, int maxCharacters) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CHARLIST.getValue());
        builder.writeAsByte(0);
        builder.writeAsByte(chars.size());
        for (final LoginCharacter chr : chars) {
            addCharEntry(builder, chr);
        }
        builder.writeAsByte(secondpw ? 1 : 0);
        builder.writeAsByte(0);
        builder.writeAsByte(maxCharacters);
        builder.writeAsByte(0);
        builder.writeAsShort(0);
        builder.writeInt(0);

        return builder.getPacket();
    }

    public static GamePacket addNewCharEntry(final LoginCharacter chr, final boolean worked) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        builder.writeAsByte(worked ? 0 : 1);
        addCharEntry(builder, chr);

        return builder.getPacket();
    }

    public static GamePacket charNameResponse(final String charname, final boolean nameUsed) {
        final PacketBuilder builder = new PacketBuilder();

        builder.writeAsShort(ServerPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        builder.writeLengthPrefixedString(charname);
        builder.writeAsByte(nameUsed ? 1 : 0);

        return builder.getPacket();
    }

    private static void addCharEntry(final PacketBuilder builder, final LoginCharacter chr) {
        GameCharacterPacket.addCharStats(builder, chr);
        GameCharacterPacket.addCharLook(builder, chr, true);
        builder.writeAsByte(0);
        builder.writeAsByte(0);
    }
}