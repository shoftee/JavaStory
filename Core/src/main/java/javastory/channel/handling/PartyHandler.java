package javastory.channel.handling;

import java.rmi.RemoteException;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.rmi.WorldChannelInterface;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.PartyOperation;

public class PartyHandler {

    public static void handleDenyPartyInvitation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
        final int action = reader.readByte();
        final int partyid = reader.readInt();
        final ChannelCharacter player = c.getPlayer();
        PartyMember member = player.getPartyMembership();
        if (member == null) {
            player.sendNotice(5, "You can't join the party as you are already in one");
            return;
        }
        try {
            WorldChannelInterface wci = ChannelServer.getInstance().getWorldInterface();
            Party party = wci.getParty(partyid);
            if (party == null) {
                player.sendNotice(5, "The party you are trying to join does not exist");
                return;
            }

            switch (action) {
                case 0x1B:
                    //accept
                    if (party.getMembers().size() < 6) {
                        wci.updateParty(partyid, PartyOperation.JOIN, new PartyMember(partyid, player));
                        player.receivePartyMemberHP();
                        player.updatePartyMemberHP();
                    } else {
                        c.write(ChannelPackets.partyStatusMessage(17));
                    }
                    break;
                case 0x16:
                    break;
                default:
                    final ChannelCharacter cfrom = ChannelServer.getInstance().getPlayerStorage().getCharacterById(party.getLeader().getCharacterId());
                    if (cfrom != null) {
                        cfrom.getClient().write(ChannelPackets.partyStatusMessage(23, player.getName()));
                    }
                    break;
            }
        } catch (RemoteException e) {
            ChannelServer.getInstance().pingWorld();
        }
    }

    public static void handlePartyOperation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
        final int operation = reader.readByte();
        final WorldChannelInterface wci = ChannelServer.getInstance().getWorldInterface();
        final ChannelCharacter player = c.getPlayer();
        PartyMember member = player.getPartyMembership();
        switch (operation) {
            case 1: // create
                if (member == null) {
                    try {
                        Party party = wci.createParty();
                        member = player.setPartyMembership(party.getId());
                        party.addMember(member);
                    } catch (RemoteException e) {
                        ChannelServer.getInstance().pingWorld();
                    }
                    c.write(ChannelPackets.partyCreated());
                } else {
                    player.sendNotice(5, "You can't create a party as you are already in one.");
                }
                break;
            case 2:
                // leave
                if (member != null) { //are we in a party? o.O"
                    try {
                        if (member.isLeader()) {
                            // disband
                            wci.updateParty(member.getPartyId(), PartyOperation.DISBAND, member);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().disbandParty();
                            }
                        } else {
                            wci.updateParty(member.getPartyId(), PartyOperation.LEAVE, member);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().leftParty(player);
                            }
                        }
                    } catch (RemoteException e) {
                        ChannelServer.getInstance().pingWorld();
                    }
                    player.setParty(null);
                }
                break;
            case 3:
                // accept invitation
                final int partyId = reader.readInt();
                if (!player.hasParty()) {
                    try {
                        Party party = wci.getParty(partyId);
                        if (party != null) {
                            if (party.getMembers().size() < 6) {
                                member = player.setPartyMembership(partyId);
                                wci.updateParty(party.getId(), PartyOperation.JOIN, member);
                                player.receivePartyMemberHP();
                                player.updatePartyMemberHP();
                            } else {
                                c.write(ChannelPackets.partyStatusMessage(17));
                            }
                        } else {
                            player.sendNotice(5, "The party you are trying to join does not exist");
                        }
                    } catch (RemoteException e) {
                        ChannelServer.getInstance().pingWorld();
                    }
                } else {
                    player.sendNotice(5, "You can't join the party as you are already in one");
                }
                break;
            case 4:
                // invite
                // TODO store pending invitations and check against them
                final String name = reader.readLengthPrefixedString();
                final ChannelCharacter inviter =
                        ChannelServer.getInstance().getPlayerStorage().getCharacterByName(name);
                if (inviter != null && inviter.getWorldId() == c.getWorldId()) {
                    PartyMember inviterMember = inviter.getPartyMembership();
                    if (inviterMember != null) {
                        try {
                            Party party = ChannelServer.getInstance().getWorldInterface().getParty(inviterMember.getPartyId());
                            if (party.getMembers().size() < 6) {
                                c.write(ChannelPackets.partyStatusMessage(22, inviter.getName()));
                                inviter.getClient().write(ChannelPackets.partyInvite(player));
                            } else {
                                c.write(ChannelPackets.partyStatusMessage(17));
                            }
                        } catch (RemoteException ex) {
                            player.sendNotice(5, "There was a problem connecting to the world server.");
                        }
                    } else {
                        c.write(ChannelPackets.partyStatusMessage(16));
                    }
                } else {
                    c.write(ChannelPackets.partyStatusMessage(19));
                }
                break;
            case 5:
                // expel
                if (member.isLeader()) {
                    try {
                        Party party = ChannelServer.getInstance().getWorldInterface().getParty(member.getPartyId());
                        final PartyMember expelled = party.getMemberById(reader.readInt());
                        if (expelled != null) {
                            wci.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                            if (player.getEventInstance() != null) {
                                /*if leader wants to boot someone, then the whole party gets expelled
                                TODO: Find an easier way to get the character behind a MaplePartyCharacter
                                possibly remove just the expellee.*/
                                if (expelled.isOnline()) {
                                    player.getEventInstance().disbandParty();
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        ChannelServer.getInstance().pingWorld();
                    }
                }
                break;
            case 6:
                // change leader
                try {
                    Party party = ChannelServer.getInstance().getWorldInterface().getParty(member.getPartyId());
                    final PartyMember newleader = party.getMemberById(reader.readInt());
                    wci.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                } catch (RemoteException e) {
                    ChannelServer.getInstance().pingWorld();
                }
                break;
            default:
                System.out.println("Unhandled Party function." + operation + "");
                break;
        }
    }
}