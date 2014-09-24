package graph;

import graph.outliers.evaluation.ConvergenceMonitor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.util.FastMath;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

public class MRFv2 {

	final int nodeCount;
	final Map<Integer, Integer> originalToConsecutive;
	final int[] consecutiveToOriginal;
	
	Multiset<Integer>[] graph;
	public final Map<Integer, Double> origOutlierScores; // outlier scores given by LOF. ids have not been changed
	final double[][] initialBeliefs;
//	double[] outlierScores; // should be normalised to between 0-1
	double[] fraudBeliefs;
	double[] normalBeliefs;
	
	double[][] partialMessages; // double[node index][fraud belief, normal Belief]
	double[][] messageFor; // stores messages for nodes. message for a node is in the corresponding index, e.g. messageFor[node][0] and messageFor[node][1]
	
	Integer ofInterest = -1;
	
//	public static double e = 0.32, e2 = 0.4;
//	public static double e = 0.1, e2 = 0.4;
	public static double e = 0.12, e2 = 0.36; // was this before
//	public static double e = 0.44, e2 = 0.52;
//	public static double e = 0.16, e2 = 0.32;
	public final double[][] propMat = new double[][]{
		new double[]{1 - e, e},
		new double[]{e2, 1 - e2}
	};
	
	private final ConvergenceMonitor cm;
	public MRFv2(Map<Integer, Multiset<Integer>> graph, Map<Integer, Double> outlierScores) {
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

				// TODO: can remove
				if (origId.equals(""))
//				if (origId.equals(500490))
					ofInterest = count;
				count++;
			}
			
