package prefwork.rating.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.UserEval;
import prefwork.core.Utils;
import prefwork.rating.Rating;

import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

public class CofiBridge implements Method {

		DataModel trainset  = null;
		Recommender recommender = null;
		int knn = 30;
		public String toString() {
			return "CofiBridge"+knn;
		}

		
		private ArrayList<User> processUsers(HashMap<Integer, ArrayList<Preference>> preferences){

			final ArrayList<User> users = new ArrayList<User>(preferences.size());
			for (final Map.Entry<Integer, ArrayList<Preference>> entries : preferences.entrySet()) {
				users.add(new GenericUser<Integer>(entries.getKey(), entries.getValue()));
			}
			return users;
		}

		public int buildModel(DataSource trainingDataset, int user) {
			trainingDataset.restartUserId();
			Integer userIdDataset;
			int count = 0;
			HashMap<Integer, ArrayList<Preference>> preferences = new HashMap<Integer, ArrayList<Preference>>();
			while( (userIdDataset= trainingDataset.userId())!=null){
				trainingDataset.setFixedUserId(userIdDataset);
				trainingDataset.restart();
				if (!trainingDataset.hasNext())
					continue; 
				
				Rating rec;
				while ((rec = (Rating)trainingDataset.next()) != null) {
					// record[0] - uzivatelID
					// record[1] - itemID
					// record[2] - rating
					// Create the instance
	
					// add the instance
					int userId = rec.getUserId();
					int objectId = rec.getObjectId();
					double rating = rec.getRating();
					if(!preferences.containsKey(userId)){
						preferences.put(userId, new ArrayList<Preference>());
					}
					ArrayList<Preference> pref = preferences.get(userId);
					pref.add(new GenericPreference(null , new GenericItem<Integer>(objectId), Utils.objectToDouble(rating)));
					count++;
				}
			}
			ArrayList<User> users = processUsers(preferences);
			try {
				trainset = new GenericDataModel(users);
				UserCorrelation userCorrelation = new PearsonCorrelation(trainset);
				UserNeighborhood neighborhood =
					  new NearestNUserNeighborhood(knn, userCorrelation,  trainset);
				
				// Construct the list of pre-compted correlations
				/*Collection<GenericItemCorrelation.ItemItemCorrelation> correlations =
					  ...;
				ItemCorrelation itemCorrelation = new GenericItemCorrelation(correlations);
				recommender = new GenericItemBasedRecommender(trainset, itemCorrelation);*/
				recommender = new GenericUserBasedRecommender(trainset, neighborhood, userCorrelation);					
			} catch (Exception e) {
				e.printStackTrace();
			}
			return count;
		}

		public Double classifyRecord(UserEval r) {
			Rating rec = (Rating)r;
			try {
				int userId = rec.getUserId();
				int objectId = rec.getObjectId();
				return recommender.estimatePreference(userId, objectId);	
			} catch (Exception e) {
			//	e.printStackTrace();
			}
			return null;
		}

		public void configClassifier(XMLConfiguration config, String section) {
			Configuration methodConf = config.configurationAt(section);
			if (methodConf.containsKey("knn")) {
				knn = methodConf.getInt("knn");
			}
		}


}
