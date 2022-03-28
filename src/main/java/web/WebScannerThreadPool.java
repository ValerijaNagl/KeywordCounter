package web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import model.ScanningJob;
import retriever.ResultRetrieverThreadPool;

public class WebScannerThreadPool {
	
	private ExecutorService pool;
	public ConcurrentHashMap<String, Boolean> webJobs;
	private ResultRetrieverThreadPool resultRetrieverThreadPool;
	
	public WebScannerThreadPool(ResultRetrieverThreadPool resultRetrieverThreadPool) {
		this.pool = Executors.newCachedThreadPool();
		this.resultRetrieverThreadPool = resultRetrieverThreadPool;
		this.webJobs = new ConcurrentHashMap<String, Boolean>();
	}
	
	public void newJob(ScanningJob job) {
		this.pool = Executors.newCachedThreadPool();
		WebScanner newJob = (WebScanner) job;
		Future<Map<String, Integer>> future = this.pool.submit(newJob);
		Map<String, Integer> map;
		try {
			map = future.get();
			resultRetrieverThreadPool.addNewWebResult(newJob.getQuery(), future);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}
	
	public void jobStarted(String url){
		webJobs.putIfAbsent(url, false);
	}
	
	public void jobEnded(String url) {
		webJobs.putIfAbsent(url, true);
	}
	
	public void shutdown() {
		this.pool.shutdown();
	}
	

}
