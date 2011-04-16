package client;

import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.TimerManager;
import tools.MaplePacketCreator;
import server.Randomizer;

public class OdinSEA {

    private static final String[] messages = {
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
    private static final int totalMessages = messages.length;
    public static int[] BlockedNPC = {
        9250025 // FM Maple TV
    };
    public static MapleClient c;

    public OdinSEA(final MapleClient c) {
        OdinSEA.c = c;
    }

    public final MapleClient getClient() {
        return c;
    }

    public static void start() {
        final TimerManager timerManager = TimerManager.getInstance();
        timerManager.register(new Runnable() {

            public final void run() {
                int crandom = Randomizer.nextInt(totalMessages);
                for (ChannelServer cserv : ChannelManager.getAllInstances()) {
                    cserv.broadcastPacket(MaplePacketCreator.sendMapleTip("[MapleTip] " + messages[crandom]));
                }
            }
        }, 300000); 
        timerManager.register(new Runnable() {

            public final void run() {
                for (ChannelServer cserv : ChannelManager.getAllInstances()) {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                        chr.saveToDb(false, false);
                    }
                }
            }
        }, 300000); 
    }
}
