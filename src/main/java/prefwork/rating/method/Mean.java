package prefwork.rating.method;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.UserEval;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;

public class Mean implements Method{

	private double mean = 0D;
	

    public String toString(){
    	return "Mean";
    }
    

	private void clear(){
		mean = 0D;
	}
	/**
	 * Builds model for specified user.
	 */
	public int buildModel(DataSource ds, int user) {
		ContentDataSource trainingDataset = (ContentDataSource)ds;
		clear();
    	trainingDataset.setFixedUserId(user);
        trainingDataset.restart();
        Rating rec;
        int length=0;
        while((rec = (Rating)trainingDataset.next())!= null) {
        	mean+=rec.getRating();
        	length++;
        }		
        mean/=length;
        return length;
	}

	/**
	 * 
	 */
	public Double classifyRecord(UserEval record) {
		return mean;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}

}
