package javastory.login;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.server.TimerManager;
import javastory.tools.LogUtil;

import com.google.common.collect.Lists;


public class LoginWorker {

    private static class AccountIpEntry {
        private final int AccountId;
        private final String IpAddress;
        
        public AccountIpEntry(final int id, final String entry) {
            this.AccountId = id;
            this.IpAddress = entry;
        }

        public int getAccountId() {
            return this.AccountId;
        }

        public String getIpAddress() {
            return this.IpAddress;
        }
    }
    
    private static Runnable persister;
    private static final List<AccountIpEntry> ACCOUNT_IP_LOG = Lists.newLinkedList();
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
                for (final AccountIpEntry logentry : ACCOUNT_IP_LOG) {
                    sb.append("AccountId : ");
                    sb.append(logentry.getAccountId());
                    sb.append(", IP : ");
                    sb.append(logentry.getIpAddress());
                    sb.append(", TIME : ");
                    sb.append(time);
                    sb.append("\n");
                }
                ACCOUNT_IP_LOG.clear();
            } finally {
                mutex.unlock();
            }
            LogUtil.log(LogUtil.IP_Log, sb.toString());
        }
    }

    public static void registerClient(final LoginClient c) {
        c.write(LoginPacket.getAuthSuccessRequest(c));
        c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {

            @Override
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
                for (final Entry<Integer, Integer> entry : channelLoad.entrySet()) {
                    final int load = Math.min(1200, entry.getValue());
                    loginServer.setLoad(entry.getKey(), load);
                }
            } catch (final RemoteException ex) {
                LoginServer.getInstance().pingWorld();
            }
        }
        c.write(LoginPacket.getWorldList(2, "Cassiopeia", loginServer.getChannels()));
        c.write(LoginPacket.getEndOfWorldList());
        mutex.lock();
        try {
            ACCOUNT_IP_LOG.add(new AccountIpEntry(c.getAccountId(), c.getSessionIP()));
        } finally {
            mutex.unlock();
        }
    }
}
