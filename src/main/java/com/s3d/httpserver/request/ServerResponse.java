package com.s3d.httpserver.request;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

import com.s3d.httpserver.error.ResponseAlreadyFinishedException;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 扩展自netty {@link HttpResponse}.
 * @author sulta
 *
 */
public interface ServerResponse extends ServerMessage, HttpResponse {

	@Override
	HttpResponseStatus getStatus();

	@Override
	ServerResponse setStatus(HttpResponseStatus status);

	@Override
	ServerResponse setProtocolVersion(HttpVersion version);

	boolean isChunkedEncoding();

	void setChunkedEncoding(final boolean chunked);

	void setCookie(Cookie cookie);

	void setCookie(String name, String value);
		
	/**
	 * default is UTF-8
	 * @param charSet
	 */
	void setCharacterEncoding(String charSet);

	/**
	 * default is UTF-8
	 * @return
	 */
	Charset getCharacterEncoding();

	/**
	 * Set the content-length for this response. This is set automatically by
	 * default if chunked transfer encoding is not active.
	 */
	void setContentLength(int length);

	/**
	 * Set the response content MIME type.
	 */
	void setContentType(String mimeType);

	/**
	 * Send a URL redirect to the client.
	 */
	void sendRedirect(String location);

	/**
	 * Get the raw output stream for writing to the client. Note that unless
	 * chunked transfer encoding is turned on, all output will still be
	 * buffered.
	 */
	OutputStream getOutputStream();

	/**
	 * Get a writer that writes data directly to the client. Note that unless
	 * chunked transfer encoding is turned on, all output will still be
	 * buffered.
	 */
	Writer getWriter();

	/**
	 * Write a string to the client.
	 */
	void write(String data) throws IOException;

	/**
	 * Write a byte stream to the client.
	 */
	void write(byte[] data) throws IOException;

	/**
	 * Write a byte stream to the client.
	 */
	void write(byte[] data, int offset, int length) throws IOException;

	/**
	 * Get the number of bytes written to the client for this response.
	 */
	long writtenBytes();

	/**
	 * Flush the output buffers. Buffers are flushed automatically, and this
	 * should not usually be necessary.
	 */
	void flush() throws IOException;
	
	/**
	 * 挂起response,已异步方式resume
	 * @param resumeFuture
	 */
	void suspend(); 

	/**
	 * 是否已挂起
	 */
	boolean isSuspended();
		
	/**
	 * Mark this response as finished, and release any resources associated with
	 * it.
	 */
	ChannelFuture finish() throws IOException , ResponseAlreadyFinishedException;

	/**
	 * Check if this response has been finished.
	 */
	boolean isFinished();

}
