import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Double> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Double> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Double getRelevance(String url) {
		Double relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private  void print() {
		List<Entry<String, Double>> entries = sort();
		for (Entry<String, Double> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        // FILL THIS IN!
        Map<String, Double> res = new HashMap<String, Double>();
        res.putAll(that.map);
        for (Entry<String, Double> entry: map.entrySet()) {
			Double val = (res.get(entry.getKey()) == null) ? 0.0 : res.get(entry.getKey());
			res.put(entry.getKey(), val+entry.getValue());
		}
		return new WikiSearch(res);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        // FILL THIS IN!
        Map<String, Double> res = new HashMap<String, Double>();
        for (Entry<String, Double> entry: map.entrySet()) {
			Double val = that.map.get(entry.getKey());
        	if (val != null) {
        		res.put(entry.getKey(), val+entry.getValue());
        	}
		}
		return new WikiSearch(res);
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        // FILL THIS IN!
		Map<String, Double> res = new HashMap<String, Double>();
        for (Entry<String, Double> entry: map.entrySet()) {
        	if (!that.map.containsKey(entry.getKey())) {
        		res.put(entry.getKey(), entry.getValue());
        	}
		}
		return new WikiSearch(res);
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Double>> sort() {
		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(map.entrySet());
		Comparator<Entry<String, Double>> comparator = new Comparator<Entry<String, Double>>() {
            @Override
            public int compare(Entry<String, Double> entry1, Entry<String, Double> entry2) {
                return (entry2.getValue()).compareTo(entry1.getValue());
            }
        };
        Collections.sort(list, comparator);
		return list;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		//Map<String, Integer> map = index.getCounts(term);
		String[] arr = term.split(" "); // split into terms

		Map<String, Double> map = new HashMap<>();

		// get total number of docs in corpus
		int col = index.getCollection();

		// interate over each term and a calc tf-idf
		for(int i = 0; i < arr.length; i++) {
			String curr = arr[i];

			Map<String, Integer> termMap = index.getCounts(curr);

			// find the number of documents where the term appears
			int relDocs = index.getURLs(curr).size();

			for(String url: termMap.keySet()) {
				int termCount = termMap.get(url);
				double tf_idf = termFrequency(termCount) * idf(col, relDocs);
				if(map.containsKey(url)) {
					map.put(url, map.get(url) + tf_idf);
				} else {
					map.put(url, tf_idf);
				}
			}
		}
		return new WikiSearch(map);
	}

	private static double termFrequency(int termCount) {
		return 1 + Math.log10(termCount);
	}

	private static double idf(int collection, int relevantDocs) {
		return Math.log10(collection/relevantDocs);
	}

	public static void main(String[] args) throws IOException {
		
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		
		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();
		
		// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();
		
		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
	}
}
