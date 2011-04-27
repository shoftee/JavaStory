package handling.login;

import com.google.common.collect.Lists;
import org.javastory.server.login.LoginServer;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.javastory.client.LoginClient;
import org.javastory.server.login.AuthReplyCode;
import server.TimerManager;
import tools.packet.LoginPacket;
import tools.Pair;
import tools.LogUtil;

public class LoginWorker {

    private static Runnable persister;
    private static final List<Pair<Integer, String>> IPLog = Lists.newLinkedList();
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
                final String time = LogUtil.CurrentReadable_Time();
                for (Pair<Integer, String> logentry : IPLog) {
                    sb.append("AccountId : ");
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
            LogUtil.log(LogUtil.IP_Log, sb.toString());
        }
    }

    public static void registerClient(final LoginClient c) {
        c.write(LoginPacket.getAuthSuccessRequest(c));
        c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {

            public void run() {
                c.disconnect();
            }
        }, 10 * 60 * 10000));
        final LoginServer loginServer = LoginServer.getInstance();
        if (System.currentTimeMillis() - lastUpdate > 300000) { 
            // Update once every 5 minutes
            lastUpdate = System.currentTimeMillis();
            try {
                final Map<Integer, Integer> channelLoad =
                        loginServer.getWorldInterface().getChannelLoad();
                if (channelLoad == null) {
                    // In an unfortunate event that client logged in before load
                    lastUpdate = 0;
                    c.write(LoginPacket.getLoginFailed(AuthReplyCode.TOO_MANY_CONNECTIONS));
                    return;
                }
                for (Entry<Integer, Integer> entry : channelLoad.entrySet()) {
                    final int load = Math.min(1200, entry.getValue());
                    loginServer.setLoad(entry.getKey(), load);
                }
            } catch (RemoteException ex) {
                LoginServer.getInstance().pingWorld();
            }
        }
        c.write(LoginPacket.getWorldList(2, "Cassiopeia", loginServer.getChannels()));
        c.write(LoginPacket.getEndOfWorldList());
        mutex.lock();
        try {
            IPLog.add(new Pair<Integer, String>(c.getAccountId(), c.getSessionIP()));
        } finally {
            mutex.unlock();
        }
    }
}
