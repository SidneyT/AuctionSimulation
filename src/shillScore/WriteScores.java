package shillScore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import createUserFeatures.BuildUserFeatures;

public class WriteScores {

	private static final String delimiter = ",";

	/**
	 * Writes shill scores of all users to a file.
	 * 
	 * @param shillScores map containing userIds mapped to ShillScore objects
	 * @param auctionCounts map containing sellerIds mapped to number of auctions they submitted
	 * @param suffix string added to the end of the filename
	 */
	public static void writeShillScores(Map<Integer, ShillScore> shillScores, Map<Integer, Integer> auctionCounts, String suffix, int[]... reweights) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults/ShillScores_" + suffix + ".csv"), Charset.defaultCharset())) {
			bw.append(shillScoreHeadings(reweights));
			bw.newLine();
			
			for (ShillScore ss : shillScores.values()) {
				if (ss.getLossCount() != 0) {
					bw.append(ss.getId() + delimiter);
					bw.append(SSRatingsString(ss, auctionCounts, reweights).toString());
					bw.newLine();
				}
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static StringBuilder SSRatingsString(ShillScore ss, Map<Integer, Integer> auctionCounts, int[]... reweights) {
		StringBuilder sb = new StringBuilder();
		sb.append(ss.getWinCount()).append(delimiter);
		sb.append(ss.getLossCount()).append(delimiter);
		sb.append(ss.getAlpha(auctionCounts).maxAlpha).append(delimiter);
		sb.append(ss.getBeta()).append(delimiter);
		sb.append(ss.getGamma()).append(delimiter);
		sb.append(ss.getDelta()).append(delimiter);
		sb.append(ss.getEpsilon()).append(delimiter);
		sb.append(ss.getZeta()).append(delimiter);
		sb.append(ss.getShillScore(auctionCounts));
		for (int i = 0; i < reweights.length; i++) {
			double reweighted = ss.getShillScore(auctionCounts, reweights[i]);
			sb.append(delimiter).append(reweighted);
		}
		return sb;
	}

	private static String shillScoreHeadings(int[]... reweights) {
		StringBuilder sb = new StringBuilder();
		sb.append("bidderId,");
		sb.append("wins,");
		sb.append("losses,");
		sb.append("alpha,");
		sb.append("beta,");
		sb.append("gamma,");
		sb.append("delta,");
		sb.append("epsilon,");
		sb.append("zeta,");
		sb.append("score");
		for (int i = 0; i < reweights.length; i++) {
			sb.append(",").append(Arrays.toString(reweights[i]).replaceAll(", ", "-"));
		}
		return sb.toString();
	}

	/**
	 * Write the shill scores of users for each auction.
	 * @param shillScores
	 * @param auctionBidders
	 * @param auctionCounts
	 * @param suffix
	 */
	public static void writeShillScoresForAuctions(Map<Integer, ShillScore> shillScores,
			Map<BuildUserFeatures.Auction, List<Integer>> auctionBidders, Map<Integer, Integer> auctionCounts, String suffix) {
		
		try (BufferedWriter bw1 = Files.newBufferedWriter(Paths.get("shillingResults/AuctionShillScoresForShillers_" + suffix + ".csv"), Charset.defaultCharset());
				BufferedWriter bw2 = Files.newBufferedWriter(Paths.get("shillingResults/AuctionShillScoresForNormal_" + suffix + ".csv"), Charset.defaultCharset())) {
			
			for (Entry<BuildUserFeatures.Auction, List<Integer>> auctionBidderEntry : auctionBidders.entrySet()) {
				BuildUserFeatures.Auction auction = auctionBidderEntry.getKey();
				List<Integer> bidders = auctionBidderEntry.getValue();
				
//				if (auction.sellerId.userType.toLowerCase().contains("shill")) {
				if (auction.sellerId >= 5000) {// TODO:
					bw1.append(auction.sellerId + "");
					bw2.append(auction.sellerId + "");
					for (int bidder : bidders) {
//						if (bidder.userType.toLowerCase().contains("shill")) { // bidder is a shill
						if (auction.sellerId >= 5000) {// TODO:
							bw1.append("," + bidder);
							bw1.append(":");
							bw1.append(shillScores.get(bidder).getShillScore(auctionCounts, auction.sellerId) + "");
						} else { // bidder is not a shill
							bw2.append("," + bidder);
							bw2.append(":");
							bw2.append(shillScores.get(bidder).getShillScore(auctionCounts, auction.sellerId) + "");
						}
					}
					bw1.newLine();
					bw2.newLine();
				}
			}
			bw1.flush();
			bw2.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeCollusiveShillScore(Map<Integer, ShillScore> sScores, Map<Integer, CollusiveShillScore> cScores, String suffix) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults", "CollusiveShillScores_" + suffix + ".csv"), Charset.defaultCharset())) {
			bw.write(collusiveShillScoreHeadings());
			bw.newLine();
			
			for (int bidderId : cScores.keySet()) {
				ShillScore sScore = sScores.get(bidderId);
				CollusiveShillScore cScore = cScores.get(bidderId);

				bw.append(bidderId + delimiter);
				bw.append(CSSRatingsString(cScore, sScore).toString());
				bw.newLine();
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static StringBuilder CSSRatingsString(CollusiveShillScore cs, ShillScore ss) {
		StringBuilder sb = new StringBuilder();
		sb.append(cs.getId()).append(delimiter);
		sb.append(cs.getEta()).append(delimiter);
		sb.append(cs.getBindingFactorB()).append(delimiter);
		sb.append(cs.alternatingBidScore(ss)).append(delimiter);
		sb.append(cs.getTheta()).append(delimiter);
		sb.append(cs.getBindingFactorA()).append(delimiter);
		sb.append(cs.alternatingAuctionScore(ss)).append(delimiter);
		sb.append(cs.hybridScore(ss));
		return sb;
	}

	private static String collusiveShillScoreHeadings() {
		StringBuilder sb = new StringBuilder();
		sb.append("bidderId,");
		sb.append("eta,");
		sb.append("bindingFactorB,");
		sb.append("altBidScore,");
		sb.append("theta,");
		sb.append("bindingFactorA,");
		sb.append("altAuctionScore,");
		sb.append("hybridScore");
		return sb.toString();
	}

}
