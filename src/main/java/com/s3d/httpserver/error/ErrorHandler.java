package com.s3d.httpserver.error;

import java.io.IOException;

import com.s3d.httpserver.request.ServerRequest;
import com.s3d.httpserver.request.ServerResponse;

public interface ErrorHandler {

	public void onError(final ServerRequest request,
			final ServerResponse response, Throwable cause) throws IOException;

}
