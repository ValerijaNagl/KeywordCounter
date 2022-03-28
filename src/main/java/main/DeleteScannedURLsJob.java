package main;

import retriever.ResultRetrieverThreadPool;
import web.WebScannerThreadPool;

public class DeleteScannedURLsJob implements Runnable {
	
	private ResultRetrieverThreadPool resultRetrieverThreadPool;
	private WebScannerThreadPool webScannerThreadPool;
	
	
	public DeleteScannedURLsJob(ResultRetrieverThreadPool resultRetrieverThreadPool, WebScannerThreadPool webScannerThreadPool) {
		this.resultRetrieverThreadPool = resultRetrieverThreadPool;
		this.webScannerThreadPool = webScannerThreadPool;
	}


	@Override
	public void run() {
		//System.out.println("usao sam ovde");
		resultRetrieverThreadPool.webResults.clear();
		resultRetrieverThreadPool.webResultsDomain.clear();
		resultRetrieverThreadPool.webSummary.clear();
		webScannerThreadPool.webJobs.clear();
	}

}
