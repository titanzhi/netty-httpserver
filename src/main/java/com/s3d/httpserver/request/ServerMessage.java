package com.s3d.httpserver.request;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpVersion;


public interface ServerMessage extends HttpMessage {

	@Override
	public HttpHeaders headers();

	@Override
	public HttpVersion getProtocolVersion();

}
