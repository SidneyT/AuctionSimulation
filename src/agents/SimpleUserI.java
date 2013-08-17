package agents;

import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.records.ReputationRecord;

public interface SimpleUserI extends EventListenerI {

	ReputationRecord getReputationRecord();

	void addFeedback(Feedback feedback);

	void submitAuction(Auction auction);
	
}