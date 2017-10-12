package com.s3d.httpserver.error;

import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author sulta
 *
 */
public class DefaultErrorHandler implements ErrorHandler {

	private final static Logger log = LoggerFactory.getLogger(DefaultErrorHandler.class);

	@Override
	public void onError(final ServerRequest request,
			final ServerResponse response, final Throwable cause)
			throws IOException {

		if (cause != null) {
			log.warn("Uncaught exception thrown in request", cause);
			response.write(cause.getClass()
					+ " was thrown while processing this request.  See logs for more details.");

		} else {
			
			response.write("Request could not be processed.  Status code: "
					+ response.getStatus());

		}

	}

}
