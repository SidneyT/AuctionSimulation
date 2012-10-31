package simulator.categories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;

public class CreateItemTypes {

	public static ArrayList<ItemType> mockItems(int numberOfItems, Collection<CategoryNode> categories) {
		Random r = new Random();
		
		ArrayList<ItemType> items = new ArrayList<>();
		ExponentialDistribution exp1 = new ExponentialDistribution(1);
		int averageValuation = 6195; // from TM data
		int minimumValuation = 400;
		ExponentialDistribution exp2 = new ExponentialDistribution(averageValuation);
		
		for (int i = 0; i < numberOfItems; i++) {
			double weight = exp1.sample();
	
			int trueValuation;
			do {
				trueValuation = (int) (exp2.sample() + 0.5);
			} while (trueValuation < minimumValuation);
			
			ItemType item = new ItemType(weight, "type" + (int) (weight * 10000), trueValuation, CategoryRecord.randomCategory(categories, r.nextDouble()));
			items.add(item);
		}
		
		CreateItemTypes.normaliseWeights(items);
		return items;
	}

	public static ItemType pickType(Collection<ItemType> items, double random) {
		for (ItemType item : items) {
			random -= item.weight;
			if (random < 0)
				return item;
		}
		throw new RuntimeException("Sum of weights should've been 1.");
	}

	public static void normaliseWeights(Collection<ItemType> items) {
		double weightSum = 0;
		for (ItemType item : items) {
			weightSum += item.weight;
		}
		for (ItemType item : items) {
			item.weight /= weightSum;
		}
	}

	/**
	 * Creates items corresponding to TradeMe categories. For each category, there is 1 item.
	 * @param numberOfItems
	 * @param categories
	 * @return
	 */
	public static ArrayList<ItemType> TMItems(List<CategoryNode> categories) {
		ArrayList<ItemType> items = new ArrayList<>();
		List<Double> averagePrice = Arrays.asList(1667.977954,3806.576259,5564.987737,1141.784542,5544.632408,
				2535.696347,1579.006121,2827.319666,8378.555295,1229.761731,5817.559521,3235.879813,1034.310468,
				3948.616336,14056.42692,6989.800117,6240.8449,2182.868802,3575.955412,8160.105714,3509.499257,12845.89461);

		for (int i = 0; i < categories.size(); i++) {
			CategoryNode category = categories.get(i); 
			double weight = category.getWeight();
			ItemType item = new ItemType(weight, "type" + (int) (weight * 10000), (int) (averagePrice.get(i) + 0.5), category.getId());
			items.add(item);
		}

		return items;
	}
	
}
