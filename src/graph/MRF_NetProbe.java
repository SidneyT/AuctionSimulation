package graph;

import graph.outliers.evaluation.ConvergenceMonitor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

public class MRF_NetProbe {

	final int nodeCount;
	final Map<Integer, Integer> originalToConsecutive;
	final int[] consecutiveToOriginal;
	
	Multiset<Integer>[] graph;
	Map<Integer, Double> origOutlierScores; // outlier scores given by LOF. ids have not been changed
	final double[][] initialBeliefs;
	boolean[] observationClassification; // initialising belief of whether a user is fraudulent or normal
	double[] fraudBeliefs;
	double[] accompliceBeliefs;
	double[] normalBeliefs;
	
	double[][] partialMessages; // double[][fraud belief, accomplice belief, normal Belief]
	double[][] messageFor; // stores messages for nodes. message for a node is in the corresponding index, e.g. messageFor[node][0] and messageFor[node][1]
	
	Integer ofInterest = -1;
	
	
	// author params
//	public static double eo = 0.2;
//	public static double ep = 0.05;
	
	// tuned using synthetic data
	public static double eo = 0.04;
	public static double ep = 0.12;

	// observation matrix
	private double[][] obsMat = new double[][]{
		new double[]{1 - eo, 	eo}, 
		new double[]{eo, 		eo}, 
		new double[]{eo, 		1 - eo}};
	
	// fraud, accomplice, honest matrix
	private double[][] propMat = new double[][]{
			new double[]{ep, 	1d - 2d*ep, 	ep}, 
			new double[]{0.5, 	2d * ep, 		0.5 - 2d*ep},
			new double[]{ep, 	(1d - ep)/2, 	(1d - ep)/2}};

	public final ConvergenceMonitor cm;
	
	public MRF_NetProbe(Map<Integer, Multiset<Integer>> graph, Map<Integer, Double> outlierScores) {
		this.nodeCount = graph.keySet().size();
		this.origOutlierScores = outlierScores;
		
		this.cm = new ConvergenceMonitor();
		
		for (Integer id : graph.keySet()) {
			if (!outlierScores.containsKey(id)) {
				System.out.println("uh oh: " + id);
			}
		}
		for (Integer id : outlierScores.keySet()) {
			if (!graph.containsKey(id)) {
				System.out.println("uh oh2: " + id);
			}
		}
		
		/**
		 * Map the user ids to a set of consecutive integers. </br>
		 * Remember the mapping from original to the new consecutive values, and vice versa.
		 */
		{
			Map<Integer, Integer> originalToConsecutive = new HashMap<>();
			int[] consecutiveToOriginal = new int[nodeCount];
			int count = 0;
			for (Integer origId : graph.keySet()) {
				originalToConsecutive.put(origId, count);
				consecutiveToOriginal[count] = origId;

//				 TODO: can remove
				if (origId.equals(""))
//				if (origId.equals(500521))
					ofInterest = count;
				count++;
			}
			
			this.originalToConsecutive = originalToConsecutive;
			this.consecutiveToOriginal = consecutiveToOriginal;
		}
		
		/**
		 * Replace original ids in the graph with the new id values, and
		 * store the mapping.
		 */
		{
			this.graph = (Multiset<Integer>[]) Array.newInstance(Multiset.class, nodeCount);
			for (int conseId = 0; conseId < consecutiveToOriginal.length; conseId++) {
				// map the consecutive key to the original
				int origId = consecutiveToOriginal[conseId];
				
				HashMultiset<Integer> conseNeighbourIds = HashMultiset.create();
				// map the neighbours to the new ids
				Multiset<Integer> neighbours = graph.get(origId);
				for (Integer neighbour : neighbours.elementSet()) {
					int conseNeighbourId = originalToConsecutive.get(neighbour);
					conseNeighbourIds.add(conseNeighbourId, neighbours.count(neighbour));
				}
				
				this.graph[conseId] = ImmutableMultiset.copyOf(conseNeighbourIds);
			}
		}
		
		/**
		 * Replace the ids in outlierScores with consecutive Ids
		 */
		double[] consecOutlierScores = new double[nodeCount];
		for (Entry<Integer, Double> entry : outlierScores.entrySet()) {
			int conseId = originalToConsecutive.get(entry.getKey());
			consecOutlierScores[conseId] = entry.getValue();
		}

		// converting outlier scores to fraud/normal classifications
		boolean[] initialObservations = convertToClassification(consecOutlierScores);
		
		// lines 2-3: message initliased with initial observations
		double[][] initialBeliefs = initialiseBeliefs(initialObservations);
		this.initialBeliefs = initialBeliefs;
		
		fraudBeliefs = new double[nodeCount];
		accompliceBeliefs = new double[nodeCount];
		normalBeliefs = new double[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			fraudBeliefs[i] = initialBeliefs[i][0];
			accompliceBeliefs[i] = initialBeliefs[i][1];
			normalBeliefs[i] = initialBeliefs[i][2];
		}
		
		partialMessages = new double[nodeCount][3];
		
		messageFor = new double[nodeCount][3];
		for (int i = 0; i < messageFor.length; i++)
			Arrays.fill(messageFor[i], 1d);
		
	}
	
