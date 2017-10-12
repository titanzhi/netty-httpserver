package com.s3d.httpserver.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.s3d.httpserver.error.ResponseAlreadyFinishedException;
import com.s3d.httpserver.logging.RequestLogger;
import com.s3d.httpserver.request.RequestHandler;
import com.s3d.httpserver.request.ServerResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * http response
 */
public class PooledServerResponse extends DefaultFullHttpResponse implements
		ServerResponse {
	
	public static final AttributeKey<PooledServerResponse> ATTR_RESPONSE =
			AttributeKey.<PooledServerResponse> valueOf("response");
	
	private static final Logger log = LoggerFactory
			.getLogger(PooledServerResponse.class);

	final ServerMessagePool pool;

	private final Collection<Cookie> cookies = new HashSet<Cookie>();

	//private HttpRequestChannelHandler channelHandler;
	private ChannelHandlerContext context;
	private RequestHandler handler;
	private PooledServerRequest request;

	private OutputStream out;
	private Writer writer;

	private Charset charSet = CharsetUtil.UTF_8;
	
	private boolean suspended = false;
	private boolean started = false;
	private volatile boolean finished = false;
		
	private long requestTime = 0;
	private RequestLogger logger;

	public PooledServerResponse(final ServerMessagePool pool_) {
		super(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		pool = pool_;
	}
	
	void init(final ChannelHandlerContext context_,
			//final HttpRequestChannelHandler channelHandler_,
			final long requestTime,
			final RequestHandler handler_, final PooledServerRequest request_,
			final RequestLogger logger_) {

		if (finished) {
			headers().clear();
			content().clear();
			cookies.clear();
			setStatus(HttpResponseStatus.OK);
		}

		retain();

		context = context_;
		//channelHandler = channelHandler_;
		handler = handler_;
		request = request_;
		logger = logger_;

		charSet = CharsetUtil.UTF_8;

		finished = false;
		suspended = false;
		started = false;

		out = new ByteBufOutputStream(content());
		writer = new OutputStreamWriter(out, charSet);

		this.requestTime = requestTime;
	}

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public void setCookie(final Cookie cookie) {
		cookies.add(cookie);
	}

	@Override
	public void setCookie(final String name, final String value) {
		cookies.add(new DefaultCookie(name, value));
	}

	@Override
	public void sendRedirect(final String location) {
		headers().set(HttpHeaders.Names.LOCATION, location);
	}

	@Override
	public PooledServerResponse setProtocolVersion(final HttpVersion version) {
		super.setProtocolVersion(version);
		return this;
	}

	@Override
	public PooledServerResponse setStatus(final HttpResponseStatus status) {
		super.setStatus(status);
		return this;
	}

	@Override
	public void setCharacterEncoding(final String charSet_) {
		charSet = Charset.forName(charSet_);
		writer = new OutputStreamWriter(out, charSet);
	}

	@Override
	public Charset getCharacterEncoding() {
		return charSet;
	}

	@Override
	public void setContentLength(final int length) {
		HttpHeaders.setContentLength(this, length);
	}

	@Override
	public void setContentType(final String mimeType) {
		headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeType);
	}

	@Override
	public boolean isChunkedEncoding() {
		return HttpHeaders.isTransferEncodingChunked(this);
	}

	@Override
	public void setChunkedEncoding(final boolean chunked) {

		if (chunked != isChunkedEncoding()) {

			if (chunked) {

				HttpHeaders.setTransferEncodingChunked(this);
				out = new HttpChunkOutputStream(context);
				writer = new OutputStreamWriter(out, charSet);

			} else {

				HttpHeaders.removeTransferEncodingChunked(this);
				out = new ByteBufOutputStream(content());
				writer = new OutputStreamWriter(out, charSet);

			}
		}
	}

	@Override
	public void write(final String data) throws IOException {
		if (data != null) {
			write(data.getBytes());
		}
	}

	@Override
	public void write(final byte[] data) throws IOException {

		checkFinished();
		if(data != null) out.write(data);
		out.flush();

	}

	@Override
	public void write(final byte[] data, final int offset, final int length)
			throws IOException {

		checkFinished();

		out.write(data, offset, length);
		out.flush();

	}

	@Override
	public long writtenBytes() {
		if (out instanceof ByteBufOutputStream) {
			return ((ByteBufOutputStream) out).writtenBytes();
		} else if (out instanceof HttpChunkOutputStream) {
			return ((HttpChunkOutputStream) out).writtenBytes();
		}
		return 0;
	}

	@Override
	public void suspend(){
		suspended  = true;
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}

	private ChannelFuture startResponse() {

		checkFinished();

		if (started) {
			throw new IllegalStateException("Response already started");
		}

		// Set headers
		try {
			headers().set(HttpHeaders.Names.SET_COOKIE,
					ServerCookieEncoder.encode(cookies));
		} catch (Exception e) {
			log.error("Set Cookies Failed", e);
		}

		if (HttpHeaders.isKeepAlive(request)) {
			headers().set(HttpHeaders.Names.CONNECTION,
					HttpHeaders.Values.KEEP_ALIVE);
		}
		
		started = true;
		
		if (!isChunkedEncoding()) {
			setContentLength(content().readableBytes());
			return context.writeAndFlush(this);
		} else {
			final DefaultHttpResponse resp = new DefaultHttpResponse(getProtocolVersion(), getStatus());
			resp.headers().add(headers());
			HttpHeaders.setTransferEncodingChunked(resp);
			return context.writeAndFlush(resp);
		}
	}

	@Override
	public ChannelFuture finish() throws IOException ,ResponseAlreadyFinishedException{
		checkFinished();
		synchronized (this) {		
			ChannelFuture writeFuture = null;

			if (context.channel().isOpen()) {
				if (isChunkedEncoding()) {
					if (!started) {
						log.debug("Warning, empty response");
						startResponse();
					}

					writeFuture =
							context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

				} else {
					writeFuture = startResponse();
				}
			}
			
			if (writeFuture != null) {
				if(!HttpHeaders.isKeepAlive(request)){
					writeFuture.addListener(ChannelFutureListener.CLOSE);
				}
				writeFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
					@Override
					public void operationComplete(Future<? super Void> future)
							throws Exception {
						freeHandlers(context);
					}
				});
			} else {
				freeHandlers(context);
			}
			
			// Record to access log
			logger.access(request, this, System.currentTimeMillis() - requestTime);
			
			
			// Keep alive, need to tell channel handler it can return us to the pool
			//if (HttpHeaders.isKeepAlive(request)) {
				//移到listener去了 // freeHandlers(context); //will finish too
			//}
			return writeFuture;
		}	
	}

	private void checkFinished() {
		if (finished) {
			throw new ResponseAlreadyFinishedException();
		}
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
		out.flush();
	}
	
	//正常结束, 出现异常 , 连接中断都会调用 free
	public void free() {
		if(!finished){
			finished = true;
			suspended = false;
			synchronized (context) {
				PooledServerResponse response = context.attr(ATTR_RESPONSE).getAndRemove();
				if(this == response){
					try {
						final RequestHandler handler = response.handler();
						if (handler != null) {
							handler.onComplete(response.request(), response);
						}
					} finally {
						request.release();
						pool.makeAvailable(request);						
						pool.makeAvailable(this);
					}
				}else{
					if(context.attr(ATTR_RESPONSE).setIfAbsent(response) != null){//还回去 但是如果没有还成功 说明context又有新的request进来了 (channelRead0) 那这个response 就强制作废了
						response.dispose();
					}
				}
			}
		}
	}
	
	private void dispose(){
		try {
			if(log.isWarnEnabled()){
				log.warn("@@ dispose a response...");
			}
			finished = true;
			suspended = false;
		} finally {
			pool.makeAvailable(this);
		}
	}
	
	PooledServerRequest request() {
		return request;
	}

	RequestHandler handler() {
		return handler;
	}
		
	public RequestHandler getRequestHandler() {
		return handler;
	}
	
	private static void freeHandlers(ChannelHandlerContext ctx) {
		PooledServerResponse response = ctx.attr(PooledServerResponse.ATTR_RESPONSE).get();
		if (response != null) {
			response.free();
		}
	}
	
	/**
	 * Writes messages as HttpChunk objects to the client.
	 */
	private class HttpChunkOutputStream extends OutputStream {

		private final ByteBuf content = Unpooled.buffer();
		private final ChannelHandlerContext context;
		private long writtenBytes = 0;

		HttpChunkOutputStream(final ChannelHandlerContext context_) {
			context = context_;
		}

		/**
		 * Adds a single byte to the output buffer.
		 */
		@Override
		public void write(final int b) throws IOException {
			content.writeByte(b);
			writtenBytes++;
		}

		public long writtenBytes() {
			return writtenBytes;
		}

		@Override
		public void flush() {

			if (!started) {
				startResponse();
			}

			final HttpContent chunk = new DefaultHttpContent(content);
			context.writeAndFlush(chunk);
		}

	}
}
