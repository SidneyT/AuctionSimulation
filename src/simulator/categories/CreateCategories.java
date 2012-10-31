package simulator.categories;

import java.util.Arrays;
import java.util.List;

public class CreateCategories {

	public static CategoryRecord mockCategories() {
		
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

	public static CategoryRecord TMCategories() {
		
		CategoryRecord cr = new CategoryRecord();
		CategoryNode root = cr.getRoot(); 

		List<String> categoryNames = Arrays.asList("Clothing","Antiques-collectables","Home-living","Books","Sports",
				"Toys-models","Baby-gear","Jewellery-watches","Building-renovation","Crafts","Electronics-photography",
				"Pottery-glass","Movies-TV","Music-instruments","Business-farming-industry","Trade-Me-Car-parts",
				"Computers","Health-beauty","Gaming","Mobile-phones","Pets-animals","Trade-Me-Motorbikes");
		List<Double> categoryWeights = Arrays.asList(0.223110923,0.108397908,0.107774501,0.08396661,0.057016751,
				0.054610402,0.039723457,0.039530201,0.029200357,0.028963462,0.027074541,0.026712965,0.024356489,
				0.021675841,0.02158233,0.021239457,0.020659689,0.020242006,0.019294429,0.00872769,0.00839105,0.007748942);
		for (int i = 0; i < categoryNames.size(); i++) {
			CategoryNode category = new CategoryNode(categoryNames.get(i));
			category.setWeight(categoryWeights.get(i));
			category.setParent(root);
		}
		
		return cr;
	}

}
