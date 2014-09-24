package graph.tmEval.visualise;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;

/**
 * For visualising egonets of suspicious users.
 */
public class Visualise {

//	public static void main(String[] args) {
//		testGraph();
//	}

	public static int edgeWeight(ImmutableMap<Integer, Multiset<Integer>> graph, Integer from, Integer to) {
		if (graph.containsKey(from))
			if (graph.get(from).contains(to))
				return graph.get(from).count(to);
		return 0;
	}
	
	public static final Random r = new Random();
	
	public static void bigGraph(ImmutableMap<Integer, Multiset<Integer>> tmGraph, HashMap<Integer, Double> fraudScores, ImmutableMap<Integer, Multiset<Integer>> winGraph, Map<Integer, String> vertexStyle) {
		bigGraph(tmGraph, fraudScores, winGraph, vertexStyle, "F:/workstuff2011/AuctionSimulation/tmFraudGraphsBig/", 9999);
	}
	public static void bigGraph(ImmutableMap<Integer, Multiset<Integer>> tmGraph, HashMap<Integer, Double> fraudScores, ImmutableMap<Integer, Multiset<Integer>> winGraph, Map<Integer, String> vertexStyle, String directory, int depthLimit) {
		HashSet<Integer> seen = new HashSet<>();
		
		for (Integer id : fraudScores.keySet()) {
			if (seen.contains(id))
				continue;
		
			double fraudScore;
			if (fraudScores.containsKey(id)) {
				fraudScore = fraudScores.get(id);
			} else{ 
				fraudScore = 0;
			}
			
			if (fraudScore < 0.5) {
				continue;
			}
			
			if (!tmGraph.containsKey(id))
				continue;
			
			final mxGraph graph = new mxGraph();
			Map<Integer, Object> vertexMap = new HashMap<>();
			HashMultimap<Integer, Integer> existingEdges = HashMultimap.create();
			
			Object parent = graph.getDefaultParent();
			graph.getModel().beginUpdate();
			try {
				Object v0 = graph.insertVertex(parent, null, id + "", r.nextInt(120), r.nextInt(120), 80, 30, vertexStyle.get(id));
				Object old = vertexMap.put(id, v0);
				seen.add(id);
				
				assert old == null;

				graphRecurse(graph, seen, vertexMap, existingEdges, tmGraph, fraudScores, winGraph, vertexStyle, id, 0, depthLimit);

			} finally {
				graph.getModel().endUpdate();
			}

			// define layout
			// mxIGraphLayout layout = new mxFastOrganicLayout(graph);
			mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
			layout.setForceConstant(150); // higher it is, the further apart the nodes are
			// mxIGraphLayout layout = new mxFastOrganicLayout(graph);

			// layout using morphing
			graph.getModel().beginUpdate();
			try {
				layout.execute(graph.getDefaultParent());
			} finally {
				JFrame f = new JFrame();
				f.setSize(600, 600);
				f.setLocation(300, 200);

				mxGraphComponent graphComponent = new mxGraphComponent(graph);
				 f.getContentPane().add(BorderLayout.CENTER, graphComponent);
				// f.setVisible(true);
				
				mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2, 20);

				morph.addListener(mxEvent.DONE, new mxIEventListener() {

					@Override
					public void invoke(Object arg0, mxEventObject arg1) {
						graph.getModel().endUpdate();
						// fitViewport();
					}

				});

				morph.startAnimation();
				morph.updateAnimation();
				
			}
			
			System.out.println(id + ":" + vertexMap.keySet());

			BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 2, Color.WHITE, true, null);
			try {
				ImageIO.write(image, "PNG", new File(directory + id + ".png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
     	}
	}
	
	public static void graphRecurse(final mxGraph graph, HashSet<Integer> seen, Map<Integer, Object> vertexMap,
			HashMultimap<Integer, Integer> existingEdges, ImmutableMap<Integer, Multiset<Integer>> tmGraph,
			HashMap<Integer, Double> fraudScores, ImmutableMap<Integer, Multiset<Integer>> winGraph,
			Map<Integer, String> vertexStyles, Integer id, int depth, int depthLimit) {
		if (depth == depthLimit)
			return;
		
		if (!tmGraph.containsKey(id))
			return;

		Object parent = graph.getDefaultParent();
		
      	Multiset<Integer> neighbours = tmGraph.get(id);
		for (Integer neighbour : neighbours.elementSet()) {
			int weight = neighbours.count(neighbour);

//			if (id.equals(2369960) && neighbour.equals(355218))
//				System.out.println("pause");
			
//			if (seen.containsKey(neighbour))
//				continue;

			String vertexStyle;
			if (vertexStyles.containsKey(neighbour)) {
				vertexStyle = vertexStyles.get(neighbour);
			} else {
				vertexStyle = "fillColor=#BB99BB;"; // no colour found, so make it light purple
			}

			Object from = vertexMap.get(id); 
			assert from != null;
			assert Arrays.asList(graph.getChildVertices(parent)).contains(from);
			
			boolean isNew;
			Object to; 
			if (isNew = !vertexMap.containsKey(neighbour)) {
				to = graph.insertVertex(parent, null, neighbour + "", r.nextInt(120), r.nextInt(120), 80, 30, vertexStyle);
				seen.add(neighbour);
				vertexMap.put(neighbour, to);
			} else {
				to = vertexMap.get(neighbour);
			}
			
			boolean edgeExists;
			Integer smaller = Math.min(id, neighbour);
			Integer larger = Math.max(id, neighbour);
			if (existingEdges.containsKey(smaller) && existingEdges.get(smaller).contains(larger)) {
				edgeExists = true;
			} else {
				edgeExists = false;
			}
			
			if (!edgeExists) {
				graph.insertEdge(parent, null, weight + "," + edgeWeight(winGraph, id, neighbour), from, to, "startArrow=none;endArrow=none;");
				existingEdges.put(smaller, larger);
			}
			
			
			if (fraudScores.containsKey(neighbour) && fraudScores.get(neighbour) >= 0.5 && isNew)
				graphRecurse(graph, seen, vertexMap, existingEdges, tmGraph, fraudScores, winGraph, vertexStyles, neighbour, depth + 1, depthLimit);
      	}
	      	
	}

	public static void boundGraph(ImmutableMap<Integer, Multiset<Integer>> tmGraph,
			HashMap<Integer, Double> fraudScores, ImmutableMap<Integer, Multiset<Integer>> winGraph,
			Map<Integer, String> vertexStyle, String directory, int depthLimit) {
		for (Integer id : fraudScores.keySet()) {
			try {
			double fraudScore;
			if (fraudScores.containsKey(id)) {
				fraudScore = fraudScores.get(id);
			} else{ 
				fraudScore = 0;
			}
			
			if (fraudScore < 0.5) {
				continue;
			}
			
			if (!tmGraph.containsKey(id))
				continue;
			
			final mxGraph graph = new mxGraph();
			Map<Integer, Object> vertexMap = new HashMap<>();
			HashMultimap<Integer, Integer> existingEdges = HashMultimap.create();
			
			Object parent = graph.getDefaultParent();
			graph.getModel().beginUpdate();
			try {
				Object v0 = graph.insertVertex(parent, null, id + "", r.nextInt(120), r.nextInt(120), 80, 30, vertexStyle.get(id));
				Object old = vertexMap.put(id, v0);
				assert old == null;

				limitedRecurse(graph, vertexMap, existingEdges, tmGraph, fraudScores, winGraph, vertexStyle, id, 0, depthLimit);

			} finally {
				graph.getModel().endUpdate();
			}

			// define layout
			// mxIGraphLayout layout = new mxFastOrganicLayout(graph);
			mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
			layout.setForceConstant(150); // higher it is, the further apart the nodes are
			// mxIGraphLayout layout = new mxFastOrganicLayout(graph);

			// layout using morphing
			graph.getModel().beginUpdate();
			try {
				layout.execute(graph.getDefaultParent());
			} finally {
				JFrame f = new JFrame();
				f.setSize(600, 600);
				f.setLocation(300, 200);

				mxGraphComponent graphComponent = new mxGraphComponent(graph);
				 f.getContentPane().add(BorderLayout.CENTER, graphComponent);
				// f.setVisible(true);
				
				mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2, 20);

				morph.addListener(mxEvent.DONE, new mxIEventListener() {

					@Override
					public void invoke(Object arg0, mxEventObject arg1) {
						graph.getModel().endUpdate();
						// fitViewport();
					}

				});

				morph.startAnimation();
				morph.updateAnimation();
				
			}
			
			System.out.println(id + ":" + vertexMap.keySet());

			BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 2, Color.WHITE, true, null);
			try {
				ImageIO.write(image, "PNG", new File(directory + id + ".png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			} catch (Exception e) {
				e.printStackTrace();
			}
     	}
	}
	
	public static void limitedRecurse(final mxGraph graph, Map<Integer, Object> vertexMap,
			HashMultimap<Integer, Integer> existingEdges, ImmutableMap<Integer, Multiset<Integer>> tmGraph,
			HashMap<Integer, Double> fraudScores, ImmutableMap<Integer, Multiset<Integer>> winGraph,
			Map<Integer, String> vertexStyles, Integer id, int depth, int depthLimit) {
		if (depth == depthLimit)
			return;
		
		if (!tmGraph.containsKey(id))
			return;

		Object parent = graph.getDefaultParent();
		
      	Multiset<Integer> neighbours = tmGraph.get(id);
		for (Integer neighbour : neighbours.elementSet()) {
			int weight = neighbours.count(neighbour);

//			if (id.equals(2369960) && neighbour.equals(355218))
//				System.out.println("pause");
			
//			if (seen.containsKey(neighbour))
//				continue;

			String vertexStyle;
			if (vertexStyles.containsKey(neighbour)) {
				vertexStyle = vertexStyles.get(neighbour);
			} else {
				vertexStyle = "fillColor=#BB99BB;"; // no colour found, so make it light purple
			}

			Object from = vertexMap.get(id); 
			assert from != null;
			assert Arrays.asList(graph.getChildVertices(parent)).contains(from);
			
			boolean isNew;
			Object to; 
			if (isNew = !vertexMap.containsKey(neighbour)) {
				to = graph.insertVertex(parent, null, neighbour + "", r.nextInt(120), r.nextInt(120), 80, 30, vertexStyle);
				vertexMap.put(neighbour, to);
			} else {
				to = vertexMap.get(neighbour);
			}
			
			boolean edgeExists;
			Integer smaller = Math.min(id, neighbour);
			Integer larger = Math.max(id, neighbour);
			if (existingEdges.containsKey(smaller) && existingEdges.get(smaller).contains(larger)) {
				edgeExists = true;
			} else {
				edgeExists = false;
			}
			
			if (!edgeExists) {
				graph.insertEdge(parent, null, weight + "," + edgeWeight(winGraph, id, neighbour), from, to, "startArrow=none;endArrow=none;");
				existingEdges.put(smaller, larger);
			}
			
			
			if (fraudScores.containsKey(neighbour) && fraudScores.get(neighbour) >= 0.5 && isNew)
				limitedRecurse(graph, vertexMap, existingEdges, tmGraph, fraudScores, winGraph, vertexStyles, neighbour, depth + 1, depthLimit);
      	}
	      	
	}

	public static void makeGraph(int testId, ImmutableMap<Integer, Multiset<Integer>> tmGraph, HashMap<Integer, String> vertexStyles, ImmutableMap<Integer, Multiset<Integer>> winGraph, String directory) {
//        JFrame f = new JFrame();
//        f.setSize(600, 600);
//        f.setLocation(300, 200);

        final mxGraph graph = new mxGraph();
        
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
//        f.getContentPane().add(BorderLayout.CENTER, graphComponent);
//        f.setVisible(true);

        Random r = new Random();
        
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
        	Multiset<Integer> neighbours = tmGraph.get(testId);
        	Map<Integer, Object> vertexMap = new HashMap<>();
        	Object v0 = graph.insertVertex(parent, null, testId + "", 100, 100, 80, 30, vertexStyles.get(testId));
        	vertexMap.put(testId, v0);
        	for (Integer neighbour : neighbours.elementSet()) {
        		
        		String vertexStyle;
        		if (!vertexStyles.containsKey(neighbour)) {
        			vertexStyle = "fillColor=#FFFFFF;";
        		} else { 
        			vertexStyle = vertexStyles.get(neighbour);
        		}
        		
        		Object v = graph.insertVertex(parent, null, neighbour + "", r.nextInt(120), r.nextInt(120), 80, 30, vertexStyle);
        		
        		vertexMap.put(neighbour, v);
        		
        		int winEdgeWeight = edgeWeight(winGraph, testId, neighbour);
        		int weight = neighbours.count(neighbour);
        		graph.insertEdge(parent, null, weight + "," + winEdgeWeight, v0, v, "startArrow=none;endArrow=none;");
//        		graph.insertEdge(parent, null, weight + "," + winEdgeWeight, v0, v);
        		
        		// check if neighbours have neighbours in the egonet
        		if (tmGraph.containsKey(neighbour))
	        		for (Integer neighbhour2 : tmGraph.get(neighbour).elementSet()) {
	        			if (neighbours.contains(neighbhour2)) {
	        				graph.insertEdge(parent, null, weight + "," + edgeWeight(winGraph, neighbour, neighbhour2), v , vertexMap.get(neighbhour2), "startArrow=none;endArrow=none;");
//	        				graph.insertEdge(parent, null, weight + "," + edgeWeight(winGraph, neighbour, neighbhour2), v , vertexMap.get(neighbhour2));
	//        				System.out.println("between " + neighbour + " and " + neighbhour2);
	        			}
	        		}
        	}
        	
        	
        } finally {
            graph.getModel().endUpdate();
        }
        
        // define layout
//        mxIGraphLayout layout = new mxFastOrganicLayout(graph);
        mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
        layout.setForceConstant(150); // higher it is, the further apart the nodes are
//        mxIGraphLayout layout = new mxFastOrganicLayout(graph);

        // layout using morphing
        graph.getModel().beginUpdate();
        try {
            layout.execute(graph.getDefaultParent());
        } finally {
            mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2, 20);

            morph.addListener(mxEvent.DONE, new mxIEventListener() {

                @Override
                public void invoke(Object arg0, mxEventObject arg1) {
                    graph.getModel().endUpdate();
                    // fitViewport();
                }

            });

            morph.startAnimation();
        }
        
        BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);
        try {
			ImageIO.write(image, "PNG", new File(directory + testId + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void testGraph() {
        JFrame f = new JFrame();
        f.setSize(500, 500);
        f.setLocation(300, 200);

        final mxGraph graph = new mxGraph();
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        f.getContentPane().add(BorderLayout.CENTER, graphComponent);
        f.setVisible(true);

        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            Object v1 = graph.insertVertex(parent, null, "node1", 100, 100, 80, 30);
            Object v2 = graph.insertVertex(parent, null, "node2", 100, 100, 80, 30);
            Object v3 = graph.insertVertex(parent, null, "node3", 100, 100, 80, 30);

            graph.insertEdge(parent, null, "Edge", v1, v2);
            graph.insertEdge(parent, null, "Edge", v2, v3);

        } finally {
            graph.getModel().endUpdate();
        }

        // define layout
//        mxIGraphLayout layout = new mxFastOrganicLayout(graph);
        mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
        layout.setForceConstant(500);

        // layout using morphing
        graph.getModel().beginUpdate();
        try {
            layout.execute(graph.getDefaultParent());
        } finally {
            mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2, 20);

            morph.addListener(mxEvent.DONE, new mxIEventListener() {

                @Override
                public void invoke(Object arg0, mxEventObject arg1) {
                    graph.getModel().endUpdate();
                    // fitViewport();
                }

            });

            morph.startAnimation();
        }
	}

}
