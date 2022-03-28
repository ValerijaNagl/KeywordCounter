package main;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import file.FileScanner;
import file.FileScannerThreadPool;
import model.ScanningJob;

public class DirectoryCrawler implements Runnable{
	
	private ConcurrentHashMap<String, Long> lastModified;
	private String file_corpus_prefix;
	private long dir_crawler_sleep_time;
	public static boolean run = true;
	private BlockingQueue<ScanningJob> jobQueue;
	private FileScannerThreadPool fileScannerThreadPool;
	private final Semaphore semaphore;
	private ArrayList<String> dirsForScanning;

	public DirectoryCrawler(ConcurrentHashMap<String, Integer> lastModified, BlockingQueue<model.ScanningJob> jobQueue, FileScannerThreadPool fileScannerThreadPool) {
		this.lastModified = new ConcurrentHashMap<>();
		this.file_corpus_prefix = Config.getInstance().file_corpus_prefix;
		this.dir_crawler_sleep_time = Config.getInstance().dir_crawler_sleep_time;
		this.jobQueue = jobQueue;
		this.semaphore = new Semaphore(1);
		this.fileScannerThreadPool = fileScannerThreadPool;
		this.dirsForScanning = new ArrayList<>();
	}
	
	public void addNewDirectoryForScanning(String directory) {
		File dir = new File(directory);
		if(!dir.isDirectory()) {
			System.err.println(directory + " is not directory.");
			return;
		}else {
			System.out.println("Adding dir " + dir.getAbsolutePath());
			
			try {
				semaphore.acquire();
				this.dirsForScanning.add(directory);
				
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			semaphore.release();
			
		}
	}

	public void run() {
		
		
		Queue<File> directoryQueue = new LinkedList<>();
		
		while(run) {
			
			try {
				semaphore.acquire();
				
				if(dirsForScanning.size() > 0) {
					for(String dir : dirsForScanning) {
						directoryQueue.add(new File(dir));
					}
				}
				
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			semaphore.release();
				
	
			while(!directoryQueue.isEmpty()) {
				
				File current = directoryQueue.poll();
				
				if(current.isDirectory()) {
					
					boolean isCorpus = isCorpus(current);
					
					if(isCorpus) startJobForDir(current);
					File[] files = current.listFiles();
					for(int i=0; i<files.length; i++) {
						
						if(files[i].isDirectory()) {
							directoryQueue.add(files[i]);
						}else{
							if(isCorpus)
							checkLastModified(files[i]);
						}
					}
				}
				
				try {
					Thread.sleep(dir_crawler_sleep_time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private boolean isCorpus(File f) {
		String name = f.getName();
		if(name!= null)
			return name.startsWith(this.file_corpus_prefix);
		else return false;
	}
	
	
	public void checkLastModified(File f) {
		lastModified.computeIfPresent(f.getAbsolutePath(), (key, value) -> {

			if (value != f.lastModified()) {

				File parent = f.getParentFile();

				if(parent!=null) {
					try {
						jobQueue.put(new FileScanner(fileScannerThreadPool, parent.getAbsolutePath()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				return f.lastModified();
			}
			return value;
		});
		
		lastModified.putIfAbsent(f.getAbsolutePath(), f.lastModified());
		
	}
	
	private void startJobForDir(File f) {
		lastModified.computeIfAbsent(f.getAbsolutePath(), (key) -> {
			try {
				jobQueue.put(new FileScanner(fileScannerThreadPool,f.getAbsolutePath()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return f.lastModified();
		});

	}

}
