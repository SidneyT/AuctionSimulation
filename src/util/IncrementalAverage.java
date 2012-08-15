package util;

public class IncrementalAverage {
	private int numElements = 0;
	private double average;
	public void incrementalAvg(int newValue) {
		this.average = this.average + (newValue - this.average)/(++numElements);
	}
	public void incrementalAvg(double newValue) {
		this.average = this.average + (newValue - this.average)/(++numElements);
	}
	
	public double getAverage() {
		return average;
	}
	public int getNumElements() {
		return numElements;
	}
	
	@Override
	public String toString() {
		return "(avg: " + this.average + ", numEle: " + this.numElements + ")";
	}
}
