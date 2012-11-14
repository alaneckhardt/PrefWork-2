package prefwork.rating.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.rating.Rating;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * SQLDataSource provides connection to arbitrary SQL database. It contains
 * methods for querying for records and users. Every subclass must instantiate
 * provider in its constructor with a database provider.
 * 
 * @author Alan
 * 
 */
public abstract class SQLDataSource extends ContentDataSource {
	// Provider of connection to SQL datase
	protected SQLConnectionProvider provider;

	// Name of dataset, for statistics
	protected String name;

	// Index of attribute that contains rating
	protected int targetAttribute;

	// SQL select for users
	protected String usersSelect = null;

	// SQL select for records with ratings
	protected String recordsSelect = null;

	// Condition on random column
	protected String betweenCondition = null;

	// Name of random column
	protected String randomColumn = null;

	// Function used for generating random values. It is database dependent.
	protected String randomFunction = "rand()";

	// Name of table that contains records and ratings
	protected String recordsTable = null;

	// Name of column with user ids
	protected String userColumn;
	protected String objectColumn;

	// Current user id
	protected Integer userID;
	int usersToLoad;

	// Name of table that contains random values (needed for update command)
	protected String randomColumnTable = null;

	// Attributes to fetch from db
	protected Instances instances;
	protected Attribute[] attributes;

	// Statement for records with ratings
	protected PreparedStatement recordsStatement;

	// Set of records with ratings
	protected ResultSet records;

	// Statement for users
	protected PreparedStatement usersStatement;

	// Set of users
	protected ResultSet users;
	
	// Count of possible classes
	protected double[] classes = null;


