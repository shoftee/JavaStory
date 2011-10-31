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
		if (this.executor != null && !this.executor.isShutdown() && !this.executor.isTerminated()) {
			return;
		}

		final ThreadFactory factory = new ThreadFactory() {

			private final AtomicInteger threadId = new AtomicInteger(1);

			@Override
			public Thread newThread(final Runnable runnable) {
				final Thread thread = new Thread(runnable);
				final String threadName = "TimerManager-Worker-" + this.threadId.getAndIncrement();
				thread.setName(threadName);
				return thread;
			}
		};

		this.executor = new ScheduledThreadPoolExecutor(3, factory);
		this.executor.setKeepAliveTime(10, TimeUnit.MINUTES);
		this.executor.allowCoreThreadTimeOut(true);
		this.executor.setCorePoolSize(3);
		this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
	}

	public void stop() {
		this.executor.shutdown();
	}

	public ScheduledFuture<?> register(final Runnable runnable, final long repeatTime, final long delay) {
		return this.executor.scheduleAtFixedRate(this.getLoggedRunnable(runnable), delay, repeatTime, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> register(final Runnable runnable, final long repeatTime) {
		return this.executor.scheduleAtFixedRate(this.getLoggedRunnable(runnable), 0, repeatTime, TimeUnit.MILLISECONDS);
	}

	private Runnable getLoggedRunnable(final Runnable runnable) {
		return new LoggedRunnable(runnable);
	}

	public ScheduledFuture<?> schedule(final Runnable runnable, final long delay) {
		return this.executor.schedule(this.getLoggedRunnable(runnable), delay, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> scheduleAtTimestamp(final Runnable r, final long timestamp) {
		return this.schedule(r, timestamp - System.currentTimeMillis());
	}

	private static class LoggedRunnable implements Runnable {

		Runnable runnable;

		public LoggedRunnable(final Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				this.runnable.run();
			} catch (final Throwable t) {
				LogUtil.outputFileError(LogUtil.Timer_Log, t);
			}
		}
	}
}