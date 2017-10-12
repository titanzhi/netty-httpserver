package com.s3d.httpserver.logging;

import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

/**
 * 记录所有request日志
 * @author sulta
 *
 */
public class NullRequestLogger implements RequestLogger {

	@Override
	public void access(final ServerRequest request,
			final ServerResponse response, final long duration) {

	}

	@Override
	public void error(final ServerRequest request,
			final ServerResponse response, final Throwable exception) {

	}
}
