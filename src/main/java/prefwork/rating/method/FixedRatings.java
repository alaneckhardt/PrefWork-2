package prefwork.rating.method;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.UserEval;
import prefwork.core.Utils;
import prefwork.rating.Rating;

public class FixedRatings extends ContentBased {
	Map<Integer,Map<Integer, Double>> ratings;
	String file;
	public String toString() {
		return "FixedRatings";
	}

	/**
	 * Loads the ratings from file into the map.
	 */
	protected void loadRatings(){
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				int userId = Integer.parseInt(line.substring(0, line.indexOf(';')));
				line = line.substring(line.indexOf(';')+1);
				int objectId = Integer.parseInt(line.substring(0, line.indexOf(';')));
				double r = Double.parseDouble(line.substring(line.indexOf(';')+1));
				if(!ratings.containsKey(userId))
					ratings.put(userId, new HashMap<Integer, Double>());
				ratings.get(userId).put(objectId, r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		file = Utils.getFromConfIfNotNull(methodConf, "file", file);
		ratings = new HashMap<Integer,Map<Integer, Double>> (); 
		if(file != null)
			loadRatings();
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
		return ratings.get(((Rating) record).getUserId()).get(((Rating) record).getObjectId());
	}

}
