package util;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	
	public static <T> List<T> getSample(Iterator<T> it, int sampleSize) {
		List<T> sample = new ArrayList<>(sampleSize);
		int seen = 0;
		Random r = new Random();
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
	
	public static void main(String[] args) {
		int[] counts = new int[20];
		
		for (int i = 0; i < 200000; i++) {
			List<Integer> results = getSample(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19).iterator(), 10);
			for (int j : results) {
				counts[j]++;
			}
		}
		System.out.println(Arrays.toString(counts));
		System.out.println("Done");
	}
}
