package Fetching;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jsoup.nodes.Document;

public class RobotInfo {
	
	// Add dummy url if there are no disallowed urls so that we can distinguish if the file has been processed or not
	public ConcurrentHashMap<String, List<URL>> disallowedURLs;
	public ConcurrentLinkedQueue<String> unknownHosts;
	
	public RobotInfo() {
		this.disallowedURLs = new ConcurrentHashMap<String, List<URL>>();
		this.unknownHosts = new ConcurrentLinkedQueue<String>();
		}
	
	// We need the url to get the protocol, the hostname and the associated list
	public void ParseFile(URL url, Document file, BlockingQueue<URL> urlQueue) {
		String content = file.body().text();
		String pattern = "(?<=User-agent:\\\\s\\\\*)(?:\\\\s+Disallow:\\\\s+([^\\\\n\\\\r#]+))+"; // This regex pattern extracts the disallowed paths for the * user-agent in captured groups
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(content);
		List<URL> disallowed = disallowedURLs.get(url.getHost());
		try {
			while (matcher.find()) {
		            for (int i = 1; i <= matcher.groupCount(); i++) {
							disallowed.add(new URL(url.getProtocol() + "://" + url.getHost() + matcher.group(i)));
						}   
		
			}
			if (disallowed.isEmpty()) {
				disallowed.add(new URL("http://example.com")); // add dummy url to mark the file as parsed if no urls are disallowed
			}
		} catch (MalformedURLException e) { 
			System.err.println(e.getMessage());
		}
		urlQueue.removeIf((u) -> disallowed.contains(u));
		disallowedURLs.put(url.getHost(), disallowed);
	}
	
	// I seriously don't understand why there are empty lists
	public void UpdateEmpty() {
		try {
			for (String host : disallowedURLs.keySet()) {
				if (disallowedURLs.get(host).isEmpty()) {
					List<URL> newList = new LinkedList<URL>();
					newList.add(new URL("http://example.com"));
					disallowedURLs.put(host, newList);
				}
			}
		}
		catch (MalformedURLException e) {
			
		}
	}
}
