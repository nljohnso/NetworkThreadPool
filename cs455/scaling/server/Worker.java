package cs455.scaling.server;

import java.util.List;

import cs455.scaling.task.Task;

public class Worker extends Thread {

	private static final boolean debug = false;
	private List<Task> synchedList = null;

	public Worker(List<Task> queue) {
		synchedList = queue;
	}

	/**
	 * Worker thread takes a task of the queue and executes it. Once it has been
	 * executed the thread then waits for another task.
	 */
	public void run() {
		while (true) {
			try {
				synchronized (synchedList) {
					if (!synchedList.isEmpty()) {
						Runnable runnable = (Runnable) synchedList.remove(0);
						runnable.run();
					} else {
						if (debug)
							System.out.println("Nothing to do, let's wait.");
						synchedList.wait();
						if (debug)
							System.out
									.println("Done waiting, have something to do.");
					}
				}
			} catch (Exception e) {
				System.out.println("Exception occurred in Worker Thread. " + e);
			}
		}
	}
}
