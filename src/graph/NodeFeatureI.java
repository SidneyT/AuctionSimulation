package graph;

import java.util.HashMap;

import com.google.common.collect.HashMultiset;

interface NodeFeatureI {
	double value(HashMap<Integer, HashMultiset<Integer>> adjacencyList, int user);
}