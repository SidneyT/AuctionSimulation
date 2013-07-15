package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import util.SumStatI;
import util.Util;
import weka.core.matrix.EigenvalueDecomposition;
import weka.core.matrix.Matrix;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

enum NodeFeature implements NodeFeatureI {

	/**
	 * Finds the number of edges for the given user's egonet. Assumes directed graph.
	 * @param adjacencyList
	 */
	EgonetEdgeCount {
		@Override
		public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
			HashMap<Integer, Multiset<Integer>> egonet = egonetAdjacencyMatrix(adjacencyList, user);
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
	EgonetWeight {
		@Override
		public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
			HashMap<Integer, Multiset<Integer>> egonet = egonetAdjacencyMatrix(adjacencyList, user);
			int totalWeight = 0;
			for (Multiset<Integer> nodeNeighbours : egonet.values()) {
				totalWeight += nodeNeighbours.size();
			}
			return totalWeight;
		}
	},
	NodeNeighbourCount {
		@Override
		public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
		if (!adjacencyList.containsKey(user))
			return 0;
		else 
			return adjacencyList.get(user).elementSet().size();
		}
	},
	NodeWeightCount {
		@Override
		public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
		if (!adjacencyList.containsKey(user))
			return 0;
		else 
			return adjacencyList.get(user).size();
		}
	},
	WeightEveness {
		@Override
		public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
			return 0;
		}
//	},
//	EgonetPrincipleComponent {
//		@Override
//		public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
////			System.out.println("doing user : " + user);
//			HashMap<Integer, Multiset<Integer>> egonet = egonetAdjacencyMatrix(adjacencyList, user);
//			// convert to a double[][]
//			
//			// map each user id to a consecutive index
//			HashMap<Integer, Integer> mapping = new HashMap<>();
//			int index = 0;
//			for (Integer egonetUser : egonet.keySet()) {
//				mapping.put(egonetUser, index++);
//			}
//			
//			double[][] adjacencies = new double[egonet.size()][egonet.size()];
//			for (Integer egonetUser : egonet.keySet()) {
//				int mappedEgonetUser = mapping.get(egonetUser);
//				Multiset<Integer> neighbourList = egonet.get(egonetUser);
//				Set<Integer> neighbourSet = egonet.get(egonetUser).elementSet();
//				for (Integer neighbour : neighbourSet) {
//					int edgeWeight = neighbourList.count(neighbour);
//					adjacencies[mappedEgonetUser][mapping.get(neighbour)] = edgeWeight;
//				}
//			}
//			
//			Matrix matrix = new Matrix(adjacencies);
//			double eigenvalue = -1;
////			if (matrix.getArray().length < 5) {
////				EigenvalueDecomposition eig = matrix.eig();
////				double[] eigenvalues = eig.getRealEigenvalues();
////				eigenvalue = eigenvalues[0];
////			} else {
//				EigenvalueDecomposition eig = matrix.eig();
//				double[] eigenvalues = eig.getRealEigenvalues();
//				eigenvalue = eigenvalues[0];
////				double firstEigenvalue = powerIteration(matrix);
//				temporary.Matrix matrixT = new temporary.Matrix();
//				matrixT.element = adjacencies;
//				matrixT.columns = matrix.getColumnDimension();
//				matrixT.rows = matrix.getRowDimension();
//				double firstEigenvalue = matrixT.leig(0.001);
//				System.out.println(firstEigenvalue + " vs " + eigenvalue);
//				System.out.print("");
////			}
//			return eigenvalue;
//		}
	};
	
	public static NodeFeatureI jaccard(final SumStatI type) {
		return new NodeFeatureI() {
			@Override
			public double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
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
		};
	}
	
//	private static double powerIteration(Matrix matrix) {
//		double error = Double.MAX_VALUE;
//		Matrix y = new Matrix(randomVector(matrix.getArray().length));
//		double theta = Double.NaN;
//		while (error > 0.00000000001) {
//			Matrix v = y.times(1 / y.normF());
//			y = matrix.times(v);
//			theta = dotProduct(v, y);
//			error = y.minus(v.times(theta)).normF();
//		}
//		return theta;
//	}
//	static Random r = new Random();
//	private static double[][] randomVector(int numElements) {
//		double[][] vector = new double[numElements][1];
//		int sumSq = 0;
//		for (int i = 0; i < numElements; i++) {
//			int val = r.nextInt(19) + 1;
//			vector[i][0] = val;
//			sumSq += val * val;
//		}
//		double length = Math.sqrt(sumSq);
//		for (int i = 0; i < numElements; i++) {
//			vector[i][0] = vector[i][0] / length;
//		}
//		return vector;
//	}
//	private static double dotProduct(Matrix vector1, Matrix vector2) {
//		double sum = 0;
//		for (int i = 0; i < vector1.getArray().length; i++) {
//			sum += vector1.getArray()[i][0] * vector2.getArray()[i][0];
//		}
//		return sum;
//	}
	
	static HashMap<Integer, HashMap<Integer, Multiset<Integer>>> cachedEgonetAdjacencyMatricies = new HashMap<>();
	
	/**
	 * Wrapper for caching egonets.
	 * @param adjacencyList
	 * @param user
	 * @return
	 */
	static HashMap<Integer, Multiset<Integer>> egonetAdjacencyMatrix(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
		if (cachedEgonetAdjacencyMatricies.containsKey(user)) {
			return cachedEgonetAdjacencyMatricies.get(user);
		} else {
			HashMap<Integer, Multiset<Integer>> egonet = egonetAdjacencyMatrixInner(adjacencyList, user);
			cachedEgonetAdjacencyMatricies.put(user, egonet);
			return egonet;
		}
	}
	
	/**
	 * Returns the adjacency list of <code>user</code>'s egonet. Frequency in the multiset represents the weight of that edge.
	 * @param adjacencyList
	 * @param user
	 * @return
	 */
	private static HashMap<Integer, Multiset<Integer>> egonetAdjacencyMatrixInner(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user) {
		if (!adjacencyList.containsKey(user)) {
			throw new RuntimeException("User does not exist in the adjacency list.");
		}
		
		HashMap<Integer, Multiset<Integer>> egonetAdjacency = new HashMap<>();
		HashMultiset<Integer> neighbours = adjacencyList.get(user);
		
		// construct the adjacency list for the egonet: the user and its neighbours
		egonetAdjacency.put(user, HashMultiset.<Integer>create(neighbours));
		for (Integer neighbour : neighbours) {
			egonetAdjacency.put(neighbour, HashMultiset.<Integer>create());
		}
		
		for (Integer neighbour : neighbours) {
			Multiset<Integer> neighbourNeighbours = egonetAdjacency.get(neighbour);
			
			if (adjacencyList.containsKey(neighbour)) {
				HashMultiset<Integer> nns = adjacencyList.get(neighbour);
				for (Integer nn : nns) {
					if (egonetAdjacency.containsKey(nn)) { // if this edge connects to someone in the egonet, add it to the adjacency list.
						neighbourNeighbours.add(nn);
					}
				}
			}
		}
		
		return egonetAdjacency;
	}

	public static HashMap<Integer, Double> values(HashMap<Integer, HashMultiset<Integer>> adjacencyList, NodeFeatureI feature) {
		HashMap<Integer, Double> edgeCounts = new HashMap<>();
		for (Integer user : adjacencyList.keySet()) {
			double weight = feature.value(adjacencyList, user);
			edgeCounts.put(user, weight);
		}
		return edgeCounts;
	}

}
