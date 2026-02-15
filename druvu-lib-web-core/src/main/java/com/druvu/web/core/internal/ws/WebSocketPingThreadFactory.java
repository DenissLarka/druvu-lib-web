package com.druvu.web.core.internal.ws;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketPingThreadFactory implements ThreadFactory {

	private static final String NAME_PREFIX = "ws-ping";
	private final AtomicInteger threadNumber = new AtomicInteger(1);

	public Thread newThread(Runnable runnable) {
		Thread thread = new Thread(null, runnable, NAME_PREFIX + '-' + threadNumber.getAndIncrement(), 0);
		if (thread.getPriority() != Thread.MIN_PRIORITY) {
			thread.setPriority(Thread.MIN_PRIORITY);
		}
		return thread;
	}
}
