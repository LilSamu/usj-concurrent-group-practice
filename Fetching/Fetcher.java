package Fetching;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;

import Concurrency.*;
import Main.Tuple;

public class Fetcher implements Runnable, CanWork {
	private ConcurrentStructures structures;
	private ExecutorService executorService;
	private HashMap<URL, Future<Document>> pendingResults;

	public Fetcher(ConcurrentStructures structures) {
		this.structures = structures;
		this.executorService = Executors.newFixedThreadPool(10); // the number of threads is temporal, change it if you want
		this.pendingResults = new HashMap<URL, Future<Document>>();
	}
	@Override
	public void run() {
	    while (!Thread.interrupted()) {
	        try {
	        	structures.robotInfo.UpdateEmpty();
            if (!structures.urlQueue.isEmpty()) {
                URL url = structures.urlQueue.take();
                String host = url.getHost();
                if (structures.robotInfo.disallowedURLs.putIfAbsent(host, new LinkedList<URL>()) == null) {
                	//Fetch robots.txt if not fetched for this host
                	CreateFetch(new URL(url.getProtocol() + "://" + url.getHost() + "/robots.txt"));
                	System.out.println("Fetching robots.txt from " + url);
                	//put the url back into the queue
                	structures.urlQueue.put(url);
                }
                else if (structures.robotInfo.disallowedURLs.get(host).isEmpty()) { // robots.txt has not been processed yet
                	//put the url back into the queue
                	structures.urlQueue.put(url);
                }
                else if (!structures.robotInfo.disallowedURLs.get(host).contains(url)) // skip if disallowed
                {
                	CreateFetch(url);
                }
                else
                {
                	System.err.println("Skipping disallowed url: " + url);
                }
            }
             CheckPending();       
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        } catch (MalformedURLException e) {
			}
	    }
	    shutdown();
	}
	
	private void CreateFetch(URL url) {
		if (url.getPath().endsWith("/robots.txt") || !structures.visitedURLs.get(url)) {
        	structures.visitedURLs.put(url, true);
            FetcherThread fetcherThread = new FetcherThread(url, structures.timestamps);
            this.pendingResults.put(url, executorService.submit(fetcherThread));
        }
	}
	
	private void CheckPending() throws InterruptedException {
		Iterator<URL> iterator = pendingResults.keySet().iterator();
        while (iterator.hasNext()) {
        	URL url = iterator.next();
            Future<Document> future = pendingResults.get(url);
            if (future.isDone()) {
                try {
                	Document result = future.get();
                    structures.resultQueue.put(new Tuple<URL, Document>(url, result));
                    System.out.println("Current pending results: " + structures.resultQueue.size());
                    //System.out.println(future.get());
                }
                catch (ExecutionException e) {
                    // handle different http errors here?
                	if (e.getCause() instanceof SocketTimeoutException) {
                		if (url.getPath().endsWith("/robots.txt")) { // if robots.txt timed out, mark the host as unknown permissions and remove all urls from that host from the queue
                			structures.robotInfo.unknownHosts.add(url.getHost());
                			structures.urlQueue.removeIf((u) -> u.getHost() == url.getHost());
                			System.err.println("Host " + url.getHost() + " timed out.");
                			continue;
                		}
                		structures.visitedURLs.put(url, false); // fetch timed out, url was not visited
                		structures.urlQueue.put(url);
                	}
                	if (e.getCause() instanceof HttpStatusException error) { // if robots.txt doesn't exist, all urls are allowed (I guess)
                		if (url.getPath().endsWith("/robots.txt") && error.getStatusCode() == 404) {
								try {
									URL dummyURL = new URL("http://example.com");
									List<URL> disallowed = structures.robotInfo.disallowedURLs.get(url.getHost());
									disallowed.add(dummyURL);
								} catch (MalformedURLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
                			}
                		}
                	}
                 finally {
                    iterator.remove(); // Usar iterator.remove() en lugar de pendingResults.remove(future)
                }
            }
        }
	}
	
	
	@Override
	public boolean Working() {
		return !structures.urlQueue.isEmpty() || !pendingResults.isEmpty();
	}
	
	private void shutdown() {
		executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) 
            {
            	executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Executor did not terminate");
            }
        } 
        catch (InterruptedException e) 
        {
        	executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
	}
}
