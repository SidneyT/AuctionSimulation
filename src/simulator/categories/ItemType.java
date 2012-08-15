package simulator.categories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import distributions.Exponential;

public class ItemType {
	private final int id;
	private double weight;
	private final String name;
	private final int trueValuation;
	private final CategoryNode category;
	
	public ItemType(int id, double weight, String name, int trueValuation, CategoryNode category) {
		this.id = id;
		this.weight = weight;
		this.name = name;
		this.trueValuation = trueValuation;
		this.category = category;
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
	
	public static ItemType pickType(Collection<ItemType> items, double random) {
		for (ItemType item : items) {
			random -= item.weight;
			if (random < 0)
				return item;
		}
		throw new RuntimeException("Sum of weights should've been 1.");
	}
	
	public static ArrayList<ItemType> createItems(int numberOfItems, Collection<CategoryNode> categories) {
		int itemId = 0;
		Random r = new Random();
		
		ArrayList<ItemType> items = new ArrayList<>();
		for (int i = 0; i < numberOfItems; i++) {
			Exponential exp1 = new Exponential(1);
			double weight = exp1.nextDouble();
			
			int averageValuation = 3000;
			int minimumValuation = 500;
			Exponential exp2 = new Exponential(averageValuation);
			int trueValuation;
			do {
			 trueValuation = (int) (exp2.nextDouble() + 0.5);
			} while (trueValuation < minimumValuation);
			ItemType item = new ItemType(itemId++, weight, "type" + (int) (weight * 10000), trueValuation, CategoryRecord.randomCategory(categories, r.nextDouble()));
			items.add(item);
		}
		
		normaliseWeights(items);
		
		return items;
	}
	
	@Override
	public String toString() {
		return "(" +  id + "," + name + "," + weight + "," + trueValuation + "," + category + ")";
	}
	
	public int getId() {
		return id;
	}

	public double getWeight() {
		return weight;
	}

	public String getName() {
		return name;
	}

	public int getTrueValuation() {
		return trueValuation;
	}

	public CategoryNode getCategory() {
		return category;
	}

	public static void main(String[] args) {
		for (ItemType item : createItems(20, MockCategories.createCategories().getCategories())) {
			System.out.println(item);
		}
	}
}
