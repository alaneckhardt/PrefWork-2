package prefwork.rating.test;

import java.util.Arrays;
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
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.StatisticalCollaborative;

public class Bootstrap implements Test {
	protected int[] trainSets;
	protected TestInterpreter resultsInterpreter;
	protected TestResults results;
	protected int run = 0;
	protected int numberOfRuns = 0;
	private static Logger log = Logger.getLogger(Bootstrap.class);
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
		int size = 0;
		//log.debug("Testing user.");
		Long startTestUser = 0L;
		Long endTestUser = 0L;
		while (rec != null) {
			//System.out.print(rec.get(2)+", ");
			startTestUser = System.currentTimeMillis();
			Double compRes = (Double)ind.classifyRecord(rec);
			endTestUser+=System.currentTimeMillis()-startTestUser;
			size++;
			if (compRes != null && !Double.isNaN(compRes) && !Double.isInfinite(compRes))
				results.addResult(userId, run, rec.getObjectId(), // TODO rewrite
						Utils.objectToDouble(rec.getRating()
								)
						// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
						, compRes);
			else
				results.addUnableToPredict(userId, run, rec.getObjectId(), // TODO
						// rewrite
						Utils.objectToDouble(rec.getRating()
								)
				// originalRatings.get(Integer.parseInt(rec.get(1).toString()))
						);

			rec = (Rating)testDataSource.next();
		}

		//System.out.println(" ");
		//results.addTestTimeUser(userId, run, endTestUser);
	}

	protected void testCollaborative(Method ind, DataSource dataSource, List<Integer> userIds, int trainSet, int runInner){
		dataSource.restartUserId();
		int userId = userIds.get(0);		
		dataSource.setFixedUserId(userId);
		size = dataSource.size();

		configTrainDatasource(dataSource, runInner, trainSet, size);
		Long startBuildUser = System.currentTimeMillis();
		int trainCount = ind.buildModel(dataSource, userId);
		results.setTrainCount(userId,  run,trainCount);
		Long endBuildUser = System.currentTimeMillis();
		results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);

		configTestDatasource(dataSource, runInner, trainSet, size);
		for (int i = 0; i < userIds.size(); i++) {
			userId = userIds.get(i);		
			dataSource.setFixedUserId(userId);
			size = dataSource.size();
			//size = 200;
			if(trainSet > size)
				continue;
			if(userId%10 == 0)
				log.debug("User "+userId+" tested.");
			startBuildUser = System.currentTimeMillis();
			testMethod(ind, userId,dataSource,0);
			endBuildUser = System.currentTimeMillis();
			results.addTestTimeUser(userId, run, endBuildUser-startBuildUser);
		}
	}
	protected void testStandard(Method ind, DataSource dataSource, List<Integer> userIds, int trainSet, int runInner){
		dataSource.restartUserId();		
		for (int i = 0; i < userIds.size(); i++) {
			int userId = userIds.get(i);		
			dataSource.setFixedUserId(userId);
			size = dataSource.size();
			
			configTrainDatasource(dataSource, runInner, trainSet, size);
			Long startBuildUser = System.currentTimeMillis();
			int trainCount = ind.buildModel(dataSource, userId);
			results.setTrainCount(userId,  run,trainCount);
			Long endBuildUser = System.currentTimeMillis();
			results.addBuildTimeUser(userId, run, endBuildUser - startBuildUser);
			
			dataSource.setFixedUserId(userId);
			size = dataSource.size();
			log.debug("userId "+userId+", tr "+trainSet);
			//size = 200;
			if(trainSet > size)
				continue;
			if(userId%10 == 0)
				log.debug("User "+userId+" tested.");
			startBuildUser = System.currentTimeMillis();
			configTestDatasource(dataSource, runInner, trainSet, size);
			testMethod(ind, userId,dataSource,0);
			endBuildUser = System.currentTimeMillis();
			results.addTestTimeUser(userId, run, endBuildUser-startBuildUser);
		}
	}
	public void test(Method ind, DataSource trainDataSource2) {
		ContentDataSource trainDataSource = (ContentDataSource)trainDataSource2;
		resultsInterpreter.setHeaderPrefix("date;ratio;dataset;method;");
		results = new TestResults(trainDataSource);
		log.info("Testing method " + ind.toString());
			log.debug("Configuring " + trainDataSource.getName());
			trainDataSource.restartUserId();
			List<Integer> userIds = Utils.getList();
			Integer userId = trainDataSource.userId();
			while(userId != null){
				userIds.add(userId);
				userId = trainDataSource.userId();
			}
			trainDataSource.restartUserId();
			trainDataSource.shuffleInstances();

			for (int trainSet : trainSets) {
				resultsInterpreter.setRowPrefix(""
						+ new Date(System.currentTimeMillis()).toString() + ";"
						+ Double.toString(trainSet) + ";"
						+ trainDataSource.getName() + ";" + ind.toString() + ";");
				log.info("trainSet " + trainSet);		
						run = 0;
					//testDataSource.setLimit(randoms[0], randoms[trainSet-1], false);
					//trainDataSource.setLimit(randoms[0], randoms[trainSet-1], true);
					//results.reset();
				while (run   < numberOfRuns) {
					trainDataSource.shuffleInstances();
					int runInner = 0;
					while (((runInner + 1) * trainSet  <= size || size == 0)&& run   < numberOfRuns) {
						if(ind instanceof StatisticalCollaborative)
							testCollaborative(ind, trainDataSource, userIds, trainSet, runInner);
						else
							testStandard(ind, trainDataSource, userIds, trainSet, runInner);
						run++;
						runInner++;
						synchronized (PrefWork.semWrite) {
							resultsInterpreter.setRowPrefix(""
									+ new Date(System.currentTimeMillis()).toString() + ";"
									+ Double.toString(trainSet) + ";"
									+ trainDataSource.getName() + ";" + ind.toString() + ";");
							PrefWork.semWrite.acquire();
							resultsInterpreter.writeTestResults(results);
							results.reset();
							PrefWork.semWrite.release();						
						}
						System.gc();
					}
				}
				//log.info("User tested.");			

		}
		log.info("Ended method " + ind.toString());
		System.gc();
	}

	@Override
	public Object getResultsInterpreter() {
		// TODO Auto-generated method stub
		return null;
	}

	/*@Override
	public TestInterpreter getResultsInterpreter() {
		return resultsInterpreter;
	}*/

}
