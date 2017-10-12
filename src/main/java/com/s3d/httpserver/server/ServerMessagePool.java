package com.s3d.httpserver.server;

import com.s3d.httpserver.util.ObjectPool;

import java.util.concurrent.Callable;

/**
 * HTTP request/response 可重用对象池
 */
public class ServerMessagePool {

	private final ObjectPool<PooledServerRequest> requestPool;
	private final ObjectPool<PooledServerResponse> responsePool;
	
	public ServerMessagePool(final int maxObjects_) {
		requestPool =
				new ObjectPool<PooledServerRequest>(maxObjects_,
						new Callable<PooledServerRequest>() {
							@Override
							public PooledServerRequest call() throws Exception {
								return new PooledServerRequest();
							}
						});

		responsePool =
				new ObjectPool<PooledServerResponse>(maxObjects_,
						new Callable<PooledServerResponse>() {
							@Override
							public PooledServerResponse call() throws Exception {
								return new PooledServerResponse(
										ServerMessagePool.this);
							}
						});
		
//		int max = maxObjects_==-1? 10000 :maxObjects_;
//		
//		List<PooledServerResponse> buffer = new ArrayList<PooledServerResponse>(max);
//		for (int i = 0; i < max; i++) {
//			buffer.add(responsePool.poll());
//		}
//		for (PooledServerResponse pooledServerResponse : buffer) {
//			responsePool.give(pooledServerResponse);
//		}
//		buffer.clear();
	}
	

	public void init(){
		
	}
	
	public PooledServerRequest getRequest() {
		return requestPool.poll();
	}

	public PooledServerResponse getResponse() {
		return responsePool.poll();
	}

	void makeAvailable(final PooledServerRequest request) {
		requestPool.give(request);
	}

	void makeAvailable(final PooledServerResponse response) {
		responsePool.give(response);
	}
	
}
