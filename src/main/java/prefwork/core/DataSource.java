package prefwork.core;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;


public interface DataSource extends java.util.Iterator<UserEval>{
	public void shuffleInstances();
	public int size();
    /**
     * Returns only object inside (or outside) the given interval. 
     * The numbers are interpreted as position in the internal list of record, not the object ids.
     * @param from start of the interval
     * @param to end of the interval
     * @param recordsFromRange whether to take the objects from inside or the outside of the interval
     */
	public void setLimit(int from,int to,boolean recordsFromRange);   
	
	public boolean hasNext();
	public UserEval next();
	public void restart();
	public void remove();
	
	public void setFixedUserId(Integer userId);	
	public Integer userId();	
	public boolean hasNextUserId();
	public void restartUserId();	
	        
	public void configDataSource(XMLConfiguration config, String section, String dataSourceName);
	
	public String getName();
	public void setName(String name);
}
