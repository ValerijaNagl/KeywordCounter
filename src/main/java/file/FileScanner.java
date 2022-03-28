package file;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.RecursiveTask;
import main.Config;
import main.Main;
import model.ScanType;
import model.ScanningJob;


public class FileScanner extends RecursiveTask<Map<String, Integer>> implements ScanningJob{
	
	private long file_scanning_size_limit;
	private List<File> files;
	private String path;
	private String[] keywords;
	private String corpus;
	private FileScannerThreadPool fileScannerThreadPool;

	public FileScanner(FileScannerThreadPool fileScannerThreadPool, String path) {
		this.file_scanning_size_limit = Config.getInstance().file_scanning_size_limit;
		this.files = getFilesFromCorpus(path);
		this.keywords = Config.getInstance().keywords;
		this.corpus = new File(path).getName();
		this.path = path;
		this.fileScannerThreadPool = fileScannerThreadPool;
	}
	
	public FileScanner(List<File> files, String corpus, FileScannerThreadPool fileScannerThreadPool) {
		this.file_scanning_size_limit = Config.getInstance().file_scanning_size_limit;
		this.files = files;
		this.keywords = Config.getInstance().keywords;
		this.corpus = corpus;
		this.fileScannerThreadPool = fileScannerThreadPool;
	}
	
	
	public List<File> getFilesFromCorpus(String corpus){
		List<File> toReturn = new ArrayList<>();
		File[] files = new File(corpus).listFiles();
		for(int i=0; i<files.length; i++) {
			if(!files[i].isDirectory()) toReturn.add(files[i]);
		}
		return toReturn;
	}



	@Override
	protected Map<String, Integer> compute() {
		
	    fileScannerThreadPool.jobStarted(this.corpus);
		
	    Map<String, Integer> result = new HashMap<>();
	    List<File> listOfFiles = splitTheJob(this.files);
	
		if(listOfFiles.size()!=0) {
			
			FileScanner newJob = new FileScanner(this.files, this.corpus, this.fileScannerThreadPool);
			newJob.fork();
			
			
			for (String word : keywords) result.put(word, 0);
			
			for(File file : listOfFiles) {
				
				if(!getFileExtension(file).equals(".txt")) {
					System.out.println("File " + file.getName() + " isn't txt.");
					continue;
				}
				
				Scanner sc;
				try {
					sc = new Scanner(file);
					while (sc.hasNext()) {
						String line = sc.nextLine();
						String[] words = line.split("\\?|\\.|\\!|\\-|\\,|\\s+");
						for (String word : words) {
							if (!word.isEmpty()) {
								if (result.containsKey(word)) {
									int help_count = result.get(word);
									result.put(word, help_count + 1);
								}
						}
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			Map<String, Integer> joinMap = newJob.join();
					
			HashMap<String, Integer> toReturn = new HashMap<String, Integer>();
			toReturn.putAll(result);
			toReturn.forEach((k, v) -> joinMap.merge(k, v, (v1, v2) -> v1 + v2));

			fileScannerThreadPool.jobEnded(this.corpus);
			return toReturn;
		}
		
		return result;
	}
	
	private String getFileExtension(File file) {
	    String name = file.getName();
	    int lastIndexOf = name.lastIndexOf(".");
	    if (lastIndexOf == -1) {
	        return ""; // empty extension
	    }
	    return name.substring(lastIndexOf);
	}
	
	
	
	public List<File> splitTheJob(List<File> files) {
		int sum = 0;
		int count = 0;
		LinkedList<File> newList = new LinkedList<>();
		for(File f : files) {
			sum+= f.length();
			count++;
			newList.add(f);
			if(sum > this.file_scanning_size_limit) {
				break;
			}
		}
		files.subList(0, count).clear();
		return newList;
	}

	@Override
	public ScanType getType() {
		return ScanType.FILE;
	}

	@Override
	public String getQuery() {
		return this.corpus;
	}
	
}
