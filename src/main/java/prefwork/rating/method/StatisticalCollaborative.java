package prefwork.rating.method;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.UserEval;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.normalizer.Normalizer;
import prefwork.rating.test.Bootstrap;

import weka.core.Attribute;
import weka.core.Instances;


public class StatisticalCollaborative implements Method {
		private static Logger log = Logger.getLogger(StatisticalCollaborative.class);
		int knn = 50;
		int count = 0;
		Instances attributes;
		Statistical[] users;
		Statistical[][] usersSorted;
		boolean computeOthers = true;
		boolean computeSelf = true;
		HashMap<Integer,HashMap<Integer,Double>> ratings  = new HashMap<Integer,HashMap<Integer,Double>>();
		double[][] sim;
		double threshold = 0.8;
		public String toString() {
			return "StatCollNoClean"+computeOthers+computeSelf+threshold+","+knn;
		}

		public double compare(Statistical s1, Statistical s2){
			Normalizer[] n1 = s1.getNormalizers();
			Normalizer[] n2 = s2.getNormalizers();
			double similarity = 0;
			int count = 0;
			/*WeightAverage wa1 = (WeightAverage) s1.getRater();
			WeightAverage wa2 = (WeightAverage) s1.getRater();
			double[] w1=wa1.getWeights();
			double[] w2=wa2.getWeights();*/
			for (int k = 0; k < attributes.numAttributes(); k++) {
				double s = n1[k].compareTo(n2[k]);
				if(Double.isInfinite(s) || Double.isNaN(s))
					continue;
				similarity += s;
				count++;
			}
			double s =s1.getRater().compareTo(s2.getRater());
			if(!Double.isInfinite(s) && !Double.isNaN(s)){
				similarity+=s;
				count+=1;
			}
			if(count == 0)
				return 0;
				
			return similarity/count;
		}
		protected List<Statistical>  trainStatisticals(DataSource trainingDataset){
			count = 0;
			trainingDataset.restartUserId();
			attributes = ((ContentDataSource)trainingDataset).getInstances();
			Integer userIdDataset;
			int i=0;
			List<Statistical> l = Utils.getList();
			while( (userIdDataset= trainingDataset.userId())!=null){
				trainingDataset.setFixedUserId(userIdDataset);
				trainingDataset.restart();
				if (!trainingDataset.hasNext())
					continue; 
				Statistical s = new Statistical();
				s.setUserId(userIdDataset);
				count += s.buildModel(trainingDataset, userIdDataset);
				if(i%50 == 0)
					log.debug("User "+i);		
					
				l.add(s);
				i++;
			}
			return l;
		}
		
		protected void loadRatings(DataSource trainingDataset){
			trainingDataset.restartUserId();
			attributes = ((ContentDataSource)trainingDataset).getInstances();
			Integer userIdDataset;
			while( (userIdDataset= trainingDataset.userId())!=null){
				ratings.put(userIdDataset, new HashMap<Integer, Double>());
				trainingDataset.setFixedUserId(userIdDataset);
				trainingDataset.restart();
				if (!trainingDataset.hasNext())
					continue;
				Rating rec;
				while ((rec = (Rating)trainingDataset.next()) != null) {
					// record[0] - uzivatelID
					// record[1] - itemID
					// record[2] - rating
					ratings.get(userIdDataset).put(Utils.objectToInteger(rec.getObjectId()), rec.getRating());
					//count++;
				}
			}
		}
		
		protected void computeSimilarities(int user){
			//Avoid making sim = new array[][]
			if(sim == null)
				sim = new double[users.length][];
			for (int j = 0; j < sim.length; j++) {
					sim[j] = new double[j];
			}
			
			for (int i = 1; i < users.length ; i++) {	
				for (int j = 0; j < i; j++) {
					Statistical s1 = users[i];
					Statistical s2 = users[j];
					double s = compare(s1, s2);
					sim[i][j] = s;
				}
			}
			UsersComp comp = new UsersComp();
			
			comp.userId = user;
			
			comp.st=this;
			usersSorted = new Statistical[users.length][];
			for (int i = 0; i < users.length; i++) {
				comp.userId = i;
				usersSorted[i] = users.clone();
				Arrays.sort(usersSorted[i], comp);
			}
		}
		
