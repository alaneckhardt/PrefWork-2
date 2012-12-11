package prefwork.rating.method;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.UserEval;
import prefwork.rating.Rating;

public class Random implements Method{

	private Long seed = 45468782313L;
	private double max,min;
	private java.util.Random r;

    public String toString(){
    	return "Random";
    }
    

	/**
	 * Builds model for specified user.
	 */
	public int buildModel(DataSource trainingDataset, int user) {
		if(r == null)
			r = new java.util.Random(seed);
		else
			r = new java.util.Random(r.nextLong());
		
    	trainingDataset.setFixedUserId(user);
        trainingDataset.restart();
        max = Double.MIN_VALUE;
        min = Double.MAX_VALUE;
        Rating rec;
        while((rec = (Rating)trainingDataset.next())!= null) {
        	double r = rec.getRating();
        	if(max<r)
        		max = r;
        	if(min>r)
        		min = r;
        }		
        return 0;
	}

	/**
	 * 
	 */
	public Double classifyRecord(UserEval e) {
		return min+(r.nextDouble()*(max-min));
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}

}
