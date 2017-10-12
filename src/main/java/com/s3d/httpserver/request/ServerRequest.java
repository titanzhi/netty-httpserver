package com.s3d.httpserver.request;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;


/**
 * 扩展自netty {@link HttpRequest}.
 * @author sulta
 *
 */
public interface ServerRequest extends ServerMessage, HttpRequest {

	@Override
	HttpMethod getMethod();

	@Override
	ServerRequest setProtocolVersion(HttpVersion version);
	
	/**
     * Set the {@link HttpMethod} of this {@link HttpRequest}.
     */
	@Override
	ServerRequest setMethod(HttpMethod method);
	
	/**
     *  Set the requested URI (or alternatively, path)
     */
	@Override
	ServerRequest setUri(String uri);

	boolean isChunkedEncoding();

	/**
	 * The request URI
	 */
	@Override
	public String getUri();

	/**
	 * The request query string.
	 */
	String getQueryString();

	/**
	 * The base URI for this request handler.
	 */
	String getHandlerUri();

	/**
	 * The extra path info after the base URI (see getHandlerUri()).
	 */
	String getPathInfo();

	/**
	 * The protocol scheme (http, https, etc)
	 */
	String getScheme();

	/**
	 * 获取当前登陆的用户名
	 * @return
	 */
	String getRemoteUser();

	/**
	 * The server hostname of this request.
	 */
	String getServerHost();

	/**
	 * The local IP address of the server.
	 */
	InetSocketAddress getServerAddress();

	/**
	 * The remote client's IP address.
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * True if connection is encrypted (SSL).
	 */
	boolean isSecure();

	/* Request parameters */

	/**
	 * A map of parsed query string parameters.
	 */
	Map<String, List<String>> getParameters();

	/**
	 * Get a single query parameter by name.
	 */
	String getParameter(String name);

	/**
	 * Get a multi-value query parameter by name.
	 */
	List<String> getParameterList(String name);

	/**
	 * Get all active cookies for this request.
	 */
	Map<String, Cookie> getCookies();

	/**
	 * Get an active cookie by name.
	 */
	Cookie getCookie(String name);

	/* Request content */

	/**
	 * The character encoding for this request.
	 */
	Charset getCharacterEncoding();

	/**
	 * The request content buffer.
	 * 
	 * @return
	 */
	ByteBuf getContent();

	/**
	 * The request content MIME type.
	 * 
	 * @return
	 */
	String getContentType();

	/**
	 * The length of the request data in bytes.
	 */
	long getContentLength();

	/**
	 * Get a raw input stream for reading the request body.
	 */
	InputStream getInputStream();

	/**
	 * Get a buffered reader for reading the request body, using default
	 * character encoding.
	 */
	BufferedReader getReader();

	/* Request attributes */

	/**
	 * Get a request-specific attribute. Useful for sharing data between
	 * handlers and filters during a single request.
	 */
	<T> RequestAttribute<T> attr(RequestAttributeKey<T> key);

}
