package Concurrency;

import java.net.URL;
import java.time.Instant;
import java.util.concurrent.*;

import org.jsoup.nodes.Document;

import Fetching.RobotInfo;
import Main.Tuple;

public class ConcurrentStructures 
{

	public BlockingQueue<URL> urlQueue = new LinkedBlockingQueue<URL>();
	public BlockingQueue<Tuple<URL, Document>> resultQueue = new LinkedBlockingQueue<Tuple<URL, Document>>();
	
	public RobotInfo robotInfo = new RobotInfo();
	
	public ConcurrentHashMap<URL, Boolean> visitedURLs = new ConcurrentHashMap<URL, Boolean>();
	public ConcurrentHashMap<String, Instant> timestamps = new ConcurrentHashMap<String, Instant>();
	public ConcurrentStructures() {
	        try {
	            URL start = new URL("https://es.wikipedia.org/wiki/Wikipedia:Portada"); // we can change this for a list of seed urls
	            visitedURLs.putIfAbsent(start, false);
	            timestamps.putIfAbsent(start.getHost(), Instant.now());
	            urlQueue.add(start);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	  }
}
