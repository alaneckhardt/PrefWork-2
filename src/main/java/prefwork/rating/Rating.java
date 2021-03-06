package prefwork.rating;

import java.io.Serializable;

import prefwork.core.UserEval;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Rating is a numerical evaluation of an object. Typically, the range is 1-5 (stars).
 * The return type of Method.classifyRecord is Double.
 * @author Alan Eckhardt
 *
 */
public class Rating implements UserEval, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6209857552600404810L;
	/**	Record containing attribute values and class value. */
	Instance record;
	/** Explicit storage of objectId. It is not contained in record.*/
	int objectId;
	/** Explicit storage of userId. It is not contained in record.*/
	int userId;
	Instances dataset;
	public Instances getDataset() {
		return dataset;
	}
	public void setDataset(Instances dataset) {
		this.dataset = dataset;
	}
	public Rating(Instances dataset){
		this.dataset = dataset;
	}
	public double get(int index){
		return record.value(index);
	}
	public Instance getRecord() {
		return record;
	}
	public void setRecord(Instance record) {
		this.record = record;
		if(record != null)
			this.record.setDataset(dataset);
	}
	public int getObjectId() {
		return objectId;
	}
	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}	
	public double getRating() {
		return record.classValue();
	}
	public void setRating(double rating) {
		record.setClassValue(rating);
	}
}
