/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.client;

import client.Inventory;

/**
 *
 * @author Tosho
 */
public interface GameCharacter {

    Inventory getEquippedItemsInventory();

    int getExp();

    int getFaceId();

    int getFame();

    int getGmLevel();

    int getGender();

    int getSkinColorId();

    int getHairId();

    int getId();

    int getInitialSpawnPoint();

    int getJobId();

    short getLevel();

    int getMapId();

    int getMeso();

    String getName();

    int getRemainingAp();

    int getRemainingSp();

    int getRemainingSp(final int skillbook);

    int getRemainingSpSize();

    int[] getRemainingSps();

    int getSubcategory();

    int getWorldId();

    PlayerStats getStats();
}