	/**
	 * Convert numeric scores to just fraud/not fraud boolean.
	 * 1 is Fraud, 0 is not.
	 * @param outlierScores
	 * @return
	 */
	private boolean[] convertToClassification(double[] outlierScores) {
		boolean[] classifications = new boolean[outlierScores.length];
		
		// two ways of converting: Simple threshold or proportion. 
		// simple threshold is easier. Everything over 1.2 is fraud
		int fraudCount = 0; // just for counting how many are classed as fraud, can remove
		for (int i = 0; i < outlierScores.length; i++) {
			boolean isFraud = (outlierScores[i] < 1.5) ? false : true;
			classifications[i] = isFraud;

			fraudCount += isFraud ? 1 : 0;
		}
//		System.out.println("initial with fraud belief: " + fraudCount + " vs " + (outlierScores.length - fraudCount));
		
		return classifications;
	}
	
	/**
	 * Giving the right message matrix given the initial observations. 
	 * (From values in the obervation matrix.)
	 * @param initialObservations
	 * @return
	 */
	private double[][] initialiseBeliefs(boolean[] initialObservations) {
		double[][] initialBeliefs = new double[nodeCount][3];
		
		for (int i = 0; i < initialObservations.length; i++) {
			int index = initialObservations[i] ? 0 : 1;
			
			initialBeliefs[i][0] = obsMat[0][index];
			initialBeliefs[i][1] = obsMat[1][index];
			initialBeliefs[i][2] = obsMat[2][index]; 
		}
		
		return initialBeliefs;
	}
	
	int maxIt = 120;
	int it = 0;
	public HashMap<Integer, Node> run(String label) {
		cm.monitor(normalBeliefs);
		while (!converged()) {
//			System.out.println("it: " + it);

			multiplyMessages();
			propagateMessages();
			
//			System.out.print("messages: ");
//			for (int i = 0; i < messageFor.length; i++)
//				System.out.print(Arrays.toString(messageFor[i]) + ",");
//			System.out.println();
			
			if (ofInterest > 0)
				System.out.println("fraudBelief: " + fraudBeliefs[ofInterest] + "|" + " accompliceBelief: " + accompliceBeliefs[ofInterest] + "|" + " normalBelief: " + normalBeliefs[ofInterest]);
			
			beliefUpdate();
			
			if (it > maxIt)
				break;
			
			it++;
			cm.monitor(normalBeliefs);
		}
		
		cm.writeResults(label + "_NP");
		
		return convertBackToOriginalIds();
	}
	
