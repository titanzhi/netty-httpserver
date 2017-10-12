package com.s3d.httpserver.error;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 超出配置的连接数之后,服务器会抛出{@code ServerTooBusyException}
 * @author sulta
 *
 */
public class ServerTooBusyException extends ServerException {

	private static final long serialVersionUID = 1L;

	public ServerTooBusyException() {
		super(HttpResponseStatus.SERVICE_UNAVAILABLE);
	}

	public ServerTooBusyException(final String message) {
		super(HttpResponseStatus.SERVICE_UNAVAILABLE, message);
	}

	public ServerTooBusyException(final String message, final Throwable cause) {
		super(HttpResponseStatus.SERVICE_UNAVAILABLE, message, cause);
	}

	public ServerTooBusyException(final Throwable cause) {
		super(HttpResponseStatus.SERVICE_UNAVAILABLE, cause);
	}

}
