package graph.outliers.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.PriorityQueue;

import util.IncrementalMean;
import util.IncrementalSD;

public class ConvergenceMonitor {
	public double[] previousValues;
	
	public void monitor(double[] values) {
		if (previousValues == null) {
			previousValues = values;
//			return;
		}
		
		IncrementalSD sd = new IncrementalSD();
		
		ArrayList<Double> diffs = new ArrayList<>();
		
		// calculate the change
		for (int i = 0; i < values.length; i++) {
			double diff = Math.abs(values[i] - previousValues[i]);
			sd.add(diff);
			
			diffs.add(diff);
		}
		
		Collections.sort(diffs, Collections.reverseOrder());
		IncrementalMean highMean = new IncrementalMean();
		highMean.add(diffs.subList(0, (int) (diffs.size() * 0.01)));
		
		this.highMean.add(highMean.average());
		
		averageChange.add(sd.average());
		deviation.add(sd.getSD());
		
		previousValues = values.clone();
	}
	
	public void writeResults(String label) {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get("ConvergenceMonitor.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			
			Date date = new Date();
			
			bw.append(label + "," + date + ",avgChange," + averageChange.size() + ",");
			bw.append(averageChange.toString().replace("[", "").replace("]", ""));
			bw.newLine();
			
			bw.append(label + "," + date + ",avgChangeSD," + averageChange.size() + ",");
			bw.append(deviation.toString().replace("[", "").replace("]", ""));
			bw.newLine();
			
			bw.append(label + "," + date + ",avg99thChange," + averageChange.size() + ",");
			bw.append(highMean.toString().replace("[", "").replace("]", ""));
			bw.newLine();
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.out.println("average change, " + cm.averageChange.toString().replace("[", "").replace("]", ""));
//		System.out.println("change deviation, " + cm.deviation.toString().replace("[", "").replace("]", ""));
//		System.out.println("high mean, " + cm.highMean.toString().replace("[", "").replace("]", ""));

	}
	
	public final ArrayList<Double> averageChange = new ArrayList<>();
	public final ArrayList<Double> deviation = new ArrayList<>();
	public final ArrayList<Double> highMean = new ArrayList<>();
	
	
}
