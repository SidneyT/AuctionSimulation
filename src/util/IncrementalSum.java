package util;

public class IncrementalSum {
	protected int numElements;
	protected double sum;
	
	public IncrementalSum() {
		numElements = 0;
		sum = 0;
	}
	
	public IncrementalSum(int numElements, double sum) {
		this.numElements = numElements;
		this.sum = sum;
	}
	
	public IncrementalSum add(double newValue) {
		sum += newValue;
		numElements++;
		assert !Double.isNaN(this.sum);
		
		return this;
	}
	
	public IncrementalSum add(Iterable<? extends Number> values) {
		for (Number value : values) {
			add(value.doubleValue());
		}
		return this;
	}


	public int numElements() {
		return numElements;
	}
	
	public double sum() {
		return sum;
	}
	
	@Override
	public String toString() {
		return "(sum: " + this.sum + ", numEle: " + this.numElements + ")";
	}
}
