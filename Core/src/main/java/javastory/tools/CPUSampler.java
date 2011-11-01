/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.tools;

import java.io.IOException;
import java.io.Writer;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CPUSampler {

	private final List<String> included = Lists.newLinkedList();
	private static CPUSampler instance = new CPUSampler();
	private long interval = 5;
	private SamplerThread sampler = null;
	private final Map<StackTrace, Integer> recorded = Maps.newHashMap();
	private int totalSamples = 0;

	public static CPUSampler getInstance() {
		return instance;
	}

	public void setInterval(final long millis) {
		this.interval = millis;
	}

	public void addIncluded(final String include) {
		for (final String alreadyIncluded : this.included) {
			if (include.startsWith(alreadyIncluded)) {
				return;
			}
		}
		this.included.add(include);
	}

	public void reset() {
		this.recorded.clear();
		this.totalSamples = 0;
	}

	public void start() {
		if (this.sampler == null) {
			this.sampler = new SamplerThread();
			this.sampler.start();
		}
	}

	public void stop() {
		if (this.sampler != null) {
			this.sampler.stop();
			this.sampler = null;
		}
	}

	public SampledStacktraces getTopConsumers() {
		final List<StacktraceWithCount> ret = Lists.newArrayList();
		final Set<Entry<StackTrace, Integer>> entrySet = this.recorded.entrySet();
		for (final Entry<StackTrace, Integer> entry : entrySet) {
			ret.add(new StacktraceWithCount(entry.getValue(), entry.getKey()));
		}
		Collections.sort(ret);
		return new SampledStacktraces(ret, this.totalSamples);
	}

	public void save(final Writer writer, final int minInvocations, final int topMethods) throws IOException {
		final SampledStacktraces topConsumers = this.getTopConsumers();
		final StringBuilder builder = new StringBuilder(); // build our summary :o
		builder.append("Top Methods:\n");
		for (int i = 0; i < topMethods && i < topConsumers.getTopConsumers().size(); i++) {
			builder.append(topConsumers.getTopConsumers().get(i).toString(topConsumers.getTotalInvocations(), 1));
		}
		builder.append("\nStack Traces:\n");
		writer.write(builder.toString());
		writer.write(topConsumers.toString(minInvocations));
		writer.flush();
	}

	private void consumeStackTraces(final Map<Thread, StackTraceElement[]> traces) {
		for (final Entry<Thread, StackTraceElement[]> trace : traces.entrySet()) {
			final int relevant = this.findRelevantElement(trace.getValue());
			if (relevant != -1) {
				final StackTrace st = new StackTrace(trace.getValue(), relevant, trace.getKey().getState());
				final Integer i = this.recorded.get(st);
				this.totalSamples++;
				if (i == null) {
					this.recorded.put(st, Integer.valueOf(1));
				} else {
					this.recorded.put(st, Integer.valueOf(i.intValue() + 1));
				}
			}
		}
	}

	private int findRelevantElement(final StackTraceElement[] trace) {
		if (trace.length == 0) {
			return -1;
		} else if (this.included.isEmpty()) {
			return 0;
		}
		int firstIncluded = -1;
		for (final String myIncluded : this.included) {
			for (int i = 0; i < trace.length; i++) {
				final StackTraceElement ste = trace[i];
				if (ste.getClassName().startsWith(myIncluded)) {
					if (i < firstIncluded || firstIncluded == -1) {
						firstIncluded = i;
						break;
					}
				}
			}
		}
		if (firstIncluded >= 0 && trace[firstIncluded].getClassName().equals("net.sf.odinms.tools.performance.CPUSampler$SamplerThread")) { // don't sample us
			return -1;
		}
		return firstIncluded;
	}

	private static class StackTrace {

		private StackTraceElement[] trace;
		private final State state;

		public StackTrace(final StackTraceElement[] trace, final int startAt, final State state) {
			this.state = state;
			if (startAt == 0) {
				this.trace = trace;
			} else {
				this.trace = new StackTraceElement[trace.length - startAt];
				System.arraycopy(trace, startAt, this.trace, 0, this.trace.length);
			}
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof StackTrace)) {
				return false;
			}
			final StackTrace other = (StackTrace) obj;
			if (other.trace.length != this.trace.length) {
				return false;
			}
			if (!(other.state == this.state)) {
				return false;
			}
			for (int i = 0; i < this.trace.length; i++) {
				if (!this.trace[i].equals(other.trace[i])) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			int ret = 13 * this.trace.length + this.state.hashCode();
			for (final StackTraceElement ste : this.trace) {
				ret ^= ste.hashCode();
			}
			return ret;
		}

		public StackTraceElement[] getTrace() {
			return this.trace;
		}

		@Override
		public String toString() {
			return this.toString(-1);
		}

		public String toString(final int traceLength) {
			final StringBuilder ret = new StringBuilder("State: ");
			ret.append(this.state.name());
			if (traceLength > 1) {
				ret.append("\n");
			} else {
				ret.append(" ");
			}
			int i = 0;
			for (final StackTraceElement ste : this.trace) {
				i++;
				if (i > traceLength) {
					break;
				}
				ret.append(ste.getClassName());
				ret.append("#");
				ret.append(ste.getMethodName());
				ret.append(" (Line: ");
				ret.append(ste.getLineNumber());
				ret.append(")\n");
			}
			return ret.toString();
		}
	}

	private class SamplerThread implements Runnable {

		private boolean running = false;
		private boolean shouldRun = false;
		private Thread rthread;

		public void start() {
			if (!this.running) {
				this.shouldRun = true;
				this.rthread = new Thread(this, "CPU Sampling Thread");
				this.rthread.start();
				this.running = true;
			}
		}

		public void stop() {
			this.shouldRun = false;
			this.rthread.interrupt();
			try {
				this.rthread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			while (this.shouldRun) {
				CPUSampler.this.consumeStackTraces(Thread.getAllStackTraces());
				try {
					Thread.sleep(CPUSampler.this.interval);
				} catch (final InterruptedException e) {
					return;
				}
			}
		}
	}

	public static class StacktraceWithCount implements Comparable<StacktraceWithCount> {

		private final int count;
		private final StackTrace trace;

		public StacktraceWithCount(final int count, final StackTrace trace) {
			super();
			this.count = count;
			this.trace = trace;
		}

		public int getCount() {
			return this.count;
		}

		public StackTraceElement[] getTrace() {
			return this.trace.getTrace();
		}

		@Override
		public int compareTo(final StacktraceWithCount o) {
			return -Integer.valueOf(this.count).compareTo(Integer.valueOf(o.count));
		}

		@Override
		public String toString() {
			return this.count + " Sampled Invocations\n" + this.trace.toString();
		}

		private double getPercentage(final int total) {
			return Math.round((double) this.count / total * 10000.0) / 100.0;
		}

		public String toString(final int totalInvoations, final int traceLength) {
			return this.count + "/" + totalInvoations + " Sampled Invocations (" + this.getPercentage(totalInvoations) + "%) " + this.trace.toString(traceLength);
		}
	}

	public static class SampledStacktraces {

		List<StacktraceWithCount> topConsumers;
		int totalInvocations;

		public SampledStacktraces(final List<StacktraceWithCount> topConsumers, final int totalInvocations) {
			super();
			this.topConsumers = topConsumers;
			this.totalInvocations = totalInvocations;
		}

		public List<StacktraceWithCount> getTopConsumers() {
			return this.topConsumers;
		}

		public int getTotalInvocations() {
			return this.totalInvocations;
		}

		@Override
		public String toString() {
			return this.toString(0);
		}

		public String toString(final int minInvocation) {
			final StringBuilder ret = new StringBuilder();
			for (final StacktraceWithCount swc : this.topConsumers) {
				if (swc.getCount() >= minInvocation) {
					ret.append(swc.toString(this.totalInvocations, Integer.MAX_VALUE));
					ret.append("\n");
				}
			}
			return ret.toString();
		}
	}
}
