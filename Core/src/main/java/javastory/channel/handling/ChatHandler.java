package javastory.channel.handling;

import java.rmi.RemoteException;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.PartyMember;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.rmi.WorldChannelInterface;
import javastory.tools.packets.ChannelPackets;

public class ChatHandler {

	public static void handleGeneralChat(final String text, final byte unk, final ChannelClient c, final ChannelCharacter chr) {
		// TODO: Add commands later.
		if (!chr.isGM() && text.length() >= 80) {
			return;
		}
		final ChannelCharacter player = c.getPlayer();
		chr.getMap().broadcastMessage(ChannelPackets.getChatText(chr.getId(), text, player.isGM(), unk), player.getPosition());
	}

	public static void handleGroupChat(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final int type = reader.readByte();
		final byte numRecipients = reader.readByte();
		final int recipients[] = new int[numRecipients];

		for (byte i = 0; i < numRecipients; i++) {
			recipients[i] = reader.readInt();
		}
		final String message = reader.readLengthPrefixedString();

		try {
			final WorldChannelInterface worldInterface = ChannelServer.getWorldInterface();
			switch (type) {
			case 0:
				worldInterface.buddyChat(recipients, chr.getId(), chr.getName(), message);
				break;
			case 1:
				final PartyMember member = chr.getPartyMembership();
				if (member == null) {
					break;
				}

				worldInterface.partyChat(member.getPartyId(), message, chr.getName());
				break;
			case 2:
				worldInterface.guildChat(chr.getGuildId(), chr.getName(), chr.getId(), message);
				break;
			}
		} catch (final RemoteException e) {
			ChannelServer.pingWorld();
		}
	}

	public static void handleMessenger(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		String input;
		final WorldChannelInterface wci = ChannelServer.getWorldInterface();
		final ChannelCharacter player = c.getPlayer();
		Messenger messenger = player.getMessenger();

		switch (reader.readByte()) {
		case 0x00: // open
			if (messenger == null) {
				final int messengerid = reader.readInt();
				if (messengerid == 0) { // create
					try {
						final MessengerMember messengerplayer = new MessengerMember(player);
						messenger = wci.createMessenger(messengerplayer);
						player.setMessenger(messenger);
						player.setMessengerPosition(0);
					} catch (final RemoteException e) {
						ChannelServer.pingWorld();
					}
				} else { // join
					try {
						messenger = wci.getMessenger(messengerid);
						if (messenger != null) {
							final int position = messenger.getLowestPosition();
							final MessengerMember messengerplayer = new MessengerMember(player, position);
							if (messenger.getMembers().size() < 3) {
								player.setMessenger(messenger);
								player.setMessengerPosition(position);
								wci.joinMessenger(messenger.getId(), messengerplayer, player.getName(), messengerplayer.getChannel());
							}
						}
					} catch (final RemoteException e) {
						ChannelServer.pingWorld();
					}
				}
			}
			break;
		case 0x02: // exit
			if (messenger != null) {
				final MessengerMember messengerplayer = new MessengerMember(player);
				try {
					wci.leaveMessenger(messenger.getId(), messengerplayer);
				} catch (final RemoteException e) {
					ChannelServer.pingWorld();
				}
				player.setMessenger(null);
				player.setMessengerPosition(4);
			}
			break;
		case 0x03: // invite
			if (messenger.getMembers().size() < 3) {
				input = reader.readLengthPrefixedString();
				final ChannelCharacter target = ChannelServer.getPlayerStorage().getCharacterByName(input);

				if (target != null) {
					if (target.getMessenger() == null) {
						target.getClient().write(ChannelPackets.messengerInvite(player.getName(), messenger.getId()));

						if (!target.isGM()) {
							c.write(ChannelPackets.messengerNote(input, 4, 1));
						} else {
							c.write(ChannelPackets.messengerNote(input, 4, 0));
						}
					} else {
						c.write(ChannelPackets.messengerChat(player.getName() + " : " + input + " is already using Maple Messenger"));
					}
				} else {
					try {
						if (wci.isConnected(input)) {
							wci.messengerInvite(player.getName(), messenger.getId(), input, c.getChannelId());
						} else {
							c.write(ChannelPackets.messengerNote(input, 4, 0));
						}
					} catch (final RemoteException e) {
						ChannelServer.pingWorld();
					}
				}
			}
			break;
		case 0x05: // decline
			final String targeted = reader.readLengthPrefixedString();
			final ChannelCharacter target = ChannelServer.getPlayerStorage().getCharacterByName(targeted);
			if (target != null) { // This channel
				if (target.getMessenger() != null) {
					target.getClient().write(ChannelPackets.messengerNote(player.getName(), 5, 0));
				}
			} else { // Other channel
				try {
					if (!player.isGM()) {
						wci.declineChat(targeted, player.getName());
					}
				} catch (final RemoteException e) {
					ChannelServer.pingWorld();
				}
			}
			break;
		case 0x06: // message
			if (messenger != null) {
				final MessengerMember messengerplayer = new MessengerMember(player);
				input = reader.readLengthPrefixedString();
				try {
					wci.messengerChat(messenger.getId(), messengerplayer.getName(), input);
				} catch (final RemoteException e) {
					ChannelServer.pingWorld();
				}
			}
			break;
		}
	}

	public static void handleWhisper(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final byte mode = reader.readByte();
		reader.readInt();
		final ChannelCharacter player = c.getPlayer();
		switch (mode) {
		case 5: { // Find
			final String recipient = reader.readLengthPrefixedString();
			final ChannelCharacter target = ChannelServer.getPlayerStorage().getCharacterByName(recipient);
			if (target != null) {
				if (!target.isGM() || player.isGM() && target.isGM()) {
					c.write(ChannelPackets.getFindReplyWithMap(target.getName(), target.getMap().getId()));
				} else {
					c.write(ChannelPackets.getWhisperReply(recipient, (byte) 0));
				}
			} else { // Not found
				// TODO: Not complete, finish when proper ChannelServer remoting
				// is done.
				c.write(ChannelPackets.getWhisperReply(recipient, (byte) 0));
			}
			break;
		}
		case 6: { // Whisper
			final String recipient = reader.readLengthPrefixedString();
			final String text = reader.readLengthPrefixedString();

			final ChannelCharacter target = ChannelServer.getPlayerStorage().getCharacterByName(recipient);
			if (target != null) {
				target.getClient().write(ChannelPackets.getWhisper(player.getName(), c.getChannelId(), text));
				if (target.isGM()) {
					c.write(ChannelPackets.getWhisperReply(recipient, (byte) 0));
				} else {
					c.write(ChannelPackets.getWhisperReply(recipient, (byte) 1));
				}
			} else { // Not found
				// TODO: Not complete, finish when proper ChannelServer remoting
				// is done.
				c.write(ChannelPackets.getWhisperReply(recipient, (byte) 0));
			}
			break;
		}
		}
	}
}
