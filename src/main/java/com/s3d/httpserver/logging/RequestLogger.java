package com.s3d.httpserver.logging;

import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

/**
 * 记录所有request日志
 * @author sulta
 *
 */
public interface RequestLogger {

	public void access(ServerRequest request, ServerResponse response,
			long duration);


	public void error(ServerRequest request, ServerResponse response,
			Throwable exception);

}
