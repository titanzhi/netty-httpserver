package com.s3d.httpserver.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.s3d.httpserver.request.RequestHandlerBase;
import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

import io.netty.channel.ChannelHandlerContext;


public class TestRequestHandler extends RequestHandlerBase{
	private final ScheduledExecutorService executor = Executors
			.newScheduledThreadPool(1);

	protected AtomicInteger requests = new AtomicInteger(0);

	public ScheduledFuture<?> lastFuture;

	protected String content = null;
	protected boolean async = false;
	protected long execTime = 0;
	protected long writeTime = 0;
	protected boolean error = false;
	protected boolean disconnect = false;

	protected Map<String, List<String>> parameters;

	public TestRequestHandler(final String content_, final boolean async_,
			final long execTime_, final long writeTime_, final boolean error_,
			final boolean disconnect_) {

		content = content_;
		async = async_;
		execTime = execTime_;
		writeTime = writeTime_;
		error = error_;
		disconnect = disconnect_;

	}

	public final void onRequest(final ChannelHandlerContext ctx ,ServerRequest request,final ServerResponse response)
			throws IOException {

		requests.incrementAndGet();
		parameters = request.getParameters();

		final Runnable task = response(response);

		// response.setChunkedEncoding(true);

		if (async) {
			response.suspend();
			
			lastFuture =
					executor.schedule(task, execTime, TimeUnit.MILLISECONDS);
		} else {
			task.run();
		}

	}

	public Runnable response(final ServerResponse response) {

		return new Runnable() {

			@Override
			public void run() {

				if (error) {
					throw new RuntimeException("Uncaught exception");
				}

				if (disconnect) {
					try {
						response.finish().sync();
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				try {
					response.write(content.getBytes());
					if (writeTime > 0) {
						try {
							Thread.sleep(writeTime);
						} catch (final InterruptedException e) {
						}
					}
					response.finish();
				} catch (final IOException e) {
					e.printStackTrace();
				}

			}

		};

	}
}
