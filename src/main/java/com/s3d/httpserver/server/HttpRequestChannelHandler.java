package com.s3d.httpserver.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import com.s3d.httpserver.auth.AuthorizationHandler;
import com.s3d.httpserver.auth.UserSessionDetail;
import com.s3d.httpserver.error.ResponseAlreadyFinishedException;
import com.s3d.httpserver.error.ServerException;
import com.s3d.httpserver.error.ServerTooBusyException;
import com.s3d.httpserver.handlers.HttpStaticFileHandler;
import com.s3d.httpserver.request.RequestHandler;
import com.s3d.httpserver.util.StringUtils;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * netty channel handler
 * 将request请求路由到自定义的RequestHandler
 * @author sulta
 *
 */
@Sharable
public class HttpRequestChannelHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {
	
	private static final Logger log = LoggerFactory.getLogger(HttpRequestChannelHandler.class);	
	
	
	public static final AttributeKey<UserSessionDetail> ATTR_SESS =
			AttributeKey.<UserSessionDetail> valueOf("usersession");
	
	private final HttpServerConfig config;
	private final ServerMessagePool messagePool;
	private final EventExecutorGroup executor;
	
	public HttpRequestChannelHandler(final HttpServerConfig config_) {
		super();
		config = config_;
		messagePool = new ServerMessagePool(config.maxConnections());
		messagePool.init();
		this.executor = config.childGroup();
	}

	@Override
	public void channelRead0(final ChannelHandlerContext ctx,
			final FullHttpRequest msg) throws Exception {
		final RequestHandler handler =
				config.getRequestMapping(msg.getUri());
		
		if (handler != null) {
			if (handler instanceof HttpStaticFileHandler) {
				HttpStaticFileHandler staticFileHandler = (HttpStaticFileHandler) handler;
				staticFileHandler.onRequest(ctx , msg);
				return;
			}
		}
		String relativePath = msg.getUri();

		//if (handler != null) {
			//relativePath = relativePath.substring(handler.path().length());
		//}

		final PooledServerRequest request = messagePool.getRequest();

		if (request == null) {
			sendServerError(ctx, new ServerTooBusyException(
					"Maximum concurrent connections reached"));
			return;
		}
		
		request.init(ctx.channel(), msg, relativePath);
		final PooledServerResponse response = messagePool.getResponse();
		final PooledServerResponse lastResponse = ctx.attr(PooledServerResponse.ATTR_RESPONSE).setIfAbsent(response);
		
		final long traceTime = System.currentTimeMillis();
		
		response.init(ctx,
				traceTime,
				//this, 
				handler, request, config.logger());
		
		if (handler == null) {
			// 404
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			config.errorHandler().onError(request, response, null);
			try {
				response.finish();
			} catch (IOException e) {
				log.error("error on finishing response", e);
			}
			return;
		}
		
		if(log.isTraceEnabled()){
			log.trace("handle:{}" , new Object[]{
					request.getHandlerUri()
			});
		}
		
		try {
			if(lastResponse != null){//如果有lastResponse存在,说明之前的请求没有完成(出错或中断) 
				lastResponse.free();
			}
			// 401
			if (response.getStatus() == HttpResponseStatus.UNAUTHORIZED ) {
				config.errorHandler().onError(request, response, null);
			} else {
				handler.onRequest(ctx ,request, response);
			}
		} catch (final Throwable t) {
			//500
			response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			try {
				config.errorHandler().onError(request, response, t);
			} catch (final Throwable t2) {
				try {
					response.write(t.getClass()
							+ " was thrown while processing this request.  Additionally, "
							+ t2.getClass()
							+ " was thrown while handling this exception.");
				} catch (IOException e) {
					log.error("error on write to response", e);
				}
			}
			
			config.logger().error(request, response, t);

			if (!response.isFinished()) {
				try {
					response.finish();
				} catch (IOException e) {
					log.error("error on finishing response", e);
				}
			}
		} finally {
			if (!response.isFinished() && !response.isSuspended()) {
				try {
					response.finish();
				} catch (IOException e) {
					log.error("error on finishing response", e);
				} catch (ResponseAlreadyFinishedException e){
					//ignore 
				}
			}
		}
		
	}

	private void sendServerError(final ChannelHandlerContext ctx,
			final ServerException cause) throws Exception {

		if (ctx.channel().isActive()) {

			final ByteBuf content = Unpooled.buffer();

			content.writeBytes((cause.getStatus().code() + " "
					+ cause.getStatus().reasonPhrase() + " - " + cause
					.getMessage()).getBytes());

			final FullHttpResponse response =
					new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							cause.getStatus());

			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
					content.readableBytes());

			response.content().writeBytes(content);

			ctx.writeAndFlush(response)
					.addListener(ChannelFutureListener.CLOSE);

		}

	}
	
	private void closeResponse(PooledServerResponse response) {
		if (!response.isFinished()) {
			final RequestHandler handler = response.handler();
			
			if (handler != null) {
				handler.onAbort(response.request(), response);
			}
		}
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) {

		final PooledServerResponse response = ctx.attr(PooledServerResponse.ATTR_RESPONSE).get();

		if (response != null) {
			try {
				closeResponse(response);
			} finally {
				response.free();
			}
		}
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx,
			final Throwable exception) throws Exception {
		
		final PooledServerResponse response = ctx.attr(PooledServerResponse.ATTR_RESPONSE).get();

		if (response != null) {
			try {
				try {
					if (!response.isFinished()) {
						if (exception.getCause() instanceof TooLongFrameException) {
							response.setStatus(HttpResponseStatus.BAD_REQUEST);
				        }else{
				        	if (exception.getCause() instanceof IOException) {
					        	//TODO 远程主机强迫... / 连接被重置  .....
				        		log.error("io error:",exception.getCause());
				        	}else{
				        		response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
								config.errorHandler().onError(response.request(),
										response, exception);
				        	}
				        }
						
						final RequestHandler handler = response.handler();

						if (handler != null) {
							handler.onException(response.request(), response,
									exception);
						}
					}
				} finally {
					config.logger().error(response.request(), response,
							exception);
				}
			} finally {
				response.free();
			}
		}else{
			sendServerError(ctx, new ServerException(HttpResponseStatus.INTERNAL_SERVER_ERROR));
		}
	}

}
