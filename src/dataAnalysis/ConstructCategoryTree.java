package dataAnalysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import simulator.database.DBConnection;
import util.IncrementalMean;
import util.IncrementalSD;

public class ConstructCategoryTree {
	//SELECT category, COUNT(DISTINCT a.listingId), AVG(b.amount) FROM auctions as a JOIN bids as b ON a.listingId=b.listingId AND a.winnerId=b.bidderId AND a.purchasedWithBuyNow=0 GROUP BY category;
	
	public static void main(String[] args) {
		System.out.println("Start.");
		go();
	}
	
	public static void go() {
		try {
			Connection conn = DBConnection.getTrademeConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT a.sellerId, a.listingId, a.winnerId, a.category, b1.amount FROM users u " + 
						"JOIN auctions a ON u.userId=a.sellerId " +
						"JOIN bids b1 ON a.listingId=b1.listingId " +
						"LEFT JOIN bids b2 ON b1.listingId=b2.listingId AND b1.amount < b2.amount " +
						"WHERE a.winnerId IS NOT NULL AND purchasedWithBuyNow=0 AND b2.amount IS NULL;");
			
			Node root = new Node("", Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			
			Multimap<String, Integer> catSellerMap = ArrayListMultimap.create(); // Map<sellerId, category>
			Multimap<String, Integer> catBuyerMap = ArrayListMultimap.create(); // Map<buyerId, category>
			HashMap<String, IncrementalSD> catMeanMap = new HashMap<>();
			while(rs.next()) {
				int sellerId = rs.getInt("sellerId");
				int buyerId = rs.getInt("winnerId");
				String category = rs.getString("category");
				double finalPrice = rs.getDouble("amount");
				
				catSellerMap.put(category, sellerId);
				catBuyerMap.put(category, buyerId);
				
				if (!catMeanMap.containsKey(category)) {
					catMeanMap.put(category, new IncrementalSD());
				}
				catMeanMap.get(category).addNext(finalPrice);
			}
			
			for (Entry<String, IncrementalSD> entry : catMeanMap.entrySet()) {
				addNodeToTree(root, Arrays.asList(entry.getKey().split("/")), entry.getValue().numElements(), entry.getValue().average());
			}
			
//			root.doRecursive(new Operation() {
//				@Override
//				public void op(Node node) {
//					combineSmallChildren(node);
//				}
//			});
			
			HashMap<String, Node> categories = flattenTree(root); 
			
			// print out list of categories
			for (Entry<String, Node> category : categories.entrySet()) {
				System.out.println(category.getKey() + "," + category.getValue().avgPrice.numElements() + "," + category.getValue().avgPrice.average());
			}
			
			// change the maps to take into account of the merging of small categories 
			for (String category : new HashSet<String>(catSellerMap.keySet())) {
				String tempCatString = category;
				while(!categories.containsKey(tempCatString)) {
					tempCatString = tempCatString.replaceAll("/MISC", "");
					tempCatString = tempCatString.substring(0, tempCatString.lastIndexOf("/"));
					tempCatString += "/MISC";
				}
				
				// remove the values using the old key, and put it in again with the new key
				catSellerMap.putAll(tempCatString, catSellerMap.removeAll(category));
				catBuyerMap.putAll(tempCatString, catBuyerMap.removeAll(category));
				
//				if (!catMeanMap.containsKey(tempCatString))
//					catMeanMap.put(tempCatString, new IncrementalSD());
//				IncrementalSD catMean = catMeanMap.get(category); 
//				catMeanMap.get(tempCatString).addAverage(catMean.numElements(), catMean.average());
				
//				System.out.println("looking for: " + category);
//				if (!categories.containsKey(category)) {
//					System.out.println("trimming to: " + category.substring(0, category.lastIndexOf("/")));
//					System.out.println(categories.containsKey(category.substring(0, category.lastIndexOf("/")) + "/MISC"));
//					
//				}
			}
			
			// flip the multimaps
//			Multimap<Integer, String> sellerCatMap = Multimaps.invertFrom(catSellerMap, ArrayListMultimap.<Integer, String>create());
//			for (int sellerId : sellerCatMap.keySet()) {
//				System.out.println(sellerId + ":" + sellerCatMap.get(sellerId).size() + ":" + new HashSet<String>(sellerCatMap.get(sellerId)).size());
//			}
			
//			Multimap<Integer, String> buyerCatMap = Multimaps.invertFrom(catBuyerMap, ArrayListMultimap.<Integer, String>create());
//			for (int buyerId : buyerCatMap.keySet()) {
//				System.out.println(buyerId + ":" + buyerCatMap.get(buyerId).size() + ":" + new HashSet<String>(buyerCatMap.get(buyerId)).size());
//			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void combineSmallChildren(Node node) {
		String combinedName = "MISC";
		int combinedFreq = 0;
		IncrementalMean avgPrice = new IncrementalMean();

		Iterator<Entry<String, Node>> it = node.children.entrySet().iterator();
		while(it.hasNext()) {
			Node child = it.next().getValue();
			if (child.avgPrice.numElements() < 200) {
				it.remove();
				combinedFreq += child.avgPrice.numElements();
				avgPrice.addAverage(child.avgPrice.numElements(), child.avgPrice.average());
			}
		}
		if (combinedFreq != 0)
			node.addChild(new Node(combinedName, combinedFreq, avgPrice.average(), true));
	}
	
	private static class Node {
		public Map<String, Node> children = Collections.emptyMap();
		public String name;
		public IncrementalMean avgPrice;
		public boolean isCategory;
		
		/**
		 * Copy constructor. Shallow copies the children collection.
		 * @param node
		 */
		public Node(Node node) {
			this.name = node.name;
			this.avgPrice = new IncrementalMean(node.avgPrice.numElements(), node.avgPrice.average());
			this.isCategory = node.isCategory;
			this.children = node.children; 
		}
		
		public Node(String name, int frequency, double avgPrice, boolean isCategory) {
			this.name = name;
			this.avgPrice = new IncrementalMean(frequency, avgPrice);
			this.isCategory = isCategory;
		}
		public void addChild(Node child) {
			if (children.equals(Collections.emptyMap())) {
				this.children = new HashMap<>();
			}
			children.put(name, child);
		}
		public void addChild(String name, int frequency, double avgPrice, boolean isCategory) {
			if (children.equals(Collections.emptyMap()))
				this.children = new HashMap<>();
				
			if (!children.containsKey(name)) {
				children.put(name, new Node(name, frequency, avgPrice, isCategory));
			} else {
				Node child = children.get(name); 
				child.avgPrice.addAverage(frequency, avgPrice);
				child.isCategory = children.get(name).isCategory || isCategory;
			}
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			return obj instanceof Node && name.equals(((Node) obj).name);
		}
		@Override
		public String toString() {
			return "(" + name + "," + avgPrice + ")";
		}
		
		public void doRecursive(Operation op) {
			op.op(this);
			for(Node child : children.values()) {
				child.doRecursive(op);
			}
		}
		
		public void printTree(String prefix) {
			if (!printCondition(this))
				return;
			System.out.println(toString());
			for(Node child : children.values()) {
				child.printTree(prefix + "-");
			}
		}
	}
	
	private interface Operation {
		void op(Node node);
	}
	
	private static HashMap<String, Node> flattenTree(Node root) {
		HashMap<String, Node> result = new HashMap<>();
		for (Node child : root.children.values()) {
			flattenTree(child, "", result);
		}
		return result;
	}

	private static void flattenTree(Node node, String ancestorNames, HashMap<String, Node> partialResult) {
		if (node.isCategory) {
			Node newNode = new Node(node);
			for (Node child : node.children.values())
				newNode.avgPrice.removeAverage(child.avgPrice.numElements(), child.avgPrice.average());
			partialResult.put(ancestorNames + node.name, newNode);
		}
		for (Node child : node.children.values()) {
				flattenTree(child, ancestorNames + node.name + "/", partialResult);
		}
	}
	
	private static boolean printCondition(Node node) {
		if (node.avgPrice.numElements() < 200)
			return false;
		return true;
	}
	
	public static void addNodeToTree(Node root, List<String> nameParts, int frequency, double avgPrice) {
		Node temp = root;
		for (int i = 0; i < nameParts.size(); i++) {
			String name = nameParts.get(i);
			if (i < nameParts.size() - 1)
				temp.addChild(name, frequency, avgPrice, false);
			else
				temp.addChild(name, frequency, avgPrice, true);
			temp = temp.children.get(name);
		}
	}
}
