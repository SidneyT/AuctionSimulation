package agents;

import simulator.records.ReputationRecord;

public interface SimpleUserI extends EventListenerI{

	ReputationRecord getReputationRecord();

}