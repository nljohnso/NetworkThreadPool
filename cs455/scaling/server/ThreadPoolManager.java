package cs455.scaling.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cs455.scaling.task.Task;

public class ThreadPoolManager {

	private LinkedList<Task> queue = null;
	private List<Task> synchedQueue;
	private List<Worker> threads = new ArrayList<Worker>();
	private boolean isStopped = false;

	public ThreadPoolManager(int noOfThreads) {
		queue = new LinkedList<Task>();
		synchedQueue = Collections.synchronizedList(queue);

		for (int i = 0; i < noOfThreads; i++) {
			threads.add(new Worker(synchedQueue));
		}

		for (Worker thread : threads) {
			thread.start();
		}
	}

	/**
	 * Adds task to the task queue and notifies any waiting threads that a job
	 * is available.
	 * 
	 * @param task
	 */
	public void execute(Task task) {
		if (this.isStopped)
			throw new IllegalStateException("ThreadPool is stopped");

		synchronized (synchedQueue) {
			queue.addLast(task);
			synchedQueue.notifyAll();
		}
	}
}
