package prefwork.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import prefwork.rating.Rating;

public class PrefWork {

	private static Logger log = Logger.getLogger(PrefWork.class);

	public static Semaphore semRuns;
	
	public static Semaphore semWrite;
	
	XMLConfiguration confRuns = new XMLConfiguration();
	XMLConfiguration confDatasources = new XMLConfiguration();
	XMLConfiguration confMethods = new XMLConfiguration();

	@SuppressWarnings("unchecked")
	public Method getMethod(String methodName)throws SecurityException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		Configuration methodConf = confMethods.configurationAt( methodName);
		String methodClass = methodConf.getString("class");

		Constructor[] a = Class.forName(methodClass).getConstructors();
		Method ind = (Method) a[0].newInstance();
		ind.configClassifier(confMethods, methodName);
		return ind;
	}
	
	@SuppressWarnings("unused")
	private void writeRecords(DataSource data){
		data.restartUserId();
		Integer userIdDataset;
		prefwork.rating.datasource.RatingNetFlix d = (prefwork.rating.datasource.RatingNetFlix)data;
		int dimension = d.getRatingsCount();
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("Netflix"+dimension + ".dat"));
			while (data.hasNextUserId()) {
				userIdDataset = data.userId();
				data.setFixedUserId(userIdDataset);
				data.restart();

				Rating rec;
				int count = 0;
				while ((rec = (Rating) data.next()) != null && count < dimension) {
					count++;
					int objectId = rec.getObjectId();
					if(count == dimension)
						out.write(objectId);	
					else
						out.write(objectId+", ");	
					count++;
				}		
				for (; count < dimension; count++) {
					if(count == dimension-1)
						out.write("0");		
					else
						out.write("0, ");						
				}
				
				out.write("\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	
	
	public DataSource getDataSource(String dataSourceName, String dbName) throws SecurityException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		Configuration dbConf = confDatasources.configurationAt( dbName);
		String dbClass = dbConf.getString("class");
		
		Constructor[] a = Class.forName(dbClass).getConstructors();
		DataSource ds = (DataSource) a[0].newInstance();
		ds.configDataSource(confDatasources, dbName,/*+".datasources."+*/ dataSourceName);
		if(confDatasources.getProperty( dataSourceName+".fillRandom")!=null){
			int fillRandomValues =  confDatasources.getInt( dataSourceName+".fillRandom");

			if(fillRandomValues == 1)
				ds.shuffleInstances();
		}
		ds.setName(dataSourceName+dbName);
		return ds;
	}
	
	public PrefWork(){
		semWrite = new Semaphore(1);
		
	}
	
	
	@SuppressWarnings("unchecked")
	public void test() {
		try {
			confRuns.setFileName("confRuns.xml");
			confDatasources.setFileName("confDatasources.xml");
			confMethods.setFileName("confMethods.xml");
			
			confRuns.load();
			confDatasources.load();
			confMethods.load();
			
			List<String> runs = confRuns.getList("run");
			log.info("Start of testing");
			//runs = new TestRun[confRuns.getInt("threads")];
			int numRuns = confRuns.getInt("threads");
			String resultsFile = "";
			semRuns = new Semaphore(numRuns);
			//Iterate through runs
			for (String run : runs) {
				int dbId=0;
				run = "runs." + run;
				// Iterate through dbs
				while (confRuns.getProperty(run + ".dbs.db(" + dbId + ").name") != null) {
					String dbName = confRuns.getString(run + ".dbs.db(" + dbId
							+ ").name");
					List<String> datasources = confRuns.getList(run
							+ ".dbs.db(" + dbId + ").datasources");
					int datasourceId = 0;
					// Iterate through datasources
					for (String datasourceName : datasources) {
						int methodId = 0;
						// Iterate through methods
						while (confRuns.getProperty(run + ".methods.method("
								+ methodId + ").name") != null) {
							String methodName = confRuns.getString(run
									+ ".methods.method(" + methodId + ").name");
							int testId = 0;
							// Iterate through tests
							while (confRuns.getProperty(run + ".tests.test("
									+ testId + ").class") != null) {
								synchronized (semRuns) {
									// Waiting for a free slot;
									semRuns.acquire();
									
									String testClass = confRuns.getString(run
											+ ".tests.test(" + testId + ").class");
									Constructor[] a = Class.forName(testClass)
											.getConstructors();
									log.info("Datasource " +datasourceName +", method "+methodName);
									System.gc();
									DataSource trainDataSource = getDataSource(datasourceName, dbName);
									trainDataSource.configDataSource(confRuns, run + ".dbs.db(" + dbId + ")", datasourceName/*.datasources("+datasourceId+")"*/);
									//DataSource testDataSource = getDataSource(datasourceName, dbName);
									//testDataSource.configDataSource(confRuns, run + ".dbs.db(" + dbId + ")", datasourceName/*.datasources("+datasourceId+")"*/);
									
									//ExportAsTHSource.exportAsTHSource(trainDataSource, "C:\\data\\", trainDataSource.getName());
									//writeRecords(trainDataSource);
									//writeRecords(testDataSource);
									//DataSourceStatistics b = new DataSourceStatistics(trainDataSource.getName());
									//b.getStatistics(trainDataSource);
									//semRuns.release();
									Method method = getMethod(methodName);
									method.configClassifier(confRuns, run
											+ ".methods.method(" + methodId + ")");
	
									Test test = (Test) a[0].newInstance();
									test.configTest(confRuns, run + ".tests.test("
											+ testId + ")");
									//resultsFile = test.getResultsInterpreter().getFilePrefix()+".csv";
									
									log.info("Testing datasource " + trainDataSource.getName());
									// Making new thread for a new test.
									new TestRun(this, trainDataSource, method, test)
											.start();
									// Wait 1 second for avoid of the colision
									// in writing of results.
									Thread.sleep(1000);
								}
								testId++;
							}
							methodId++;
						}
						datasourceId++;
					}
					dbId++;
				}
			}
			//Waiting for all other threads to finish.
			for(int i=0;i<numRuns;i++)
				semRuns.acquire();
				
			log.info("End of testing");
			
			Process p = Runtime.getRuntime().exec(
					"cmd " 
					, null,null);
			p.getOutputStream().write(("loadResults.bat " + resultsFile+"\n").getBytes());
			p.getOutputStream().flush();
			p.getOutputStream().write("exit\n".getBytes());
			p.getOutputStream().flush();
			BufferedReader stdOut= new BufferedReader(new InputStreamReader(p
					.getInputStream()));
			BufferedReader stdErr= new BufferedReader(new InputStreamReader(p
					.getErrorStream()));
			while(true){
				try{
					p.exitValue();
					break;
				}catch (Exception e) {
					while(stdOut.ready()||stdErr.ready()){
					if(stdOut.ready())
						stdOut.readLine();
					else
						stdErr.readLine();
					}
				}
			}
			p.waitFor();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
	     PropertyConfigurator.configure("log4j.properties");
	     PrefWork cp = new PrefWork();
	     cp.test();
	}
	
	class TestRun extends Thread{		
		DataSource trainDataSource;
		DataSource testDataSource;	
		Method method ;
		Test test;
		PrefWork conf;
		public TestRun(PrefWork conf, DataSource trainDataSource,
		Method method ,
		Test test){
			this.trainDataSource = trainDataSource;
			this.method = method;
			this.test = test;
			this.conf = conf;
		}
		public void run(){
				test.test(method,  trainDataSource);
				semRuns.release();
		}
		
	}

	public class Semaphore {
		private int count;

		public Semaphore(int n) {
			this.count = n;
		}

		public synchronized void acquire() {
			while (count == 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					// keep trying
				}
			}
			count--;
		}

		public synchronized void release() {
			count++;
			notify(); // alert a thread that's blocking on this semaphore
		}
	}

}

