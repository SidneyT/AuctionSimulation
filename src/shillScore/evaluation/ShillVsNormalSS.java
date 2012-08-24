package shillScore.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.Frequency;

import shillScore.ShillScore;
import shillScore.BuildShillScore.Auction;
import shillScore.BuildShillScore.User;
import shillScore.evaluation.BayseanAverageSS.BayseanSS;


import au.com.bytecode.opencsv.CSVReader;

/**
 * Calculate statistics about the shill scores of shillers vs. normal users.
 */
public class ShillVsNormalSS {
	
	public static void main(String[] args) {
//		go("SingleShill");
		ssPercentiles("TrevathanSimpleShill");
	}
	
	/**
	 * For each file containing ShillScores, get the shill scores of shills and normal users.
	 * Then find the percentile the ShillScores of the shills fall in.
	 * @param filenamePart used for filename filter, to look for files with the names you want
	 */
	public static void ssPercentiles(final String filenamePart) {
		File[] files = new File("shillingResults/").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.contains(filenamePart);
			}
		});
		
		ArrayList<Double> allPercentiles = new ArrayList<Double>();
		for (File file : files) {
			try {
				allPercentiles.addAll(filePercentiles(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		writeTpFps(Paths.get("shillingResults", "comparisons", "TpFpsSS.csv"), generateTpFp(allPercentiles));
		
	}
	
	public static List<Double> filePercentiles(File file) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(file));
		
		reader.readNext();
		String[] nextLine;
		List<Double> normalSS = new ArrayList<>();
		List<Double> shillerSS = new ArrayList<>();
		while ((nextLine = reader.readNext()) != null) {
			int id = Integer.parseInt(nextLine[0]);
			double ss = Double.parseDouble(nextLine[9]); // 10th element is the shill score
			
			// sort scores of normal/shill users into different lists
			if (id <= 4000) {
				normalSS.add(ss);
			} else {
				shillerSS.add(ss);
			}
		}
		reader.close();
		
		// get the runNumber
		String[] bitsOfName = file.getName().replaceAll(".csv", "").split("\\.");
		System.out.println(file.getName());
		int runNum = Integer.parseInt(bitsOfName[bitsOfName.length - 1]);

//		writePercentiles(Paths.get("shillingResults/comparisons/normal" + runNum + ".csv"), normalShillScores);
//		writePercentiles(Paths.get("shillingResults/comparisons/shill" + runNum + ".csv"), shillerShillScores);
		return percentiles(normalSS, shillerSS);
	}
	
	/**
	 * For each value in values2, calculate the percentile that value falls in for
	 * values1.
	 * @param values1
	 * @param values2
	 * @return number between 0 and 1.
	 */
	public static List<Double> percentiles(List<Double> values1, List<Double> values2) {
		Frequency frequency = new Frequency();
		for (double v : values1) {
			frequency.addValue(v);
		}
		
		List<Double> percentiles = new ArrayList<>(); 
		for (double v : values2) {
			double percentile = (double) frequency.getCumFreq(v) / values1.size();
			percentiles.add(percentile);
		}
		return percentiles;
	}
	
	public static List<TpFpPair> generateTpFp(List<Double> percentiles) {
		Collections.sort(percentiles);
		
		List<TpFpPair> results = new ArrayList<>();
		int numberOfElements = percentiles.size();
		
		for (int i = 0; i < percentiles.size(); i++) {
			double tp = (double) i / numberOfElements;
			double fp = percentiles.get(i);
//			System.out.println(tp + ", " + fp);
			results.add(new TpFpPair(tp, fp));
		}
		
		return results;
	}
	public static class TpFpPair {
		public final double tp;
		public final double fp;
		public TpFpPair(double tp, double fp) {
			this.tp = tp;
			this.fp = fp;
		}
		@Override
		public String toString() {
			return "(" + tp + "|" + fp + ")";
		}
	}
	
	public static <T> void writePercentiles(Path path, String runLabel, final List<List<T>> percentiless) {
		assert !percentiless.isEmpty();
		
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			for (int j = 0; j < percentiless.get(0).size(); j++) {
				bw.append(runLabel);
				bw.append(",");
				bw.append(new Date().toString());
				for (int i = 0; i < percentiless.size(); i++){
					List<T> percentiles = percentiless.get(i);
					bw.append("," + percentiles.get(j));
				}
				bw.newLine();
			}
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeTpFps(Path path, List<TpFpPair> tpFps) {
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			for (TpFpPair tpFp : tpFps) {
				bw.append(tpFp.tp + ",");
				bw.append(tpFp.fp + "");
				bw.newLine();
			}
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Orders the SS for bidders in the auction in descending order, then prints out a
	 * boolean array, showing the rank of the shill.
	 * @param shillScores
	 * @param auctionBidders
	 * @param auctionCounts
	 * @param path
	 */
	public static void ssRankForShills(Map<Integer, ShillScore> shillScores, Map<Auction, List<User>> auctionBidders, Map<Integer, Integer> auctionCounts, Path path, String label, int[]... reweights) {
		
		writeRanks(shillScores, auctionBidders, auctionCounts, path, label, ShillScore.DEFAULT_WEIGHTS);
		for (int i = 0; i < reweights.length; i++) {
			String weightString = Arrays.toString(reweights[i]).replace(", ", "");
			writeRanks(shillScores, auctionBidders, auctionCounts, path, label + "." + weightString, reweights[i]);
		}
		
//		return new RankPair(shillFirstCount, normalFirstCount);
		return;
	}
	
	private static class ScoreBidderPair implements Comparable<ScoreBidderPair> {
		final double ss;
		final User bidder;
		ScoreBidderPair(double ss, User bidder) {
			this.ss = ss;
			this.bidder = bidder;
		}
		@Override
		public int compareTo(ScoreBidderPair o) {
			return Double.compare(this.ss, o.ss);
		}
		@Override
		public String toString() {
			return "(" + ss + ":" + bidder + ")";
		}
	}

	private static void writeRanks(Map<Integer, ShillScore> shillScores, Map<Auction, List<User>> auctionBidders, Map<Integer, Integer> auctionCounts, Path path, String label, int[] weights) {
		int shillFirstCount = 0;
		int normalFirstCount = 0;
		
		for (Entry<Auction, List<User>> auctionBidderEntry : auctionBidders.entrySet()) {
			Auction auction = auctionBidderEntry.getKey();
			List<User> bidders = auctionBidderEntry.getValue();
			
			if (auction.seller.userType.toLowerCase().contains("puppet")) {
				Set<Integer> seen = new HashSet<>();
				List<ScoreBidderPair> pairs = new ArrayList<>();
				for (User bidder : bidders) {
					{ // for ignoring duplicates
						if (seen.contains(bidder.id))
							continue;
						seen.add(bidder.id);
					}
					{ // for removing the winner
						if (bidder.id == auction.winnerId)
							continue;
					}
					
					ScoreBidderPair pair = new ScoreBidderPair(shillScores.get(bidder.id).getShillScore(auctionCounts, auction.seller.id, weights), bidder);
					pairs.add(pair);
				}
				Collections.sort(pairs, Collections.reverseOrder());
//				System.out.println(pairs);

				if (!pairs.isEmpty()) {
//					System.out.print("[");
//					for (ScoreBidderPair pair : pairs) {
						ScoreBidderPair pair = pairs.get(0);
						if (pair.bidder.userType.toLowerCase().contains("puppet")) {
//							System.out.print("1,");
							shillFirstCount++;
						} else {
//							System.out.print("0,");
							normalFirstCount++;
						}
//					}
//					System.out.println("]");
				}
			}
			
		}
		
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			bw.append(label);
			bw.append(",");
			bw.append(new Date().toString());
			bw.append(",");
			bw.append(shillFirstCount + "");
			bw.append(",");
			bw.append(normalFirstCount + "");
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param shillScores
	 * @param auctionBidders
	 * @param auctionCounts
	 * @param path
	 * @param label
	 */
	public static void writeRanks(Map<Integer, ShillScore> shillScores, BayseanSS bayseanSS, Map<Auction, List<User>> auctionBidders, Map<Integer, Integer> auctionCounts, Path path, String label) {
		int shillFirstCount = 0;
		int normalFirstCount = 0;
		
		for (Entry<Auction, List<User>> auctionBidderEntry : auctionBidders.entrySet()) {
			Auction auction = auctionBidderEntry.getKey();
			List<User> bidders = auctionBidderEntry.getValue();
			
			if (auction.seller.userType.toLowerCase().contains("puppet")) {
				Set<Integer> seen = new HashSet<>();
				List<ScoreBidderPair> pairs = new ArrayList<>();
				for (User bidder : bidders) {
					{ // for ignoring duplicates
						if (seen.contains(bidder.id))
							continue;
						seen.add(bidder.id);
					}
					{ // for removing the winner
						if (bidder.id == auction.winnerId)
							continue;
					}
					
					ScoreBidderPair pair = new ScoreBidderPair(bayseanSS.bss(shillScores.get(bidder.id), auction.seller.id), bidder);
					pairs.add(pair);
				}
				Collections.sort(pairs, Collections.reverseOrder());
//				System.out.println(pairs);

				if (!pairs.isEmpty()) {
//					System.out.print("[");
//					for (ScoreBidderPair pair : pairs) {
						ScoreBidderPair pair = pairs.get(0);
						if (pair.bidder.userType.toLowerCase().contains("puppet")) {
//							System.out.print("1,");
							shillFirstCount++;
						} else {
//							System.out.print("0,");
							normalFirstCount++;
						}
//					}
//					System.out.println("]");
				}
			}
			
		}
		
		try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			bw.append(label);
			bw.append(",");
			bw.append(new Date().toString());
			bw.append(",");
			bw.append(shillFirstCount + "");
			bw.append(",");
			bw.append(normalFirstCount + "");
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class RankPair {
		public final int shillFirstCount, normalFirstCount;
		public RankPair(int shillFirstCount, int normalFirstCount) {
			this.shillFirstCount = shillFirstCount;
			this.normalFirstCount = normalFirstCount;
		}
	}
	
}