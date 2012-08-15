package simulator.categories;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Not thread safe
 */
public class CategoryRecord {
	
	private static final Logger logger = Logger.getLogger(CategoryRecord.class);
	
	private CategoryNode root;
	// Map<id, CategoryNode>
	private Map<Integer, CategoryNode> categoryMap;
	
	public CategoryRecord() {
		this.root = CategoryNode.createRoot(nextId());
		this.categoryMap = new HashMap<Integer, CategoryNode>();
	}
	
	private int idCount = 0;
	
	public int nextId() {
		return idCount++; 
	}
	
	public CategoryNode getRoot() {
		return this.root;
	}
	
	public Collection<CategoryNode> getCategories() {
		return categoryMap.values();
	}
	
	public void addCategory(CategoryNode category) {
		assert(category.getId() != -1);
		this.categoryMap.put(category.getId(), category);
		
//		SaveObjects.saveCategory(category);

	}
	
	public static CategoryNode randomCategory(Collection<CategoryNode> categories, double random) {
//		System.out.println("random num is: " + random);
		for (CategoryNode category : categories) {
			random -= category.getWeight();
//			System.out.println("tested: " + category + " random " + random + " remaining.");
			if (random < 0) {
//				System.out.println("<0, using " + category + ".");
				logger.debug("Random category is " + category.getName());
				return category;
			}
		}
		assert false;
		throw new RuntimeException();
	}
	
}
