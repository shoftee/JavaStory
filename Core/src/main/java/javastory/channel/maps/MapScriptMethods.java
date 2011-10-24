package javastory.channel.maps;

import java.awt.Point;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.client.SkillFactory;
import javastory.channel.life.LifeFactory;
import javastory.game.ItemInfoProvider;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;
import javastory.tools.packets.UIPacket;

public class MapScriptMethods {

	private static final Point witchTowerPos = new Point(-60, 184);
	private static final String[] mulungEffects = {
		"I have been waiting for you! If you have an ounce of courage in you, you'll be walking in that door right now!",
		"How brave of you to take on Mu Lung Training Tower!",
		"I will make sure you will regret taking on Mu Lung Training Tower!",
		"I do like your intestinal fortitude! But don't confuse your courage with recklessness!",
		"If you want to step on the path to failure, by all means to do so!"
	};

	private static enum onFirstUserEnter {

		dojang_Eff, PinkBeen_before, onRewordMap, StageMsg_together, astaroth_summon, boss_Ravana, killing_BonusSetting, killing_MapSetting, NULL;

		private static onFirstUserEnter fromString(String Str) {
			try {
				return valueOf(Str);
			} catch (IllegalArgumentException ex) {
				return NULL;
			}
		}
	};

	private static enum onUserEnter {

		babyPigMap,
		crash_Dragon,
		evanleaveD,
		getDragonEgg,
		meetWithDragon,
		go1010100,
		go1010200,
		go1010300,
		go1010400,
		evanPromotion,
		PromiseDragon,
		evanTogether,
		incubation_dragon,
		TD_MC_Openning,
		TD_MC_gasi,
		TD_MC_title,
		cygnusJobTutorial,
		cygnusTest,
		startEreb,
		dojang_Msg,
		dojang_1st,
		reundodraco,
		undomorphdarco,
		explorationPoint,
		goAdventure,
		go10000,
		go20000,
		go30000,
		go40000,
		go50000,
		go1000000,
		go1010000,
		go1020000,
		go2000000,
		goArcher,
		goPirate,
		goRogue,
		goMagician,
		goSwordman,
		goLith,
		iceCave,
		mirrorCave,
		aranDirection,
		rienArrow,
		rien,
		check_count,
		Massacre_first,
		Massacre_result,
		aranTutorAlone,
		evanAlone,
		dojang_QcheckSet,
		NULL;

		private static onUserEnter fromString(String Str) {
			try {
				return valueOf(Str);
			} catch (IllegalArgumentException ex) {
				return NULL;
			}
		}
	};

	public static void startScript_FirstUser(ChannelClient c, String scriptName) {
		final ChannelCharacter player = c.getPlayer();
		switch (onFirstUserEnter.fromString(scriptName)) {
		case dojang_Eff: {
			int temp = (player.getMapId() - 925000000) / 100;
			int stage = (int) (temp - (Math.floor(temp / 100) * 100));
			sendDojoClock(c, getTiming(stage) * 60);
			sendDojoStart(c, stage - getDojoStageDec(stage));
			break;
		}
		case PinkBeen_before: {
			handlePinkBeanStart(c);
			break;
		}
		case onRewordMap: {
			reloadWitchTower(c);
			break;
		}
		case StageMsg_together: {
			player.getMap().startMapEffect("Solve the question and gather the amount of passes!", 5120017);
			break;
		}
		default: {
			System.out.println("Unhandled script : " + scriptName + ", type : onFirstUserEnter - MAPID " + player.getMapId());
			break;
		}
		}
	}