	/**
	 * Converts the fraudBelief array with consecutive ids to their original ids. 
	 */
	private HashMap<Integer, Node> convertBackToOriginalIds() {
		HashMap<Integer, Node> nodeBeliefs = new HashMap<>();
		
		for (int consecutiveId = 0; consecutiveId < consecutiveToOriginal.length; consecutiveId++) {
			int originalId = consecutiveToOriginal[consecutiveId];
			Node node = new Node(fraudBeliefs[consecutiveId], accompliceBeliefs[consecutiveId], normalBeliefs[consecutiveId]);
			
			nodeBeliefs.put(originalId, node);
		}
		
		return nodeBeliefs;
	}
	
	private Multiset<Integer> getNeighbours(int node) {
		return graph[node];
	}
	
	/**
	 * Lines 7 & 8. Find product of beliefs from all neighbours of each node.
	 */
	private void multiplyMessages() {
		for (int node = 0; node < nodeCount; node++) {
			Multiset<Integer> neighbours = getNeighbours(node);
			
			//TODO: can remove
			if (neighbours.contains(ofInterest)) {
				System.out.print("");
			}
			
			double[] beliefs = new double[]{1d, 1d, 1d}; // new message: initialise beliefs to 1
			
			// adding effects of the observation on this node's beliefs
			multiply(beliefs, initialBeliefs[node]);
			
			int[] countTo0 = new int[3];
			for (Integer neighbour : neighbours.elementSet()) {
				double neighbourFraudBelief = fraudBeliefs[neighbour];
				double neighbourAccompliceBelief = accompliceBeliefs[neighbour];
				double neighbourNormalBelief = normalBeliefs[neighbour];
				double[] neighbourPropoagatedBelief = propMatrix(neighbourFraudBelief, neighbourAccompliceBelief, neighbourNormalBelief);
				
				if (beliefs[0] < 1e-100 && beliefs[1] < 1e-100 && beliefs[2] < 1e-100) {
					beliefs[0] *= 1e100;
					beliefs[1] *= 1e100;
					beliefs[1] *= 1e100;
				}
				
//				add in the if < e-233 stuff
				for (int i = 0; i < beliefs.length; i++) {
					if (beliefs[i] * neighbourPropoagatedBelief[i] <= 1e-320) {
						countTo0[i]++;
						beliefs[i] *= 1e120;
					}
				}
				
//				System.out.println("beliefs: " + Arrays.toString(beliefs));
				multiply(beliefs, neighbourPropoagatedBelief);
				
				//TODO: can remove
				if (neighbours.contains(ofInterest)) {
//					System.out.println(toOrigId(ofInterest) + "'s neighbour: " + toOrigId(node) + ", "+ toOrigId(neighbour) + "|" + Arrays.toString(neighbourPropoagatedBelief));
				}
				
				notNanOrInf(beliefs);
			}
			
			beliefs = increaseBeliefs(countTo0, beliefs);
			
			normalise(beliefs); // Trying to avoid multiplying to 0...
			partialMessages[node] = beliefs;
		}
	}
	
	private static double[] increaseBeliefs(int[] countTo0, double[] beliefs) {
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < countTo0.length; i++) {
			max = Math.max(countTo0[i], max);
			min = Math.min(countTo0[i], min);
		}
		
		if (max == 0)
			return beliefs;
		
		int[] newCountTo0 = new int[3];
		for (int i = 0; i < countTo0.length; i++) {
			newCountTo0[i] = countTo0[i] - min;
		}
		
		double[] newBeliefs = new double[beliefs.length];
		for (int i = 0; i < beliefs.length; i++) {
			newBeliefs[i] = beliefs[i];
			for (int j = 0; j < newCountTo0[i]; j++) {
				newBeliefs[i] *= 1e-120;
			}
		}
		
