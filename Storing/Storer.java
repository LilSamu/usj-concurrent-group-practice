package Storing; 
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator; 
import java.util.List; import 
org.jsoup.nodes.Document; 
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors; 
import java.util.concurrent.Future; 
import java.util.concurrent.LinkedBlockingQueue; 
import java.util.concurrent.ThreadPoolExecutor; 
import java.util.concurrent.TimeUnit; 
import Concurrency.*; 
import Main.Tuple; 


public class Storer implements Runnable, CanWork {
    private ThreadPoolExecutor writingService;
    private ExecutorService parsingService;
    private ConcurrentStructures structures;
    private HashMap<URL, Future<List<URL>>> pendingParses;
    private OutputWriter writer;

    public Storer(ConcurrentStructures structures) {
        this.structures = structures;
        // 1000 DAYS!!! (Like in TTFAF)
        this.writingService = new ThreadPoolExecutor(30, 30, 1000, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        this.parsingService = Executors.newFixedThreadPool(10);
        this.writer = new OutputWriter();
        this.pendingParses = new HashMap<URL, Future<List<URL>>>();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                HandleResults();
                CheckPending();
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        shutdown();
    }
    
    private void HandleResults() throws InterruptedException {
    	if (!structures.resultQueue.isEmpty()) {
        	
            Tuple<URL, Document> result = structures.resultQueue.take();
            if (result.getFirst().getPath().endsWith("/robots.txt")) {
            	parsingService.execute(() -> { 
            		System.out.println("Parsing robots.txt from " + result.getFirst());
            		structures.robotInfo.ParseFile(result.getFirst(), result.getSecond(), structures.urlQueue); 
            		});
            	return;
            }
            System.out.println("Parsing data from: " + result.getFirst());
            Parser parser = new Parser(result.getSecond());
            this.pendingParses.put(result.getFirst(), parsingService.submit(parser));
            writingService.execute(() -> writer.write(new Tuple<URL, String>(result.getFirst(), result.getSecond().data())));
    	
    	}	
    }
    
    private void CheckPending() throws InterruptedException {
    	Iterator<URL> iterator = pendingParses.keySet().iterator();
    	while (iterator.hasNext()) {
        	URL url = iterator.next();
            Future<List<URL>> future = pendingParses.get(url);
            if (future.isDone()) {
                try {
                    List<URL> extractedUrls = future.get();
                    for (URL extractedUrl : extractedUrls) {
                    	String host = extractedUrl.getHost();
                    	if (structures.robotInfo.unknownHosts.contains(host)) { // if unknown permissions, skip the url
                    		System.err.println("Skipping url with unknown permissions. Host: " + host);
                        	continue;
                        }
                        if (structures.visitedURLs.putIfAbsent(extractedUrl, false) == null) {
                            structures.urlQueue.put(extractedUrl);
                            structures.timestamps.putIfAbsent(extractedUrl.getHost(), Instant.now());
                        }
                    }
                    System.out.println("Current pending urls: " + structures.urlQueue.size());
                    System.out.println("Visited urls: " + structures.visitedURLs.size());
                } catch (Exception e) {
                	// if the parser failed, mark the url as unvisited
                	structures.visitedURLs.put(url, false);
                	System.err.println("Failed to parse data from " + url + ": " + e.getMessage());
                } finally {
                    iterator.remove(); // Usar iterator.remove() en lugar de pendingParses.remove(future)
                }
            }
        }
    }
    
    

    @Override
    public boolean Working() {
        return !structures.resultQueue.isEmpty() || writingService.getActiveCount() != 0; // the second condition doesn't work?
    }

    public void shutdown() {
    	writingService.shutdown();
    	parsingService.shutdown();
        try {
            if (!writingService.awaitTermination(5, TimeUnit.SECONDS)) {
            	writingService.shutdownNow();
                if (!writingService.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Writing executor did not terminate");
            }
            if (!parsingService.awaitTermination(5, TimeUnit.SECONDS)) {
            	parsingService.shutdownNow();
                if (!parsingService.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Parsing executor did not terminate");
            }
        } catch (InterruptedException e) {
        	writingService.shutdownNow();
        	parsingService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
