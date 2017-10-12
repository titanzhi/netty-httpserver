package com.s3d.httpserver.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import com.s3d.httpserver.auth.AuthorizationHandler;
import com.s3d.httpserver.error.DefaultErrorHandler;
import com.s3d.httpserver.error.ErrorHandler;
import com.s3d.httpserver.logging.NullRequestLogger;
import com.s3d.httpserver.logging.RequestLogger;
import com.s3d.httpserver.request.RequestHandler;

import java.net.SocketAddress;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP server config
 * @author sulta
 *
 */
public class HttpServerConfig {
	private static final Logger log = LoggerFactory.getLogger(HttpServerConfig.class);
	private final Map<String, AuthorizationHandler> authorizationHandlers =
			new ConcurrentHashMap<String, AuthorizationHandler>();
	
	private final Map<String, RequestHandler> handlers =
			new ConcurrentSkipListMap<String, RequestHandler>(
					new ReverseLengthComparator());
	private SSLEngine engine;
	private SocketAddress address;
	private int maxConnections = -1;
	private int maxRequestSize = 1024 * 1024; //1Mb
	private int idleTimeSeconds = 60 * 30;
	private ErrorHandler errorHandler = new DefaultErrorHandler();
	private RequestLogger requestLogger = new NullRequestLogger();
	private EventLoopGroup parentGroup = null;
	private EventLoopGroup childGroup = null;
	private Class<? extends ServerChannel> socketChannelClass = null;
	
	public HttpServerConfig socketChannelClass(final Class<? extends ServerChannel> socketChannelClass_) {
		socketChannelClass = socketChannelClass_;
		return this;
	}
	
	public HttpServerConfig sslEngine(SSLEngine engine) {
		this.engine = engine;
		return this;
	}
	
	public HttpServerConfig address(final SocketAddress address_) {
		address = address_;
		return this;
	}

	public HttpServerConfig maxConnections(final int max) {
		maxConnections = max;
		return this;
	}
	
	public HttpServerConfig IdleTimeSeconds(final int s) {
		idleTimeSeconds = s;
		return this;
	}

	public HttpServerConfig maxRequestSize(final int max) {
		maxRequestSize = max;
		return this;
	}

	public HttpServerConfig errorHandler(final ErrorHandler handler) {
		errorHandler = handler;
		return this;
	}

	public HttpServerConfig logger(final RequestLogger logger_) {
		requestLogger = logger_;
		return this;
	}

	public HttpServerConfig parentGroup(final EventLoopGroup group) {
		parentGroup = group;
		return this;
	}

	public HttpServerConfig childGroup(final EventLoopGroup group) {
		childGroup = group;
		return this;
	}
	
	public HttpServerConfig requestHandler(final String prefix,
			final RequestHandler handler) {
		Object oldHandler = this.handlers.get(prefix);
		if (oldHandler != null) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + handler +
					"' to " + prefix + ": There is already  mapped.");
		}			
		handlers.put(prefix, handler);
		if(log.isInfoEnabled()){
			log.info("Mapped \"" + prefix + "\" onto " + handler.getClass().getName());
		}
		return this;
	}

	public HttpServerConfig authorizationHandler(
			final AuthorizationHandler authHandler) {
		authorizationHandlers.put(authHandler.getMethod(),
				authHandler);
		return this;
	}
	
	public SSLEngine getSSLEngine() {
		return engine;
	}

	public  Class<? extends ServerChannel> socketChannelClass(){
		return socketChannelClass;
	}
	public SocketAddress address() {
		return address;
	}
	
	public int IdleTimeSeconds() {
		return idleTimeSeconds;
	}

	public int maxConnections() {
		return maxConnections;
	}

	public int maxRequestSize() {
		return maxRequestSize;
	}

	public ErrorHandler errorHandler() {
		return errorHandler;
	}

	public RequestLogger logger() {
		return requestLogger;
	}

	public EventLoopGroup parentGroup() {
		return parentGroup;
	}

	public EventLoopGroup childGroup() {
		return childGroup;
	}
	
	public RequestHandler getRequestMapping(final String uri) {
		for (final Map.Entry<String, RequestHandler> entry : handlers.entrySet()) {
			if (uri.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	public Object removeRequestHandler(final String path) {
		if(log.isInfoEnabled()){
			log.info("removed Mapping on  \"" + path + "\"");
		}
		return handlers.remove(path);
	}

	public boolean hasAuthorizationHandlers() {
		return !authorizationHandlers.isEmpty();
	}

	public AuthorizationHandler getAuthorizationHandler(final String method) {
		return authorizationHandlers.get(method);
	}
	
	private class ReverseLengthComparator implements Comparator<String> {
		@Override
		public int compare(final String o1, final String o2) {

			final int l1 = o1.length();
			final int l2 = o2.length();

			if (l1 < l2) {
				return 1;
			} else if (l2 < l1) {
				return -1;
			} else {
				return o1.compareTo(o2);
			}

		}

	}

}