	public static void startScript_User(ChannelClient c, String scriptName) {
		String data = "";
		final ChannelCharacter player = c.getPlayer();
		final byte genderByte = player.getGender().asNumber();
		switch (onUserEnter.fromString(scriptName)) {
		case cygnusTest:
		case cygnusJobTutorial: {
			showIntro(c, "Effect/Direction.img/cygnusJobTutorial/Scene" + (player.getMapId() - 913040100));
			break;
		}
		case dojang_QcheckSet:
		case evanTogether:
		case aranTutorAlone:
		case evanAlone: { // no idea
			c.write(ChannelPackets.enableActions());
			break;
		}
		case startEreb:
		case mirrorCave:
		case babyPigMap:
		case evanleaveD: {
			c.write(UIPacket.IntroDisableUI(false));
			c.write(UIPacket.IntroLock(false));
			c.write(ChannelPackets.enableActions());
			break;
		}
		case dojang_Msg: {
			player.getMap().startMapEffect(mulungEffects[Randomizer.nextInt(mulungEffects.length)], 5120024);
			break;
		}
		case dojang_1st: {
			player.writeMulungEnergy();
			break;
		}
		case undomorphdarco:
		case reundodraco: {
			player.cancelEffect(ItemInfoProvider.getInstance().getItemEffect(2210016), false, -1);
			break;
		}
		case goAdventure: {
			// BUG in MSEA v.91, so let's skip this part.
			showIntro(c, "Effect/Direction3.img/goAdventure/Scene" + genderByte);
			break;
		}
		case crash_Dragon:
			showIntro(c, "Effect/Direction4.img/crash/Scene" + genderByte);
			break;
		case getDragonEgg:
			showIntro(c, "Effect/Direction4.img/getDragonEgg/Scene" + genderByte);
			break;
		case meetWithDragon:
			showIntro(c, "Effect/Direction4.img/meetWithDragon/Scene" + genderByte);
			break;
		case PromiseDragon:
			showIntro(c, "Effect/Direction4.img/PromiseDragon/Scene" + genderByte);
			break;
		case evanPromotion:
			switch (player.getMapId()) {
			case 900090000:
				data = "Effect/Direction4.img/promotion/Scene0" + genderByte;
				break;
			case 900090001:
				data = "Effect/Direction4.img/promotion/Scene1";
				break;
			case 900090002:
				data = "Effect/Direction4.img/promotion/Scene2" + genderByte;
				break;
			case 900090003:
				data = "Effect/Direction4.img/promotion/Scene3";
				break;
			case 900090004:
				c.write(UIPacket.IntroDisableUI(false));
				c.write(UIPacket.IntroLock(false));
				c.write(ChannelPackets.enableActions());
				final GameMap mapto = ChannelServer.getMapFactory().getMap(910000000);
				player.changeMap(mapto, mapto.getPortal(0));
				return;
			}
			showIntro(c, data);
			break;
		case TD_MC_title: {
			c.write(UIPacket.IntroDisableUI(false));
			c.write(UIPacket.IntroLock(false));
			c.write(ChannelPackets.enableActions());
			break;
		}
		case explorationPoint: {
			if (player.getMapId() == 104000000) {
				c.write(UIPacket.IntroDisableUI(false));
				c.write(UIPacket.IntroLock(false));
				c.write(ChannelPackets.enableActions());
				c.write(UIPacket.MapNameDisplay(player.getMapId()));
			}
			break;
		}
		case go10000:
		case go1020000:
			c.write(UIPacket.IntroDisableUI(false));
			c.write(UIPacket.IntroLock(false));
			c.write(ChannelPackets.enableActions());
			break;
		case go20000:
		case go30000:
		case go40000:
		case go50000:
		case go1000000:
		case go2000000:
		case go1010000:
		case go1010100:
		case go1010200:
		case go1010300:
		case go1010400: {
			c.write(UIPacket.MapNameDisplay(player.getMapId()));
			break;
		}
		case goArcher: {
			showIntro(c, "Effect/Direction3.img/archer/Scene" + genderByte);
			break;
		}
		case goPirate: {
			showIntro(c, "Effect/Direction3.img/pirate/Scene" + genderByte);
			break;
		}
		case goRogue: {
			showIntro(c, "Effect/Direction3.img/rogue/Scene" + genderByte);
			break;
		}
		case goMagician: {
			showIntro(c, "Effect/Direction3.img/magician/Scene" + genderByte);
			break;
		}
		case goSwordman: {
			showIntro(c, "Effect/Direction3.img/swordman/Scene" + genderByte);
			break;
		}
		case goLith: {
			showIntro(c, "Effect/Direction3.img/goLith/Scene" + genderByte);
			break;
		}
		case TD_MC_Openning: {
			showIntro(c, "Effect/Direction2.img/open");
			break;
		}
		case TD_MC_gasi: {
			showIntro(c, "Effect/Direction2.img/gasi");
			break;
		}
		case aranDirection: {
			switch (player.getMapId()) {
			case 914090010:
				data = "Effect/Direction1.img/aranTutorial/Scene0";
				break;
			case 914090011:
				data = "Effect/Direction1.img/aranTutorial/Scene1" + genderByte;
				break;
			case 914090012:
				data = "Effect/Direction1.img/aranTutorial/Scene2" + genderByte;
				break;
			case 914090013:
				data = "Effect/Direction1.img/aranTutorial/Scene3";
				break;
			case 914090100:
				data = "Effect/Direction1.img/aranTutorial/HandedPoleArm" + genderByte;
				break;
			case 914090200:
				data = "Effect/Direction1.img/aranTutorial/Maha";
				break;
			}
			showIntro(c, data);
			break;
		}
		case iceCave: {
			player.changeSkillLevel(SkillFactory.getSkill(20000014), (byte) -1, (byte) 0);
			player.changeSkillLevel(SkillFactory.getSkill(20000015), (byte) -1, (byte) 0);
			player.changeSkillLevel(SkillFactory.getSkill(20000016), (byte) -1, (byte) 0);
			player.changeSkillLevel(SkillFactory.getSkill(20000017), (byte) -1, (byte) 0);
			player.changeSkillLevel(SkillFactory.getSkill(20000018), (byte) -1, (byte) 0);
			player.changeSkillLevel(SkillFactory.getSkill(20000014), (byte) -1, (byte) 0);
			c.write(UIPacket.ShowWZEffect("Effect/Direction1.img/aranTutorial/ClickLirin"));
			c.write(UIPacket.IntroDisableUI(false));
			c.write(UIPacket.IntroLock(false));
			c.write(ChannelPackets.enableActions());
			break;
		}
		case rienArrow: {
			if (player.getInfoQuest(21019).equals("miss=o;helper=clear")) {
				player.updateInfoQuest(21019, "miss=o;arr=o;helper=clear");
				c.write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/aranTutorial/tutorialArrow3"));
			}
			break;
		}
		case rien: {
			if (player.getQuestCompletionStatus(21101) == 2 && player.getInfoQuest(21019).equals("miss=o;arr=o;helper=clear")) {
				player.updateInfoQuest(21019, "miss=o;arr=o;ck=1;helper=clear");
			}
			c.write(UIPacket.IntroDisableUI(false));
			c.write(UIPacket.IntroLock(false));
			break;
		}
		default: {
			System.out.println("Unhandled script : " + scriptName + ", type : onUserEnter - MAPID " + player.getMapId());
			break;
		}
		}
	}

