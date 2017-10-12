package com.s3d.httpserver.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.s3d.httpserver.request.RequestAttribute;
import com.s3d.httpserver.request.RequestAttributeKey;
import com.s3d.httpserver.request.RequestHandlerBase;
import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

/**
 * 在异步响应的模式下,用户如果取消了request,则需要将异步请求时产生的future也cancel掉
 * 如果有这种需求,则将RequestHandler继承此类，并在onRequest里面注册cancelOnAbort
 * @author sulta
 * @deprecated 不需要了
 */
public abstract class CancellableRequestHandler extends RequestHandlerBase {

	private static final Logger log = LoggerFactory
			.getLogger(CancellableRequestHandler.class);

	private final RequestAttributeKey<CancelFutureList> ATTR_CANCEL_TASKS = new RequestAttributeKey<CancelFutureList>(
			"cancel-tasks");

	@Override
	public void onAbort(final ServerRequest request,
			final ServerResponse response) {

		cancelTasks(request);

	}

	@Override
	public void onException(final ServerRequest request,
			final ServerResponse response, final Throwable exception) {

		log.debug(
				"Request encountered an uncaught exception, cancelling tasks",
				exception);

		cancelTasks(request);

	}

	protected synchronized void cancelTasks(final ServerRequest request) {

		synchronized (ATTR_CANCEL_TASKS) {

			final List<Future<?>> tasks = request.attr(ATTR_CANCEL_TASKS).get();

			if (tasks != null) {
				for (final Future<?> future : tasks) {
					if (!future.isDone()) {
						try {
							future.cancel(true);
						} catch (final Exception e) {
							log.debug("Uncaught exception while cancelling", e);
						}
					}
				}
			}

		}

	}

	protected synchronized void cancelOnAbort(final ServerRequest request,
			final ServerResponse response, final Future<?> future) {

		synchronized (ATTR_CANCEL_TASKS) {

			final RequestAttribute<CancelFutureList> attr = request
					.attr(ATTR_CANCEL_TASKS);
			CancelFutureList tasks = attr.get();

			if (tasks == null) {
				tasks = new CancelFutureList();
				attr.set(tasks);
			}

			tasks.add(future);

		}

	}

	private class CancelFutureList extends ArrayList<Future<?>> {
		private static final long serialVersionUID = 1L;
	}

}
