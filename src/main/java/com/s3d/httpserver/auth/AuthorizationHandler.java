package com.s3d.httpserver.auth;

import io.netty.channel.ChannelHandlerContext;
import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

import java.io.IOException;

public interface AuthorizationHandler {

	/**
	 * 认证方式
	 */
	public String getMethod();
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public UserSessionDetail authenticate(ChannelHandlerContext ctx,ServerRequest request, ServerResponse response)
			throws IOException;
}
