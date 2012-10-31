package agents.bidders;

public class BuyerAuctionParticipationFrequency {

	//	public static double numberOfAuctionsPer100Days(double random) {
	//		// x = [(x1^(n+1) - x0^(n+1))*y + x0^(n+1)]^(1/(n+1))
	//		// modified to x = x0 - [(x1^(n+1) - x0^(n+1))*y + x0^(n+1)]^(1/(n+1))
	//		double param = 60; 
	//		
	//		double number = 101 - Math.pow((Math.pow(1, param) - Math.pow(100, param)) * random + Math.pow(100, param), 1/param) - 5;
	//		if (number < 1) {
	//			return 1;
	//		} else if (number > 8) {
	//			return 8;
	//		} else {
	//			return number;
	//		}
	//	}
	public static int numberOfAuctionsPer100Days(double random) {
		if (random < 0.6) return 1;
		else if (random < 0.78) return 2;
		else if (random < 0.86) return 3;
		else if (random < 0.91) return 4;
		else if (random < 0.94) return 5;
		else if (random < 0.956273) return 6;
		else if (random < 0.964617) return 7;
		else if (random < 0.970626) return 8;
		else if (random < 0.975345) return 9;
		else if (random < 0.979018) return 10;
		else if (random < 0.981647) return 11;
		else if (random < 0.984031) return 12;
		else if (random < 0.986350) return 13;
		else if (random < 0.987999) return 14;
		else if (random < 0.989501) return 15;
		else if (random < 0.990497) return 16;
		else if (random < 0.991460) return 17;
		else if (random < 0.992293) return 18;
		else if (random < 0.993126) return 19;
		else if (random < 0.993779) return 20;
		else if (random < 0.994416) return 21;
		else if (random < 0.994987) return 22;
		else if (random < 0.995542) return 23;
		else if (random < 0.995869) return 24;
		else if (random < 0.996277) return 25;
		else return 26;
	}

}
