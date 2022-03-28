package retriever;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import file.FileScannerThreadPool;
import main.Config;
import main.DeleteScannedURLsJob;
import main.Main;
import web.WebScannerThreadPool;

import java.util.concurrent.Callable;

public class ResultRetrieverThreadPool {
	
	private ExecutorService pool;
	public ConcurrentHashMap<String, Future<Map<String, Integer>>> fileResults, webResults;
	public ConcurrentHashMap<String, Future<Map<String, Map<String, Integer>>>> zapocetiResultDomain;
	public Future<Map<String, Map<String, Integer>>> zapocetiWebSummary, zapocetiFileSummary;
	public ConcurrentHashMap<String, Map<String, Integer>> webResultsDomain, webSummary, fileSummary;
	private ConcurrentHashMap<String, Boolean> zastareoDomen, zastareoWebSummary, zastareoCorpus;
	private WebScannerThreadPool webScannerThreadPool;
	private ScheduledExecutorService executor;
	
	public ResultRetrieverThreadPool() {
		this.fileResults = new ConcurrentHashMap<>();
		this.webResults = new ConcurrentHashMap<>();
		this.webResultsDomain = new ConcurrentHashMap<>();
		this.fileSummary = new ConcurrentHashMap<>();
		this.webSummary = new ConcurrentHashMap<>();
		this.zastareoDomen = new ConcurrentHashMap<>();
		this.zastareoWebSummary = new ConcurrentHashMap<>();
		this.zastareoCorpus = new ConcurrentHashMap<>();
		this.pool = Executors.newCachedThreadPool();
		executor = Executors.newScheduledThreadPool(5);
		this.zapocetiResultDomain = new ConcurrentHashMap<String, Future<Map<String,Map<String,Integer>>>>();
		this.zapocetiWebSummary = null;
		this.zapocetiFileSummary = null;
		deleteScannedUrls();
	}
	
	public void deleteScannedUrls() {
		DeleteScannedURLsJob job = new DeleteScannedURLsJob(this, webScannerThreadPool);
		executor.scheduleWithFixedDelay(job, Config.getInstance().url_refresh_time,  Config.getInstance().url_refresh_time, TimeUnit.MILLISECONDS);
	}

	public void setWebScannerThreadPool(WebScannerThreadPool webScannerThreadPool) {
		this.webScannerThreadPool = webScannerThreadPool;
	}
	
	public void addNewWebResult(String url, Future<Map<String, Integer>> result){
				
		webResults.computeIfAbsent(url, (key) -> {
			try {
				String domen = new URI(url).getHost().toString();
				
				if(webResultsDomain.containsKey(domen)) {
					zastareoDomen.put(domen, true);
				}
				
				if(webSummary.containsKey(domen)) {
					zastareoWebSummary.put(domen, true);
				}
			} catch (URISyntaxException e) {
				
				e.printStackTrace();
			}
			
			return result;
		});
	}

	
	public void addNewFileResult(String corpus,Future<Map<String, Integer>> result) {
		
		fileResults.computeIfPresent(corpus, (key, value) -> {
			zastareoCorpus.put(corpus, true);
			return result;
		});
		
		fileResults.putIfAbsent(corpus, result);
		
	}