		return newBeliefs;
	}
	
	/**
	 * Lines 9 - 11.
	 */
	private void propagateMessages() {
		for (int node = 0; node < nodeCount; node++) {
			
			Multiset<Integer> neighbours = getNeighbours(node);
			
			// TODO: remove
			if (node == ofInterest) {
				System.out.print("neighbours: ");
				for (Integer neighbour : neighbours) {
					System.out.print(toOrigId(neighbour) + ",");
				}
				System.out.println();
			}
			
			double targetFraudBelief = fraudBeliefs[node];
			double targetAccompliceBelief = accompliceBeliefs[node];
			double targetNormalBelief = normalBeliefs[node];
			double[] toDivide = propMatrix(targetFraudBelief, targetAccompliceBelief, targetNormalBelief);
			
			// gather messages from neighbours
			for (int neighbour : neighbours.elementSet()) {
				// line 10: divide message to exclude the belief of the node this message is for
				double[] partialMessage = partialMessages[neighbour].clone();
				partialMessage[0] /= toDivide[0];
				partialMessage[1] /= toDivide[1];
				partialMessage[2] /= toDivide[2];

				// line 11: complete message using propagation matrix
				double[] completeSingleMessage = propMatrix(partialMessage[0], partialMessage[1], partialMessage[2]);
				
//				normalise(completeSingleMessage);			
//				System.out.println("array: " + Arrays.toString(messageFor[node]));
				notNanOrInf(messageFor[node]);
				
				messageFor[node] = decreaseBeliefs(messageFor[node]);
				
				// line 16: multiplying message
				multiply(messageFor[node], completeSingleMessage);
//				normalise(messageFor[node]);

				notNanOrInf(messageFor[node]);
			}
		}
	}
	
	public static double[] decreaseBeliefs(double[] beliefs) {
		double[] newBeliefs = beliefs.clone();
		
		boolean closeToMax = false;
		for (int i = 0; i < beliefs.length; i++) {
			if (beliefs[i] > 1e280) {
				closeToMax = true;
				break;
			}
		}
		
		if (closeToMax) {
			for (int i = 0; i < beliefs.length; i++) {
				newBeliefs[i] /= 1e50;
			}
		}

		return newBeliefs;
		
	}
	
	/**
	 * Multiplies each element at each index in array1 with the element
	 * with the same index in array2.
	 * Changes values in array1.
	 * @param array1
	 * @param array2
	 */
	private static void multiply(double[] array1, double[] array2) {
		assert array1.length == array2.length;
		
		for (int i = 0; i < array1.length; i++) {
			assert !Double.isNaN(array1[i]);
			assert !Double.isNaN(array2[i]);

			array1[i] = array1[i] * array2[i];
		}
	}
	
