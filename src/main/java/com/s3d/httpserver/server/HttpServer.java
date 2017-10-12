package com.s3d.httpserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High performance HTTP server.
 */
public class HttpServer {
	private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
	
	private Channel serverChannel;
	private HttpServerConfig config;
	private HttpRequestChannelHandler channelHandler;
	private ConnectionTracker clientTracker;

	private final ChannelGroup channelGroup = new DefaultChannelGroup(
			GlobalEventExecutor.INSTANCE);

	public HttpServer configure(final HttpServerConfig config_) {

		config = config_;
		channelHandler = new HttpRequestChannelHandler(config);
		clientTracker = new ConnectionTracker(config.maxConnections());

		return this;

	}

	public ChannelFuture listen() throws Exception {

		if (config == null) {
			throw new IllegalStateException("Server has not been configured");
		}

		if (serverChannel != null) {
			throw new IllegalStateException("Server is already running.");
		}
		
		final ChannelFuture future = new ServerBootstrap() //
				.group(config.parentGroup(), config.childGroup()) //
				.channel(config.socketChannelClass()) //
				.localAddress(config.address()) //
				.childHandler(new HttpServerChannelInitializer(config.getSSLEngine())) //
				.option(ChannelOption.SO_REUSEADDR, true) //
				.option(ChannelOption.SO_SNDBUF, 262144) //
				.option(ChannelOption.SO_RCVBUF, 262144) //
				.bind();

		serverChannel = future.channel();

		return future;

	}


	public ChannelFuture shutdown() {

		if (serverChannel == null) {
			throw new IllegalStateException("Server is not running.");
		}

		final ChannelFuture future = serverChannel.close();
		serverChannel = null;

		return future;

	}

	public ChannelFuture shutdownFuture() {
		return serverChannel.closeFuture();
	}

	public ChannelGroupFuture kill() {

		if (serverChannel == null) {
			throw new IllegalStateException("Server is not running.");
		}

		channelGroup.add(serverChannel);
		final ChannelGroupFuture future = channelGroup.close();
		channelGroup.remove(serverChannel);
		serverChannel = null;

		return future;

	}

	public boolean isRunning() {
		return serverChannel != null;
	}

	public HttpServerConfig config() {
		return config;
	}

	private class HttpServerChannelInitializer extends
			ChannelInitializer<SocketChannel> {
		private final SSLEngine engine;
		
		public HttpServerChannelInitializer(SSLEngine engine) {
			this.engine = engine;
		}
		
		@Override
		public void initChannel(final SocketChannel ch) throws Exception {

			final ChannelPipeline pipeline = ch.pipeline();
			
			if(this.engine!=null){
				pipeline.addLast("ssl", new SslHandler(engine));
			}
			pipeline.addLast(new HttpResponseEncoder(), //
					//new HttpContentCompressor(),
					new ChunkedWriteHandler(), //
					clientTracker, //
					new HttpRequestDecoder(), //
					new HttpObjectAggregator(config.maxRequestSize()), //
					new IdleStateHandler(0, 0, config.IdleTimeSeconds()), //
					// new MessageLoggingHandler(LogLevel.INFO), //
					channelHandler);

		}

	}

	@Sharable
	private class ConnectionTracker extends ChannelInboundHandlerAdapter {

		private int maxConnections = -1;

		public ConnectionTracker(final int connections) {
			maxConnections = connections;
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			if (this == ctx.pipeline().last()) { 
				log.warn( 
	                    "EXCEPTION, please implement " + getClass().getName() + 
	                    ".exceptionCaught() for proper handling.", cause.getCause()); 
	        } 
			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void channelActive(final ChannelHandlerContext context) {

			if (maxConnections > -1 && channelGroup.size() >= maxConnections) {

				final ByteBuf content = Unpooled.buffer();

				content.writeBytes("503 Service Unavailable - Server Too Busy"
						.getBytes());

				final FullHttpResponse response =
						new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
								HttpResponseStatus.SERVICE_UNAVAILABLE);

				response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
						content.readableBytes());

				response.content().writeBytes(content);

				context.writeAndFlush(response).addListener(
						ChannelFutureListener.CLOSE);

				return;

			}

			channelGroup.add(context.channel());
			context.fireChannelActive();

		}

		@Override
		public void channelInactive(final ChannelHandlerContext context) {

			channelGroup.remove(context.channel());
			context.fireChannelInactive();

		}
	}
}