	public boolean hasNext() {
		try {
			// Try if we are at the end.
			records.getObject(1);
			return true;
		} catch (SQLException e) {
			// We are at the end of cursor, so we close it.
			try {
				records.close();
				if (recordsStatement != null)
					recordsStatement.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public void configDataSource(XMLConfiguration config, String section, String dataSourceName) {

		Configuration dbConf = config.configurationAt(section);
		provider.setHost(Utils.getFromConfIfNotNull(dbConf, "host", provider.getHost()));
		provider.setDb(Utils.getFromConfIfNotNull(dbConf, "db", provider.getDb()));
		provider.setPassword(Utils.getFromConfIfNotNull(dbConf, "password", provider.getPassword()));
		provider.setUserName(Utils.getFromConfIfNotNull(dbConf, "userName", provider.getUserName()));
		provider.setUrl(Utils.getFromConfIfNotNull(dbConf, "url", provider.getUrl()));
		try {
			provider.connect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Surround with try catch to avoid exception in confRuns, where this doen't have to be present
		try{
			Configuration dsConf = config.configurationAt(section+".datasources."+dataSourceName);
			ArrayList<Attribute> attrs = new ArrayList<Attribute>();
			if(dsConf.containsKey("attributes.attribute(" + 0 + ").name")){
				userColumn = (String) dsConf.getProperty("attributes.attribute(0).name");
				objectColumn = (String) dsConf.getProperty("attributes.attribute(1).name");
				int attrId = 2;
				// Iterate through attributes
				while (dsConf.getProperty("attributes.attribute(" + attrId + ").name") != null) {
					String attrName = dsConf.getString("attributes.attribute(" + attrId
							+ ").name");
					String attrType = dsConf.getString("attributes.attribute(" + attrId
							+ ").type");
					Attribute attr = null;
					if ("numerical".equals(attrType)) {
						attr = new Attribute(attrName,attrId);
					} else if ("nominal".equals(attrType)) {
						attr = new Attribute(attrName, (ArrayList<String>) null, attrId);
					} else if ("list".equals(attrType)) {	
						ArrayList<Attribute> list = new ArrayList<Attribute>();
						list.add(new Attribute("list",(ArrayList<String>)null));
						attr = new Attribute(attrName,  new Instances("list"+attrId, list,0), attrId);			
					}
					attrs.add(attr);
					attrId++;
				}
				attributes = new Attribute[attrs.size()];
				for (int i = 0; i < attributes.length; i++) {
					attributes[i] = (Attribute) attrs.get(i);
				}
				instances = new Instances("name", attrs,0);
				instances.setClassIndex(0);
			}
			targetAttribute = Utils.getIntFromConfIfNotNull(dsConf,"targetAttribute", targetAttribute);
			recordsTable = Utils.getFromConfIfNotNull(dsConf, "recordsTable", recordsTable);
			randomColumn = Utils.getFromConfIfNotNull(dsConf, "randomColumn", randomColumn);
			randomColumnTable = Utils.getFromConfIfNotNull(dsConf, "randomColumnTable", randomColumnTable);
			
			if(dsConf.containsKey("classes")){
				List classesTemp = dsConf.getList("classes");
				classes = new double[classesTemp.size()]; 
				for (int i = 0; i < classesTemp.size(); i++) {
					classes[i]=Utils.objectToDouble(classesTemp.get(i));
				}
			}
			if(dsConf.containsKey("usersSelect")){
				usersSelect = Arrays
				.deepToString(dsConf.getStringArray("usersSelect"))
				.substring(
						1,
						Arrays.deepToString(
								dsConf.getStringArray("usersSelect")).length() - 1);
			}
		}
		catch(Exception e){}
	}

	public void shuffleInstances() {
		String updateCommand = "update " + randomColumnTable + " set "
				+ randomColumn + "= " + randomFunction;
		try {
			PreparedStatement stat = provider.getConn().prepareStatement(
					updateCommand);
			stat.execute();
			provider.getConn().commit();
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String[] getAttributesNames() {
		String[] names = new String[instances.numAttributes()];
		for (int i = 0; i < names.length; i++)
			names[i] = instances.attribute(i).name();
		return names;
	}

	public Instances getInstances() {
		return instances;
	}


	public void setAttributes(Attribute[] attributes) {
		this.attributes = attributes;
	}

	@SuppressWarnings("unchecked")
	public double[] getClasses() {
		if(classes != null)
			return classes;
		/*return new double[]{5,4,3,2,1};*/
		String countSelect = "SELECT distinct "
				+ getAttributesNames()[targetAttribute] + " FROM "
				+ recordsTable;
		PreparedStatement stmt;
		try {
			stmt = provider.getConn().prepareStatement(countSelect);
			ResultSet rs = stmt.executeQuery();
			List classesList = Utils.getList();
			while(rs.next()){
				classesList.add(rs.getObject(1));
			}
			classes = new double[classesList.size()];
			for(int i=0;i<classesList.size();i++){
				Object o = classesList.get(i);
				if(o instanceof Double)
					classes[i]=(Double)o;				
				else
					classes[i]=Utils.objectToDouble(o);
									
			}
			Arrays.sort(classes);
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return classes;
	}

	public Rating next() {
		if (!hasNext())
			return null;

		Rating l = new Rating(instances);
		try {

			//Skipping UserId and ObjectId
			SparseInstance d = new SparseInstance(attributes.length);
			d.setDataset(this.instances);
			l.setUserId(Utils.objectToInteger(records.getObject(1)));
			l.setObjectId(Utils.objectToInteger(records.getObject(2)));
			for (int i = 0; i < attributes.length; i++){
				if(attributes[i].isNominal() || attributes[i].isString())
					d.setValue(i, records.getObject(i+3).toString());
				else
					d.setValue(i, Utils.objectToDouble(records.getObject(i+3)));
			}
			l.setRecord(d);
			records.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return l;
	}

	public int getTargetAttribute() {
		return targetAttribute;
	}

	public Integer userId() {
		try {
			if (users.next() == false)
				return null;

			int userId = users.getInt(1);
			return userId;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				usersStatement.close();
				// We are at the end of users cursor, so we close it.
				users.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return 0;
		}
	}

	public void restartUserId() {
		try {
			clearUsers();
			usersStatement = provider.getConn().prepareStatement(usersSelect);
			users = usersStatement.executeQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	protected void clearUsers() {
		try{
			if (usersStatement != null)
				usersStatement.close();
			if (users != null)
				users.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void clearRecords() {
		for (Attribute attr : attributes) {
			/*attr.setValues(null);
			attr.setVariance(null);*/
		}
		try {
			if (records != null)
				records.close();
			if (recordsStatement != null)
				recordsStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected String getObjectQuery(String select){
		String recordsSelect = "SELECT "+ select + " FROM " + recordsTable;
		if (betweenCondition != null) {
			recordsSelect += " WHERE " + betweenCondition;
		}
		if (userID != null) {
			// We add where clause, if it is not present
			if (betweenCondition == null
					|| !recordsSelect.endsWith(betweenCondition))
				recordsSelect += " WHERE ";
			// AND if where is already there
			else
				recordsSelect += " AND ";

			recordsSelect += userColumn + " = " + userID;
		}
		return recordsSelect;
	}
	public void restart() {
		try {
			clearRecords();
			// Getting rid of "[" and "]"
			recordsSelect = getObjectQuery(userColumn + ", "+ objectColumn +", "
					+ Arrays.toString(getAttributesNames()).substring(1,
							Arrays.toString(getAttributesNames()).length() - 1));
			recordsStatement = provider.getConn().prepareStatement(
					recordsSelect);
			records = recordsStatement.executeQuery();
			records.next();
		} catch (Exception e) {
			System.err.println(recordsSelect);
			e.printStackTrace();
		}
	}
	

	@Override
	public int size() {
		String recordsSelect = getObjectQuery(" COUNT(*) as size ");
		try {
			PreparedStatement recordsStatement = provider.getConn().prepareStatement(
					recordsSelect);
			ResultSet records = recordsStatement.executeQuery();
			records.next();
			int size = records.getInt(1);
			return size;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public void setFixedUserId(Integer value) {
		userID = value;
	}
	


	public void setLimit(int from, int to, boolean fromRange) {
		betweenCondition = "(1 = 1)";
		String fromS = Double.toString(from*1.0/size());
		String toS = Double.toString(to*1.0/size());
		if (fromRange)
			betweenCondition = (randomColumn + " >= " + fromS + " and "
					+ randomColumn + " < " + toS);
		else
			betweenCondition = (randomColumn + " < " + fromS + " or "
					+ randomColumn + " >= " + toS);
		betweenCondition = " (" + betweenCondition + ") ";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRandomColumn() {
		return randomColumn;
	}

	public void setRandomColumn(String randomColumn) {
		this.randomColumn = randomColumn;
	}


}