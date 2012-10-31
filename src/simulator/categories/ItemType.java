package simulator.categories;

import java.util.concurrent.atomic.AtomicInteger;


public class ItemType {
	
	private static final AtomicInteger itemTypeIdCount = new AtomicInteger();
	
	private final int id;
	double weight;
	private final String name;
	private final int trueValuation;
	private final int categoryId;
	
	public ItemType(double weight, String name, int trueValuation, int categoryId) {
		this.id = itemTypeIdCount.getAndIncrement();
		
		this.weight = weight;
		this.name = name;
		this.trueValuation = trueValuation;
		this.categoryId = categoryId;
	}
	
	public ItemType(int id, double weight, String name, int trueValuation, int categoryId) {
		this.id = id;
		
		this.weight = weight;
		this.name = name;
		this.trueValuation = trueValuation;
		this.categoryId = categoryId;
	}

	@Override
	public String toString() {
		return "(" +  id + "," + name + "," + weight + "," + trueValuation + "," + categoryId + ")";
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

	public int getCategoryId() {
		return categoryId;
	}

	public static void main(String[] args) {
		for (ItemType item : CreateItemTypes.mockItems(20, CreateCategories.mockCategories().getCategories())) {
			System.out.println(item);
		}
	}
}
