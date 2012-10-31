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
	
	private final CategoryNode root;
	// Map<id, CategoryNode>
	private Map<Integer, CategoryNode> categoryMap;
	
	public CategoryRecord() {
		this.root = CategoryNode.createRoot();
		this.categoryMap = new HashMap<Integer, CategoryNode>();
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
	
	public static int randomCategory(Collection<CategoryNode> categories, double random) {
//		System.out.println("random num is: " + random);
		for (CategoryNode category : categories) {
			random -= category.getWeight();
//			System.out.println("tested: " + category + " random " + random + " remaining.");
			if (random < 0) {
//				System.out.println("<0, using " + category + ".");
				logger.debug("Random category is " + category.getName());
				return category.getId();
			}
		}
		throw new RuntimeException();
	}
	
}
