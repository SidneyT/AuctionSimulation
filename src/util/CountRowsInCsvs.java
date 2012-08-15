package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Count the number of rows in the csv files in a folder
 */
public class CountRowsInCsvs {
	public static void main(String[] args) {
		removeColumns();
	}

	static void removeColumns() {
		int[] columnsToKeep = {7, 10, 12, 13};
		String newFilenamePrefix = "-10-5-6ln-11";

		try {
			File dir = new File("F:/workstuff2011/Auction Simulation/synData/completeFeatures");
			for (File file : dir.listFiles()) {
				BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());

				String[] nameParts = file.getName().split("_");
				nameParts[0] = newFilenamePrefix;
				String newFilename = "";
				for (int i = 0; i < nameParts.length; i++) {
					newFilename += nameParts[i];
					if (i != nameParts.length - 1)
						newFilename += ("_");
				}
				BufferedWriter bw = Files.newBufferedWriter(Paths.get(newFilename), Charset.defaultCharset());
				while(br.ready()) {
					String[] lineParts = br.readLine().split(",");
					for (int i = 0; i < columnsToKeep.length; i++) {
						bw.write(lineParts[columnsToKeep[i]]);
						if (i != columnsToKeep.length - 1)
							bw.write(",");
					}
					bw.newLine();
				}
				bw.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	static void countRows() {
		File dir = new File("F:/workstuff2011/Auction Simulation/synData/completeFeatures");
		for (File file : dir.listFiles()) {
			int lineCount = 0;
			try (BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset())){
				while(br.ready()) {
					br.readLine();
					lineCount++;
				}
				System.out.println(file.getName() + ":" + lineCount);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