	private static int getTiming(int ids) {
		if (ids <= 5) {
			return 5;
		} else if (ids >= 7 && ids <= 11) {
			return 6;
		} else if (ids >= 13 && ids <= 17) {
			return 7;
		} else if (ids >= 19 && ids <= 23) {
			return 8;
		} else if (ids >= 25 && ids <= 29) {
			return 9;
		} else if (ids >= 31 && ids <= 35) {
			return 10;
		} else if (ids >= 37 && ids <= 38) {
			return 15;
		}
		return 0;
	}

	private static int getDojoStageDec(int ids) {
		if (ids <= 5) {
			return 0;
		} else if (ids >= 7 && ids <= 11) {
			return 1;
		} else if (ids >= 13 && ids <= 17) {
			return 2;
		} else if (ids >= 19 && ids <= 23) {
			return 3;
		} else if (ids >= 25 && ids <= 29) {
			return 4;
		} else if (ids >= 31 && ids <= 35) {
			return 5;
		} else if (ids >= 37 && ids <= 38) {
			return 6;
		}
		return 0;
	}

	private static void showIntro(final ChannelClient c, final String data) {
		c.write(UIPacket.IntroDisableUI(true));
		c.write(UIPacket.IntroLock(true));
		c.write(UIPacket.ShowWZEffect(data));
	}

	private static void sendDojoClock(ChannelClient c, int time) {
		c.write(ChannelPackets.getClock(time));
	}

	private static void sendDojoStart(ChannelClient c, int stage) {
		c.write(ChannelPackets.environmentChange("Dojang/start", 4));
		c.write(ChannelPackets.environmentChange("dojang/start/stage", 3));
		c.write(ChannelPackets.environmentChange("dojang/start/number/" + stage, 3));

		PacketBuilder builder = new PacketBuilder();

		// 79 00 01 00 01 00 00 00
		builder.writeAsShort(ServerPacketOpcode.BOSS_ENV.getValue());
		builder.writeAsShort(1);
		builder.writeAsShort(1);
		builder.writeAsShort(0);

		c.write(builder.getPacket());
	}

	private static void handlePinkBeanStart(ChannelClient c) {
		final GameMap map = c.getPlayer().getMap();
		map.killAllMonsters(true);
		map.respawn(true);

		if (map.containsNPC(2141000) == -1) {
			map.spawnNpc(2141000, new Point(-190, -42));
		}
	}

	private static void reloadWitchTower(ChannelClient c) {
		final ChannelCharacter player = c.getPlayer();
		final GameMap map = player.getMap();
		map.killAllMonsters(false);

		final int level = player.getLevel();
		int mob;
		if (level <= 10) {
			mob = 9300367;
		} else if (level <= 20) {
			mob = 9300368;
		} else if (level <= 30) {
			mob = 9300369;
		} else if (level <= 40) {
			mob = 9300370;
		} else if (level <= 50) {
			mob = 9300371;
		} else if (level <= 60) {
			mob = 9300372;
		} else if (level <= 70) {
			mob = 9300373;
		} else if (level <= 80) {
			mob = 9300374;
		} else if (level <= 90) {
			mob = 9300375;
		} else if (level <= 100) {
			mob = 9300376;
		} else {
			mob = 9300377;
		}
		map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(mob), witchTowerPos);
	}
}
