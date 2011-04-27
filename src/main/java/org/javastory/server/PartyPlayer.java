/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import org.javastory.client.ChannelCharacter;

/**
 *
 * @author Tosho
 */
public class PartyPlayer {
    
    private int id;
    
    public PartyPlayer(ChannelCharacter character) {
        // TODO: Observer pattern
        this.id = character.getId();
    }
    
    public int getId() {
        return this.id;
    }
}
