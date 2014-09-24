package graph.tmEval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.CsvManipulation;
import util.IncrementalSD;

import com.google.common.collect.ArrayListMultimap;

public class CompareFeatureValues {

	/**
	 * Finds the average values for features for users in groupIds.
	 * 
	 * @param csvThing
	 * @param groupIds
	 * @param nullsInfinitiesAs0
	 * @return
	 */
	public static Map<String, Double> groupAverage(
			CsvManipulation.CsvThing csvThing, Iterable<Integer> groupIds,
			boolean nullsInfinitiesAs0) {
		HashMap<Integer, IncrementalSD> means = new HashMap<>();

		String[] headingRow = csvThing.headingRow;
		for (int i = 0; i < headingRow.length - 1; i++) {
			means.put(i, new IncrementalSD());
		}

		ArrayListMultimap<Integer, Double> featureValuesMap = csvThing.featureValues;
		for (int id : groupIds) {
			if (!featureValuesMap.containsKey(id))
				continue;

			List<Double> featureValues = featureValuesMap.get(id);
			for (int i = 0; i < featureValues.size(); i++) {
				Double value = featureValues.get(i);
				if (value == null || Double.isInfinite(value))
					if (nullsInfinitiesAs0)
						value = 0d;
					else
						continue;

				means.get(i).add(value);
			}
		}

		Map<String, Double> means2 = new HashMap<>();
		for (int i : means.keySet()) {
			String featureName = headingRow[i + 1];
			means2.put(featureName, means.get(i).average());
		}

		return means2;
	}

}
