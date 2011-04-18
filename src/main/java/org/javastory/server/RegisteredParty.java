/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Set;

/**
 *
 * @author Tosho
 */
public class RegisteredParty {

    private PartyPlayer leader;
    private Set<PartyPlayer> members;
    private int partyId;

    public RegisteredParty(int id, PartyPlayer leader) {
        this.leader = leader;
        this.partyId = id;
        this.members = Sets.newHashSet(leader);
    }

    public void addMember(PartyPlayer player) {
        checkArgument(!this.members.contains(player));
        members.add(player);
    }
    
    public void removeMember(PartyPlayer player) {
        checkArgument(this.members.contains(player));
        members.remove(player);
    }
    
    public PartyPlayer getMemberById(int characterId) {
        for (PartyPlayer player : members) {
            if (player.getId() == characterId) {
                return player;
            }
        }
        return null;
    }
    
    public int getId() {
        return this.partyId;
    }
    
    public PartyPlayer getLeader() {
        return this.leader;
    }
    
    public void setLeader(PartyPlayer player) {
        checkArgument(this.members.contains(player));
        this.leader = player;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final RegisteredParty other = (RegisteredParty) obj;
        if (this.partyId != other.partyId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.partyId;
        return hash;
    }
}