		public int buildModel(DataSource trainingDataset, int user) {
			log.debug("Loading ratings ");		
			loadRatings(trainingDataset);
			log.debug("Training ");		
			List<Statistical> l = trainStatisticals(trainingDataset);
			log.debug("toArray");		
			users = new Statistical[l.size()];
			l.toArray(users);
			log.debug("Compute similarities ");		
			computeSimilarities(user);
			log.debug("All done.");	
			return count;
		}

		protected double getSimilarityFromSorted(int u1, int u2){
			if(u1 == usersSorted[u1][u2].getUserId())
				return 1;
			try {

				if(u1<usersSorted[u1][u2].getUserId())
					return this.sim[usersSorted[u1][u2].getUserId()][u1];
				else
					return this.sim[u1][usersSorted[u1][u2].getUserId()];
			} catch (Exception e) {
				if(u1<usersSorted[u1][u2].getUserId())
					return this.sim[usersSorted[u1][u2].getUserId()][u1];
				else
					return this.sim[u1][usersSorted[u1][u2].getUserId()];
			}
		}
		public Double classifyRecord(UserEval ue) {
			try {
				Rating rec = (Rating)ue;
				Integer objectId = rec.getObjectId();
				Integer userId = rec.getUserId();
				int count = 0;
				double div = 0;
				double res = 0;
				// Compute the rating using Statistical
				if(computeSelf){
					div = 1;
					res = usersSorted[userId][0].classifyRecord(rec);					
				}
				// Get knn users and their rating of objectId 
				for (int i = 0; i < usersSorted.length && count < knn; i++) {
					double sim = getSimilarityFromSorted(userId, i);
					
					if(sim<threshold)
						break;
					Double compRes = ratings.get(usersSorted[userId][i].getUserId()).get(objectId);
					// If the user didn't rated objectId, the use Statistical to evaluate it. (If it is permitted).
					if(compRes == null && computeOthers){
						compRes = usersSorted[userId][i].classifyRecord(rec);										
					}
					if (compRes == null || Double.isNaN(compRes) || Double.isInfinite(compRes))
						continue;	
					
					count++;
					div+=sim;					
					res+=sim*compRes;
				}
				if(div == 0)
					return null;
				return res/div;	
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public void configClassifier(XMLConfiguration config, String section) {
			Configuration methodConf = config.configurationAt(section);
			computeOthers = Utils.getBooleanFromConfIfNotNull(methodConf, "computeOthers", computeOthers);
			computeSelf = Utils.getBooleanFromConfIfNotNull(methodConf, "computeSelf", computeSelf);
			knn = Utils.getIntFromConfIfNotNull(methodConf, "knn", knn);
			threshold = Utils.getDoubleFromConfIfNotNull(methodConf, "threshold", threshold);
			
		}


		
}
/**
 * Compares two users. Returns which one is more similar to userId, which is given beforehand.
 * @author Alan Eckhardt
 *
 */
class UsersComp implements Comparator<Statistical>{
	int userId = 0;
	StatisticalCollaborative st;
	@Override
	public int compare(Statistical o1, Statistical o2) {
		if(o1.getUserId()== userId)
			return -1;
		if(o2.getUserId()== userId)
			return 1;
		double sim1, sim2;
		if(o1.getUserId()<userId)
			sim1 = st.sim[userId][o1.getUserId()];
		else
			sim1 = st.sim[o1.getUserId()][userId];

		if(o2.getUserId()<userId)
			sim2 = st.sim[userId][o2.getUserId()];
		else
			sim2 = st.sim[o2.getUserId()][userId];
		return -Double.compare(sim1, sim2);
	}
	
}
