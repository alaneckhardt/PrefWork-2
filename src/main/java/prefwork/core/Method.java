package prefwork.core;

import org.apache.commons.configuration.XMLConfiguration;


public interface Method {

	/**
	 * Builds the user model for a user and a training set.
	 * @param trainingDataset
	 * @param user
	 * @return Number of acquired objects.
	 */
    public int buildModel(DataSource trainingDataset, int user);
    
    /** Evaluates the given record. 
     * @param record
     * @return The predicted user evaluation of the record. 
     * The return type is dependent on the type of UserEval.
     */
    public Object classifyRecord(UserEval record);
    public void configClassifier(XMLConfiguration config, String section);
    public String toString();
}
