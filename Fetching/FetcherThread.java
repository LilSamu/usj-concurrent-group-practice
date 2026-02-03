package Fetching;

import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class FetcherThread implements Callable<Document>
{
	private URL url;
	public final int maxTries = 3;
	public final long rateLimitMilliseconds = 2000;
	public ConcurrentHashMap<String, Instant> timestamps = new ConcurrentHashMap<String, Instant>();
	
	public FetcherThread(URL url, ConcurrentHashMap<String, Instant> timestampData)
	{
		this.url = url;
		this.timestamps = timestampData;
	}
	
	@Override
	public Document call() throws Exception {
		String host = url.getHost();
		while (true) {
			long timeSinceLastPing = Duration.between(timestamps.get(host), Instant.now()).toMillis();
			if (timeSinceLastPing < rateLimitMilliseconds) {
				//System.out.println("Limiting rate to: " + host);
				Thread.sleep(rateLimitMilliseconds);
			}
			else {
				System.out.println("Fetching data from: " + url);
				for (int tries = 0; tries < maxTries; tries++) {
					try {
						timestamps.put(host, Instant.now());
						return Jsoup.connect(url.toString()).timeout(5000).get(); //timeout of 5s
					} 
					catch (Exception e) {
						if (e instanceof SocketTimeoutException) {
							System.err.println(url + " timed out");	
						}
						else if (e instanceof HttpStatusException error) {
							if (error.getStatusCode() == 404) {
								throw error;
							}
						}
						else
						{
							System.err.println("Error fetching: " + url.toString() + ": " + e.getMessage());
						}
						if (tries == maxTries - 1) {
							throw e;
						}
					}
				}
				return null;
			}
		}
	}
}
