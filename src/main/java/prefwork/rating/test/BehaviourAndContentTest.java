package prefwork.rating.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.PrefWork;
import prefwork.core.Test;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.BehaviourAndContentData;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.BehaviourAndContentMethod;
import prefwork.rating.method.StatisticalCollaborative;

public class BehaviourAndContentTest implements Test {
	protected int[] trainSets;
	protected TestInterpreter resultsInterpreter;
	protected TestResults results;
	protected int run = 0;
	protected int usersToTest = 100000;
	protected int numberOfRuns = 0;
	private static Logger log = Logger.getLogger(BehaviourAndContentTest.class);
	protected int size = 0;
	/**How many ratings == 5 should be in training set.*/
	protected int  boughtInTrain = 0;
	/**How many ratings == 5 should be in testing set.*/
	protected int  boughtInTest = 0;
	
	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		trainSets = Utils.stringListToIntArray(testConf
				.getList("trainSets"));
		numberOfRuns = Utils.getIntFromConfIfNotNull(testConf, "numberOfRuns", numberOfRuns);
		usersToTest = Utils.getIntFromConfIfNotNull(testConf, "usersToTest", usersToTest);
		boughtInTest = Utils.getIntFromConfIfNotNull(testConf, "boughtInTest", boughtInTest);
		boughtInTrain = Utils.getIntFromConfIfNotNull(testConf, "boughtInTrain", boughtInTrain);
		try {
			resultsInterpreter = Utils
					.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		resultsInterpreter.setFilePrefix(testConf.getString("path"));
	}

	public static void configTrainDatasource(DataSource ds, int run, int trainSet, int size) {
		if((run + 1) * trainSet==size)
			ds.setLimit((run) * trainSet, size, true);
		else
			ds.setLimit((run) * trainSet, (run+1) * trainSet, true);
	}

	public static void configTestDatasource(DataSource ds, int run, int trainSet, int size) {
		if((run + 1) * trainSet==size)
			ds.setLimit(((run) * trainSet), (size), false);
		else
			ds.setLimit(((run) * trainSet), ((run+1) * trainSet), false);
	}

	protected void testMethod(Method ind, Integer userId, DataSource testDataSource, Integer targetAttribute ){
		testDataSource.setFixedUserId(userId);
		testDataSource.restart();
		Rating  rec = (Rating)testDataSource.next();
		Long startTestUser = 0L;
		Long endTestUser = 0L;
		List<Integer> seen = new ArrayList<Integer>();
		while (rec != null) {
			startTestUser = System.currentTimeMillis();
			Double compRes = (Double)ind.classifyRecord(rec);
			endTestUser+=System.currentTimeMillis()-startTestUser;
			if (compRes != null && !Double.isNaN(compRes) && !Double.isInfinite(compRes))
				results.addResult(userId, run, rec.getObjectId(), rec.getRating(), compRes);
			else
				results.addUnableToPredict(userId, run, rec.getObjectId(),rec.getRating());
			seen.add(rec.getObjectId());
			rec = (Rating)testDataSource.next();
		}
		
		//Test objects of other users.
		testDataSource.restartUserId();
		List<Integer> userIds = Utils.getList();
		userIds.add(userId);
		Integer otherUserId = testDataSource.userId();
		testDataSource.setLimit(-1, -1, false);
		Double tempRating;
		int tempUserId;
		while (otherUserId != null) {
			if(userIds.contains(otherUserId)){
				otherUserId = testDataSource.userId();
				continue;
			}
			userIds.add(otherUserId);
			testDataSource.setFixedUserId(otherUserId);
			testDataSource.restart();
			rec = (Rating)testDataSource.next();
			while (rec != null) {
				if(seen.contains(rec.getObjectId())){
					rec = (Rating)testDataSource.next();
					continue;
				}
				tempRating = rec.getRating();
				tempUserId = rec.getUserId();
				rec.setRating(0);
				rec.setUserId(userId);
				startTestUser = System.currentTimeMillis();
				Double compRes = (Double)ind.classifyRecord(rec);
				endTestUser+=System.currentTimeMillis()-startTestUser;
				//Use 0 as object rating.
				if (compRes != null && !Double.isNaN(compRes) && !Double.isInfinite(compRes))
					results.addResult(userId, run, rec.getObjectId(), 0.0, compRes);
				else
					results.addUnableToPredict(userId, run, rec.getObjectId(),0.0);
				seen.add(rec.getObjectId());
				rec.setRating(tempRating);
				rec.setUserId(tempUserId);
				rec = (Rating)testDataSource.next();
			}
			otherUserId = testDataSource.userId();
		}
		testDataSource.setFixedUserId(userId);

	}
	
