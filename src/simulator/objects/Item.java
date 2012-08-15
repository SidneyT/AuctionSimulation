package simulator.objects;

import simulator.categories.ItemType;

public class Item {
	private final ItemType type;
	private final String name;
	
	public Item(ItemType type, String name) {
		this.type = type;
		this.name = name;
	}

	public ItemType getType() {
		return type;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "(" + type + "," + name + ")";
	}

}