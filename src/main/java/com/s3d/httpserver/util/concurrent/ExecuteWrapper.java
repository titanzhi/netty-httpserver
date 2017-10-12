package com.s3d.httpserver.util.concurrent;

import com.s3d.httpserver.server.HttpRequestChannelHandler;

import java.util.concurrent.TimeUnit;

import javolution.text.TextBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteWrapper implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(HttpRequestChannelHandler.class);
	private static final long maximumRuntimeInMillisecWithoutWarning = 3000;
	private final Runnable runnable;

	public ExecuteWrapper(Runnable runnable) {
		this.runnable = runnable;
	}

	@Override
	public final void run() {
		ExecuteWrapper.execute(runnable,
				getMaximumRuntimeInMillisecWithoutWarning());
	}

	protected long getMaximumRuntimeInMillisecWithoutWarning() {
		return maximumRuntimeInMillisecWithoutWarning;
	}

	public static void execute(Runnable runnable) {
		execute(runnable, Long.MAX_VALUE);
	}

	public static void execute(Runnable runnable,
			long maximumRuntimeInMillisecWithoutWarning) {
		long begin = System.nanoTime();

		try {
			runnable.run();
		} catch (RuntimeException e) {
			log.warn("Exception in a Runnable execution:", e);
		} finally {
			long runtimeInNanosec = System.nanoTime() - begin;
			Class<? extends Runnable> clazz = runnable.getClass();

			RunnableStatsManager.handleStats(clazz, runtimeInNanosec);

			long runtimeInMillisec = TimeUnit.NANOSECONDS
					.toMillis(runtimeInNanosec);

			if (runtimeInMillisec > maximumRuntimeInMillisecWithoutWarning) {
				TextBuilder tb = TextBuilder.newInstance();

				tb.append(clazz);
				tb.append(" - execution time: ");
				tb.append(runtimeInMillisec);
				tb.append("msec");

				log.warn(tb.toString());

				TextBuilder.recycle(tb);
			}
		}
	}
}