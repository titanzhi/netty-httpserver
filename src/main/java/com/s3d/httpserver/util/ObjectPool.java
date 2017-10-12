package com.s3d.httpserver.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pool of reusable objects which creates new instances on demand.
 * 
 * @param <T>
 *            The poolable object type
 */
public class ObjectPool<T> {

	// TODO replace with something more efficient
	/* The backing queue */
	private final BlockingQueue<T> objectPool;

	/* 用户定义的回调方法,用于Object的生成 */
	private final Callable<T> objectCreator;

	/* 已创建的object计算器 */
	private final AtomicInteger created = new AtomicInteger(0);

	/* 池内object上限 */
	private final int maxObjects;

	/**
	 * 无数量限制的构造函数
	 * @param creator_
	 */
	public ObjectPool(final Callable<T> creator_) {
		this(-1, creator_);
	}

	/**
	 * 有数量限制的构造函数
	 * @param maxObjects_
	 * @param creator_
	 */
	public ObjectPool(final int maxObjects_, final Callable<T> creator_) {
		maxObjects = maxObjects_;
		objectCreator = creator_;
		if (maxObjects_ == -1) {
			objectPool = new LinkedBlockingQueue<T>();
		} else {
			objectPool = new ArrayBlockingQueue<T>(maxObjects);
		}
	}

	/**
	 * 弹出一个object
	 * 
	 * @return 
	 */
	public T poll() {
		final T obj = getOrCreate();
		if (obj != null) {
			return obj;
		}
		return objectPool.poll();
	}

	/**
	 * 阻塞式弹出,在指定的时间超时未返回则抛出 InterruptedException
	 * @param timeout
	 * @param units
	 * @return
	 * @throws InterruptedException
	 */
	public T poll(final long timeout, final TimeUnit units)
			throws InterruptedException {
		final T obj = getOrCreate();
		if (obj != null) {
			return obj;
		}
		return objectPool.poll(timeout, units);
	}

	/**
	 * 阻塞式弹出
	 * @return
	 * @throws InterruptedException
	 */
	public T take() throws InterruptedException {
		final T obj = getOrCreate();
		if (obj != null) {
			return obj;
		}
		return objectPool.take();
	}

	/**
	 * 按需生产或者从pool中获取现有的object
	 * @return
	 */
	protected T getOrCreate() {

		// First just try to pull an object off the queue
		final T instance = objectPool.poll();

		if (instance == null) {
			if(maxObjects == -1){
				try {
					return objectCreator.call();
				} catch (final Exception e) {
					throw new RuntimeException(
							"Unhandled exception in object creator", e);
				}
			}

			int count = created.get();
			// Try to increase the counter as long as the pool is not full
			while (count < maxObjects) {

				if (created.compareAndSet(count, count + 1)) {

					try {

						// If successful, we can create a new object.
						return objectCreator.call();

					} catch (final Exception e) {

						// Create failed, restore counter
						created.decrementAndGet();

						throw new RuntimeException(
								"Unhandled exception in object creator", e);

					}

				}

				// compareAndSet() failed, get new size and try again until the
				// pool is full
				count = created.get();

			}

		}

		return instance;

	}
	
	/**
	 * 归还
	 * @param object
	 */	
	public void give(final T object) {
		if (!objectPool.offer(object)) {
			throw new IllegalStateException(
					"Attempted to return an object to a full pool. "
							+ "Only return objects created by this pool.");
		}
	}

}