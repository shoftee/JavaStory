package client;

import java.util.List;
import java.util.Arrays;
import server.maps.GameMapObjectType;

public class GameConstants {

	public static final List<GameMapObjectType> rangedMapobjectTypes = Arrays.asList(
		GameMapObjectType.ITEM,
		GameMapObjectType.MONSTER,
		GameMapObjectType.DOOR,
		GameMapObjectType.REACTOR,
		GameMapObjectType.SUMMON,
		GameMapObjectType.NPC,
		GameMapObjectType.MIST
	);

	private static final int[] exp = {0, 15, 34, 57, 92, 135, 372, 560, 840, 1242, 1144,
	1573, 2144, 2800, 3640, 4700, 5893, 7360, 9144, 11120, 13478,
	16268, 19320, 22881, 27009, 31478, 36601, 42446, 48722, 55816, 76560,
	86784, 98208, 110932, 124432, 139372, 155865, 173280, 192400, 213345, 235372,
	259392, 285532, 312928, 342624, 374760, 408336, 444544, 483532, 524160, 567772,
	598886, 631704, 666321, 702836, 741351, 781976, 824828, 870028, 917705, 967995,
	1021040, 1076993, 1136012, 1198265, 1263930, 1333193, 1406252, 1483314, 1564600, 1650340,
	1740778, 1836172, 1936794, 2042930, 2154882, 2272969, 2397528, 2528912, 2667496, 2813674,
	2967863, 3130501, 3302052, 3483004, 3673872, 3875200, 4087561, 4311559, 4547832, 4797052,
	5059931, 5337215, 5629694, 5938201, 6263614, 6606860, 6968915, 7350811, 7753635, 8178534,
	8626717, 9099461, 9598112, 10124088, 10678888, 11264090, 11881362, 12532460, 13219239, 13943652,
	14707764, 15513749, 16363902, 17260644, 18206527, 19204244, 20256636, 21366700, 22537594, 23772654,
	25075395, 26449526, 27898960, 29427822, 31040466, 32741483, 34535716, 36428272, 38424541, 40530206,
	42751261, 45094030, 47565183, 50171755, 52921167, 55821246, 58880250, 62106888, 65510344, 69100311,
	72887008, 76881216, 81094306, 85538273, 90225770, 95170142, 100385465, 105886588, 111689173, 117809740,
	124265713, 131075474, 138258409, 145834970, 153826726, 162256430, 171148082, 180526996, 190419876, 200854884,
	211861732, 223471754, 235718006, 248635352, 262260569, 276632448, 291791906, 307782102, 324648561, 342439302,
	361204976, 380999008, 401877753, 423900654, 447130409, 471633156, 497478652, 524740482, 553496260, 583827855,
	615821621, 649568646, 685165008, 722712050, 762316670, 804091623, 848155844, 894634784, 943660769, 995373379,
	1049919840, 1107455447, 1168144005, 1232158296, 1299680571, 1370903066, 1446028554, 1525270918, 1608855764, 1767659560
	};

