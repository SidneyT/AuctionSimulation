package simulator.categories;

import java.util.ArrayList;
import java.util.List;

public class CategoryNode {
	
	private final int id;
	private List<CategoryNode> children;
	private CategoryNode parent;
	private String name;
	private double weight; // probability this category is chosen
	
	public int getId() {
		return this.id;
	}
	
	public CategoryNode(int id, String name) {
		this.id = id;
		this.name = name;
		children = new ArrayList<CategoryNode>();
	}
	
	public List<CategoryNode> getChildren() {
		return children;
	}
	public void setChildren(List<CategoryNode> children) {
		this.children = children;
	}
	
	public void addChild(CategoryNode child) {
		this.children.add(child);
	}
	
	public CategoryNode getParent() {
		return parent;
	}
	public void setParent(CategoryNode parent) {
		this.parent = parent;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	private boolean isRoot = false;
	public static final CategoryNode createRoot(int id) {
		CategoryNode root = new CategoryNode(id, "root");
		root.isRoot = true;
		return root;
	}
	
	public boolean isRoot() {
		return isRoot;
	}

	@Override
	public String toString() {
		return "(" + id + "," + name + "," + weight + ")";
	}
}
