package com.s3d.httpserver.logging;

import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author sulta
 *
 */
public class Log4jRequestLogger implements RequestLogger {
	
	private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS");
	private static final Logger errorLogger = LoggerFactory.getLogger("ERROR");
	@Override
	public void access(ServerRequest request, ServerResponse response,
			long duration) {
		accessLogger.info("requeset:{} , dration: {}" , request.getHandlerUri() , duration);
	}

	@Override
	public void error(ServerRequest request, ServerResponse response,
			Throwable exception) {
		errorLogger.info("requeset: "+ request.getHandlerUri() , exception);
	}

}