//	private static double[] fudge(double[] scores) {
//		double[] newScores = new double[3];
//		for (int i = 0; i < scores.length; i++) {
//			newScores[i] = scores[i] * 0.999 + 0.005; 
//		}
//		return newScores;
//	}
	
	/**
	 * Calculate the belief probabilities of this node, given the neighbour's beliefs.
	 * @param fraud
	 * @param accomplice
	 * @param normal
	 * @return
	 */
	private double[] propMatrix(double fraud, double accomplice, double normal) {
		double fraudResult = 0;
		double accompliceResult = 0;
		double normalResult = 0;
		
		fraudResult = fraud * propMat[0][0] + accomplice * propMat[1][0] + normal * propMat[2][0];
		accompliceResult = fraud * propMat[0][1] + accomplice * propMat[1][1] + normal * propMat[2][1];
		normalResult = fraud * propMat[0][2] + accomplice * propMat[1][2] + normal * propMat[2][2];
		
		double[] beliefs = new double[]{fraudResult, accompliceResult, normalResult};
		return beliefs;
	}
	
	private void beliefUpdate() {
		for (int node = 0; node < nodeCount; node++) {
			double[] completeMessage = messageFor[node];
			beliefsTest(completeMessage);

//			System.out.println("completeMessage: " + Arrays.toString(completeMessage)); // TODO: for debuggin
			
			// factor in observation for this node 
			multiply(completeMessage, initialBeliefs[node]);
			
			beliefsTest(completeMessage);
			
			//TODO: can remove
			if (node == ofInterest) {
				System.out.println("completeMessage: " + Arrays.toString(completeMessage));
			}
			
			normalise(completeMessage);
//			completeMessage = fudge(completeMessage);

			beliefsTest(completeMessage);
			
			fraudBeliefs[node] = completeMessage[0];
			accompliceBeliefs[node] = completeMessage[1];
			normalBeliefs[node] = completeMessage[2];
			
			
			notNanOrInf(completeMessage);
		}

		// empty out the old messages
		for (int i = 0; i < messageFor.length; i++)
			Arrays.fill(messageFor[i], 1d);
	}
	
	private static void normalise(double[] beliefs) {
		assert beliefs.length == 3;
		
		double sum = beliefs [0] + beliefs[1] + beliefs[2];

		notNanOrInf(beliefs);
		notAllZeros(beliefs);
		
		for (int i = 0; i < beliefs.length; i++) {
			beliefs[i] = beliefs[i] / sum; 
		}
	}
	
	double[] previousFraudBelief = null;
	double[] previousAccompliceBelief = null;
	private boolean converged() {
		if (previousFraudBelief == null || previousAccompliceBelief == null) {
			previousFraudBelief = Arrays.copyOf(fraudBeliefs, fraudBeliefs.length);
			previousAccompliceBelief = Arrays.copyOf(accompliceBeliefs, accompliceBeliefs.length);
			return false;
		}
		
		boolean converged = true;
		for (int i = 0; i < previousFraudBelief.length; i++) {
			double diffF = Math.abs((previousFraudBelief[i] - fraudBeliefs[i]) / previousFraudBelief[i]);
			double diffA = Math.abs((previousAccompliceBelief[i] - accompliceBeliefs[i]) / previousAccompliceBelief[i]);
			if (diffF > 0.001 || diffA > 0.001) {
				converged = false;
//				if (it > 20) {
//					System.out.println(consecutiveToOriginal[i] + " has not converged " + previousFraudBelief[i] + ", " + fraudBeliefs[i]);
//				}
			}
		}
		
		previousFraudBelief = fraudBeliefs.clone();
		previousAccompliceBelief = accompliceBeliefs.clone();
		
		return converged;
	}
	
	int toOrigId(int consecutiveId) {
		return consecutiveToOriginal[consecutiveId];
	}
	
	public Map<Integer, Double> outlierScores() {
		return this.origOutlierScores;
	}
	
	public static class Node {
		public final double[] beliefs;
		public final State state;
		
		Node(double... beliefs) {
			this.beliefs = beliefs;
			assert beliefs.length == 3;
			
			double maxValue = -1;
			int maxIndex = -1;
			for (int i = 0; i < 3; i++) {
				if (beliefs[i] > maxValue) {
					maxValue = beliefs[i];
					maxIndex = i;
				}
			}
			
			state = State.values()[maxIndex];
		}
		
		@Override
		public String toString() {
			return Arrays.toString(beliefs) + ":" + state;
		}
	}
	
	public static enum State {
		FRAUD, ACCOMPLICE, NORMAL;
	}
	
	private static void beliefsTest(double... beliefs) {
		assert beliefs.length == 3;
		for (int i = 0; i < beliefs.length; i++) {
//			assert beliefs[i] > 0 : Arrays.toString(beliefs);
//			assert beliefs[i] <= 1;
			assert !Double.isNaN(beliefs[i]) : Arrays.toString(beliefs);
			assert !Double.isInfinite(beliefs[i]) : Arrays.toString(beliefs);
		}
		
	}
	private static void notNanOrInf(double... beliefs) {
		assert beliefs.length == 3;
		for (int i = 0; i < beliefs.length; i++) {
			assert !Double.isNaN(beliefs[i]) : Arrays.toString(beliefs);
			assert !Double.isInfinite(beliefs[i]) : Arrays.toString(beliefs);
		}
		
	}
	private static void notAllZeros(double... beliefs) {
		assert beliefs.length == 3;
		boolean allZeros = true;
		for (int i = 0; i < beliefs.length; i++) {
			if (beliefs[i] > 0)
				allZeros = false;
		}
		assert !allZeros: "All zeros: " + Arrays.toString(beliefs);
		
	}
	
