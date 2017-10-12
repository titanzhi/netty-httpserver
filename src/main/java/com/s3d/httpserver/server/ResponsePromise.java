package com.s3d.httpserver.server;

import java.util.concurrent.Future;


public interface ResponsePromise<V> extends Future<V>{
	
	void setResumeId(String resumeId);
	
	String getErrorMessage();
	
	byte[] getResponseContent();
}	
