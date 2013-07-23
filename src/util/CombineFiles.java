package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads the rows from all CSV files in a folder and writes them all into the same file.
 */
public class CombineFiles {
	public static void main(String[] args) throws IOException {
//		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/shillingResults/waitStart");
////		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/shillingResults/trevathan");
//		Path outputFolder = Paths.get(synDataFolder.toString(), "combined");
//		writeSelectively(synDataFolder, "Puppet", "1", outputFolder, "ShillScores_", "ratings_waitStart_fraud.csv");
//		writeSelectively(synDataFolder, "Cluster", "0", outputFolder, "ShillScores_", "ratings_waitStart_normal.csv");
		
//		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/single_feature_shillvsnormal/waitStart");
//		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/single_feature_shillvsnormal/trevathan");
//		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/shillingResults/trevathan");
//		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/single_feature_shillvsnormal/hybrid");
		Path synDataFolder = Paths.get("F:/workstuff2011/AuctionSimulation/shillingResults/hybridlp");
		Path outputFolder = Paths.get(synDataFolder.toString(), "combined");
		
		String ratings = "ShillScores_";
		String features = "syn_";
		writeSelectively(synDataFolder, features, "Puppet", "1", outputFolder, "syn_", "features_hybrid_fraud.csv");
		writeSelectively(synDataFolder, features, "Cluster", "0", outputFolder, "syn_", "features_hybrid_normal.csv");
		writeSelectively(synDataFolder, ratings, "Puppet", "1", outputFolder, "ShillScores_", "ratings_hybrid_fraud.csv");
		writeSelectively(synDataFolder, ratings, "Cluster", "0", outputFolder, "ShillScores_", "ratings_hybrid_normal.csv");
		
		System.out.println("Finished.");
	}
	
	private static void writeSelectively(Path inputFolder, final String filenameStartsWith, String lineStartsWith, String classLabel, Path outputFolder, final String fileNameStartsWith, String outputFilename) throws IOException {
		BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFolder.toString(), outputFilename), Charset.defaultCharset());
		int lineCount = 0;
		
		boolean first = true;
		
		for (File file : inputFolder.toFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
//				return name.startsWith(fileNameStartsWith) && name.endsWith(".csv");
				return name.startsWith(filenameStartsWith) && name.endsWith(".csv");
			}
		})) {

			BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());

			if (first) { // copy the first line with the column headings
				first = false;
				bw.append(br.readLine()).append(",isFraud");
				bw.newLine();
			} else { // skip the first line with the column headings
				br.readLine();
			}
			
			while(br.ready()) {
				String line = br.readLine();
				if (line.startsWith(lineStartsWith)) {
					lineCount++;
					if (lineCount != 0 && lineCount % 1048576 == 0) {
						bw.flush();
						bw.close();
						bw = Files.newBufferedWriter(Paths.get(outputFolder.toString(), outputFilename + lineCount / 1048576), Charset.defaultCharset());
					}
					bw.append(line).append(",").append(classLabel);
					bw.newLine();
				}
			}
		}
		bw.flush();
	}
}
