package com.s3d.httpserver.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import com.s3d.httpserver.request.RequestHandlerBase;
import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 静态文件下载支持
 * 
 * @author sulta
 * 
 */
public abstract class HttpStaticFileHandler extends RequestHandlerBase {

	private static final Logger log = LoggerFactory.getLogger(HttpStaticFileHandler.class);	
	
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";

	public static final int HTTP_CACHE_SECONDS = 60;

	public abstract String getStaticFolderPath();
	
	@Override
	public final void onRequest(ChannelHandlerContext ctx,ServerRequest request,
			ServerResponse response) throws IOException {
		return;
	}
	
	public void onRequest(ChannelHandlerContext ctx, FullHttpRequest request)
			throws Exception {
		if (!request.getDecoderResult().isSuccess()) {
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}

		if (request.getMethod() != HttpMethod.GET) {
			sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}

		final String uri = request.getUri();
		final String path = sanitizeUri(uri);
		if (path == null) {
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}

		File file = new File(path);
		if (file.isHidden() || !file.exists()) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}

		if (file.isDirectory() || !file.isFile()) {
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}

		// Cache Validation
		String ifModifiedSince = request.headers().get(HttpHeaders.Names.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
			SimpleDateFormat dateFormatter = new SimpleDateFormat(
					HTTP_DATE_FORMAT, Locale.US);
			Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

			// Only compare up to the second because the datetime format we send to the client does not have milliseconds
			long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
			long fileLastModifiedSeconds = file.lastModified() / 1000;
			if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
				sendNotModified(ctx);
				return;
			}
		}

		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException ignore) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}
		long fileLength = raf.length();
		
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders.setContentLength(response, fileLength);
		setContentTypeHeader(response, file);
		setDateAndCacheHeaders(response, file);
		if (HttpHeaders.isKeepAlive(request)) {
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		// Write the initial line and the header.
		ctx.write(response);

		// Write the content.
		ChannelFuture lastContentFuture;
		if (ctx.pipeline().get(SslHandler.class) == null) {
			ctx.write(new DefaultFileRegion(raf.getChannel(),
					0, fileLength), ctx.newProgressivePromise());
			// Write the end marker.
			lastContentFuture = ctx
					.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		} else {
			lastContentFuture = ctx.writeAndFlush(new HttpChunkedInput(
					new ChunkedFile(raf, 0, fileLength, 8192)), ctx
					.newProgressivePromise());
			// HttpChunkedInput will write the end marker (LastHttpContent) for us.
		}

		// Decide whether to close the connection or not.
		if (!HttpHeaders.isKeepAlive(request)) {
			// Close the connection when the whole content is written out.
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}

		if (uri.isEmpty() || uri.charAt(0) != '/') {
			return null;
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		//Commented By TangGang  2015.8.7
		//
		/*if (uri.contains(File.separator + '.')
				|| uri.contains('.' + File.separator) || uri.charAt(0) == '.'
				|| uri.charAt(uri.length() - 1) == '.'
				|| INSECURE_URI.matcher(uri).matches()) {
			return null;
		}*/

		// Convert to absolute path.
		String path = getStaticFolderPath();
		if (path == null || path.trim().length() == 0)
			path = SystemPropertyUtil.get("user.dir");
		
		// Added by TangGang 2015.8.7  get the static file
		String fileName = null;
		if (uri.indexOf("?") > -1) {
			fileName = uri.substring(0, uri.indexOf("?") );
		} else {
			fileName = uri;
		}
		
		//return path + uri;
		return path + fileName;
	}
	
	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

	private static void sendError(ChannelHandlerContext ctx,
			HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
				status, Unpooled.copiedBuffer("Failure: " + status + "\r\n",
						CharsetUtil.UTF_8));
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * When file timestamp is the same as what the browser is sending up, send a
	 * "304 Not Modified"
	 * 
	 * @param ctx
	 *            Context
	 */
	private static void sendNotModified(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.NOT_MODIFIED);
		setDateHeader(response);

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * Sets the Date header for the HTTP response
	 * 
	 * @param response
	 *            HTTP response
	 */
	private static void setDateHeader(FullHttpResponse response) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT,
				Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		Calendar time = new GregorianCalendar();
		response.headers().set(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));
	}

	/**
	 * Sets the Date and Cache headers for the HTTP Response
	 * 
	 * @param response
	 *            HTTP response
	 * @param fileToCache
	 *            file to extract content type
	 */
	private static void setDateAndCacheHeaders(HttpResponse response,
			File fileToCache) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT,
				Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		// Date header
		Calendar time = new GregorianCalendar();
		response.headers().set(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
		response.headers().set(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
		response.headers().set(HttpHeaders.Names.CACHE_CONTROL,
				"private, max-age=" + HTTP_CACHE_SECONDS);
		response.headers().set(HttpHeaders.Names.LAST_MODIFIED,
				dateFormatter.format(new Date(fileToCache.lastModified())));
	}

	/**
	 * Sets the content type header for the HTTP Response
	 * 
	 * @param response
	 *            HTTP response
	 * @param file
	 *            file to extract content type
	 */
	private static void setContentTypeHeader(HttpResponse response, File file) {
		try {
			String type = Files.probeContentType(file.toPath());
			if(type!=null) {
				response.headers().set(HttpHeaders.Names.CONTENT_TYPE , type);
				return;
			}
			log.warn("could not guess its content type: {}", file.toPath());
		} catch (IOException e) {
		}
	}
}
