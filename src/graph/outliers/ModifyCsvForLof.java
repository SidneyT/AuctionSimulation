package graph.outliers;

import graph.outliers.OutlierDetection.Triplet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import util.CsvManipulation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Class for reducing the number of duplicate points and adding jitter for points in a csv file.
 */
public class ModifyCsvForLof {

	public static void main(String[] args) throws IOException {
//		new ModifyCsvForLof().run();
		String path = "F:/workstuff2011/AuctionSimulation/lof_features_fixed2";
		File[] files = new File(path).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return !pathname.isDirectory();
			}
		});
		for (File file : files) {
			addJitter(file.getCanonicalPath());
		}
	}

	public void run() {
		try {
			Multimap<Pair<Double, Double>, Integer> values = readFile("bidder_uniquevstotal_test.csv");
			writeFile("bidder_uniquevstotal_jitter.csv", values);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	String headings;
//	Multimap<Pair<Double, Double>, Integer> values;
	private void writeFile(String filePath, Multimap<Pair<Double, Double>, Integer> values) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), Charset.defaultCharset());
		
		for (Pair<Double, Double> pair : values.keySet()) {
			Collection<Integer> ids = values.get(pair);
			int pairFreq = ids.size();
			
			boolean addJitter = false;
			if (pairFreq > 100) {
				pairFreq = 100;
				addJitter = true;
			}

			
			int i = 0;
			for (Integer id : ids) {
				writer.append(id + ",");
				double val1 = pair.getKey();
				double val2 = pair.getValue();
				
				if (addJitter) {
					val1 += (Math.random() - 0.5) / 10000000;
					val2 += (Math.random() - 0.5) / 10000000;
				}
					
				writer.append(val1 + "," + val2);
				writer.newLine();

				if (i++ > pairFreq)
					break;
			}
		}
		writer.flush();
	}
	
	
	
	private Multimap<Pair<Double, Double>, Integer> readFile(String filePath) throws IOException {
		BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), Charset.defaultCharset());
		
		Multimap<Pair<Double, Double>, Integer> values = HashMultimap.create();
		
		if (reader.ready()) {
			headings = reader.readLine();
		}
		
		while (reader.ready()) {
			String line = reader.readLine();
			
			String[] parts = line.split(",");
			
			int id = Integer.parseInt(parts[0]);
			
			double val1, val2;
			val1 = Double.parseDouble(parts[1]);
			val2 = Double.parseDouble(parts[2]);
			
			Pair<Double, Double> pair = new Pair<>(val1, val2);
			values.put(pair, id);
		}
		
//		this.values = values;
		return values;
	}
	
	private static double addJitter(double value) {
		return value += Math.random() / 100000000;
//		return value;
	}
	
	private static void addJitter(String filePath) throws IOException {
		List<String[]> lines = CsvManipulation.readWholeFile(Paths.get(filePath), false);
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), Charset.defaultCharset());
		
		writer.append(join(lines.get(0)));
		writer.newLine();
		
		for (int i = 1; i < lines.size(); i++) {
			String[] line = lines.get(i);
			for (int j = 0; j < line.length; j++) {
				if (j <= 1) { // ignore the first 2 columns
					writer.append(line[j]).append(",");
					continue;
				}
				String value = line[j];
				double valueD;
				if (value.equals("null") || value.contains("Infinity") && !lines.get(0)[j].endsWith("Count")) {
					value = "0";
					valueD = Double.parseDouble(value);
//					valueD = Math.log1p(valueD);
					valueD = addJitter(valueD);
					writer.append(valueD + "");
				} else {
					if (value.equals("null") || value.contains("Infinity"))
						value = "0";
					valueD = Double.parseDouble(value);
//					valueD = Math.log1p(valueD);
					valueD = addJitter(valueD);
					writer.append(valueD + "");
				}
				if (j < line.length - 1) {
					writer.append(",");
				}
			}
			writer.newLine();	
		}
		writer.close();
	}
	private static String join(String... parts) {
		StringBuffer sb = new StringBuffer();
		for (String part : parts) {
			sb.append(part).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

}
