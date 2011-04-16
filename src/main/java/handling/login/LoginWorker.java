package handling.login;

import org.javastory.server.login.LoginServer;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import client.MapleClient;
import server.TimerManager;
import tools.packet.LoginPacket;
import tools.Pair;
import tools.FileOutputUtil;

public class LoginWorker {

    private static Runnable persister;
    private static final List<Pair<Integer, String>> IPLog = new LinkedList<Pair<Integer, String>>();
    private static long lastUpdate = 0;
    private static final Lock mutex = new ReentrantLock();

    protected LoginWorker() {
        persister = new PersistingTask();
        TimerManager.getInstance().register(persister, 1800000); // 30 min once
    }

    private static class PersistingTask implements Runnable {

        @Override
        public void run() {
            final StringBuilder sb = new StringBuilder();
            mutex.lock();
            try {
                final String time = FileOutputUtil.CurrentReadable_Time();
                for (Pair<Integer, String> logentry : IPLog) {
                    sb.append("ACCID : ");
                    sb.append(logentry.getLeft());
                    sb.append(", IP : ");
                    sb.append(logentry.getRight());
                    sb.append(", TIME : ");
                    sb.append(time);
                    sb.append("\n");
                }
                IPLog.clear();
            } finally {
                mutex.unlock();
            }
            FileOutputUtil.log(FileOutputUtil.IP_Log, sb.toString());
        }
    }

    public static void registerClient(final MapleClient c) {
        if (c.finishLogin() == 0) {
            c.getSession().write(LoginPacket.getAuthSuccessRequest(c));
            c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {

                public void run() {
                    c.getSession().close(false);
                }
            }, 10 * 60 * 10000));
        } else {
            c.getSession().write(LoginPacket.getLoginFailed(7));
            return;
        }
        final LoginServer LS = LoginServer.getInstance();
        if (System.currentTimeMillis() - lastUpdate > 300000) { // Update once every 5 minutes
            lastUpdate = System.currentTimeMillis();
            try {
                final Map<Integer, Integer> channelLoad = 
                        LS.getWorldInterface().getChannelLoad();
                if (channelLoad == null) { 
                    // In an unfortunate event that client logged in before load
                    lastUpdate = 0;
                    c.getSession().write(LoginPacket.getLoginFailed(7));
                    return;
                }
                for (Entry<Integer, Integer> entry : channelLoad.entrySet()) {
                    final int load = Math.min(1200, entry.getValue());
                    LS.setLoad(entry.getKey(), load);
                }
            } catch (RemoteException ex) {
                LoginServer.getInstance().pingWorld();
            }
        }
        c.getSession().write(LoginPacket.getWorldList(2, "Cassiopeia", LS.getChannels()));
        c.getSession().write(LoginPacket.getEndOfWorldList());
        mutex.lock();
        try {
            IPLog.add(new Pair<Integer, String>(c.getAccID(), c.getSession().getRemoteAddress().toString()));
        } finally {
            mutex.unlock();
        }
    }
}
