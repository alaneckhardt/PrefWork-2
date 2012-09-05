package prefwork.rating.method;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.UserEval;
import prefwork.rating.Rating;

public class Ideal extends ContentBased {

	public String toString() {
		return "Ideal";
	}

	public void configClassifier(XMLConfiguration config, String section) {

	}

	/**
	 * Nothing here, no build is required.
	 */
	@Override
	public int buildModel(DataSource trainingDataset, int user) {
		return 0;
	}

	/**
	 * Returns the rating of the object.
	 */
	@Override
	public Object classifyRecord(UserEval record) {
		return ((Rating) record).getRating();
	}

}
