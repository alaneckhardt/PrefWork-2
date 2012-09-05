package prefwork.rating.datasource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import prefwork.core.DataSource;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import weka.core.Attribute;
import weka.core.Instances;

/**
 * Datasource that computes ratings and adds them to the objects obtained by inner datasource.
 * The method of computing ratings is in the configuration confDataSources.xml
 * @author Alan
 *
 */
public class RatingNetFlix extends ContentDataSource {
	int ratingsCount = 200;
	int usersToLoad;
	public int getRatingsCount() {
		return ratingsCount;
	}


	public void setRatingsCount(int ratingsCount) {
		this.ratingsCount = ratingsCount;
	}


	public int getUsersToLoad() {
		return usersToLoad;
	}


	public void setUsersToLoad(int usersToLoad) {
		this.usersToLoad = usersToLoad;
	}
	private static Logger log = Logger.getLogger(RatingNetFlix.class);
	String basePath = "C:\\tmp\\";
	String lastFile = null;
	ContentDataSource innerDataSource;
	protected Instances innerAttributes;
	

	public String getName(){
		return "RatingNetFlix2"+(((IMDbMemory)innerDataSource).getName())+innerDataSource.getClass().getSimpleName()+usersToLoad+ratingsCount;
	}

	
	public void configDataSource(XMLConfiguration config, String section, String dataSourceName) {		
		//super.configDataSource(config, section);
		Configuration dbConf = config.configurationAt( section);
		ratingsCount = Utils.getIntFromConfIfNotNull(dbConf, "ratingsCount", ratingsCount);
		basePath = Utils.getFromConfIfNotNull(dbConf, "basePath", basePath);
		usersToLoad = Utils.getIntFromConfIfNotNull(dbConf, "usersToLoad", usersToLoad);

		
		String dbClass = dbConf.getString("innerclass");


		if(innerDataSource == null && dbClass != null){
		Constructor[] a;
			try {
				a = Class.forName(dbClass).getConstructors();
				innerDataSource = (ContentDataSource) a[0].newInstance();
				innerDataSource.configDataSource(config, section, dataSourceName);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			// Configure the inner datasource
			innerDataSource.configDataSource(config,  section, dataSourceName);
			//innerName = innerDataSource.getName();
			innerAttributes = innerDataSource.getInstances();
			innerDataSource.setName(innerDataSource.getName()+"inner");
		}
		if(lastFile == null || !lastFile.equals("FlixR"+getName())){
			loadData();
			lastFile = "FlixR"+getName();
		}
	}
	

	@SuppressWarnings("unchecked")
	protected void loadDataFromFile(String fileName){
		Long start;
			java.io.File f = new java.io.File(basePath+fileName+".ser");
			if(f.exists()){
				start = System.currentTimeMillis();

				userRecords = (Rating[][]) Utils.loadDataFromFile(basePath+fileName+".ser");
				log.info("casFile" + (System.currentTimeMillis()-start));
			}
			else{

				start = System.currentTimeMillis();
				loadDataFromDb();
				log.info("casDb" + (System.currentTimeMillis()-start));
				//log.debug("Loaded file:"+fileName);

				start = System.currentTimeMillis();
				/*for (int i = 0; i < userRecords.length; i++) {
					for (int j = 0; j < userRecords[i].length; j++) {
						//userRecords[i][j].setRecord(null);
						userRecords[i][j].getDataset().compactify();
					}
				}*/
				Utils.writeDataToFile(basePath+fileName+".ser", userRecords);
				log.info("casWriteFile" + (System.currentTimeMillis()-start));				
			}
			userCount = userRecords.length;
	}
	private void loadData() {
		currentUser = 0;		
		// Configure the inner datasource
		instances = innerDataSource.getInstances();
	
		loadDataFromFile("FlixR"+getName());
		//loadDataFromDb();
	}
	/**
	 * Copies data from innerDataSource to this.records
	 */
	@SuppressWarnings("unchecked")
	private void loadDataFromDb() {
		//Random r = new Random(seed);
		((IMDbMemory)innerDataSource).usersSelect="select userid from UserCounts where  count>="+ratingsCount+" and count<"+(ratingsCount+20)+"  and rownum <= "+usersToLoad+ " order by count";
		innerDataSource.restartUserId();
		Integer userIdDataset;
		//HashMap<Integer, List<Rating>> userRecords = new HashMap<Integer, List<Rating>>();
		List<Integer> userIds = Utils.getList(usersToLoad);
		this.userRecords = new Rating[usersToLoad][];
		userCount = 0;		
		while( (userIdDataset= innerDataSource.userId())!=null){
			userIds.add(userIdDataset);
			innerDataSource.setFixedUserId(userIdDataset);
			innerDataSource.restart();
			if (!innerDataSource.hasNext())
				continue;
			Rating rec2;
			int count = 0;
			this.userRecords[userCount] = new Rating[ratingsCount+10];
			while ((rec2 = (Rating) innerDataSource.next()) != null && count < ratingsCount) {
				//userRecords.get(userIdDataset).add(rec2);
				this.userRecords[userCount][count] = rec2;
				rec2.setUserId(userCount);
				count++;
			}
			int i = userRecords[userCount].length-1;
			for (; i >= 0; i--) {
				if(userRecords[userCount][i]!=null)
					break;
			}
			userRecords[userCount] = Arrays.copyOf(userRecords[userCount], i+1);
			if(count == 0){
				userCount--;
			}
			userCount++;
		}
		innerDataSource.restart();
	}

	
	/*
	public void fillRandomValues() {
		if(userRecords == null)
			return;
		
		RandomComparator comp = null;

		for (int i = 0; i < userRecords.length; i++) {
		//for(int userId : userRecords.keySet()){
			
			Random rand = new Random(i);
			
			for (Rating rec3 : userRecords[i]) {
				rec3.add(rand.nextDouble());
				if (comp == null)
					comp = new RandomComparator(rec3.size() - 1);
			}
			
			Arrays.sort(userRecords[i], comp);			
			// Removing the random number at the end of the record
			for (int j = 0; j < userRecords[i].length; j++) {
				Rating rec3 = userRecords[i][j];
				//Removing the random column
				rec3.remove(rec3.size() - 1);
				//Setting new objectid
				//rec3.set(1, j);
				
			}
		}
	}

	public Integer getUserId() {
		if(currentUser>=userRecords.length)
			return null;
		currentUser = currentUser;
		currentUser++;
		return currentUser;
	}
	
	public boolean hasNextRecord() {
		if(userRecords[currentUser] == null)
			return false;
		if(objectIndex>=size())
			return false;
		if(trainSetSize == 0)
			return true;
		//Test mode
		if(testMode && (objectIndex<run*trainSetSize || objectIndex>=(run+1)*trainSetSize))
			return true;
		//Train mode
		if(!testMode && objectIndex>=run*trainSetSize && objectIndex<(run+1)*trainSetSize)
			return true;
		return false;
	}

	public void restart() {
		if(testMode && run == 0)
			objectIndex = (run+1)*trainSetSize;
		else if(testMode)
			objectIndex = 0;
		else
			objectIndex = run*trainSetSize;
	}*/
	
	/*public Rating getRecord() {
		if (!hasNext())
			return null;
		Rating record = userRecords[currentUser][objectIndex];
		//In test mode, skip the object used in training phase
		if(testMode && objectIndex+1==run*trainSetSize){
			objectIndex = (run+1)*trainSetSize;
		}
		else{
			objectIndex++;
		}
		return record;
	}*/




}
