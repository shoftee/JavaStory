/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import client.GameCharacter;

/**
 *
 * @author Tosho
 */
public class PartyPlayer {
    
    private int id;
    
    public PartyPlayer(GameCharacter character) {
        // TODO: Observer pattern
        this.id = character.getId();
    }
    
    public int getId() {
        return this.id;
    }
}
