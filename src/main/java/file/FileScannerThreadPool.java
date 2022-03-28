package file;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import main.Main;
import model.ScanningJob;
import retriever.ResultRetrieverThreadPool;

public class FileScannerThreadPool {
	
	private ForkJoinPool pool;
	public ConcurrentHashMap<String, Boolean> corpusJobs;
	private ResultRetrieverThreadPool resultRetrieverThreadPool;
	
	public FileScannerThreadPool(ResultRetrieverThreadPool resultRetrieverThreadPool) {
		this.pool = ForkJoinPool.commonPool();
		this.resultRetrieverThreadPool = resultRetrieverThreadPool;
		this.corpusJobs = new ConcurrentHashMap<String, Boolean>();
	}
	
	public void newJob(ScanningJob job) {
		this.pool = ForkJoinPool.commonPool();
		RecursiveTask<Map<String, Integer>> jobRunnable = (FileScanner) job;
		System.out.println("Starting file scan for file|" + job.getQuery());
		Future<Map<String, Integer>> future = pool.submit(jobRunnable);
		resultRetrieverThreadPool.addNewFileResult(job.getQuery(), future);
	}
	
	public void jobStarted(String url){
		corpusJobs.put(url, false);
	}
	
	public void jobEnded(String url) {
		corpusJobs.put(url, true);
	}
	
	
	public void setResultRetrieverThreadPool(ResultRetrieverThreadPool resultRetrieverThreadPool) {
		this.resultRetrieverThreadPool = resultRetrieverThreadPool;
	}
	
	public void shutdown() {
		this.pool.shutdown();
	}
}