	public String getForDomain(String domain) {
		
		if(webResultsDomain.containsKey(domain)) {
			if(zastareoDomen.containsKey(domain)){
					Callable<Map<String, Map<String, Integer>>> retrieverJob = new ResultRetriever(webResults, fileResults, domain, RetrieverJobType.DOMAIN_GET);
				    Future<Map<String, Map<String, Integer>>> future = pool.submit(retrieverJob);
				    zapocetiResultDomain.put(domain, future);
					     try {
					    	Map<String, Map<String, Integer>> map = future.get();
					    	 if(map.containsKey(domain)) {
					    		 Map<String, Integer> result = future.get().get(domain);
							     webResultsDomain.put(domain, result);
							     zastareoDomen.remove(domain);
							     return result.toString();
					    	 }else {
					    		 return "Page with this domain isn't scanned";
					    	 }
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					  return null;
			}else{
				return webResultsDomain.get(domain).toString();
			}
		}else {
			Callable<Map<String, Map<String, Integer>>> retrieverJob = new ResultRetriever(webResults, fileResults, domain, RetrieverJobType.DOMAIN_GET);
		    Future<Map<String, Map<String, Integer>>> future = pool.submit(retrieverJob);
		    // stavljamo zapocete poslove u hash mapu zbog query poziva
		    zapocetiResultDomain.put(domain, future);
			     try {
			    	Map<String, Map<String, Integer>> map = future.get();
			    	 if(map.containsKey(domain)) {
			    		 Map<String, Integer> result = future.get().get(domain);
					     webResultsDomain.put(domain, result);
					     return result.toString();
			    	 }else {
			    		 return "Page with this domain isn't scanned";
			    	 }
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			  return null;
		}
	}
	
	// vraca rezultat ukoliko je 
	public String queryForDomain(String domain) {
		if(zapocetiResultDomain != null) {
		if(zapocetiResultDomain.containsKey(domain)) {
			Future<Map<String, Map<String, Integer>>> future = zapocetiResultDomain.get(domain);
			if(future.isDone()) {
				try {
					return future.get().get(domain).toString();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}else {
				 return "Result for this domain is not ready yet.";
			}
		}
		}
		return "There is no result for this domain";
		
	}
	
	
	public String getWebSummary() {
		
		 if(zastareoWebSummary.isEmpty()) {
			 if(webSummary.isEmpty()) {
					 Callable<Map<String, Map<String, Integer>>> retrieverJob = new ResultRetriever(webResults, fileResults, "", RetrieverJobType.WEBSUMMARY);
				     Future<Map<String, Map<String, Integer>>> future = pool.submit(retrieverJob); 
				     zapocetiWebSummary = future;
				     Map<String, Map<String, Integer>> map;
					try {
						map = future.get();
						for (Entry<String, Map<String, Integer>> entry : map.entrySet()) {
				    		 webSummary.put(entry.getKey(), entry.getValue());
				    	 }
					     zastareoWebSummary.clear();
					     return webSummary.toString();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				     
			  }else {
				  return webSummary.toString();  
			  }
			  
		 }else {
			 Callable<Map<String, Map<String, Integer>>> retrieverJob = new ResultRetriever(webResults, fileResults, "", RetrieverJobType.WEBSUMMARY);
		     Future<Map<String, Map<String, Integer>>> future = pool.submit(retrieverJob); 
		     zapocetiWebSummary = future;
		     zastareoWebSummary.clear();
		     try {
		    	 Map<String, Map<String, Integer>> map = future.get();
		    	 for (Entry<String, Map<String, Integer>> entry : map.entrySet()) {
		    		 webSummary.put(entry.getKey(), entry.getValue());
		    	 }
			     zastareoWebSummary.clear();
			     return webSummary.toString();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
		 }
		
		return null;
	}
	
	public String queryWebSummary() {
		if(zapocetiWebSummary != null) {
			if(zapocetiWebSummary.isDone()) {
				 return webSummary.toString();
			}else {
				return "Summary is not ready yet.";
			}
		}
		return "There are no result for summary.";
	}
	
	public String getFileSummary() {
		 if(zastareoCorpus.isEmpty()) {
			  if(fileSummary.isEmpty()) {
				  Callable<Map<String, Map<String, Integer>>> retrieverJob = new ResultRetriever(webResults, fileResults, "", RetrieverJobType.FILESUMMARY);
				     Future<Map<String, Map<String, Integer>>> future = pool.submit(retrieverJob);
				     zapocetiFileSummary = future;
				     try {
				    	 Map<String, Map<String, Integer>> map = future.get();
				    	 
				    	 for (Entry<String, Map<String, Integer>> entry : map.entrySet()) {
				    		 fileSummary.put(entry.getKey(), entry.getValue());
				    	 }
						
						return fileSummary.toString();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					} catch (ExecutionException e1) {
						e1.printStackTrace();
					}
			  }else {
				  return fileSummary.toString();  
			  }
		 }else {
			 Callable<Map<String, Map<String, Integer>>> retrieverJob = new ResultRetriever(webResults, fileResults, "", RetrieverJobType.FILESUMMARY);
		     Future<Map<String, Map<String, Integer>>> future = pool.submit(retrieverJob);
		     zapocetiFileSummary = future;
			     try {
			    	 Map<String, Map<String, Integer>> map = future.get();
			    	 
			    	 for (Entry<String, Map<String, Integer>> entry : map.entrySet()) {
			    		 fileSummary.put(entry.getKey(), entry.getValue());
			    	 }
			    	 zastareoCorpus.clear();
			    	 return fileSummary.toString();  
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
		 }
		 return null;
	}
	
	
	public String queryFileSummary() {
		if(zapocetiFileSummary != null) {
			if(zapocetiFileSummary.isDone()) {
				 return fileSummary.toString();
			}else {
				return "Summary is not ready yet.";
			}
		}
		return "There are no result for summary.";
	}
	
	public String clearWebSummary() {
		if(webSummary.isEmpty()) {
			return "Web summary je prazan";
		}
		webSummary.clear();
		zapocetiWebSummary = null;
		return "Brisanje summary web rezultata";
	}
	
	public String clearFileSummary() {
		if(fileSummary.isEmpty()) {
			return "File summary je prazan";
		}
		fileSummary.clear();
		zapocetiFileSummary = null;
		return "Brisanje summary file rezultata";
	}
	
	
	public String getResultForCorpus(String corpus) {
		if(this.fileResults.containsKey(corpus)) {
			try {
				return fileResults.get(corpus).get().toString();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return "There is not job for this corpus";
	}
	
	public String getQueryForCorpus(String corpus) {
		try {
			if(fileResults.containsKey(corpus)) {
				Future<Map<String,Integer>> result = fileResults.get(corpus);
				// ukoliko posao nije zavrsen, necemo da blokiramo
				if(result.isDone()) {
					return fileResults.get(corpus).get().toString();
				}else {
					return "Job for this corpus exists but isn't done";
				}
			} else {
				return "There is not job for this corpus";
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void shutdown() {
		this.executor.shutdown();
		this.pool.shutdown();
	}


}
