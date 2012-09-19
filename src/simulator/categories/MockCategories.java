package simulator.categories;

public class MockCategories {

	public static CategoryRecord createCategories() {
		
		CategoryRecord cr = new CategoryRecord();
		CategoryNode root = cr.getRoot(); 
		
		CategoryNode art = new CategoryNode("art");
		art.setWeight(0.05);
		root.addChild(art);
		cr.addCategory(art);
		
		CategoryNode books = new CategoryNode("books");
		books.setWeight(0.05);
		root.addChild(books);
		cr.addCategory(books);
		
		CategoryNode clothing = new CategoryNode("clothing");
		clothing.setWeight(0.15);
		root.addChild(clothing);
		cr.addCategory(clothing);
		
		CategoryNode health = new CategoryNode("health");
		health.setWeight(0.05);
		root.addChild(health);
		cr.addCategory(health);
		
		CategoryNode movies = new CategoryNode("movies");
		movies.setWeight(0.2);
		root.addChild(movies);
		cr.addCategory(movies);
		
		CategoryNode sports = new CategoryNode("sports");
		sports.setWeight(0.1);
		root.addChild(sports);
		cr.addCategory(sports);

		CategoryNode electronics = new CategoryNode("electronics");
		electronics.setWeight(0.4);
		root.addChild(electronics);
		cr.addCategory(electronics);

		return cr;
	}
	
}
