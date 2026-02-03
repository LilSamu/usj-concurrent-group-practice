package Concurrency;
import Storing.Storer;
import Fetching.Fetcher;

public class ThreadManager 
{
	// finish when fetcher and storer have stopped working and queues are empty
	public void Run()
	{
		ConcurrentStructures structures = new ConcurrentStructures();
		Fetcher fetcher = new Fetcher(structures);
		Storer storer = new Storer(structures);
		
		Thread fetcherThread = new Thread(fetcher);
		Thread storerThread = new Thread(storer);
		
		fetcherThread.setPriority(Thread.MAX_PRIORITY);
		storerThread.setPriority(Thread.MAX_PRIORITY);

		fetcherThread.start();
		
		storerThread.start();
		
		try {
			while (true) {
				boolean fetcherWorking = fetcher.Working();
				boolean storerWorking = storer.Working();
				boolean queuesEmpty = structures.urlQueue.isEmpty() && structures.resultQueue.isEmpty();
				
				if (!fetcherWorking && !storerWorking && queuesEmpty) {
					Thread.sleep(5000); // Wait for a few seconds to confirm
					if (!fetcher.Working() && !storer.Working() && structures.urlQueue.isEmpty() && structures.resultQueue.isEmpty()) {
						System.out.println("All tasks completed. Shutting down.");
						fetcherThread.interrupt();
						storerThread.interrupt();
						break;
					}
				}
				Thread.sleep(1000); // Check periodically
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("ThreadManager finished.");
		}
	}
}
