package graph;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.util.FastMath;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

public class MRF {

	final int nodeCount;
	final Map<Integer, Integer> originalToConsecutive;
	final int[] consecutiveToOriginal;
	
	Multiset<Integer>[] graph;
	double[] outlierScores; // should be normalised to between 0-1
	double[] fraudBeliefs;
	double[] normalBeliefs;
	
	double[][] partialMessages; // double[][fraud belief, normal Belief]
	double[][] messageFor; // stores messages for nodes. message for a node is in the corresponding index, e.g. messageFor[node][0] and messageFor[node][1]
	
	Integer ofInterest = 0;
	
	public MRF(Map<Integer, Multiset<Integer>> graph, Map<Integer, Double> outlierScores) {
		this(graph, outlierScores, 3);
	}
	public MRF(Map<Integer, Multiset<Integer>> graph, Map<Integer, Double> outlierScores, int normaliseConst) {
		int nodeCount = outlierScores.size();
		this.nodeCount = nodeCount;
		this.normaliseConst = normaliseConst;
		
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
//				if (origId.equals(-1))
				if (origId.equals(260118))
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
			this.outlierScores = new double[nodeCount];
			for (Entry<Integer, Double> entry : outlierScores.entrySet()) {
				int conseId = originalToConsecutive.get(entry.getKey());
				this.outlierScores[conseId] = normaliseOutlierScore(entry.getValue());
				if (this.outlierScores[conseId] > 1)
					System.out.println("pause");
			}
		}
		
//		this.fraudBeliefs = new double[nodeCount];
		// initialise fraudBeliefs with outlierScores
		this.fraudBeliefs = Arrays.copyOf(this.outlierScores, this.outlierScores.length);
		this.normalBeliefs = new double[nodeCount];
		for (int i = 0; i < fraudBeliefs.length; i++) {
			this.normalBeliefs[i] = 1 - this.fraudBeliefs[i];
		}
		partialMessages = new double[nodeCount][2];
		messageFor = new double[nodeCount][2];
		for (int i = 0; i < messageFor.length; i++)
			Arrays.fill(messageFor[i], 1d);
	}
	
	public Map<Integer, Double> normalisedOutlierScores() {
		HashMap<Integer, Double> outlierScores = new HashMap<>();
		
		for (int consecutiveId = 0; consecutiveId < this.outlierScores.length; consecutiveId++) {
			Integer originalId = consecutiveToOriginal[consecutiveId];
			outlierScores.put(originalId, this.outlierScores[consecutiveId]);
		}
		
		return outlierScores;
	}
	
	// Propagation Matrix
	// 			Fraud	Normal
	// Fraud	0.2		0.8
	// Normal	0.5		0.5
	private final static double[][] propagationMatrix = new double[][]{
			new double[]{0.5, 0.5},
			new double[]{0.2, 0.8}
		}; 

	int maxIt = 500;
	public HashMap<Integer, Double> run() {
		while (!converged()) {
			System.out.println("it: " + (500 - maxIt));

			for (int i = 0; i < messageFor.length; i++)
				Arrays.fill(messageFor[i], 1d);

			multiplyMessages();
			propagateMessages();
			
//			System.out.print("messages: ");
//			for (int i = 0; i < messageFor.length; i++)
//				System.out.print(Arrays.toString(messageFor[i]) + ",");
//			System.out.println();
			
			if (ofInterest > 0)
				System.out.println("fraudBelief: " + fraudBeliefs[ofInterest] + "|" + " normalBelief: " + normalBeliefs[ofInterest]);
			
			beliefUpdate();
			
			if (maxIt-- < 0)
				break;
		}
		normaliseAllBeliefs();
		
		return convertBackToOriginalIds();
	}
	
	/**
	 * Converts the fraudBelief array with consecutive ids to their original ids. 
	 */
	private HashMap<Integer, Double> convertBackToOriginalIds() {
		HashMap<Integer, Double> fraudScores = new HashMap<>();
		
		for (int consecutiveId = 0; consecutiveId < fraudBeliefs.length; consecutiveId++) {
			Integer originalId = consecutiveToOriginal[consecutiveId];
			fraudScores.put(originalId, fraudBeliefs[consecutiveId]);
		}
		
		return fraudScores;
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
	
	public void propagate() {
		
	}
	
	public double getFraudBelief(int id) {
		return fraudBeliefs[id];
	}
	public double getNormalBelief(int id) {
		return normalBeliefs[id];
	}

	public double getOutlierScore(int id) {
		return this.outlierScores[id];
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
			
			double fraudBelief = 1;
			double normalBelief = 1;
			for (Integer neighbour : neighbours.elementSet()) {
				double neighbourFraudBelief = getFraudBelief(neighbour);
				
//				System.out.println(neighbourFraudBelief);
				
				fraudBelief *= neighbourFraudBelief;
				normalBelief *= getNormalBelief(neighbour);

				//TODO: can remove
				if (neighbours.contains(ofInterest)) {
					System.out.println(toOrigId(ofInterest) + "'s neighbour: " + toOrigId(node) + ", "+ toOrigId(neighbour) + "|" + neighbourFraudBelief);
				}
				
				if (fraudBelief > 1 || normalBelief > 1) {
					assert false;
				}
			}
			
			if (fraudBelief > 1 || normalBelief > 1) {
				assert false;
			}
			if (fraudBelief == 0 || normalBelief == 0) {
				assert false;
			}
			partialMessages[node][0] = fraudBelief;
			partialMessages[node][1] = normalBelief;
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
			
			// gather messages from neighbours
			for (int neighbour : neighbours.elementSet()) {
				double fraudBelief = 1;
				double normalBelief = 1;
				// line 11: complete message by using observation matrix AND propagation matrix
				// observation
				double fraudPhi = hiddenCompatabilityFunctionF(outlierScores[neighbour]);
				fraudBelief *= fraudPhi;
				double normalPhi = hiddenCompatabilityFunctionN(1 - outlierScores[neighbour]);
				normalBelief *= normalPhi;
				// propagation
				double fraudPsi = neighbourCompatibilityFunctionF(fraudBeliefs[neighbour]);
				fraudBelief *= fraudPsi;
				double normalPsi = neighbourCompatibilityFunctionN(1 - fraudBeliefs[neighbour]);
				normalBelief *= normalPsi;
				
				// factor in edge weight
				int edgeWeight = neighbours.count(neighbour);
				if (edgeWeight != 1) {
					fraudBelief = FastMath.pow(fraudBelief, edgeWeight);
					normalBelief = FastMath.pow(normalBelief, edgeWeight);
				}
				
				// line 10: divide message to exclude the belief of the node this message is for
				// fraudBelief and normalBelief together makes up the message.
				double fraudPartialMessage = partialMessages[neighbour][0];
				fraudPartialMessage /= targetFraudBelief;
				
				double normalPartialMessage = partialMessages[neighbour][1];
				normalPartialMessage /= targetNormalBelief;

				fraudPartialMessage = scale(fraudPartialMessage, normalPartialMessage);
				normalPartialMessage = 1 - fraudPartialMessage;
				
				fraudBelief *= fraudPartialMessage;
				normalBelief *= normalPartialMessage;
				
				// TODO: remove
				if (node == ofInterest) {
					System.out.println("ofInterest: " + neighbour + ", " + fraudPhi + ", " + fraudPsi + ", " + fraudPartialMessage + ", " + normalPartialMessage);
				}
				
//				if (fraudBelief < 0.0000001 || normalBelief < 0.0000001) {
//					System.out.println("pause");
//				}

				if (fraudBelief == 0 || normalBelief == 0) {
					assert false;
				}

				fraudBelief = fraudBelief * 0.8 + .1;
				normalBelief = normalBelief * 0.8 + .1;
				
				if (messageFor[node][0] * fraudBelief == 0 || messageFor[node][1] * normalBelief == 0) {
					assert false;
				}
				if (messageFor[node][0] * fraudBelief > 1 || messageFor[node][1] * normalBelief > 1) {
					assert false;
				}
				if (Double.isNaN(messageFor[node][0] * fraudBelief) || Double.isNaN(messageFor[node][1] * normalBelief)) {
					assert false;
				}

				messageFor[node][0] *= fraudBelief;
				messageFor[node][1] *= normalBelief;
			}
			
		}
	}
	
	private static double scale(double fraudPartialMessage, double normalPartialMessage) {
		if (fraudPartialMessage == 1 && normalPartialMessage == 1)
			return 0.5;
		double loggedFraud = FastMath.log(fraudPartialMessage);
		double loggedNormal = FastMath.log(normalPartialMessage);
		double fraudScaled = 1 - loggedFraud / (loggedFraud + loggedNormal);
		
		if (Double.isNaN(fraudScaled) || Double.isInfinite(fraudScaled) || fraudScaled <= 0 || fraudScaled >= 1)
			System.out.println("pause");
		
		return fraudScaled;
	}
	
	private void beliefUpdate() {
		for (int node = 0; node < nodeCount; node++) {
			double fraudMsg = messageFor[node][0];
			double normalMsg = messageFor[node][1];
			
			if (fraudMsg == 0 || normalMsg == 0) {
				System.out.println("pause");
			}
			
			//TODO: can remove
			if (node == ofInterest) {
				System.out.println("fraudMsg: " + fraudMsg + " | normalMsg: " + normalMsg);
			}
			
			// observation
//			double fraudPhi = hiddenCompatabilityFunctionF(outlierScores[node]);
//			fraudMsg *= fraudPhi;
//			double normalPhi = hiddenCompatabilityFunctionN(1 - outlierScores[node]);
//			normalMsg *= normalPhi;
			
			// geometric mean
//			fraudMsg = fraudMsg * fraudMsg * outlierScores[node];
//			fraudMsg = FastMath.pow(fraudMsg, 1d/3);
//			normalMsg = normalMsg * normalMsg * outlierScores[node];
//			normalMsg = FastMath.pow(normalMsg, 1d/3);
			fraudMsg = geometricMean(fraudMsg, outlierScores[node]);
//			fraudMsg = geometricMean(fraudMsg, outlierScores[node]);
			normalMsg = geometricMean(normalMsg, 1 - outlierScores[node]);
//			normalMsg = geometricMean(normalMsg, 1 - outlierScores[node]);
			
			// normalisation
			double normalisedFraud = normaliseBeliefs(fraudMsg, normalMsg);
			double normalisedNormal = 1.0 - normalisedFraud;
			
			if (Double.isNaN(normalisedFraud) || Double.isNaN(normalisedNormal)) {
				assert false;
			}
			if (normalisedFraud == 0 || normalisedNormal == 0) {
				assert false;
			}
			if (normalisedFraud == 1 || normalisedNormal == 1) {
				assert false;
			}
			if (normalisedFraud > 1 || normalisedNormal > 1) {
				assert false;
			}
			fraudBeliefs[node] = normalisedFraud;
			normalBeliefs[node] = normalisedNormal;
		}
	}
	
	private static double geometricMean(double num1, double num2) {
		assert !Double.isNaN(num1);
		assert !Double.isInfinite(num1);
		assert !Double.isNaN(num2);
		assert !Double.isInfinite(num2);
		
		return FastMath.sqrt(num1 * num2);
	}
	
	double[] previousFraudBelief = null;
	public boolean converged() {
		if (previousFraudBelief == null) {
			previousFraudBelief = Arrays.copyOf(fraudBeliefs, fraudBeliefs.length);
			return false;
		}
		
		boolean converged = true;
		for (int i = 0; i < previousFraudBelief.length; i++) {
			double diff = Math.abs((previousFraudBelief[i] - fraudBeliefs[i]) / previousFraudBelief[i]);
			if (diff > 0.001) {
				converged = false;
			}
		}
		
		previousFraudBelief = Arrays.copyOf(fraudBeliefs, fraudBeliefs.length);
		
		return converged;
	}
	
	public double hiddenCompatabilityFunctionF(double hiddenFraudBelief) {
//		return 1 - FastMath.abs(hiddenFraudBelief - observedFraudBelief);
//		return (hiddenFraudBelief + 0.5) / 2;
		return hiddenFraudBelief;
	}
	public double hiddenCompatabilityFunctionN(double hiddenNormalBelief) {
//		return 1 - FastMath.abs(hiddenNormalBelief - observedNormalBelief);
//		return (hiddenNormalBelief + 0.5) / 2;
		return hiddenNormalBelief;
	}
	public double neighbourCompatibilityFunctionF(double selfHiddenFraudBelief) {
//		if (selfHiddenFraudBelief > upperLimit)
//			return 0.95;
//		else if (selfHiddenFraudBelief < lowerLimit)
//			return 0.05;
//		else 
//			return selfHiddenFraudBelief;
		return selfHiddenFraudBelief * 0.8 + 0.1;
//		return geometricMean(selfHiddenFraudBelief, 0.5);
	}
	public double neighbourCompatibilityFunctionN(double selfHiddenNormalBelief) {
//		if (selfHiddenNormalBelief > upperLimit)
//			return 0.95;
//		else if (selfHiddenNormalBelief < lowerLimit)
//			return 0.05;
//		else 
//			return selfHiddenNormalBelief;
		return selfHiddenNormalBelief * 0.8 + 0.1;
//		return geometricMean(selfHiddenNormalBelief, 0.5);
	}
	
	
	
	/**
	 * Returns the normalised fBelief.
	 * E.g. if parameters were (0.3, 0.2), then 0.6 is returned.
	 * @param fBelief
	 * @param nBelief
	 * @return
	 */
	public double normaliseBeliefs(double fBelief, double nBelief) {
		assert fBelief > 0 && fBelief < 1;
		assert nBelief > 0 && nBelief < 1;
		
		double sum = fBelief + nBelief;
//		return ;
		
		return (fBelief / sum * 0.98) + 0.01;
		
	}
	
	private static double normaliseOutlierScore(double outlierScore) {
		double score = normaliseOutlierScoreInner(outlierScore);
		
		score = (score * 0.7) + 0.2; 
		return score;
		
	}
	/**
	 * Given a outlier score, returns the "probability" that it's a suspicious user.
	 * E.g. with a score of 3, returns (3 - 0.5) / 3 = 0.83.
	 * The probability of being "normal" is then the complement; e.g. 1 - 0.83 = 0.17.
	 * @param outlierScore
	 * @return
	 */
	public final double normaliseConst;
	private static double normaliseOutlierScoreInner(double outlierScore) {
//		return (outlierScore - 1) / (outlierScore - 0.2);
		if (outlierScore <= 1)
			return 0;
//		return (FastMath.pow(outlierScore, normaliseConst) - 1) / (FastMath.pow(normaliseConst, 3));
		
		// y = -1/a*(x - b) + 1
		// b = 1 - 1/a
		double a = 2d;
		double b = 1d - 1d/a;
		double newScore = -1/(a * (outlierScore - b)) + 1;
		return newScore;
	}
	
	int toOrigId(int consecutiveId) {
		return consecutiveToOriginal[consecutiveId];
	}
	
	public static void main(String[] args) {
//		for (int i = 0; i < 40; i++) {
//			System.out.println(i * 0.1 + "," + normaliseOutlierScore(i * 0.1));
//		}
//		System.out.println(scale(0.2, 0.4)); // should be 0.36278272962498237
//		MRF mrf = test1();
////		MRF mrf = test2();
//		mrf.run();
//		System.out.println("init: " + Arrays.toString(mrf.outlierScores));
//		System.out.println("belief: " + Arrays.toString(mrf.fraudBeliefs) + ", " + Arrays.toString(mrf.normalBeliefs));
//		System.out.println("Finished");
	}
	
	public static MRF test3() {
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
		
		MRF mrf = new MRF(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRF test2() {
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
		
		MRF mrf = new MRF(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRF test1() {
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
		
		MRF mrf = new MRF(mockGraph, mockOutlierScores);
		return mrf;
	}
	public static MRF test1b() {
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
		
		MRF mrf = new MRF(mockGraph, mockOutlierScores);
		return mrf;
	}
	
}
