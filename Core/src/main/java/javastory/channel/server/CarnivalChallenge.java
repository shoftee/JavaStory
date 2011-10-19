/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.channel.server;

import java.rmi.RemoteException;

import javastory.channel.ChannelCharacter;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.game.Jobs;

/**
 * TODO : Make this a function for NPC instead.. cleaner
 * @author Rob
 */
public final class CarnivalChallenge {

    ChannelCharacter challenger;
    String challengeinfo = "";

    public CarnivalChallenge(ChannelCharacter challenger) {
        this.challenger = challenger;
        try {
            final int partyId = challenger.getPartyMembership().getPartyId();
            Party party = challenger.getClient().getChannelServer().getWorldInterface().getParty(partyId);
            challengeinfo += "#b";
            for (PartyMember pc : party.getMembers()) {
                ChannelCharacter c = challenger.getMap().getCharacterById_InMap(pc.getCharacterId());
                challengeinfo += (c.getName() + " / Level" + c.getLevel() + " / " + Jobs.getJobNameById(c.getJobId()));
            }
            challengeinfo += "#k";
        } catch (RemoteException ex) {
        }
    }

    public ChannelCharacter getChallenger() {
        return challenger;
    }

    public String getChallengeInfo() {
        return challengeinfo;
    }

}