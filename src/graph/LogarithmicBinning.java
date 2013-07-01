package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

import util.IncrementalMean;

import com.google.common.collect.ArrayListMultimap;

public class LogarithmicBinning {

	public static void main(String[] args) {
		LogarithmicBinning logBins = new LogarithmicBinning(1, 0.2, 1.2, 240);
		System.out.println(logBins.binStarts);
		logBins.addValue(1.05, 10);
		logBins.addValue(1.05, 8);
		logBins.addValue(1.05, 20);
		logBins.addValue(1.8, 30);
		logBins.addValue(2, 10);
		logBins.addValue(50, 40);
		System.out.println(logBins.binMedians());
	}

	// Multimap<BinIndex, Values>. Structure holding the values for all bins.
	final ArrayListMultimap<Integer, Double> binsContents = ArrayListMultimap.create();
	final ArrayList<Double> binStarts;
	
	int startingBinValue;
	double firstBinWidth;
	double binWidthGrowth;
	double maximumBinValue;
	public LogarithmicBinning(int startingBinValue, double firstBinWidth, double binWidthGrowth, double maximumBinValue) {
		assert (startingBinValue > 0);
		assert (firstBinWidth > 0);
		assert (binWidthGrowth > 1);
		assert (maximumBinValue > startingBinValue);
		
		this.startingBinValue = startingBinValue;
		this.firstBinWidth = firstBinWidth;
		this.binWidthGrowth = binWidthGrowth;
		this.maximumBinValue = maximumBinValue;
		
		binStarts = createBins();
	}
	
	/**
	 * Creates logarithmic bins.
	 * Calling with constructor parameters (1, 0.4, 2, 240) gives bins [1.0, 1.4, 2.2, 3.8, 7.0, 13.4, 26.23, 51.8, 103.0, 205.4, 410.2].
	 * 
	 * @param startingBinValue
	 * @param firstBinWidth
	 * @param binWidthGrowth
	 * @param maximumBinValue
	 * @return
	 */
	private ArrayList<Double> createBins() {
		ArrayList<Double> bins = new ArrayList<>();
		
		for (int i = 0; ; i++) {
			double binValue = startingBinValue + (FastMath.pow(binWidthGrowth, i) - 1) * firstBinWidth * firstBinWidth/(binWidthGrowth - 1);
			bins.add(binValue);
			if (binValue > maximumBinValue)
				break;
		}
		
		return bins;
	}
	
	public ArrayList<Double> getBins() {
		return binStarts;
	}
	
	/**
	 * 
	 * Calling with constructor parameters (1, 0.1, 2, 50) and value of 7 returns 6.
	 * I.e. bin index 6 contains the value 7. Similarly calling with a value of 1.05 returns 0.
	 * 
	 * @param value
	 * @return
	 */
	public int findBinIndex(double value) {
		assert value < startingBinValue : "Value " + value + " must be greater than startingBinValue " + startingBinValue + ".";
		return (int) (FastMath.log((value - startingBinValue) / firstBinWidth + 1) / FastMath.log(binWidthGrowth));
	}
	
	/**
	 * 
	 * @param v1 will be used to find the right log-bin to put v2
	 * @param v2 the value put into the bin
	 */
	public void addValue(double v1, double v2) {
		int binIndex = findBinIndex(v1);
		binsContents.put(binIndex, v2);
	}

	public void addValues(List<int[]> valuePairs) {
		for (int[] pair : valuePairs)
			addValue(pair[0], pair[1]);
	}

	
	public ArrayList<Double> binMedians() {
		ArrayList<Double> binsMedians = new ArrayList<>();
		
		for (int i = 0; i < binStarts.size() - 1; i++) {
			List<Double> binValues = binsContents.get(i);
			Collections.sort(binValues);
			
			int middleElement = binValues.size() / 2;
			if (binValues.isEmpty()) {
				binsMedians.add(Double.NaN);
			} else if (binValues.size() % 2 == 1) {
				binsMedians.add(binValues.get(middleElement));
			} else
				binsMedians.add((binValues.get(middleElement) + binValues.get(middleElement - 1)) / 2);
		}
		
		
		return binsMedians;
	}
	public ArrayList<Double> binMeans() {
		ArrayList<Double> binsMeans = new ArrayList<>();
		
		for (int i = 0; i < binStarts.size() - 1; i++) {
			List<Double> binValues = binsContents.get(i);
			IncrementalMean mean = new IncrementalMean();
			for (double value : binValues) {
				mean.addNext(value);
			}
			binsMeans.add(mean.average());
		}
		
		return binsMeans;
	}
	
}
