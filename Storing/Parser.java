package Storing;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Parser implements Callable<List<URL>>
{
	private Document data;
	public Parser(Document data)
	{
		this.data = data;
	}

	@Override
	public List<URL> call() throws Exception 
	{
		List<URL> result = new LinkedList<URL>();
		Elements links = data.select("a");
		for (Element link : links)
		{
			String url = link.attr("href");
			if (url.startsWith("https://")) //only secure
			{
				//System.out.println(url);
				result.add(new URL(url));
			}
		}
		return result;
	}
}
