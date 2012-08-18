import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import createShillScores.evaluation.ShillVsNormalSS;

public class RecalculateSS {

	public static void main(String[] args) throws IOException {
		File[] files = getAllFiles(new File("shillingResults"), "ShillScores_SimpleShillPair.20.TrevathanStrategy");
		File outputDir = new File("shillingResults/comparisons"); 
		for (File file : files)
			recalculateForFile(outputDir, file, new double[]{1,1,1,1,1,1});
	}
	
	public static void recalculateForFile(File outputDir, File file, double[] weights)  throws IOException{
		BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());
		BufferedWriter bw = Files.newBufferedWriter(new File(outputDir, "ssPercentiles_reweighted.csv").toPath(), Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

		if (br.ready())
			br.readLine(); // headings, so read and ignore.

		String label = Arrays.toString(weights).replaceAll(", ", "");
		String date = new Date().toString();
		
		List<Double> originalShillPercentiles = ShillVsNormalSS.filePercentiles(file);
		
		List<Double> normalScores = new ArrayList<>();
		List<Double> shillScores = new ArrayList<>();

		while (br.ready()) {
			// calculate the new weight
			String line = br.readLine();
			String[] values = line.split(",");
			double[] ratings = new double[6];
			for (int i = 0; i < 6; i++) {
				ratings[i] = Double.parseDouble(values[i + 3]);
			}
			double newWeight = reweight(ratings, weights);
			if (Integer.parseInt(values[0]) < 5000) { // normal bidder
				normalScores.add(newWeight);
			} else { // shill bidder
				shillScores.add(newWeight);
			}
		}

		// calculate percentiles
		List<Double> percentiles = ShillVsNormalSS.percentiles(normalScores, shillScores);
		assert originalShillPercentiles.size() == percentiles.size();
		for (int i = 0; i < percentiles.size(); i++) {
			// write label
			bw.append(file.getName());
			bw.append(".").append(label);
			// write date
			bw.append(",").append(date);
			// write original percentile found using original weights
			bw.append(",").append(originalShillPercentiles.get(i) + "");
			// write percentile
			bw.append(",").append(percentiles.get(i) + "");
			bw.newLine();
		}
		bw.flush();
	}
	
	public static double reweight(double[] ratings, double[] weights) {
		assert ratings.length == 6;
		assert weights.length == 6;
		
		double weightSum = 0;
		for (int i = 0; i < weights.length; i++) {
			weightSum += weights[i];
		}
		double reweighted = 0;
		for (int i = 0; i < ratings.length; i++) {
			reweighted += ratings[i] * weights[i];
		}
		reweighted /= weightSum;
		
		return reweighted;
	}
	
	public static File[] getAllFiles(File dir, final String nameStartsWith) {
		return dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(nameStartsWith);
			}
		});
		
	}
	
}