			this.originalToConsecutive = originalToConsecutive;
			this.consecutiveToOriginal = consecutiveToOriginal;
		}
		
		/**
		 * Replace original ids in the graph with the new id values
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
		{
			this.initialBeliefs = new double[nodeCount][2];
//			this.outlierScores = new double[nodeCount];
			for (Entry<Integer, Double> entry : outlierScores.entrySet()) {
				int conseId = originalToConsecutive.get(entry.getKey());
				double normalisedValue = normaliseOutlierScore(entry.getValue());
//				this.outlierScores[conseId] = normalisedValue;
				
				initialBeliefs[conseId][0] = normalisedValue;
				initialBeliefs[conseId][1] = 1 - normalisedValue;
			}
		}
		
		
		// initialise fraudBeliefs with outlierScores
		this.fraudBeliefs = new double[nodeCount];
		this.normalBeliefs = new double[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			this.fraudBeliefs[i] = initialBeliefs[i][0];
			this.normalBeliefs[i] = initialBeliefs[i][1];
		}
		partialMessages = new double[nodeCount][2];
		messageFor = new double[nodeCount][2];
		for (int i = 0; i < messageFor.length; i++)
			Arrays.fill(messageFor[i], 1d);
	}
	
	public Map<Integer, Double> normalisedInitialBeliefs() {
		HashMap<Integer, Double> outlierScores = new HashMap<>();
		
		for (int consecutiveId = 0; consecutiveId < this.nodeCount; consecutiveId++) {
			Integer originalId = consecutiveToOriginal[consecutiveId];
			outlierScores.put(originalId, this.initialBeliefs[consecutiveId][0]);
		}
		
		return outlierScores;
	}
	
	int maxIt = 100;
	public int it = 0;
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
				System.out.println("fraudBelief: " + fraudBeliefs[ofInterest] + "|" + " normalBelief: " + normalBeliefs[ofInterest]);
			
			beliefUpdate();
			
			if (it > maxIt)
				break;
			
			it++;
			
			cm.monitor(normalBeliefs);
		}
		normaliseAllBeliefs();

		cm.writeResults(label);
		
//		System.out.println("average change, " + cm.averageChange.toString().replace("[", "").replace("]", ""));
//		System.out.println("change deviation, " + cm.deviation.toString().replace("[", "").replace("]", ""));
//		System.out.println("high mean, " + cm.highMean.toString().replace("[", "").replace("]", ""));
		
		return convertBackToOriginalIds();
	}
	
	/**
	 * Converts the fraudBelief array with consecutive ids to their original ids. 
	 */
	private HashMap<Integer, Node> convertBackToOriginalIds() {
		HashMap<Integer, Node> fraudScores = new HashMap<>();
		
		for (int consecutiveId = 0; consecutiveId < this.nodeCount; consecutiveId++) {
			Integer originalId = consecutiveToOriginal[consecutiveId];
			fraudScores.put(originalId, new Node(fraudBeliefs[consecutiveId], normalBeliefs[consecutiveId]));
		}
		
		return fraudScores;
	}

	public static class Node {
		public final double[] beliefs;
		public final State state;
		
		Node(double... beliefs) {
			this.beliefs = beliefs;
			assert beliefs.length == 2;
			
			double maxValue = -1;
			int maxIndex = -1;
			for (int i = 0; i < 2; i++) {
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
		FRAUD, NORMAL;
	}
	
	private void normaliseAllBeliefs() {
//		double max = -1;
//		for (int i = 0; i < fraudBeliefs.length; i++) {
//			if (max < fraudBeliefs[i]) {
//				max = fraudBeliefs[i];
//			}
//		}
//		for (int i = 0; i < fraudBeliefs.length; i++) {
//			fraudBeliefs[i] /= max;
//			normalBeliefs[i] = 1 - fraudBeliefs[i];
//		}
		if (ofInterest > 0)
			System.out.println("fraudBelief: " + fraudBeliefs[ofInterest] + "|" + " normalBelief: " + normalBeliefs[ofInterest]);
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
				System.out.println("pause");
			}
			
			double[] beliefs = new double[]{1d, 1d}; // new message: initialise beliefs to 1
			
			multiply(beliefs, initialBeliefs[node]); // effect of this node's observation
			
			int count0to0 = 0;
			int count1to0 = 0;
			for (Integer neighbour : neighbours.elementSet()) {
				double neighbourFraudBelief = fraudBeliefs[neighbour];
				double neighbourNormalBelief = normalBeliefs[neighbour];
				double[] neighbourPropagatedBelief = propMatrix(neighbourFraudBelief, neighbourNormalBelief);
//				System.out.println(neighbourFraudBelief);
				
				if (beliefs[0] < 1e-100 && beliefs[1] < 1e-100) {
					beliefs[0] *= 1e100;
					beliefs[1] *= 1e100;
				}
				
				if (beliefs[0] * neighbourFraudBelief <= 1e-323 && beliefs[1] * neighbourNormalBelief > 1e-323) {
					count0to0++;
					beliefs[0] *= 1e120;
				} else if (beliefs[0] * neighbourFraudBelief > 1e-323 && beliefs[1] * neighbourNormalBelief <= 1e-323) {
					count1to0++;
					beliefs[1] *= 1e120;
				}
				
				// allow one of the beliefs to become zero, but not both
				// this can be fixed in propagateMessage();
				if (beliefs[0] * neighbourFraudBelief == 0 && beliefs[1] * neighbourNormalBelief == 0) {
					assert false;
				}
				
				multiply(beliefs, neighbourPropagatedBelief);

				//TODO: can remove
				if (neighbours.contains(ofInterest)) {
					System.out.println(toOrigId(ofInterest) + "'s neighbour: " + toOrigId(node) + ", "+ toOrigId(neighbour) + "|" + neighbourFraudBelief);
				}
				
				beliefsTestEasy(beliefs);
			}
			
			if (count0to0 > 0 || count1to0 > 0) {
				if (count0to0 > count1to0) {
					beliefs[0] = Double.MIN_VALUE;
					beliefs[1] = 0.99999999999999999;
				} else if (count0to0 < count1to0) {
					beliefs[0] = 0.99999999999999999;
					beliefs[1] = Double.MIN_VALUE;
				} else {
					beliefs[0] = 0.5;
					beliefs[1] = 0.5;
				}
			}
			
			normalise(beliefs);
			partialMessages[node] = beliefs;
		}
	}
	
	private double[] propMatrix(double... beliefs) {
		assert beliefs.length == 2;
		
		double fraudBelief = beliefs[0];
		double normalBelief = beliefs[1];
		
		double fraudResult = fraudBelief * propMat[0][0] + normalBelief * propMat[1][0];
		double normalResult = fraudBelief * propMat[0][1] + normalBelief * propMat[1][1];
		
		double[] propagatedBeliefs = new double[]{fraudResult, normalResult};
		return propagatedBeliefs;
	}

	private static void multiply(double[] array1, double[] array2) {
		assert array1.length == array2.length;
		
		for (int i = 0; i < array1.length; i++) {
//			assert array1[i] * array2[i] > 0;
			
			array1[i] = array1[i] * array2[i];
			
			assert !Double.isNaN(array1[i]);
			assert !Double.isNaN(array2[i]);
		}
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
			double targetNormalBelief = normalBeliefs[node];
			double[] toDivide = propMatrix(targetFraudBelief, targetNormalBelief);
			
			// gather messages from neighbours of this node
			for (int neighbour : neighbours.elementSet()) {
				// line 10: divide message to exclude the belief of the node this message is for
				// fraudBelief and normalBelief together makes up the message.
				double[] partialMessage = partialMessages[neighbour].clone();
				partialMessage[0] /= toDivide[0];
				partialMessage[1] /= toDivide[1];

				double[] completeSingleMessage = propMatrix(partialMessage[0], partialMessage[1]);
				
				// factor in edge weight and the belief of the neighbour
				int edgeWeight = neighbours.count(neighbour);
//				edgeWeight = 1;
				double[] propagatedNeighbourBelief = propMatrix(fraudBeliefs[neighbour], normalBeliefs[neighbour]); // TODO: can use observed or use node belief
				for (int i = 0; i < 2; i++) {
					propagatedNeighbourBelief[i] = FastMath.pow(propagatedNeighbourBelief[i], edgeWeight);
				}
				multiply(propagatedNeighbourBelief, propMatrix(initialBeliefs[neighbour])); // TODO: can use observed or use node belief
				
				normalise(propagatedNeighbourBelief);
				beliefsTestEasy(completeSingleMessage);
				multiply(completeSingleMessage, propagatedNeighbourBelief);
				
				multiply(messageFor[node], completeSingleMessage);
				
				normalise(messageFor[node]);
				beliefsTestEasy(messageFor[node]);
			}
			
		}
	}

	private static void beliefsTestEasy(double... beliefs) {
		assert beliefs.length == 2;
		for (int i = 0; i < beliefs.length; i++) {
//			assert beliefs[i] > 0;
			assert !Double.isNaN(beliefs[i]) : Arrays.toString(beliefs);
			assert !Double.isInfinite(beliefs[i]) : Arrays.toString(beliefs);
		}
		
	}
	
	private void beliefUpdate() {
		for (int node = 0; node < nodeCount; node++) {
			double[] completeMessage = messageFor[node];

			beliefsTestEasy(completeMessage);
			
			multiply(completeMessage, initialBeliefs[node]);

			//TODO: can remove
			if (node == ofInterest) {
				System.out.println("completeMessage: " + Arrays.toString(completeMessage));
			}
			
			completeMessage[0] = (fraudBeliefs[node] + completeMessage[0]) / 2;
			completeMessage[1] = (normalBeliefs[node] + completeMessage[1]) / 2;
			
			normalise(completeMessage);
			
			beliefsTestEasy(completeMessage);
			
//			System.out.println("changing belief from " + fraudBeliefs[node] + " to " + completeMessage[0]);
			
			fraudBeliefs[node] = completeMessage[0];
			normalBeliefs[node] = completeMessage[1];
		}
		
		// empty out the old messages
		for (int i = 0; i < messageFor.length; i++)
			Arrays.fill(messageFor[i], 1d);
	}
	
	private static void normalise(double[] beliefs) {
		assert beliefs.length == 2;
		
		double sum = beliefs [0] + beliefs[1];
		assert !Double.isNaN(sum);
		assert !Double.isInfinite(sum);
		for (int i = 0; i < beliefs.length; i++) {
			beliefs[i] = beliefs[i] / sum; 
		}
	}

	double[] previousFraudBelief = null;
	public boolean converged() {
		if (previousFraudBelief == null) {
			previousFraudBelief = fraudBeliefs.clone();
			return false;
		}
		
		boolean converged = true;
		for (int i = 0; i < previousFraudBelief.length; i++) {
			double absDiff = Math.abs(previousFraudBelief[i] - fraudBeliefs[i]);
			double diff = absDiff / previousFraudBelief[i];
			if (diff > 0.005 && absDiff > 0.005) {
				converged = false;
//				if (it > 20)
//					System.out.println(consecutiveToOriginal[i] + " has not converged " + previousFraudBelief[i] + ", " + fraudBeliefs[i]);
			}
		}
		
		previousFraudBelief = fraudBeliefs.clone();
		
		return converged;
	}
	
