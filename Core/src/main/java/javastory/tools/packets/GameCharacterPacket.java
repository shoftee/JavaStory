/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.tools.packets;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javastory.client.GameCharacter;
import javastory.game.IItem;
import javastory.game.Inventory;
import javastory.game.Jobs;
import javastory.io.PacketBuilder;

/**
 * 
 * @author shoftee
 */
public final class GameCharacterPacket {

	private GameCharacterPacket() {
	}

	public static void addCharStats(final PacketBuilder builder, final GameCharacter chr) {
		builder.writeInt(chr.getId()); // character id
		builder.writePaddedString(chr.getName(), 13);
		builder.writeAsByte(chr.getGender().asNumber()); // gender (0 = male, 1 = female)
		builder.writeAsByte(chr.getSkinColorId()); // skin color
		builder.writeInt(chr.getFaceId()); // face
		builder.writeInt(chr.getHairId()); // hair
		builder.writeZeroBytes(24);
		builder.writeAsByte(chr.getLevel()); // level
		builder.writeAsShort(chr.getJobId()); // job
		chr.getStats().connectData(builder);
		builder.writeAsShort(chr.getRemainingAp()); // remaining ap
		if (Jobs.isEvan(chr.getJobId())) {
			final int size = chr.getRemainingSpSize();
			builder.writeAsByte(size);
			for (int i = 0; i < chr.getRemainingSps().length; i++) {
				if (chr.getRemainingSp(i) > 0) {
					builder.writeAsByte(i + 1);
					builder.writeAsByte(chr.getRemainingSp(i));
				}
			}
		} else {
			builder.writeAsShort(chr.getRemainingSp()); // remaining sp
		}
		builder.writeInt(chr.getExp()); // exp
		builder.writeAsShort(chr.getFame()); // fame
		builder.writeInt(0); // Gachapon exp
		builder.writeLong(0); // This must be something, just leave it lol
		builder.writeInt(chr.getMapId()); // current map id
		builder.writeAsByte(chr.getInitialSpawnPoint()); // spawnpoint
		builder.writeAsShort(chr.getSubcategory()); // 1 = Dual Blade
	}

	public static void addCharLook(final PacketBuilder builder, final GameCharacter chr, final boolean mega) {
		builder.writeAsByte(chr.getGender().asNumber());
		builder.writeAsByte(chr.getSkinColorId());
		builder.writeInt(chr.getFaceId());
		builder.writeAsByte(mega ? 0 : 1);
		builder.writeInt(chr.getHairId());

		final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
		final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
		Inventory equip = chr.getEquippedItemsInventory();

		// masking items
		for (final IItem item : equip) {
			byte pos = (byte) (item.getPosition() * -1);
			if (pos < 100 && myEquip.get(pos) == null) {
				myEquip.put(pos, item.getItemId());
			} else if (pos > 100 && pos != 111) {
				pos -= 100;
				if (myEquip.get(pos) != null) {
					maskedEquip.put(pos, myEquip.get(pos));
				}
				myEquip.put(pos, item.getItemId());
			} else if (myEquip.get(pos) != null) {
				maskedEquip.put(pos, item.getItemId());
			}
		}
		for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
			builder.writeByte(entry.getKey());
			builder.writeInt(entry.getValue());
		}
		// end of masking items
		builder.writeAsByte(0xFF);

		// regular items
		for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
			builder.writeByte(entry.getKey());
			builder.writeInt(entry.getValue());
		}
		// ending regular items
		builder.writeAsByte(0xFF);

		final IItem cWeapon = equip.getItem((byte) -111);
		builder.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
		builder.writeInt(0);
		builder.writeLong(0);
	}
}
