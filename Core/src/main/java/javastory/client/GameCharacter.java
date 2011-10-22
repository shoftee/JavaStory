/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.client;

import javastory.game.Gender;
import javastory.game.Inventory;
import javastory.game.PlayerStats;

/**
 * 
 * @author shoftee
 */
public interface GameCharacter {

	Inventory getEquippedItemsInventory();

	int getExp();

	int getFaceId();

	int getFame();

	int getGmLevel();

	Gender getGender();

	int getSkinColorId();

	int getHairId();

	int getId();

	int getInitialSpawnPoint();

	int getJobId();

	int getLevel();

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
