package web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import main.Config;
import model.ScanType;
import model.ScanningJob;

public class WebScanner implements Callable< Map<String, Integer>>, ScanningJob{
	
	private String url;
	private int hop_count;
	private String[] keywords;
	private BlockingQueue<ScanningJob> jobQueue;
	private WebScannerThreadPool webScannerThreadPool;
	
	public WebScanner(String url, int hop_count, BlockingQueue<ScanningJob> jobQueue, WebScannerThreadPool webScannerThreadPool) {
		this.url = url;
		this.hop_count = hop_count;
		this.keywords = Config.getInstance().keywords;
		this.jobQueue = jobQueue;
		this.webScannerThreadPool = webScannerThreadPool;
	}


	@Override
	public Map<String, Integer> call() {
		
		webScannerThreadPool.jobStarted(this.url);
		Map<String, Integer> result = new HashMap<String, Integer>();
		Document doc;
		try {
			doc = Jsoup.connect(this.url).get();
		
		System.out.println("Starting web scan for " + this.url);
		Elements links = doc.select("a[href]");
		
		
		for (String word : this.keywords) {
			result.put(word, 0);
		}
		
		// aw https://www.grammarly.com/blog/articles/
		// get web|www.grammarly.com
		// query web|www.grammarly.com
		
		if (this.hop_count != 0) {
			for (Element link : links) {
				
				webScannerThreadPool.webJobs.computeIfAbsent(link.attr("abs:href"), (key) -> {
					try {
						jobQueue.put(new WebScanner(link.attr("abs:href"), this.hop_count-1, jobQueue, this.webScannerThreadPool));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return false;
				});
				
		    }
		}
		
	   Element body = doc.body();
	
	   String[] words = body.text().split("\\?|\\.|\\!|\\-|\\,|\\s+");
		for (String word : words) {
			if (!word.isEmpty()) {
				if (result.containsKey(word))
					result.put(word, result.get(word) + 1);
			}
		}
			
		
		webScannerThreadPool.jobEnded(this.url);
		
		} catch (IOException e) {
			System.out.println(this.url + " is not valid.");
		}catch (IllegalArgumentException e) {
			System.out.println(this.url + " is not valid.");
		}catch(Exception e) {
			e.printStackTrace();
		} 
		return result;
	}



	@Override
	public ScanType getType() {
		return  ScanType.WEB;
	}


	@Override
	public String getQuery() {
		return this.url;
	}
	

}
