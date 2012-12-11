package prefwork.rating.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.Test;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.BehaviourAndContentData;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.BehaviourAndContentMethod;

public class CreateFixedRatings implements Test {
	private static Logger log = Logger.getLogger(CreateFixedRatings.class);
	protected int size = 0;
	Rating[][] userRecords;
	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
	}

	public static void configTrainDatasource(DataSource ds, int run,
			int trainSet, int size) {
		if ((run + 1) * trainSet == size)
			ds.setLimit((run) * trainSet, size, true);
		else
			ds.setLimit((run) * trainSet, (run + 1) * trainSet, true);
	}

	public static void configTestDatasource(DataSource ds, int run,
			int trainSet, int size) {
		if ((run + 1) * trainSet == size)
			ds.setLimit(((run) * trainSet), (size), false);
		else
			ds.setLimit(((run) * trainSet), ((run + 1) * trainSet), false);
	}


	/**
	 * Runs one test for all users from the dataset. The settings of the
	 * datasource are given as parameters.
	 * 
	 * @param ind
	 * @param dataSource
	 * @param userIds
	 * @param trainSet
	 * @param runInner
	 */
	protected void testOneRun(BehaviourAndContentMethod ind,
			BehaviourAndContentData dataSource, Integer userId, int trainSet) {
		dataSource.getBehaviour().setLimit(-1, -1, false);
		dataSource.getContent().setLimit(-1, -1, false);
		dataSource.getBehaviour().restart();
		dataSource.getContent().restart();
		if (!checkClasses(getClassesCounts(dataSource.getBehaviour()), 1)) {
			return;
		}
		dataSource.getBehaviour().setFixedUserId(userId);
		dataSource.getContent().setFixedUserId(userId);
		dataSource.getBehaviour().shuffleInstances();
		dataSource.getContent().shuffleInstances();
		size = dataSource.size();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
		size = dataSource.size();

		configTrainDatasource(dataSource.getBehaviour(), 0, size, size);
		configTrainDatasource(dataSource.getContent(), 0, size, size);
		Long startBuildUser, endBuildUser;
		int trainCount;
		if (ind.getBehaviour() != null) {
			startBuildUser = System.currentTimeMillis();
			trainCount = ind.getBehaviour().buildModel(
					dataSource.getBehaviour(), userId);
			endBuildUser = System.currentTimeMillis();
			startBuildUser = System.currentTimeMillis();
			writeRatings(dataSource, ind.getBehaviour());
			// Fill the ratings of content with predicted ratings from
			// behaviour.
			//dataSource.usePredictedRatingsForContent(ind.getBehaviour());
			//dataSource.getContent().setFixedUserId(userId);
		}
		/*startBuildUser = System.currentTimeMillis();

		trainCount = ind.getContent().buildModel(dataSource.getContent(),
				userId);
		endBuildUser = System.currentTimeMillis();
		if (ind.getBehaviour() != null) {
			// Replace back the ratings to user ratings.
			dataSource.useUserRatingsForContent();
			dataSource.getContent().setFixedUserId(userId);
		}

		configTestDatasource(dataSource.getContent(), 0, trainSet, size);
		// Testing content
		size = dataSource.getContent().size();
		startBuildUser = System.currentTimeMillis();
		endBuildUser = System.currentTimeMillis();*/

	}

	/**
	 * 
	 * @param counts
	 * @param atLeast
	 *            how many bought items, i.e. those with rating == 5
	 * @return
	 */
	protected boolean checkClasses(Map<Double, Integer> counts, int atLeast) {
		/*
		 * if(counts.size() < 2) return false;
		 */
		if (atLeast <= 0)
			return true;

		for (Double d : counts.keySet()) {
			if (d == 1.0 && counts.get(d) >= atLeast)
				return true;
		}

		return false;
	}

	/**
	 * Checks if there is at least two different classes in the train and test
	 * data.
	 * 
	 * @return
	 */
	protected Map<Double, Integer> getClassesCounts(DataSource ds) {
		ds.restart();
		Rating rec = (Rating) ds.next();
		Map<Double, Integer> l = new HashMap<Double, Integer>();
		while (rec != null) {
			double d = rec.getRating();
			if (!l.containsKey(d))
				l.put(d, 0);
			l.put(d, (l.get(d)) + 1);
			rec = (Rating) ds.next();
		}
		return l;
	}

	protected void setOneUser(BehaviourAndContentData trainDataSource){

		Integer userId;
		Rating[][] userRecords = new Rating[1][];
		List<Rating> list= Utils.getList();
		while ((userId = trainDataSource.userId()) != null) {
			trainDataSource.setFixedUserId(userId);
			trainDataSource.restart();
			Rating rec = (Rating)trainDataSource.next();
			while (rec != null) {
			//	rec.setUserId(0);
				list.add(rec);
				rec = (Rating)trainDataSource.next();
			}			
		}
		userRecords[0] = new Rating[list.size()];
		userRecords[0] = list.toArray(userRecords[0]);
		ContentDataSource th = (ContentDataSource)trainDataSource.getBehaviour();
		th.setUserRecords(userRecords);
	}
	
	protected void writeRatings(BehaviourAndContentData trainDataSource, Method m){
		BufferedWriter out = null;
		try {
			//Restore original user records.
			trainDataSource.getBehaviour().setUserRecords(this.userRecords);
			out = new BufferedWriter(new FileWriter(m.toString() + ".csv",false));
			Integer userId;
			trainDataSource.restartUserId();
			while ((userId = trainDataSource.userId()) != null) {
				trainDataSource.setFixedUserId(userId);
				trainDataSource.restart();
				Rating rec = (Rating)trainDataSource.next();
				while (rec != null) {
					Double d = (Double)m.classifyRecord(rec);
					out.write(rec.getUserId()+";"+rec.getObjectId()+";"+d+"\n");
					rec = (Rating)trainDataSource.next();
				}			
			}
		} catch (Exception e) {
		}
		finally{
			if(out != null){
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
				}	
			}
		}
	
	}
	public void test(Method ind, DataSource dataSource) {
		BehaviourAndContentData trainDataSource = (BehaviourAndContentData) dataSource;
		BehaviourAndContentMethod m = (BehaviourAndContentMethod) ind;
		// Start with behaviour dataset to get the users.
		trainDataSource.useBehaviour();
		log.info("Testing method " + m.toString());
		log.debug("Configuring " + trainDataSource.getName());
		userRecords = trainDataSource.getBehaviour().getUserRecords();
		setOneUser(trainDataSource);
		List<Integer> userIds = Utils.getList();
		userIds.add(0);
		trainDataSource.restartUserId();
		trainDataSource.shuffleInstances();
		trainDataSource.getBehaviour().restartUserId();
		trainDataSource.getContent().restartUserId();
		for (int i = 0; i < userIds.size(); i++) {
			Integer userId = userIds.get(i);
			trainDataSource.getBehaviour().setFixedUserId(userId);
			trainDataSource.getContent().setFixedUserId(userId);
			testOneRun(m, trainDataSource, userId, trainDataSource.size());
			System.gc();
		}

		log.info("Ended method " + m.toString());
		System.gc();
	}

	@Override
	public Object getResultsInterpreter() {
		return null;
	}
}
