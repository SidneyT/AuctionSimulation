package graph.outliers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import util.CsvManipulation;

public class CutOffRows {
	public static void main(String[] args) throws IOException {
		String filePath = "F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_0_bidderGraphFeatures.csv";
		List<String[]> lines = CsvManipulation.readWholeFile(Paths.get(filePath), false);
		
		for (int max : new int[]{100,200,300,400}) {
			int count = 0;
			BufferedWriter writer = Files.newBufferedWriter(Paths.get("F:/workstuff2011/AuctionSimulation/lof_features_fixed/syn_repFraud_20k_0_"+max+"_bidderGraphFeatures.csv"), Charset.defaultCharset());
			for (String[] line : lines) {
				if (count == max)
					break;
				if (line[1].equals("Puppet"))
					count++;
				writer.append(line[0]);
				for (int i = 1; i < line.length; i++) {
					writer.append("," + line[i]);
				}
				writer.newLine();
			}
			writer.close();
		}
	}
}
