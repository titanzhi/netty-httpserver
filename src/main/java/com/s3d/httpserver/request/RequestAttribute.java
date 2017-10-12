package com.s3d.httpserver.request;


public class RequestAttribute<T> {

	private T value = null;

	public void set(final T value_) {
		value = value_;
	}

	public T get() {
		return value;
	}

}
