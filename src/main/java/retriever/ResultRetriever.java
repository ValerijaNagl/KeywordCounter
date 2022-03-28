package retriever;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import sun.security.krb5.Config;


public class ResultRetriever  implements Callable<Map<String, Map<String, Integer>>>{
	
	
	private RetrieverJobType type;
	private String[] keywords;
	private String domain;
	private ConcurrentHashMap<String, Future<Map<String, Integer>>> fileResults, webResults;
//	private ConcurrentHashMap<String, Map<String, Integer>> webResults;
	
	public ResultRetriever(ConcurrentHashMap<String,Future<Map<String, Integer>>> webResults,ConcurrentHashMap<String, Future<Map<String, Integer>>> fileResults, 
						 String domain, RetrieverJobType type) {

		this.webResults = webResults;
		this.fileResults = fileResults;
		this.domain = domain;
		this.type = type;
		this.keywords = main.Config.getInstance().keywords;

	}
	
	
	
	public Map<String, Map<String, Integer>> summaryWeb(){
		
		Map<String, Map<String, Integer>> result = new HashMap<String, Map<String, Integer>>();
		
		
		for (Entry<String, Future<Map<String, Integer>>> entry : this.webResults.entrySet()) {
		    String url = entry.getKey();
		    
		    try {
				String domen = new URI(url).getHost();
				
				if (result.containsKey(domen)) {
			    	Map<String, Integer> map;
					
						map = entry.getValue().get();
						for (Map.Entry<String, Integer> e : map.entrySet()) {
							result.get(domen).put(e.getKey(),result.get(domen).get(e.getKey()) + e.getValue());
				    	}
					
			    }else{
					result.put(domen, entry.getValue().get());
			    }
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}catch (InterruptedException e) {
				
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		} 
		
		return result;
	}
	
	public Map<String, Map<String, Integer>> summaryFile(){
		
		Map<String, Map<String, Integer>> result = new HashMap<String, Map<String, Integer>>();
		
		for (Map.Entry<String, Future<Map<String, Integer>>> entry : this.fileResults.entrySet()) {
		    try {
				result.put(entry.getKey(), entry.getValue().get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		} 
		
		return result;
	}
	
	public Map<String, Map<String, Integer>> result_for_domain(){
		
		Map<String, Map<String, Integer>> result = new HashMap<String, Map<String, Integer>>();
		Map<String, Integer> counter = new HashMap<String, Integer>();
		// ovaj fleg koristim da bih znala da li uopste postoji url sa ovim domenom
		boolean flag = false;
		for (String key : this.keywords) counter.put(key, 0);
		
		for (Entry<String, Future<Map<String, Integer>>> entry : this.webResults.entrySet()) {
		    String url = entry.getKey();
		    if (url.startsWith("https://"+this.domain)) {
		    	flag = true;
		    	Map<String, Integer> map;
				
				try {
					for (Map.Entry<String, Integer> e : entry.getValue().get().entrySet()) {
						counter.put(e.getKey(), counter.get(e.getKey()) + e.getValue());  
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
		    }
		} 
		
		if(flag)  result.put(this.domain, counter);
		else result.put("nodomain", counter);
		return result;
	}
	
	
	

	@Override
	public Map<String, Map<String, Integer>> call() throws Exception {
		Map<String, Map<String, Integer>> result = null;
		if (this.type.equals(RetrieverJobType.DOMAIN_GET)) {
	    	result =  this.result_for_domain();
	    	return result;
	    }
		if (this.type.equals(RetrieverJobType.WEBSUMMARY)) {
	    	result =  this.summaryWeb();
	    	return result;
	    }
		if (this.type.equals(RetrieverJobType.FILESUMMARY)) {
	    	result =  this.summaryFile();
	    	return result;
	    }
		return result;
	}

}
