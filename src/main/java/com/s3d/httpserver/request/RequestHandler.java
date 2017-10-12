package com.s3d.httpserver.request;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author sulta
 *
 */
public interface RequestHandler {
	
	/**
	 * 客户端有新的请求时触发
	 * @param ctx  
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	void onRequest(ChannelHandlerContext ctx,ServerRequest request, ServerResponse response)
			throws IOException;

	/**
	 * 
	 * @param request
	 * @param response
	 * @param exception
	 */
	void onException(ServerRequest request, ServerResponse response,
			Throwable exception);

	/**
	 * 客户端中断了请求
	 * @param request
	 * @param response
	 */
	void onAbort(ServerRequest request, ServerResponse response);
	
	/**
	 * 客户端请求完成（正常完成，出错，中断）时触发
	 * @param request
	 * @param response
	 */
	void onComplete(ServerRequest request, ServerResponse response);
}
