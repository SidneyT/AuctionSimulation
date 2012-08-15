package createShillScores;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import createShillScores.BuildShillScore.Auction;
import createShillScores.BuildShillScore.User;

public class WriteScores {

	/**
	 * Writes shill scores of all users to a file.
	 * 
	 * @param shillScores map containing userIds mapped to ShillScore objects
	 * @param auctionCounts map containing sellerIds mapped to number of auctions they submitted
	 * @param suffix string added to the end of the filename
	 */
	public static void writeShillScores(Map<Integer, ShillScore> shillScores, Map<Integer, Integer> auctionCounts, String suffix, int[]... reweights) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults/ShillScores_" + suffix + ".csv"), Charset.defaultCharset())) {
			bw.write(shillScoreHeadings(reweights));
			bw.newLine();
			
			for (Entry<Integer, ShillScore> shillScoreEntry : shillScores.entrySet()) {
				int bidderId = shillScoreEntry.getKey();
				ShillScore shillScore = shillScoreEntry.getValue();
				
				if (shillScore.lossCount != 0) {
					bw.append(bidderId + ",");
					bw.append(shillScore.winCount + ",");
					bw.append(shillScore.lossCount + ",");
					bw.append(shillScore.getAlpha(auctionCounts).maxAlpha + ",");
					bw.append(shillScore.getBeta() + ",");
					bw.append(shillScore.getGamma() + ",");
					bw.append(shillScore.getDelta() + ",");
					bw.append(shillScore.getEpsilon() + ",");
					bw.append(shillScore.getZeta() + "");
					bw.append("," + shillScore.getShillScore(auctionCounts));
					for (int i = 0; i < reweights.length; i++) {
						double reweighted = shillScore.getShillScore(auctionCounts, reweights[i]);
						bw.append("," + reweighted);
					}
					
					bw.newLine();
				}
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			sb.append(",").append(Arrays.toString(reweights[i]).replaceAll(", ", ""));
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
			Map<Auction, List<User>> auctionBidders, Map<Integer, Integer> auctionCounts, String suffix) {
		
		try (BufferedWriter bw1 = Files.newBufferedWriter(Paths.get("shillingResults/AuctionShillScoresForShillers_" + suffix + ".csv"), Charset.defaultCharset());
				BufferedWriter bw2 = Files.newBufferedWriter(Paths.get("shillingResults/AuctionShillScoresForNormal_" + suffix + ".csv"), Charset.defaultCharset())) {
			
			for (Entry<Auction, List<User>> auctionBidderEntry : auctionBidders.entrySet()) {
				Auction auction = auctionBidderEntry.getKey();
				List<User> bidders = auctionBidderEntry.getValue();
				
				if (auction.seller.userType.toLowerCase().contains("shill")) {
					bw1.append(auction.seller.id + "");
					bw2.append(auction.seller.id + "");
					for (User bidder : bidders) {
						if (bidder.userType.toLowerCase().contains("shill")) { // bidder is a shill
							bw1.append("," + bidder.id);
							bw1.append(":");
							bw1.append(shillScores.get(bidder.id).getShillScore(auctionCounts, auction.seller.id) + "");
						} else { // bidder is not a shill
							bw2.append("," + bidder.id);
							bw2.append(":");
							bw2.append(shillScores.get(bidder.id).getShillScore(auctionCounts, auction.seller.id) + "");
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
				
				bw.append(bidderId + ",");
				bw.append(cScore.getEta() + ",");
				bw.append(cScore.getBindingFactorB() + ",");
				bw.append(cScore.alternatingBidScore(sScore) + ",");
				bw.append(cScore.getTheta() + ",");
				bw.append(cScore.getBindingFactorA() + ",");
				bw.append(cScore.alternatingAuctionScore(sScore) + ",");
				bw.append(cScore.hybridScore(sScore) + "");
				bw.newLine();
			}
			
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