//	public static void main(String[] args) {
//		System.out.println(Arrays.toString(propMatrix(.2, .2, .8)));
//		System.out.println(Arrays.toString(propMatrix(.1, .1, .4)));
////		for (int i = 0; i < 40; i++) {
////			System.out.println(i * 0.1 + "," + normaliseOutlierScore(i * 0.1));
////		}
////		System.out.println(scale(0.2, 0.4)); // should be 0.36278272962498237
////		MRF mrf = test1();
//////		MRF mrf = test2();
////		mrf.run();
////		System.out.println("init: " + Arrays.toString(mrf.outlierScores));
////		System.out.println("belief: " + Arrays.toString(mrf.fraudBeliefs) + ", " + Arrays.toString(mrf.normalBeliefs));
////		System.out.println("Finished");
//	}
	
	public static MRF_NetProbe test3() {
		Map<Integer, Multiset<Integer>> mockGraph = new HashMap<>();
		mockGraph.put(0, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,3,3,3})));
		mockGraph.put(1, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,2,2,2})));
		mockGraph.put(2, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,1,1})));
		mockGraph.put(3, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,0,0})));
		mockGraph.put(4, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,5})));
		mockGraph.put(5, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{2,4})));
		mockGraph.put(6, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{5})));
		
		Map<Integer, Double> mockOutlierScores = new HashMap<>();
		mockOutlierScores.put(0, 1d);
		mockOutlierScores.put(1, 3d);
		mockOutlierScores.put(2, 2d);
		mockOutlierScores.put(3, 1d);
		mockOutlierScores.put(4, 3d);
		mockOutlierScores.put(5, 3d);
		mockOutlierScores.put(6, 1d);
		
		MRF_NetProbe mrf = new MRF_NetProbe(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRF_NetProbe test2() {
		Map<Integer, Multiset<Integer>> mockGraph = new HashMap<>();
		mockGraph.put(0, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,3,3,3})));
		mockGraph.put(1, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,2,2,2})));
		mockGraph.put(2, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,1,1})));
		mockGraph.put(3, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,0,0})));
		mockGraph.put(4, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,5})));
		mockGraph.put(5, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{2,4})));
		mockGraph.put(6, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{5})));
		
		Map<Integer, Double> mockOutlierScores = new HashMap<>();
		mockOutlierScores.put(0, 1d);
		mockOutlierScores.put(1, 3d);
		mockOutlierScores.put(2, 2d);
		mockOutlierScores.put(3, 1d);
		mockOutlierScores.put(4, 3d);
		mockOutlierScores.put(5, 3d);
		mockOutlierScores.put(6, 1d);
		
		MRF_NetProbe mrf = new MRF_NetProbe(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRF_NetProbe test1() {
		Map<Integer, Multiset<Integer>> mockGraph = new HashMap<>();
		mockGraph.put(0, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,3,3})));
		mockGraph.put(1, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,2,2,2})));
		mockGraph.put(2, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1})));
		mockGraph.put(3, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,0})));
		
		Map<Integer, Double> mockOutlierScores = new HashMap<>();
		mockOutlierScores.put(0, 1d);
		mockOutlierScores.put(1, 3d);
		mockOutlierScores.put(2, 2d);
		mockOutlierScores.put(3, 1d);
		
		MRF_NetProbe mrf = new MRF_NetProbe(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRF_NetProbe test1b() {
		Map<Integer, Multiset<Integer>> mockGraph = new HashMap<>();
		mockGraph.put(0, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1,3})));
		mockGraph.put(1, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0,2})));
		mockGraph.put(2, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{1})));
		mockGraph.put(3, HashMultiset.create(Arrays.<Integer>asList(new Integer[]{0})));
		
		Map<Integer, Double> mockOutlierScores = new HashMap<>();
		mockOutlierScores.put(0, 1d);
		mockOutlierScores.put(1, 3d);
		mockOutlierScores.put(2, 2d);
		mockOutlierScores.put(3, 1d);
		
		MRF_NetProbe mrf = new MRF_NetProbe(mockGraph, mockOutlierScores);
		return mrf;
	}
	
}
