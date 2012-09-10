package util;

public class IncrementalMean {
	protected int numElements = 0;
	protected double average = 0;
	public void addNext(int newValue) {
		this.average = this.average + (newValue - this.average)/(++numElements);
	}
	public void addNext(double newValue) {
		this.average = this.average + (newValue - this.average)/(++numElements);
	}
	public void addNext(long newValue) {
		this.average = this.average + (newValue - this.average)/(++numElements);
	}
	public double getAverage() {
		if (numElements == 0)
			return Double.NaN;
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
