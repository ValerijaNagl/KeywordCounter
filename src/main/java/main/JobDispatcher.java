package main;

import java.util.concurrent.BlockingQueue;

import file.FileScannerThreadPool;
import model.ScanType;
import model.ScanningJob;
import web.WebScannerThreadPool;

public class JobDispatcher implements Runnable {

	private BlockingQueue<ScanningJob> jobQueue;
	public static boolean run = true;
	private FileScannerThreadPool fileScannerThreadPool;
	private WebScannerThreadPool webScannerThreadPool;
	

	public JobDispatcher(BlockingQueue<ScanningJob> jobQueue, FileScannerThreadPool fileScannerThreadPool,
			WebScannerThreadPool webScannerThreadPool) {
		this.jobQueue = jobQueue;
		this.fileScannerThreadPool = fileScannerThreadPool;
		this.webScannerThreadPool = webScannerThreadPool;
	}


	@Override
	public void run() {
		while(run) {
			try {
				ScanningJob newJob = jobQueue.take();
				
				//if(newJob == null) continue;
				
				if(newJob.getType().equals(ScanType.FILE)) {
					fileScannerThreadPool.newJob(newJob);
				}
				
				if(newJob.getType().equals(ScanType.WEB)) {
					webScannerThreadPool.newJob(newJob);
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
