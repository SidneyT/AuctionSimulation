package util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Sample <T> {
	public final List<T> sample;
	public int seen;
	public final int sampleSize;
	public final Random r;
	
	public Sample(int sampleSize) {
		this.sampleSize = sampleSize;
		 sample = new ArrayList<>();
		 seen = 0;
		 sampleSize = 0;
		 r = new Random();
	}
	
	public void add(T element) {
		seen++;
		if (seen <= sampleSize) {
			sample.add(element);
		} else if (r.nextInt(seen) < sampleSize) {
				sample.set(r.nextInt(sampleSize), element);
		}
	}
	
	public List<T> getSample() {
		return sample;
	}
	
	
	public static <T> List<T> randomSample(Iterator<T> it, int sampleSize, Random r) {
		List<T> sample = new ArrayList<>(sampleSize);
		int seen = 0;
		while(it.hasNext()) {
			T item = it.next();
			seen++;
			if (seen <= sampleSize) {
				sample.add(item);
			} else if (r.nextInt(seen) < sampleSize) { // element kept with p=1/seen
				// remove a random element
				sample.set(r.nextInt(sampleSize), item);
			}
		}
		
		if (seen < sampleSize)
			throw new RuntimeException("Not enough elements to meet sample size; only " + seen + " but needed " + sampleSize + ".");
		return sample;
	}
	
	/**
	 * Floydfs Algorithm, from <code>http://eyalsch.wordpress.com/2010/04/01/random-sample/</code>
	 * @param items
	 * @param sampleSize
	 * @return
	 */
	public static <T> ArrayList<T> randomSample(List<T> items, int sampleSize, Random r){
	    HashSet<T> res = new HashSet<T>(sampleSize);
	    int n = items.size();
	    for(int i = n - sampleSize; i < n; i++){
	        int pos = r.nextInt(i+1);
	        T item = items.get(pos);
	        if (res.contains(item))
	            res.add(items.get(i));
	        else
	            res.add(item);
	    }
	    return new ArrayList<>(res);
	}

	/**
	 * Floyd's algorithm, selects numbers between 0 inclusive and maxValue exclusive.
	 * @param maxValue
	 * @param sampleSize
	 * @param r
	 * @return
	 */
	public static List<Integer> randomSample(int maxValue, int sampleSize, Random r){
	    HashSet<Integer> res = new HashSet<Integer>(sampleSize);
	    int n = maxValue;
	    for(int i = n - sampleSize; i < n; i++){
	        int pos = r.nextInt(i+1);
	        Integer item = pos;
	        if (res.contains(item))
	            res.add(i);
	        else
	            res.add(pos);
	    }
	    return new ArrayList<>(res);
	}

	public static void main(String[] args) {
		Random r = new Random();
		int[] counts = new int[20];
		for (int i = 0; i < 200000; i++) {
			List<Integer> results = randomSample(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19).iterator(), 10, r);
			for (int j : results) {
				counts[j]++;
			}
		}
		System.out.println(Arrays.toString(counts));
		
		System.out.println("Done");
	}
	
	
}
