package prefwork.rating.test;

import java.util.Date;
import java.util.List;

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
	protected int numberOfRuns = 0;
	private static Logger log = Logger.getLogger(BehaviourAndContentTest.class);
	protected int size = 0;
	@SuppressWarnings("unchecked")
	public void configTest(XMLConfiguration config, String section) {
		Configuration testConf = config.configurationAt(section);
		trainSets = Utils.stringListToIntArray(testConf
				.getList("trainSets"));
		numberOfRuns = Utils.getIntFromConfIfNotNull(testConf, "numberOfRuns", numberOfRuns);
		
		try {
			resultsInterpreter = Utils
					.getTestInterpreter(config, section);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resultsInterpreter.setFilePrefix(testConf.getString("path"));

	}

	/**
	 * Gets all random numbers that are present for a given user.
	 * 
	 * @param ds
	 */
	/*protected static Double[] getRandoms(DataSource ds, int userId) {
		ds.setLimit(0.0, 0.0, false);
		List<Double> rands = CommonUtils.getList();
		ds.setFixedUserId(userId);
		ds.restart();
		Rating  rec = ds.getRecord();
		double max = 0;
		while (rec != null) {
			rands.add(CommonUtils.objectToDouble(rec.get(1)));
			if(max<CommonUtils.objectToDouble(rec.get(2)))
				max = CommonUtils.objectToDouble(rec.get(2));
	//		System.out.print(rec.get(1)+", ");
			rec = ds.getRecord();
			
		}
//		System.out.println(" ");
		Double[] randoms = new Double[rands.size()];
		rands.toArray(randoms);
		Arrays.sort(randoms);
		if(randoms.length > 0)
			randoms[0]=0.0;
		return randoms;
		//ds.setAttributes(attrs);
	}
	*/
	public static void configTrainDatasource(DataSource ds, int run, int trainSet, int size) {
		if((run + 1) * trainSet==size)
			ds.setLimit((run) * trainSet, size, true);
		else
			ds.setLimit((run) * trainSet, (run+1) * trainSet,
				true);
	}
/*randoms.length-1-*/
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
		while (rec != null) {
			startTestUser = System.currentTimeMillis();
			Double compRes = (Double)ind.classifyRecord(rec);
			endTestUser+=System.currentTimeMillis()-startTestUser;
			if (compRes != null && !Double.isNaN(compRes) && !Double.isInfinite(compRes))
				results.addResult(userId, run, rec.getObjectId(), rec.getRating(), compRes);
			else
				results.addUnableToPredict(userId, run, rec.getObjectId(),rec.getRating());

			rec = (Rating)testDataSource.next();
		}
	}

	
	protected void testStandard(BehaviourAndContentMethod ind, BehaviourAndContentData dataSource, List<Integer> userIds, int trainSet, int runInner){
		dataSource.getBehaviour().restartUserId();	
		dataSource.getContent().restartUserId();
		for (int i = 0; i < userIds.size(); i++) {
			int userId = userIds.get(i);		
			dataSource.getBehaviour().setFixedUserId(userId);
			dataSource.getContent().setFixedUserId(userId);
			size = dataSource.size();
			
			configTrainDatasource(dataSource.getBehaviour(), runInner, trainSet, size);
			configTrainDatasource(dataSource.getContent(), runInner, trainSet, size);
			Long startBuildUser = System.currentTimeMillis();
			int trainCount = ind.getBehaviour().buildModel(dataSource.getBehaviour(), userId);
			results.setTrainCount(userId,  run,trainCount);
			Long endBuildUser = System.currentTimeMillis();startBuildUser = System.currentTimeMillis();

			//Fill the ratings of content with predicted ratings from behaviour.
			dataSource.usePredictedRatingsForContent(ind.getBehaviour());
			
			startBuildUser = System.currentTimeMillis();
			trainCount = ind.getContent().buildModel(dataSource.getContent(), userId);
			results.setTrainCount(userId,  run,trainCount);
			endBuildUser = System.currentTimeMillis();
			results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);

			//TODO - replace back the ratings to user ratings.
			dataSource.useUserRatingsForContent();
			
			dataSource.getBehaviour().setFixedUserId(userId);
			dataSource.getContent().setFixedUserId(userId);
			
			//Testing behaviour
			size = dataSource.getBehaviour().size();
			log.debug("userId "+userId+", tr "+trainSet);
			if(trainSet > size)
				continue;
			if(userId%10 == 0)
				log.debug("User "+userId+" tested.");
			startBuildUser = System.currentTimeMillis();
			configTestDatasource(dataSource.getBehaviour(), runInner, trainSet, size);
			testMethod(ind.getBehaviour(), userId,dataSource.getBehaviour(),0);
			endBuildUser = System.currentTimeMillis();
			results.addTestTimeUser(userId, run, endBuildUser-startBuildUser);
			
			//Testing content
			size = dataSource.getBehaviour().size();
			log.debug("userId "+userId+", tr "+trainSet);
			if(trainSet > size)
				continue;
			if(userId%10 == 0)
				log.debug("User "+userId+" tested.");
			startBuildUser = System.currentTimeMillis();
			configTestDatasource(dataSource.getBehaviour(), runInner, trainSet, size);
			testMethod(ind.getBehaviour(), userId,dataSource.getBehaviour(),0);
			endBuildUser = System.currentTimeMillis();
			results.addTestTimeUser(userId, run, endBuildUser-startBuildUser);
			
		}
	}
	public void test(Method ind, DataSource dataSource) {
		BehaviourAndContentData trainDataSource = (BehaviourAndContentData)dataSource;
		BehaviourAndContentMethod m = (BehaviourAndContentMethod) ind;
		// ContentDataSource trainDataSource =
		// (ContentDataSource)trainDataSource2;
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		results = new TestResults(trainDataSource);
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
			run = 0;
			while (run < numberOfRuns) {
				trainDataSource.getBehaviour().shuffleInstances();
				trainDataSource.getContent().shuffleInstances();
				int runInner = 0;
				while (((runInner + 1) * trainSet <= size || size == 0) && run < numberOfRuns) {
					testStandard(m, trainDataSource, userIds, trainSet, runInner);
					run++;
					runInner++;
					synchronized (PrefWork.semWrite) {
						resultsInterpreter.setRowPrefix("" + new Date(System.currentTimeMillis()).toString() + ";" + Double.toString(trainSet) + ";" + trainDataSource.getName() + ";" + ind.toString() + ";");
						PrefWork.semWrite.acquire();
						resultsInterpreter.writeTestResults(results);
						results.reset();
						PrefWork.semWrite.release();
					}
					System.gc();
				}
			}
		}
		log.info("Ended method " + m.toString());
		System.gc();
	}

	@Override
	public Object getResultsInterpreter() {
		return null;
	}
}
