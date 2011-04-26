package handling.channel.handler;

import java.rmi.RemoteException;

import client.ChannelCharacter;
import client.ChannelClient;
import handling.world.Party;
import handling.world.PartyMember;
import handling.world.PartyOperation;
import handling.world.remote.WorldChannelInterface;
import org.javastory.io.PacketFormatException;
import org.javastory.server.channel.ChannelManager;
import tools.MaplePacketCreator;
import org.javastory.io.PacketReader;

public class PartyHandler {

    public static final void handleDenyPartyInvitation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
        final int action = reader.readByte();
        final int partyid = reader.readInt();
        if (c.getPlayer().getParty() == null) {
            try {
                WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                Party party = wci.getParty(partyid);
                if (party != null) {
                    if (action == 0x1B) { //accept
                        if (party.getMembers().size() < 6) {
                            wci.updateParty(partyid, PartyOperation.JOIN, new PartyMember(c.getPlayer()));
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.write(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else if (action != 0x16) {
                        final ChannelCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeader().getId());
                        if (cfrom != null) {
                            cfrom.getClient().write(MaplePacketCreator.partyStatusMessage(23, c.getPlayer().getName()));
                        }
                    }
                } else {
                    c.getPlayer().sendNotice(5, "The party you are trying to join does not exist");
                }
            } catch (RemoteException e) {
                c.getChannelServer().pingWorld();
            }
        } else {
            c.getPlayer().sendNotice(5, "You can't join the party as you are already in one");
        }
    }

    public static void handlePartyOperation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
        final int operation = reader.readByte();
        final WorldChannelInterface wci = ChannelManager.getInstance(c.getChannelId()).getWorldInterface();
        Party party = c.getPlayer().getParty();
        PartyMember partyplayer = new PartyMember(c.getPlayer());
        switch (operation) {
            case 1: // create
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.createParty(partyplayer);
                        c.getPlayer().setParty(party);
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                    c.write(MaplePacketCreator.partyCreated());
                } else {
                    c.getPlayer().sendNotice(5, "You can't create a party as you are already in one");
                }
                break;
            case 2: // leave
                if (party != null) { //are we in a party? o.O"
                    try {
                        if (partyplayer.equals(party.getLeader())) { // disband
                            wci.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        } else {
                            wci.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                            }
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                    c.getPlayer().setParty(null);
                }
                break;
            case 3: // accept invitation
                final int partyid = reader.readInt();
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.getParty(partyid);
                        if (party != null) {
                            if (party.getMembers().size() < 6) {
                                wci.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                                c.getPlayer().receivePartyMemberHP();
                                c.getPlayer().updatePartyMemberHP();
                            } else {
                                c.write(MaplePacketCreator.partyStatusMessage(17));
                            }
                        } else {
                            c.getPlayer().sendNotice(5, "The party you are trying to join does not exist");
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().pingWorld();
                    }
                } else {
                    c.getPlayer().sendNotice(5, "You can't join the party as you are already in one");
                }
                break;
            case 4: // invite
                // TODO store pending invitations and check against them
                final ChannelCharacter invited = c.getChannelServer().getPlayerStorage().getCharacterByName(reader.readLengthPrefixedString());
                if (invited != null && invited.getWorldId() == c.getWorldId()) {
                    if (invited.getParty() == null) {
                        if (party.getMembers().size() < 6) {
                            c.write(MaplePacketCreator.partyStatusMessage(22, invited.getName()));
                            invited.getClient().write(MaplePacketCreator.partyInvite(c.getPlayer()));
                        } else {
                            c.write(MaplePacketCreator.partyStatusMessage(16));
                        }
                    } else {
                        c.write(MaplePacketCreator.partyStatusMessage(17));
                    }
                } else {
                    c.write(MaplePacketCreator.partyStatusMessage(19));
                }
                break;
            case 5: // expel
                if (partyplayer.equals(party.getLeader())) {
                    final PartyMember expelled = party.getMemberById(reader.readInt());
                    if (expelled != null) {
                        try {
                            wci.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                            if (c.getPlayer().getEventInstance() != null) {
                                /*if leader wants to boot someone, then the whole party gets expelled
                                TODO: Find an easier way to get the character behind a MaplePartyCharacter
                                possibly remove just the expellee.*/
                                if (expelled.isOnline()) {
                                    c.getPlayer().getEventInstance().disbandParty();
                                }
                            }
                        } catch (RemoteException e) {
                            c.getChannelServer().pingWorld();
                        }
                    }
                }
                break;
            case 6: // change leader
                final PartyMember newleader = party.getMemberById(reader.readInt());
                try {
                    wci.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                } catch (RemoteException e) {
                    c.getChannelServer().pingWorld();
                }
                break;
            default:
                System.out.println("Unhandled Party function." + operation + "");
                break;
        }
    }
}