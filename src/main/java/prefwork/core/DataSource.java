package prefwork.core;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;


public interface DataSource extends java.util.Iterator<UserEval>{
	/**
	 * Shuffle the instances. Used for bootstraping.
	 */
	public void shuffleInstances();
	/**
	 * 
	 * @return size of the dataset for the current user.
	 */
	public int size();
    /**
     * Returns only object inside (or outside) the given interval. 
     * The numbers are interpreted as position in the internal list of record, not the object ids.
     * @param from start of the interval
     * @param to end of the interval
     * @param recordsFromRange whether to take the objects from inside or the outside of the interval
     */
	public void setLimit(int from,int to,boolean recordsFromRange);   
	
	/**
	 * @return true, if there is another object for current user.
	 */
	public boolean hasNext();
	/**
	 *  @return next object for current user.
	 */
	public UserEval next();
	/**
	 *  Restarts the iterator for current user.
	 */
	public void restart();
	/**
	 * From Iterator. Not used.
	 */
	public void remove();
	/**
	 * Set current user.
	 * @param userId
	 */
	public void setFixedUserId(Integer userId);	
	/**
	 * @return Next user id.
	 */
	public Integer userId();
	/**
	 * @return True if there is another user in the dataset.
	 */
	public boolean hasNextUserId();
	/***
	 * Restarts the iterator for users.
	 */
	public void restartUserId();	
	        
	/**
	 * Configuration method.
	 * @param config
	 * @param section
	 * @param dataSourceName
	 */
	public void configDataSource(XMLConfiguration config, String section, String dataSourceName);

	public String getName();
	public void setName(String name);
}
