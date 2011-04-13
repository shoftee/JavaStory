package client;

import handling.channel.ChannelServer;
import server.TimerManager;
import tools.MaplePacketCreator;
import server.Randomizer;

public class OdinSEA {

	public static int[] BlockedNPC = {
	//9250025 // FM Maple TV
	};

	public static MapleClient c;

	public OdinSEA(final MapleClient c) {
		OdinSEA.c = c;
	}

	public final MapleClient getClient() {
		return c;
	}

	public static void start() {
		TimerManager.getInstance().register(new Runnable() {
			public final void run() {
				String[] messages = {
					"We are now running Beta Test, kindly invite your friends to test our server!",
					"Read the God Damn forum, i am very serious. Please!",
					"Read the God Damn MapleTips, i am very serious.",
					"1 single question again, you're so dead!",
					"Data will not be wiped after our Beta Test! Which mean your items, characters, etc will still remain!",
					"Visit our website @ http://odinsea.co.cc/ and forum @ http://www.odinsea.tk/",
					"Visit our Facebook page at http://www.facebook.com/OdinSEA",
					"Please note that we have just published out Golden Temple system!",
					"Our server history : AsianMS v0.55 > LaksaMS v0.61/v0.62 > MyMaple v0.74/v0.75 > OdinSEA v0.79/v0.82/v0.94/v1.0/v1.01/v1.02",
					"Try @dispose and @ea if you are unable to click on npc or attack",
					"For new players on this server, kindly check @help. For more information visit our website, forum or even facebook page",
					"You might get banned for massive pm-ing GMs/Staffs",
					"Contact GM using --> @togm [message here] <-- This will cost you 500 @cash"
				};
				int totalmessages = messages.length;
				int crandom = Randomizer.nextInt(totalmessages);
				for (ChannelServer cserv : ChannelServer.getAllInstances()) {
					cserv.broadcastPacket(MaplePacketCreator.sendMapleTip("[MapleTip] " + messages[crandom]));
				}
			}
		}, 300000); // 5 minutes once
		TimerManager.getInstance().register(new Runnable() {
			public final void run() {
				for (ChannelServer cserv : ChannelServer.getAllInstances()) {
					for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
						chr.saveToDB(false, false);
					}
				}
			}
		}, 300000); // 5 minutes once
		/*TimerManager.getInstance().register(new Runnable() {
			public void run() {
				int mapid = 24004611;
				int mapid2 = 24004612;
				//int eggid = 4001094; // Nine Spirit Egg
				int neweggid = 2041200; //Dragon Stone
				short gain = 1;
				short loose = -1;
				for (ChannelServer cserv : ChannelServer.getAllInstances()) {
					for (int w = 5; w <= 6; w++){
						for (MapleCharacter player : cserv.getMapFactory(w).getMap(mapid).getCharacters()) {
							if (player.haveItem(eggid, 1, true, true)){ // equipped
								MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, eggid, loose, true, true);
								MapleInventoryManipulator.addById(player.getClient(), neweggid, gain);
							}
						}
					}
				}
			}
		}, 60000);// 1min??*/
		/*TimerManager.getInstance().register(new Runnable() {
		public final void run() {
		StringBuilder conStr = new StringBuilder("Connected Clients: ");
		Map<Integer, Integer> connected = null;
		try {
		connected = ChannelServer.getInstance(1).getWorldInterface().getConnected();
		} catch (RemoteException ex) {
		Logger.getLogger(ChannelServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		boolean first = true;
		for (int i : connected.keySet()) {
		if (!first) {
		conStr.append(", ");
		} else {
		first = false;
		}
		if (i == 0) {
		conStr.append("Total: ");
		conStr.append(connected.get(i));
		} else {
		conStr.append("Channel");
		conStr.append(i);
		conStr.append(": ");
		conStr.append(connected.get(i));
		}
		}
		System.out.println(conStr.toString());
		}
		}, 120000);
		TimerManager.getInstance().register(new Runnable() {
		public final void run() {
		MapleMap map;
		for (int i = 1; i <= 22; i++) {
		for (ChannelServer cserv : ChannelServer.getAllInstances()) {
		map = cserv.getMapFactory(5).getMap(910000000 + i);
		if (map.getAllPlayer().size() <= 0) {
		map.killAllMonsters(false);
		}
		}
		}
		for (ChannelServer cserv : ChannelServer.getAllInstances()) {
		cserv.broadcastPacket(MaplePacketCreator.serverNotice(6, "FREE MARKET has just been optimized!"));
		}
		}
		}, 600000);*/
	}
}
