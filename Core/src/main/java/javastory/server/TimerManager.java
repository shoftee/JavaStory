package javastory.server;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javastory.tools.LogUtil;


public final class TimerManager {

    private static TimerManager instance = new TimerManager();

    private ScheduledThreadPoolExecutor executor;

    public static TimerManager getInstance() {
	return instance;
    }
    
    private TimerManager() {
        this.start();
    }

    public void start() {
	//starting the same timermanager twice is no - op
        if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
	    return; 
	}

	final ThreadFactory factory = new ThreadFactory() {

	    private final AtomicInteger threadId = new AtomicInteger(1);

	    @Override
	    public Thread newThread(Runnable runnable) {
		final Thread thread = new Thread(runnable);
                final String threadName = 
                        "TimerManager-Worker-" + threadId.getAndIncrement();
		thread.setName(threadName);
		return thread;
	    }
	};

	executor = new ScheduledThreadPoolExecutor(3, factory);
	executor.setKeepAliveTime(10, TimeUnit.MINUTES);
	executor.allowCoreThreadTimeOut(true);
	executor.setCorePoolSize(3);
	executor.setMaximumPoolSize(5);
	executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    public void stop() {
	executor.shutdown();
    }

    public ScheduledFuture<?> register(Runnable runnable, 
            long repeatTime, long delay) {
	return executor.scheduleAtFixedRate(
                getLoggedRunnable(runnable), delay, 
                repeatTime, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> register(Runnable runnable, long repeatTime) {
	return executor.scheduleAtFixedRate(
                getLoggedRunnable(runnable), 0, 
                repeatTime, TimeUnit.MILLISECONDS);
    }

    private Runnable getLoggedRunnable(Runnable runnable) {
        return new LoggedRunnable(runnable);
    }

    public ScheduledFuture<?> schedule(Runnable runnable, long delay) {
	return executor.schedule(
                getLoggedRunnable(runnable), delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(Runnable r, long timestamp) {
	return schedule(r, timestamp - System.currentTimeMillis());
    }

    private class LoggedRunnable implements Runnable {

	Runnable runnable;

	public LoggedRunnable(final Runnable runnable) {
	    this.runnable = runnable;
	}

	@Override
	public void run() {
	    try {
		runnable.run();
	    } catch (Throwable t) {
		LogUtil.outputFileError(LogUtil.Timer_Log, t);
	    }
	}
    }
}