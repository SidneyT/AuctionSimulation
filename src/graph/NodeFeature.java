package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;

import util.SumStatI;
import util.Util;
import weka.core.matrix.EigenvalueDecomposition;
import weka.core.matrix.Matrix;

import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public enum NodeFeature implements NodeFeatureI {

	/**
	 * Finds the number of edges for the given user's egonet. Assumes directed graph.
	 * @param adjacencyList
	 */
	EgonetEdgeCount {
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			Map<Integer, Multiset<Integer>> egonet = GraphOperations.egonetAdjacencyMatrix(adjacencyList, user);
			int edgeCount = 0;
			for (Multiset<Integer> nodeNeighbours : egonet.values()) {
				edgeCount += nodeNeighbours.elementSet().size();
			}
			return edgeCount;
		}
	},

	/**
	 * Finds the total edge weight for the given user's egonet. Assumes directed graph.
	 * @param adjacencyList
	 */
	EgonetWeightCount {
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			Map<Integer, Multiset<Integer>> egonet = GraphOperations.egonetAdjacencyMatrix(adjacencyList, user);
			int totalWeight = 0;
			for (Multiset<Integer> nodeNeighbours : egonet.values()) {
				totalWeight += nodeNeighbours.size();
			}
			return totalWeight;
		}
	},
	NodeEdgeCount {
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			if (!adjacencyList.containsKey(user))
				return 0;
			else 
				return adjacencyList.get(user).elementSet().size();
		}
	},
	NodeWeightCount {
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			if (!adjacencyList.containsKey(user))
				return 0;
			else 
				return adjacencyList.get(user).size();
		}
	}, 
	/**
	 * Number of times that a known user is seen. (Turns out to be node edge weight - node edge count)
	 */
	NodeRepeatCount {
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			Multiset<Integer> neighbours = adjacencyList.get(user);
			int uniqueNeighbours = neighbours.elementSet().size();
			int repeatCount = neighbours.size() - uniqueNeighbours;
			
			return repeatCount;
		}
	},
	/**
	 * Number of users that have been seen twice or more.
	 */
	NodeRepeatCountUnique {
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			Multiset<Integer> neighbours = adjacencyList.get(user);
			int count = 0;
			for (Integer neighbour : neighbours.elementSet()) {
				int timesSeen = neighbours.count(neighbour);
				if (timesSeen > 1) {
					count++;
				}
			}
			return count;
		}
	};

	/**
	 * Finds the jaccard value between this user and each of its neighbours. These values are then combined.
	 * </br>
	 * How these values are combined into 1 depends on the SumStatI parameter.
	 */
	public static Jaccard jaccard(final SumStatI type) {
		return new Jaccard(type);
	}
	
	private static class Jaccard implements NodeFeatureI {
		SumStatI type;
		public Jaccard(SumStatI type) {
			this.type = type;
		}
		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			Set<Integer> bidderNeighbours = adjacencyList.get(user).elementSet(); // set of bidder's neighbours
			
			ArrayList<Double> neighbourJaccard = new ArrayList<>(bidderNeighbours.size()); 
			int i = 0; // index
			
			for (Integer neighbour : bidderNeighbours) {
				Set<Integer> neighbourNeighbours;
				if (adjacencyList.containsKey(neighbour)) {
					neighbourNeighbours = adjacencyList.get(neighbour).elementSet(); // set of neighbour's neighbours
				} else { 
					neighbourNeighbours = Collections.emptySet();
				}
					
				double jaccard;
				// calculate jaccard index
				int aAndB = Sets.intersection(bidderNeighbours, neighbourNeighbours).size();
				int aOrB = Sets.union(bidderNeighbours, neighbourNeighbours).size();
				jaccard = (double) aAndB / aOrB;
				neighbourJaccard.add(jaccard);
			}
			
			double maxJaccard = type.summaryValue(neighbourJaccard);
			return maxJaccard;
		}
		
		@Override
		public String toString() {
			return "Jaccard_" + type;
		}
	}
	
	/**
	 * Returns the first eigenvalue of node's egonet.
	 * Matrix should be symmetric.
	 */
	public static FirstEigenvalue firstEigenvalue_sym() {
		return new FirstEigenvalue();
	}

	public static class FirstEigenvalue implements NodeFeatureI {
		HashMap<Integer, Double> firstEigenvalues = null;
		
		private void calculateEigenvalues(Map<Integer, Multiset<Integer>> adjacencyList) {
			firstEigenvalues = new HashMap<>();
			for (int userId : adjacencyList.keySet()) {
				Map<Integer, Multiset<Integer>> egonet = GraphOperations.egonetAdjacencyMatrix(adjacencyList, userId);
				
				// convert egonet adjacency list to a matrix
				double[][] matrix = new double[egonet.size()][egonet.size()]; 
				List<Integer> ids = new ArrayList<>(egonet.keySet());
				for (int neighbour : egonet.keySet()) {
					int index = ids.indexOf(neighbour);
					for (int nneighbour : egonet.get(neighbour)) {
						matrix[index][ids.indexOf(nneighbour)]++;
					}
				}
				
				DoubleMatrix eigenValues = Eigen.symmetricEigenvalues(new DoubleMatrix(matrix));
				firstEigenvalues.put(userId, eigenValues.max());
			}
		}

		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			if (firstEigenvalues == null)
				calculateEigenvalues(adjacencyList);
			return firstEigenvalues.get(user);
		}
		
		@Override
		public String toString() {
			return "FirstEigenvalue";
		}
	}
	
	public static FirstEigenvalueAsym firstEigenvalue_asym(Map<Integer, Multiset<Integer>> adjacencyList) {
		return new FirstEigenvalueAsym(adjacencyList);
	}
	
	public static class FirstEigenvalueAsym implements NodeFeatureI {
		final Map<Integer, Multiset<Integer>> adjacencyList;
		final HashMap<Integer, Double> firstEigenvalues = new HashMap<>();
		
		public FirstEigenvalueAsym(Map<Integer, Multiset<Integer>> adjacencyList) {
			this.adjacencyList = adjacencyList;
			
			calculateEigenvalues();
		}
		
		private void calculateEigenvalues() {
			for (int userId : adjacencyList.keySet()) {
				Map<Integer, Multiset<Integer>> egonet = GraphOperations.egonetAdjacencyMatrix(adjacencyList, userId);
				
				// convert egonet adjacency list to a matrix
				double[][] matrix = new double[egonet.size()][egonet.size()]; 
				List<Integer> ids = new ArrayList<>(egonet.keySet());
				for (int neighbour : egonet.keySet()) {
					int index = ids.indexOf(neighbour);
					for (int nneighbour : egonet.get(neighbour)) {
						matrix[index][ids.indexOf(nneighbour)]++;
					}
				}
				
				ComplexDoubleMatrix eigenValues = Eigen.eigenvalues(new DoubleMatrix(matrix));
				firstEigenvalues.put(userId, eigenValues.getReal().max());
			}
		}

		@Override
		public double value(Map<Integer, Multiset<Integer>> adjacencyList, int user) {
			return firstEigenvalues.get(user);
		}
		
		@Override
		public String toString() {
			return "FirstEigenvalue";
		}
	}
