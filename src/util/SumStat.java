package util;

import java.util.List;

public enum SumStat implements SumStatI {
	Mean {
		@Override
		public double summaryValue(List<Double> values) {
			IncrementalMean mean = new IncrementalMean();
			for (double value : values) {
				mean.add(value);
			}
			return mean.average();
		}
	},
	Median {
		@Override
		public double summaryValue(List<Double> values) {
			int middleElement = values.size() / 2;
			double median;
			if (values.isEmpty()) {
				median = Double.NaN;
			} else if (values.size() % 2 == 1) {
				median = values.get(middleElement);
			} else
				median = (values.get(middleElement) + values.get(middleElement - 1)) / 2;
			
			return median;
		}
	},
	Max {
		@Override
		public double summaryValue(List<Double> values) {
			double currentMax = Double.NEGATIVE_INFINITY;
			for (double value : values) {
				if (value > currentMax)
					currentMax = value;
			}
			return currentMax;
		}
	};
}
