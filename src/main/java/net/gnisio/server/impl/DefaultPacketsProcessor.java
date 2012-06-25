package net.gnisio.server.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.gnisio.server.PacketsProcessor;

/**
 * Default implementation of packets processor. It uses BlockingQueue and
 * Executors.newFixedThreadPool
 * 
 * @author c58
 * 
 */
public class DefaultPacketsProcessor extends PacketExecutionLogic implements PacketsProcessor, Runnable {
	// For threads
	private final BlockingQueue<Packet> packetsQueue;
	private final ExecutorService threadPool;
	private final int threadPoolSize;
	private boolean isActive = true;

	public DefaultPacketsProcessor(ServerContext serverContext, int threadPoolSize) {
		super(serverContext);

		// For thread pool
		this.threadPoolSize = threadPoolSize;
		this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
		this.packetsQueue = new LinkedBlockingQueue<Packet>();

		initThreadPool();
	}

	/**
	 * Initialize threads pool
	 */
	private void initThreadPool() {
		for (int i = 0; i < this.threadPoolSize; i++) {
			this.threadPool.execute(this);
		}
	}

	@Override
	public void queueMessage(Packet mess) {
		if (mess != null)
			this.packetsQueue.add(mess);
	}

	@Override
	public void run() {
		while (isActive) {
			try {
				// Get the packet
				Packet packet = packetsQueue.take();

				// Process it
				processRawPacket(packet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stopProcessing() {
		// Send stop signal for all workers
		isActive = false;

		// Wait all termination
		try {
			threadPool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