//	static HashMap<Integer, HashMap<Integer, Multiset<Integer>>> cachedEgonetAdjacencyMatricies = new HashMap<>(); // Map<userId, Map<neighbourId, neighbour's neighbours>>
	public static Map<EdgeTypeI, Map<Integer, Map<Integer, Multiset<Integer>>>> cachedEgonetAdjacencyMatricies = new HashMap<>();
	public static Map<Integer, Map<Integer, Multiset<Integer>>> findAllEgonets(EdgeTypeI edgeType, Map<Integer, Multiset<Integer>> adjacencyList) {
		Map<Integer, Map<Integer, Multiset<Integer>>> egonets = new HashMap<>();
		for (int user : adjacencyList.keySet()) {
			Map<Integer, Multiset<Integer>> egonet = GraphOperations.egonetAdjacencyMatrix(adjacencyList, user);
			egonets.put(user, egonet);
		}
		cachedEgonetAdjacencyMatricies.put(edgeType, egonets);
		return egonets;
	}
	
//	static HashMap<Integer, Multiset<Integer>> egonetAdjacencyMatrix(EdgeType edgeType, Map<Integer, Multiset<Integer>> adjacencyList, int user) {
//	}
	
	public static HashMap<Integer, Double> values(Map<Integer, Multiset<Integer>> adjacencyList, NodeFeatureI feature) {
		HashMap<Integer, Double> featureValue = new HashMap<>();
		for (Integer user : adjacencyList.keySet()) {
			double weight = feature.value(adjacencyList, user);
			featureValue.put(user, weight);
		}
		return featureValue;
	}
	
	/**
	 * Similar to {@link #values(Map, NodeFeatureI)} but only users with ids contained in wantedUsers are returned.
	 * @param adjacencyList
	 * @param feature
	 * @param wantedUsers
	 * @return
	 */
	public static HashMap<Integer, Double> values(Map<Integer, Multiset<Integer>> adjacencyList, NodeFeatureI feature, Set<Integer> wantedUsers) {
		HashMap<Integer, Double> featureValue = new HashMap<>();
		for (Integer user : wantedUsers) {
			// only want to find values for users we are interested in
			if (!adjacencyList.containsKey(user)) {
				continue;
			}
			
			double weight = feature.value(adjacencyList, user);
			featureValue.put(user, weight);
		}
		return featureValue;
	}
	
}
