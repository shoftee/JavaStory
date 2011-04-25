package handling.channel.handler;

import java.rmi.RemoteException;

import client.GameClient;
import client.GameCharacter;
import client.messages.CommandProcessor;
import handling.world.Messenger;
import handling.world.MessengerMember;
import handling.world.remote.WorldChannelInterface;
import org.javastory.io.PacketFormatException;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import tools.MaplePacketCreator;
import org.javastory.io.PacketReader;

public class ChatHandler {

    public static final void handleGeneralChat(final String text, final byte unk, final GameClient c, final GameCharacter chr) {
        if (!CommandProcessor.getInstance().processCommand(c, text)) {
            if (!chr.isGM() && text.length() >= 80) {
                return;
            }
            chr.getMap().broadcastMessage(MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isGM(), unk), c.getPlayer().getPosition());
        }
    }

    public static final void handlePartyChat(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
        final int type = reader.readByte();
        final byte numRecipients = reader.readByte();
        int recipients[] = new int[numRecipients];

        for (byte i = 0; i < numRecipients; i++) {
            recipients[i] = reader.readInt();
        }
        final String chattext = reader.readLengthPrefixedString();

        try {
            switch (type) {
                case 0:
                    c.getChannelServer().getWorldInterface().buddyChat(recipients, chr.getId(), chr.getName(), chattext);
                    break;
                case 1:
                    c.getChannelServer().getWorldInterface().partyChat(chr.getParty().getId(), chattext, chr.getName());
                    break;
                case 2:
                    c.getChannelServer().getWorldInterface().guildChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                    break;
                case 3:
                    c.getChannelServer().getWorldInterface().allianceChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                    break;
            }
        } catch (RemoteException e) {
            c.getChannelServer().pingWorld();
        }
    }

    public static final void handleMessenger(final PacketReader reader, final GameClient c) throws PacketFormatException {
        String input;
        final WorldChannelInterface wci = ChannelManager.getInstance(c.getChannelId()).getWorldInterface();
        Messenger messenger = c.getPlayer().getMessenger();

        switch (reader.readByte()) {
            case 0x00: // open
                if (messenger == null) {
                    int messengerid = reader.readInt();
                    if (messengerid == 0) { // create
                        try {
                            final MessengerMember messengerplayer = new MessengerMember(c.getPlayer());
                            messenger = wci.createMessenger(messengerplayer);
                            c.getPlayer().setMessenger(messenger);
                            c.getPlayer().setMessengerPosition(0);
                        } catch (RemoteException e) {
                            c.getChannelServer().pingWorld();
                        }
                    } else { // join
                        try {
                            messenger = wci.getMessenger(messengerid);
                            final int position = messenger.getLowestPosition();
                            final MessengerMember messengerplayer = new MessengerMember(c.getPlayer(), position);
                            if (messenger != null) {
                                if (messenger.getMembers().size() < 3) {
                                    c.getPlayer().setMessenger(messenger);
                                    c.getPlayer().setMessengerPosition(position);
                                    wci.joinMessenger(messenger.getId(), messengerplayer, c.getPlayer().getName(), messengerplayer.getChannel());
                                }
                            }
                        } catch (RemoteException e) {
                            c.getChannelServer().pingWorld();
                        }
                    }
                }
                break;
            case 0x02: // exit
                if (messenger != null) {
                    final MessengerMember messengerplayer = new MessengerMember(c.getPlayer());
                    try {
                        wci.leaveMessenger(messenger.getId(), messengerplayer);
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                    c.getPlayer().setMessenger(null);
                    c.getPlayer().setMessengerPosition(4);
                }
                break;
            case 0x03: // invite
                if (messenger.getMembers().size() < 3) {
                    input = reader.readLengthPrefixedString();
                    final GameCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);

                    if (target != null) {
                        if (target.getMessenger() == null) {
                            target.getClient().write(MaplePacketCreator.messengerInvite(c.getPlayer().getName(), messenger.getId()));

                            if (!target.isGM()) {
                                c.write(MaplePacketCreator.messengerNote(input, 4, 1));
                            } else {
                                c.write(MaplePacketCreator.messengerNote(input, 4, 0));
                            }
                        } else {
                            c.write(MaplePacketCreator.messengerChat(c.getPlayer().getName() +
                                    " : " + input +
                                    " is already using Maple Messenger"));
                        }
                    } else {
                        try {
                            if (wci.isConnected(input)) {
                                wci.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannelId());
                            } else {
                                c.write(MaplePacketCreator.messengerNote(input, 4, 0));
                            }
                        } catch (RemoteException e) {
                            c.getChannelServer().pingWorld();
                        }
                    }
                }
                break;
            case 0x05: // decline
                final String targeted = reader.readLengthPrefixedString();
                final GameCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) { // This channel
                    if (target.getMessenger() != null) {
                        target.getClient().write(MaplePacketCreator.messengerNote(c.getPlayer().getName(), 5, 0));
                    }
                } else { // Other channel
                    try {
                        if (!c.getPlayer().isGM()) {
                            wci.declineChat(targeted, c.getPlayer().getName());
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                }
                break;
            case 0x06: // message
                if (messenger != null) {
                    final MessengerMember messengerplayer = new MessengerMember(c.getPlayer());
                    input = reader.readLengthPrefixedString();
                    try {
                        wci.messengerChat(messenger.getId(), input, messengerplayer.getName());
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                }
                break;
        }
    }

    public static final void handleWhisper(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final byte mode = reader.readByte();
        reader.readInt();
        switch (mode) {
            case 5: { // Find
                final String recipient = reader.readLengthPrefixedString();
                GameCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player != null) {
                    if (!player.isGM() || c.getPlayer().isGM() && player.isGM()) {
                        if (player == null) { // cs? lol
                            c.write(MaplePacketCreator.getFindReplyWithCS(player.getName()));
                        } else {
                            c.write(MaplePacketCreator.getFindReplyWithMap(player.getName(), player.getMap().getId()));
                        }
                    } else {
                        c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } else { // Not found
                    for (ChannelServer cserv : ChannelManager.getAllInstances()) {
                        player = cserv.getPlayerStorage().getCharacterByName(recipient);
                        if (player != null) {
                            if (!player.isGM() || c.getPlayer().isGM() &&
                                    player.isGM()) {
                                c.write(MaplePacketCreator.getFindReply(player.getName(), (byte) player.getClient().getChannelId()));
                            } else {
                                c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            }
                            return;
                        }
                    }
                    c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                }
                break;
            }
            case 6: { // Whisper
                final String recipient = reader.readLengthPrefixedString();
                final String text = reader.readLengthPrefixedString();

                GameCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player != null) {
                    player.getClient().write(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannelId(), text));
                    if (player.isGM()) {
                        c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    } else {
                        c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    }
                } else { // Not found
                    for (ChannelServer cserv : ChannelManager.getAllInstances()) {
                        player = cserv.getPlayerStorage().getCharacterByName(recipient);
                        if (player != null) {
                            break;
                        }
                    }
                    if (player != null) {
                        try {
                            ChannelManager.getInstance(c.getChannelId()).getWorldInterface().whisper(c.getPlayer().getName(), player.getName(), c.getChannelId(), text);
                            if (!c.getPlayer().isGM() && player.isGM()) {
                                c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            } else {
                                c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                            }
                        } catch (RemoteException re) {
                            c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            c.getChannelServer().pingWorld();
                        }
                    } else {
                        c.write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                }
                break;
            }
        }
    }
}
