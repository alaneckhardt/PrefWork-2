package prefwork.rating.method.normalizer;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;

import weka.core.Attribute;
import weka.core.Instances;

/**
 * <p> Normalizer transforms given object into a number from [0,1]. </p> <p>
 * Copyright (c) 2006 </p>
 * @author Alan Eckhardt
 * @version 1.0
 */
public interface Normalizer extends Comparator<Rating>, Cloneable {
	/**
	 * Normalizes attribute value at the index obtained in init().
	 * @param record The record, which contains value to be normalized. Value is taken from index that is obtained in method init()
	 * @return The number between [0,1].
	 */
	public Double normalize(Rating record);

	/**
	 * Initializes Normalizer with given attribute.
	 * @param attribute
	 */
	public void init(ContentDataSource data, ContentBased method, int attributeIndex);

	public void addValue(Rating r);
	public void process();

	/**
	 * Method for cloning Normalizer. Used for easier configuration.
	 * @return Instance of Normalizer.
	 */
	public Normalizer clone();
	

	/**
	 * Configures Normalizer.
	 * @param config Configuration
	 * @param section Section, in which is the configuration for current normalizer.
	 */
    public void configClassifier(XMLConfiguration config, String section);
    
    /**
     * 
     * Estimates the similarity of the given normalizer. It is often reasonable to compare only the normalizers of the same class.
     * @param n Other normalizer to compare with.
     * @return The degree of similarity between 0 and 1.
     */
    public double compareTo(Normalizer n);
    
}