	/**
	 * Configures the datasource so that in test and train sets is at least on positive rating.
	 * @param dataSource
	 * @return
	 */
	protected boolean checkDataSource(DataSource dataSource, int runInner, int trainSet, int userId,boolean distinctTrain){
		configTrainDatasource(dataSource, runInner, trainSet, size);
		Map<Double,Integer> classesTrain = getClassesCounts(dataSource);
		configTestDatasource(dataSource, runInner, trainSet, size);
		Map<Double,Integer> classesTest = getClassesCounts(dataSource);
		while (!checkClasses(classesTrain,boughtInTrain, distinctTrain) || !checkClasses(classesTest,boughtInTest, false)) {
			dataSource.shuffleInstances(userId);
			configTrainDatasource(dataSource, runInner, trainSet, size);
			classesTrain = getClassesCounts(dataSource);
			configTestDatasource(dataSource, runInner, trainSet, size);
			classesTest = getClassesCounts(dataSource);
		}		
		return true;
	}

	/**
	 * Runs one test for all users from the dataset. The settings of the datasource are given as parameters.
	 * @param ind
	 * @param dataSource
	 * @param userIds
	 * @param trainSet
	 * @param runInner
	 */
	protected void testOneRun(BehaviourAndContentMethod ind, BehaviourAndContentData dataSource, Integer userId, int trainSet) {

		run = 0;
		dataSource.getBehaviour().setLimit(-1, -1, false);
		dataSource.getContent().setLimit(-1, -1, false);
		dataSource.getBehaviour().restart();
		dataSource.getContent().restart();
		if (!checkClasses(getClassesCounts(dataSource.getBehaviour()),boughtInTest + boughtInTrain, false)) {
			return;
		}
		if (!checkClasses(getClassesCounts(dataSource.getContent()),boughtInTest + boughtInTrain, true)) {
			return;
		}
		//We need at least one object in the test set.
		if(dataSource.getContent().size() <= trainSet)
			return;
		if(dataSource.getBehaviour().size() <= trainSet)
			return;
		while (run < numberOfRuns) {
			dataSource.getBehaviour().setFixedUserId(userId);
			dataSource.getContent().setFixedUserId(userId);
			dataSource.getBehaviour().shuffleInstances(userId);
			dataSource.getContent().shuffleInstances(userId);
			size = dataSource.size();
			int runInner = 0;
			// Train set bigger than size of the dataset.
			if ((runInner + 1) * trainSet > size - 1 && size > 0)
				break;
			while (((runInner + 1) * trainSet <= size - 1 || size == 0) && run < numberOfRuns) {

				/*try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
				}*/
				size = dataSource.size();
				checkDataSource(dataSource.getBehaviour(),runInner, trainSet, userId, false);
				checkDataSource(dataSource.getContent(),runInner, trainSet, userId, false);

				configTrainDatasource(dataSource.getBehaviour(), runInner, trainSet, size);
				configTrainDatasource(dataSource.getContent(), runInner, trainSet, size);
				Long startBuildUser, endBuildUser;
				int trainCount;
				if (ind.getBehaviour() != null) {
					trainCount = ind.getBehaviour().buildModel(dataSource.getBehaviour(), userId);
					results.setTrainCount(userId, run, trainCount);
					// Fill the ratings of content with predicted ratings from
					// behaviour.
					dataSource.usePredictedRatingsForContent(ind.getBehaviour(), userId);
					dataSource.getContent().setFixedUserId(userId);
				}
				//Only one class in learned ratings, continue
				Map<Double,Integer> counts = getClassesCounts(dataSource.getContent());
				/*if(counts.size() >= 1 && !checkClasses(counts, boughtInTrain, true)){					
					dataSource.getBehaviour().shuffleInstances(userId);
					dataSource.getContent().shuffleInstances(userId);					
					continue;
				}*/
				//checkDataSource(dataSource.getContent(),runInner, trainSet, userId, true);
				startBuildUser = System.currentTimeMillis();
				trainCount = ind.getContent().buildModel(dataSource.getContent(), userId);
				results.setTrainCount(userId, run, trainCount);
				endBuildUser = System.currentTimeMillis();
				results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);
				if (ind.getBehaviour() != null) {
					// Replace back the ratings to user ratings.
					dataSource.useUserRatingsForContent(userId);
					dataSource.getContent().setFixedUserId(userId);
				}

				configTestDatasource(dataSource.getContent(), runInner, trainSet, size);
				// Testing content
				size = dataSource.getContent().size();
				log.debug("userId " + userId + ", tr " + trainSet);
				if (trainSet > size)
					continue;
				if (userId % 10 == 0)
					log.debug("User " + userId + " tested.");
				startBuildUser = System.currentTimeMillis();				
				testMethod(ind.getContent(), userId, dataSource.getContent(), 0);
				endBuildUser = System.currentTimeMillis();
				results.addTestTimeUser(userId, run, endBuildUser - startBuildUser);

				run++;
				runInner++;
			}
		}
	}
	
	/**
	 * 
	 * @param counts
	 * @param atLeast how many bought items, i.e. those with rating == 5
	 * @return
	 */
	protected boolean checkClasses(Map<Double,Integer> counts, int atLeast, boolean distinct){
		if(distinct && counts.size() < 2)
			return false;
		if(atLeast <= 0)
			return true;
		
		for(Double d : counts.keySet()){
			if(d == 1.0 && counts.get(d) >= atLeast)
				return true;
		}
		
		return false;
	}
	/**
	 * Checks if there is at least two different classes in the train and test data.
	 * @return
	 */
	protected Map<Double,Integer> getClassesCounts(DataSource ds){
		ds.restart();
		Rating  rec = (Rating)ds.next();
		Map<Double, Integer> l = new HashMap<Double,Integer>();
		while (rec != null) {
			double d = rec.getRating();
			if(!l.containsKey(d))			
				l.put(d, 0);
			l.put(d, (l.get(d))+1);
			rec = (Rating)ds.next();
		}
		return l;
	}
	public void test(Method ind, DataSource dataSource) {
		BehaviourAndContentData trainDataSource = (BehaviourAndContentData)dataSource;
		BehaviourAndContentMethod m = (BehaviourAndContentMethod) ind;
		// ContentDataSource trainDataSource =
		// (ContentDataSource)trainDataSource2;
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		results = new TestResults(trainDataSource.getContent());
		//Start with behaviour dataset to get the users.
		trainDataSource.useBehaviour();
		log.info("Testing method " + m.toString());
		log.debug("Configuring " + trainDataSource.getName());
		trainDataSource.restartUserId();
		List<Integer> userIds = Utils.getList();
		Integer userId = trainDataSource.userId();
		while (userId != null) {
			userIds.add(userId);
			userId = trainDataSource.userId();
		}
		trainDataSource.restartUserId();
		trainDataSource.shuffleInstances();

		for (int trainSet : trainSets) {
			resultsInterpreter.setRowPrefix("" + new Date(System.currentTimeMillis()).toString() + ";" + Double.toString(trainSet) + ";" + trainDataSource.getName() + ";" + ind.toString() + ";");
			log.info("trainSet " + trainSet);
			trainDataSource.getBehaviour().restartUserId();	
			trainDataSource.getContent().restartUserId();		
			for (int i = 0; i < userIds.size() && i < usersToTest; i++) {
				userId = userIds.get(i);		
				trainDataSource.getBehaviour().setFixedUserId(userId);
				trainDataSource.getContent().setFixedUserId(userId);
				testOneRun(m, trainDataSource, userId, trainSet);
					synchronized (PrefWork.semWrite) {
						resultsInterpreter.setRowPrefix("" + new Date(System.currentTimeMillis()).toString() + ";" + Double.toString(trainSet) + ";" + trainDataSource.getName() + ";" + ind.toString() + ";");
						PrefWork.semWrite.acquire();
						resultsInterpreter.writeTestResults(results);
						results.reset();
						PrefWork.semWrite.release();
					}
					//System.gc();
				
			}
		}
		log.info("Ended method " + m.toString());
		//System.gc();		
	}

	@Override
	public Object getResultsInterpreter() {
		return null;
	}
}