//	public static double paramO = 0.1;
	public static double paramO = 0.2;
	public static double normaliseOutlierScore(double outlierScore) {
		double score = normaliseOutlierScoreInner(outlierScore);
		
//		score = (score * 0.8) + 0.1; 
//		score = (score * 0.5) + 0.25; 
//		score = (score * 0.7) + 0.15; 

		score = score * (1d - paramO * 2) + paramO;
		
		return score;
	}
	
//	public static double paramP = 2;
	public static double paramP = 0.4;
	/**
	 * Given a outlier score, returns the "probability" that it's a suspicious user.
	 * E.g. with a score of 3, returns (3 - 0.5) / 3 = 0.83.
	 * The probability of being "normal" is then the complement; e.g. 1 - 0.83 = 0.17.
	 * @param outlierScore
	 * @return
	 */
	private static double normaliseOutlierScoreInner(double outlierScore) {
		if (outlierScore <= 1)
			return 0;
		
		// y = -1/a*(x - b) + 1
		// b = 1 - 1/a
		double a = paramP;
		double b = 1d/a - 1d;
		double newScore = 1 - 1/(a * (outlierScore + b));
		
		assert newScore > 0 && newScore < 1 : newScore;
		
		return newScore;
	}
	
	int toOrigId(int consecutiveId) {
		return consecutiveToOriginal[consecutiveId];
	}
	
//	public static void main(String[] args) {
////		for (int i = 0; i < 40; i++) {
////			System.out.println(i * 0.1 + "," + normaliseOutlierScore(i * 0.1));
////		}
////		System.out.println(scale(0.2, 0.4)); // should be 0.36278272962498237
//		MRFv2 mrf = test1();
////		MRF mrf = test2();
//		mrf.run();
//		System.out.println("init: " + Arrays.toString(mrf.initialBeliefs));
//		System.out.println("belief: " + Arrays.toString(mrf.fraudBeliefs) + ", " + Arrays.toString(mrf.normalBeliefs));
//		System.out.println("Finished");
//	}
	
	public static MRFv2 test3() {
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
		
		MRFv2 mrf = new MRFv2(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRFv2 test2() {
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
		
		MRFv2 mrf = new MRFv2(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRFv2 test1() {
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
		
		MRFv2 mrf = new MRFv2(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRFv2 test1b() {
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
		
		MRFv2 mrf = new MRFv2(mockGraph, mockOutlierScores);
		return mrf;
	}
	
}
