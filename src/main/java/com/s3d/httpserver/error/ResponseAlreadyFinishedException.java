package com.s3d.httpserver.error;

public class ResponseAlreadyFinishedException extends IllegalStateException {
	private static final String CAUSE = "ServerResponse has already finished";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1440476664160311750L;
	
	public ResponseAlreadyFinishedException() {
		super(CAUSE);
	}
}
