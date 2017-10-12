package com.s3d.httpserver.request;



/**
 * RequestHandler 基类
 * @author sulta
 *
 */
public abstract class RequestHandlerBase implements RequestHandler {
		
	@Override
	public void onAbort(final ServerRequest request,
			final ServerResponse response) {
	}

	@Override
	public void onException(final ServerRequest request,
			final ServerResponse response, final Throwable exception) {
	}

	@Override
	public void onComplete(final ServerRequest request,
			final ServerResponse response) {
	}
}
