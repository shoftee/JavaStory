/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.channel.server;

import java.rmi.RemoteException;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.game.Jobs;

/**
 * TODO : Make this a function for NPC instead.. cleaner
 * 
 * @author Rob
 */
public final class CarnivalChallenge {

	ChannelCharacter challenger;
	String challengeinfo = "";

	public CarnivalChallenge(final ChannelCharacter challenger) {
		this.challenger = challenger;
		try {
			final int partyId = challenger.getPartyMembership().getPartyId();
			final Party party = ChannelServer.getWorldInterface().getParty(partyId);
			this.challengeinfo += "#b";
			for (final PartyMember pc : party.getMembers()) {
				final ChannelCharacter c = challenger.getMap().getCharacterById_InMap(pc.getCharacterId());
				this.challengeinfo += c.getName() + " / Level" + c.getLevel() + " / " + Jobs.getJobNameById(c.getJobId());
			}
			this.challengeinfo += "#k";
		} catch (final RemoteException ex) {
		}
	}

	public ChannelCharacter getChallenger() {
		return this.challenger;
	}

	public String getChallengeInfo() {
		return this.challengeinfo;
	}

}