	private static final int[] closeness = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161, 2793,
	3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074,
	30000
	};

	private static final int[] mountexp = {0, 6, 25, 50, 105, 134, 196, 254, 263, 315, 367, 430, 543, 587, 679, 725, 897, 1146, 1394, 1701, 2247,
	2543, 2898, 3156, 3313, 3584, 3923, 4150, 4305, 4550
	};

	public static final int getExpNeededForLevel(final int level) {
		return exp[level];
	}

	public static final int getClosenessNeededForLevel(final int level) {
		return closeness[level - 1];
	}

	public static final int getMountExpNeededForLevel(final int level) {
		return mountexp[level - 1];
	}

	public static final int getBookLevel(final int level) {
		return (int) ((5 * level) * (level + 1));
	}

	public static final int getTimelessRequiredEXP(final int level) {
		return 70 + (level * 10);
	}

	public static final int getReverseRequiredEXP(final int level) {
		return 60 + (level * 5);
	}

	public static final int maxViewRangeSq() {
		return 800000; // 800 * 800
	}

	public static final boolean isJobFamily(final int baseJob, final int currentJob) {
		return currentJob >= baseJob
		&& currentJob / 100 == baseJob / 100;
	}

	public static final boolean isAdventurer(final int job) {
		return job >= 0 && job < 1000;
	}

	public static final boolean isKOC(final int job) {
		return job >= 1000 && job < 2000;
	}

	public static final boolean isAran(final int job) {
		return job >= 2000 && job <= 2112 && job != 2001;
	}

	public static final boolean isEvan(final int job) {
		return job == 2001 || (job >= 2200 && job <= 2218);
	}

	public static final boolean isRecoveryIncSkill(final int id) {
		switch (id) {
			case 1110000:
			case 2000000:
			case 1210000:
			case 11110000:
			case 4100002:
			case 4200001:
			return true;
		}
		return false;
	}

	public static final boolean isLinkedAranSkill(final int id) {
		switch (id) {
			case 21110007:
			case 21110008:
			case 21120009:
			case 21120010:
			case 4321001:
			return true;
		}
		return false;
	}

	public static final int getLinkedAranSkill(final int id) {
		switch (id) {
			case 21110007:
			case 21110008:
			return 21110002;
			case 21120009:
			case 21120010:
			return 21120002;
			case 4321001:
			return 4321000;
		}
		return id;
	}

	public static final int getBOF_ForJob(final int job) {
		if (isAdventurer(job)) {
			return 12;
		} else if (isKOC(job)) {
			return 10000012;
		} else if (isEvan(job)) {
			return 20010012;
		}
		return 20000012;
	}

	public static final boolean isElementAmp_Skill(final int skill) {
		switch (skill) {
			case 2110001:
			case 2210001:
			case 12110001:
			case 22150000:
			return true;
		}
		return false;
	}

	public static final int getMPEaterForJob(final int job) {
		switch (job) {
			case 210:
			case 211:
			case 212:
			return 2100000;
			case 220:
			case 221:
			case 222:
			return 2200000;
			case 230:
			case 231:
			case 232:
			return 2300000;
		}
		return 2100000; // Default, in case GM
	}

	public static final int getJobShortValue(int job) {
		if (job >= 1000) {
			job -= (job / 1000) * 1000;
		}
		job /= 100;
		if (job == 4) { // For some reason dagger/ claw is 8.. IDK
			job *= 2;
		} else if (job == 3) {
			job += 1;
		} else if (job == 5) {
			job += 11; // 16
		}
		return job;
	}

	public static final boolean isMulungSkill(final int skill) {
		switch (skill) {
			case 1009:
			case 1010:
			case 1011:
			case 10001009:
			case 10001010:
			case 10001011:
			case 20001009:
			case 20001010:
			case 20001011:
			case 20011009:
			case 20011010:
			case 20011011:
			return true;
		}
		return false;
	}

	public static final boolean isThrowingStar(final int itemId) {
		return itemId >= 2070000 && itemId < 2080000;
	}

	public static final boolean isBullet(final int itemId) {
		final int id = itemId / 10000;
		if (id == 233) {
			return true;
		} else {
			return false;
		}
	}

	public static final boolean isRechargable(final int itemId) {
		final int id = itemId / 10000;
		switch (id) {
			case 233:
			case 207:
			return true;
		}
		return false;
	}

	public static final int getMasterySkill(final int job) {
		if (job >= 1410 && job <= 1412) {
			return 14100000;
		} else if (job >= 410 && job <= 412) {
			return 4100000;
		} else if (job >= 520 && job <= 522) {
			return 5200000;
		}
		return 0;
	}

	public static final boolean isOverall(final int itemId) {
		return itemId >= 1050000 && itemId < 1060000;
	}

	public static final boolean isPet(final int itemId) {
		return itemId >= 5000000 && itemId <= 5000100;
	}

	public static final boolean isArrowForCrossBow(final int itemId) {
		return itemId >= 2061000 && itemId < 2062000;
	}

	public static final boolean isArrowForBow(final int itemId) {
		return itemId >= 2060000 && itemId < 2061000;
	}

	public static final boolean isMagicWeapon(final int itemId) {
		final int s = itemId / 10000;
		return s == 137 || s == 138;
	}

	public static final boolean isWeapon(final int itemId) {
		return itemId >= 1302000 && itemId < 1500000;
	}

	public static final InventoryType getInventoryType(final int itemId) {
		final byte type = (byte) (itemId / 1000000);
		if (type < 1 || type > 5) {
			return InventoryType.UNDEFINED;
		}
		return InventoryType.getByType(type);
	}

	public static final WeaponType getWeaponType(final int itemId) {
	int cat = itemId / 10000;
	cat = cat % 100;
	switch (cat) {
		case 30:
		return WeaponType.SWORD1H;
		case 31:
		return WeaponType.AXE1H;
		case 32:
		return WeaponType.BLUNT1H;
		case 33:
		return WeaponType.DAGGER;
		case 34:
		return WeaponType.KATARA;
		case 37:
		return WeaponType.WAND;
		case 38:
		return WeaponType.STAFF;
		case 40:
		return WeaponType.SWORD2H;
		case 41:
		return WeaponType.AXE2H;
		case 42:
		return WeaponType.BLUNT2H;
		case 43:
		return WeaponType.SPEAR;
		case 44:
		return WeaponType.POLE_ARM;
		case 45:
		return WeaponType.BOW;
		case 46:
		return WeaponType.CROSSBOW;
		case 47:
		return WeaponType.CLAW;
		case 48:
		return WeaponType.KNUCKLE;
		case 49:
		return WeaponType.GUN;
	}
	return WeaponType.NOT_A_WEAPON;
	}

	public static final boolean isShield(final int itemId) {
	int cat = itemId / 10000;
	cat = cat % 100;
	return cat == 9;
	}

	public static final boolean isEquip(final int itemId) {
	return itemId / 1000000 == 1;
	}

	public static final boolean isCleanSlate(int itemId) {

		return itemId / 100 == 20490;
	}

	public static final boolean isChaosScroll(int itemId) {

		return itemId / 100 == 20491;
	}

	public static final boolean isSpecialScroll(final int scrollId) {
	switch (scrollId) {
		case 2040727: // Spikes on show
		case 2041058: // Cape for Cold protection
		return true;
	}
	return false;
	}

	public static final boolean isTwoHanded(final int itemId) {
	switch (getWeaponType(itemId)) {
		case AXE2H:
		case GUN:
		case KNUCKLE:
		case BLUNT2H:
		case BOW:
		case CLAW:
		case CROSSBOW:
		case POLE_ARM:
		case SPEAR:
		case SWORD2H:
		return true;
		default:
		return false;
	}
	}

	public static final boolean isTownScroll(final int id) {
	return id >= 2030000 && id < 2030020;
	}

	public static final boolean isGun(final int id) {
	return id >= 1492000 && id <= 1500000;
	}

	public static final boolean isUse(final int id) {
	return id >= 2000000 && id <= 2490000;
	}

	public static final boolean isSummonSack(final int id) {
	return id / 10000 == 210;
	}

	public static final boolean isMonsterCard(final int id) {
	return id / 10000 == 238;
	}

	public static final boolean isSpecialCard(final int id) {
	return id / 100 >= 2388;
	}

	public static final int getCardShortId(final int id) {
	return id % 10000;
	}

	public static final boolean isGem(final int id) {
	return id >= 4250000 && id <= 4251402;
	}

	public static final boolean isCustomQuest(final int id) {
	if (id > 99999) {
		return true;
	}
	switch (id) {
		case 7200: // Papulatus record and count
		case 20022: // Cygnus tutor quest
		case 20021: // Cygnus tutor quest
		case 20020: // Cygnus tutor quest
		return true;
	}
	return false;
	}

	public static final int getTaxAmount(final int meso) {
	if (meso >= 100000000) {
		return (int) Math.round(0.06 * meso);
	} else if (meso >= 25000000) {
		return (int) Math.round(0.05 * meso);
	} else if (meso >= 10000000) {
		return (int) Math.round(0.04 * meso);
	} else if (meso >= 5000000) {
		return (int) Math.round(0.03 * meso);
	} else if (meso >= 1000000) {
		return (int) Math.round(0.018 * meso);
	} else if (meso >= 100000) {
		return (int) Math.round(0.008 * meso);
	}
	return 0;
	}

	public static final int EntrustedStoreTax(final int meso) {
	if (meso >= 100000000) {
		return (int) Math.round(0.03 * meso);
	} else if (meso >= 25000000) {
		return (int) Math.round(0.025 * meso);
	} else if (meso >= 10000000) {
		return (int) Math.round(0.02 * meso);
	} else if (meso >= 5000000) {
		return (int) Math.round(0.015 * meso);
	} else if (meso >= 1000000) {
		return (int) Math.round(0.009 * meso);
	} else if (meso >= 100000) {
		return (int) Math.round(0.004 * meso);
	}
	return 0;
	}

	public static final short getSummonAttackDelay(final int id) {
	switch (id) {
		case 15001004: // Lightning
		case 14001005: // Darkness
		case 13001004: // Storm
		case 12001004: // Flame
		case 11001004: // Soul
		case 3221005: // Freezer
		case 3211005: // Golden Eagle
		case 3121006: // Phoenix
		case 3111005: // Silver Hawk
		case 2321003: // Bahamut
		case 2311006: // Summon Dragon
		case 2221005: // Infrit
		case 2121005: // Elquines
		return 3030;
		case 5211001: // Octopus
		case 5211002: // Gaviota
		case 5220002: // Support Octopus
		return 1530;
		case 3211002: // Puppet
		case 3111002: // Puppet
		case 1321007: // Beholder
	   // case 4341006:
		return 0;
	}
	return 3030;
	}


	public static final short getAttackDelay(final int id) {
	switch (id) { // Assume it's faster(2)
		case 3121004: // Storm of Arrow
		case 13111002: // Storm of Arrow
		case 5221004: // Rapidfire
		case 4221001: //Assassinate?
		case 5201006: // Recoil shot/ Back stab shot
		return 120;
		case 13101005: // Storm Break
		return 360;
		case 5001003: // Double Fire
		return 390;
		case 5001001: // Straight/ Flash Fist
		case 15001001: // Straight/ Flash Fist
		case 1321003: // Rush
		case 1221007: // Rush
		case 1121006: // Rush
		return 450;
		case 5211004: // Flamethrower
		case 5211005: // Ice Splitter
		return 480;
		case 0: // Normal Attack, TODO delay for each weapon type
		case 5111002: // Energy Blast
		case 15101005: // Energy Blast
		case 1001004: // Power Strike
		case 11001002: // Power Strike
		case 1001005: // Slash Blast
		case 11001003: // Slash Blast
		case 1311005: // Sacrifice
		return 570;
		case 3111006: // Strafe
		case 311004: // Arrow Rain
		case 13111000: // Arrow Rain
		case 3111003: // Inferno
		case 3101005: // Arrow Bomb
		case 4001344: // Lucky Seven
		case 14001004: // Lucky seven
		case 4121007: // Triple Throw
		case 14111005: // Triple Throw
		case 4111004: // Shadow Meso
		case 4101005: // Drain
		case 4211004: // Band of Thieves
		case 4201004: // Steal
		case 4001334: // Double Stab
		case 5221007: // Battleship Cannon
		case 1211002: // Charged blow
		case 2301002: // Heal
		case 1311003: // Dragon Fury : Spear
		case 1311004: // Dragon Fury : Pole Arm
		case 3211006: // Strafe
		case 3211004: // Arrow Eruption
		case 3211003: // Blizzard Arrow
		case 3201005: // Iron Arrow
		case 3221001: // Piercing
		case 4111005: // Avenger
		case 14111002: // Avenger
		case 5201001: // Invisible shot
		case 5101004: // Corkscrew Blow
		case 15101003: // Corkscrew Blow
		case 1121008: // Brandish
		case 11111004: // Brandish
		case 1221009: // Blast
		return 600;
		case 5201004: // Blank Shot/ Fake shot
		case 5211000: // Burst Fire/ Triple Fire
		case 5001002: // Sommersault Kick
		case 15001002: // Sommersault Kick
		case 4221007: // Boomerang Stab
		case 1311001: // Spear Crusher, 16~30 pts = 810
		case 1311002: // PA Crusher, 16~30 pts = 810
		case 2221006: // Chain Lightning
		return 660;
		case 4121008: // Ninja Storm
		case 4201005: // Savage blow
		case 5211006: // Homing Beacon
		case 5221008: // Battleship Torpedo
		case 5101002: // Backspin Blow
		case 2001005: // Magic Claw
		case 12001003: // Magic Claw
		case 2001004: // Energy Bolt
		case 2301005: // Holy Arrow
		case 2121001: // Big Bang
		case 2221001: // Big Bang
		case 2321001: // Big Bang
		case 2321007: // Angel's Ray
		case 2101004: // Fire Arrow
		case 12101002: // Fire Arrow
		case 2101005: // Poison Breath
		case 2121003: // Fire Demon
		case 2221003: // Ice Demon
		case 2121006: // Paralyze
		case 2201005: // Thunderbolt
		case 2201004: // Cold Beam
		case 4211006: // Meso Explosion
		case 5121005: // Snatch
		case 12111006: // Fire Strike
		case 11101004: // Soul Blade
		return 750;
		case 15111007: // Shark Wave
		case 2111006: // Elemental Composition
		case 2211006: // Elemental Composition
		return 810;
		case 13111006: // Wind Piercing
		case 4211002: // Assaulter
		case 5101003: // Double Uppercut
		return 900;
		case 5121003: // Energy Orb
		case 2311004: // Shining Ray
		case 2211002: // Ice Strike
		return 930;
		case 13111007: // Wind Shot
		return 960;
		case 14101006: // Vampire
		case 4121003: // Showdown
		case 4221003: // Showdown
		return 1020;
		case 12101006: // Fire Pillar
		return 1050;
		case 5121001: // Dragon Strike
		return 1060;
		case 2211003: // Thunder Spear
		case 1311006: // Dragon Roar
		return 1140;
		case 11111006: // Soul Driver
		return 1230;
		case 12111005: // Flame Gear
		return 1260;
		case 2111003: // Poison Mist
		return 1320;
		case 5111006: // Shockwave
		case 15111003: // Shockwave
		case 2111002: // Explosion
		return 1500;
		case 5121007: // Barrage
		case 15111004: // Barrage
		return 1830;
		case 5221003: // Ariel Strike
		case 5121004: // Demolition
		return 2160;
		case 2321008: // Genesis
		return 2700;
		case 2121007: // Meteor Shower
		case 10001011: // Meteo Shower
		case 2221007: // Blizzard
		return 3060;
	}
	// TODO delay for final attack, weapon type, swing,stab etc
	return 330; // Default usually
	}
	public static final byte gachaponRareItem(final int id) {
		switch (id) {
			case 2000005: // Power Elixir
			case 2040105: // Scroll for Face Eqp. for Eva
			case 2040605: // Cursed Scroll for Bottomwear for DEF
			case 2040609: // Cursed Scroll for Bottomwear for HP
			case 2040607: // Cursed Scroll for Bottomwear for Jump
			case 2044505: // Cursed Scroll for Bow for ATT
			case 2041031: // Cursed Scroll for Cape for HP
			case 2041037: // Cursed Scroll for Cape for INT
			case 2041041: // Cursed Scroll for Cape for LUK
			case 2044705: // Cursed Scroll for Claw for ATT
			case 2043305: // Cursed Scroll for Dagger for ATT
			case 2040309: // Cursed scroll for Earring for DEF
			case 2040103: // Cursed Scroll for Face Eqp. for HP
			case 2040811: // Cursed Scroll for Gloves for ATT
			case 2040815: // Cursed scroll for Glove for INT
			case 2040015: // Cursed scroll for Helmet for ACC
			case 2040011: // Cursed Scroll for Helmet for HP
			case  2040511: // Cursed Scroll for Overall Armor for DEF
			case 2040509: // Cursed Scroll for Overall Armor for DEX
			case 2040521: // Cursed Scroll for Overall Armor for LUK
			case 2044405: // Cursed Scroll for Pole Arm for ATT
			case 2040713: // Cursed Scroll for Shoes for DEX
			case 2040717: // Cursed Scroll for Shoes for Speed
			case 2043805: // Cursed Scroll for Staff for Magic Att
			case 2040407: // Cursed Scroll for Topwear for STR
			case 2040206: // Scroll for Eye Eqp. for INT
			case 2040106: // Scroll for Face Eqp. for Eva
			case 2040101: // Scroll for Face Eqp. for HP
			case 2040606: // Cursed Scroll for Bottomwear for Jump
			case 2041036: // Cursed Scroll for Cape for INT
			case 2041034: // Cursed Scroll for Cape for STR
			case 2044604: // Cursed Scroll for Crossbow for ATT
			case 2043304: // Cursed Scroll for Dagger for ATT
			case 2040306: // Cursed Scroll for Earring for DEX
			case 2040814: // Cursed scroll for Glove for INT
			case 2040008: // Cursed Scroll for Helmet for DEF
			case 2043006: // Cursed scroll for One handed sword for Magic ATT
			case 2040508: // Cursed Scroll for Overall Armor for DEX
			case 2040520: // Cursed Scroll for Overall Armor for LUK
			case 2044404: // Cursed Scroll for Pole Arm for ATT
			case 2040904: // Cursed Scroll for Shield for DEF
			case 2040908: // Cursed Scroll for Shield for HP
			case 2040921: // Cursed Scroll for Shield for Magic Att
			case 2040916: // Cursed Scroll for Shield for Weapon Att
			case 2040410: // Cursed scroll for Top wear for LUK
			case 2040404: // Scroll for Topwear for DEF
			case 1012070: // Strawberry Icecream Bar
			case 1012071: // Chocolate Icecream Bar
			case 1012072: // Melon Icecream Bar
			case 1012073: // Watermelon Icecream Bar
			case 2022179: // Onyx Apple
			case 2049100: // Chaos Scroll 60$
			case 2340000: // White Scroll
			case 1132004: // Black Belt
			case 2049000: // Reverse Scroll
			case 2049001: // Reverse Scroll
			case 2049002: // Reverse Scroll
			case 2040006: // Miracle
			case 2040007: // Miracle
			case 2040303: // Miracle
			case 2040403: // Miracle
			case 2040506: // Miracle
			case 2040507: // Miracle
			case 2040603: // Miracle
			case 2040709: // Miracle
			case 2040710: // Miracle
			case 2040711: // Miracle
			case 2040806: // Miracle
			case 2040903: // Miracle
			case 2041024: // Miracle
			case 2041025: // Miracle
			case 2043003: // Miracle
			case 2043103: // Miracle
			case 2043203: // Miracle
			case 2043303: // Miracle
			case 2043703: // Miracle
			case 2043803: // Miracle
			case 2044003: // Miracle
			case 2044103: // Miracle
			case 2044203: // Miracle
			case 2044303: // Miracle
			case 2044403: // Miracle
			case 2044503: // Miracle
			case 2044603: // Miracle
			case 2044908: // Miracle
			case 2044815: // Miracle
			case 2044019: // Miracle
			case 2044703: // Miracle
			case 1372039: // Elemental wand lvl 130
			case 1372040: // Elemental wand lvl 130
			case 1372041: // Elemental wand lvl 130
			case 1372042: // Elemental wand lvl 130
			case 1092049: // Dragon Khanjar
			return 2;
			case 1382037: // Blade Staff
			case 1102084: // Pink Gaia Cape
			case 1102041: // Pink Adventurer Cape
			case 1402044: // Pumpkin Lantern
			case 1082149: // Brown Work glove
			case 1102086: // Purple Gaia Cape
			case 1102042: // Purple Adventurer Cape
			case 3010065: // Pink Parasol
			case 3010064: // Brown Sand Bunny Cushion
			case 3010063: // Starry Moon Cushion
			case 3010068: // Teru Teru Chair
			case 3010054: // Baby Bear's Dream
			case 3012001: // Round the Campfire
			case 3012002: // Rubber Ducky Bath
			case 3010020: // Portable Meal Table
			case 3010041: // Skull Throne
			return 2;
		}
		return 0;
	}
	public final static int[] goldrewards = {
	1402037, 1, // Rigbol Sword
	2290096, 1, // Maple Warrior 20
	2290049, 1, // Genesis 30
	2290041, 1, // Meteo 30
	2290047, 1, // Blizzard 30
	2290095, 1, // Smoke 30
	2290017, 1, // Enrage 30
	2290075, 1, // Snipe 30
	2290085, 1, // Triple Throw 30
	2290116, 1, // Areal Strike
	1302059, 3, // Dragon Carabella
	2049100, 1, // Chaos Scroll
	2340000, 1, // White Scroll
	1092049, 1, // Dragon Kanjar
	1102041, 1, // Pink Cape
	1432018, 3, // Sky Ski
	1022047, 3, // Owl Mask
	3010051, 1, // Chair
	3010020, 1, // Portable meal table
	2040914, 1, // Shield for Weapon Atk

	1432011, 3, // Fair Frozen
	1442020, 3, // HellSlayer
	1382035, 3, // Blue Marine
	1372010, 3, // Dimon Wand
	1332027, 3, // Varkit
	1302056, 3, // Sparta
	1402005, 3, // Bezerker
	1472053, 3, // Red Craven
	1462018, 3, // Casa Crow
	1452017, 3, // Metus
	1422013, 3, // Lemonite
	1322029, 3, // Ruin Hammer
	1412010, 3, // Colonian Axe

	1472051, 1, // Green Dragon Sleeve
	1482013, 1, // Emperor's Claw
	1492013, 1, // Dragon fire Revlover

	1382050, 1, // Blue Dragon Staff
	1382045, 1, // Fire Staff, Level 105
	1382047, 1, // Ice Staff, Level 105
	1382048, 1, // Thunder Staff
	1382046, 1, // Poison Staff

	1332032, 4, // Christmas Tree
	1482025, 3, // Flowery Tube

	4001011, 4, // Lupin Eraser
	4001010, 4, // Mushmom Eraser
	4001009, 4, // Stump Eraser

	2030008, 5, // Bottle, return scroll
	1442012, 4, // Sky Snowboard
	1442018, 3, // Frozen Tuna
	2040900, 4, // Shield for DEF
	2000005, 10, // Power Elixir
	2000004, 10, // Elixir
	4280000, 4}; // Gold Box
	public final static int[] silverrewards = {
	1002452, 3, // Starry Bandana
	1002455, 3, // Starry Bandana
	2290084, 1, // Triple Throw 20
	2290048, 1, // Genesis 20
	2290040, 1, // Meteo 20
	2290046, 1, // Blizzard 20
	2290074, 1, // Sniping 20
	2290064, 1, // Concentration 20
	2290094, 1, // Smoke 20
	2290022, 1, // Berserk 20
	2290056, 1, // Bow Expert 30
	2290066, 1, // xBow Expert 30
	2290020, 1, // Sanc 20
	1102082, 1, // Black Raggdey Cape
	1302049, 1, // Glowing Whip
	2340000, 1, // White Scroll
	1102041, 1, // Pink Cape
	1452019, 2, // White Nisrock
	4001116, 3, // Hexagon Pend
	4001012, 3, // Wraith Eraser
	1022060, 2, // Foxy Racoon Eye

	1432011, 3, // Fair Frozen
	1442020, 3, // HellSlayer
	1382035, 3, // Blue Marine
	1372010, 3, // Dimon Wand
	1332027, 3, // Varkit
	1302056, 3, // Sparta
	1402005, 3, // Bezerker
	1472053, 3, // Red Craven
	1462018, 3, // Casa Crow
	1452017, 3, // Metus
	1422013, 3, // Lemonite
	1322029, 3, // Ruin Hammer
	1412010, 3, // Colonian Axe

	1002587, 3, // Black Wisconsin
	1402044, 1, // Pumpkin lantern
	2101013, 4, // Summoning Showa boss
	1442046, 1, // Super Snowboard
	1422031, 1, // Blue Seal Cushion
	1332054, 3, // Lonzege Dagger
	1012056, 3, // Dog Nose
	1022047, 3, // Owl Mask
	3012002, 1, // Bathtub
	1442012, 3, // Sky snowboard
	1442018, 3, // Frozen Tuna
	1432010, 3, // Omega Spear
	1432036, 1, // Fishing Pole
	2000005, 10, // Power Elixir
	2000004, 10, // Elixir
	4280001, 4}; // Silver Box
	public static final int[] fishingReward = {
	0, 80, // Meso
	1, 60, // EXP
	2022179, 1, // Onyx Apple
	1302021, 5, // Pico Pico Hammer
	1072238, 1, // Voilet Snowshoe
	1072239, 1, // Yellow Snowshoe
	2049100, 1, // Chaos Scroll
	1302000, 3, // Sword
	1442011, 1, // Surfboard
	4000517, 8, // Golden Fish
	4000518, 25, // Golden Fish Egg
	4031627, 2, // White Bait (3cm)
	4031628, 1, // Sailfish (120cm)
	4031630, 1, // Carp (30cm)
	4031631, 1, // Salmon(150cm)
	4031632, 1, // Shovel
	4031633, 2, // Whitebait (3.6cm)
	4031634, 1, // Whitebait (5cm)
	4031635, 1, // Whitebait (6.5cm)
	4031636, 1, // Whitebait (10cm)
	4031637, 2, // Carp (53cm)
	4031638, 2, // Carp (60cm)
	4031639, 1, // Carp (100cm)
	4031640, 1, // Carp (113cm)
	4031641, 2, // Sailfish (128cm)
	4031642, 2, // Sailfish (131cm)
	4031643, 1, // Sailfish (140cm)
	4031644, 1, // Sailfish (148cm)
	4031645, 2, // Salmon (166cm)
	4031646, 2, // Salmon (183cm)
	4031647, 1, // Salmon (227cm)
	4031648, 1, // Salmon (288cm)
	4031629, 1 // Pot
	};

	public final static int getSkillBook(final int job) {
		if (job >= 2210 && job <= 2218) {
			return job - 2209;
		}
		return 0;
	}

	public final static int getSkillBookForSkill(final int skillid) {
		return getSkillBook(skillid / 10000);
	}

	public static final boolean isKatara(int itemId) {
		return itemId / 10000 == 134;
	}

	public static final boolean isDagger(int itemId) {
		return itemId / 10000 == 133;
	}

	public static final boolean isApplicableSkill(int skil) {
		return skil < 22190000 && (skil % 10000 < 8000 || skil % 10000 > 8003); //no additional/resistance/db/decent skills
	}

	public static final boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
		return skil >= 90000000 || (skil % 10000 >= 8000 && skil % 10000 <= 8003);
	}

	public static final boolean isEvanDragonItem(final int itemId) {
		return itemId >= 1940000 && itemId < 1980000; //194 = mask, 195 = pendant, 196 = wings, 197 = tail
	}
}