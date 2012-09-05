package prefwork.rating.method.rater;

import java.util.List;

import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;
import prefwork.rating.method.normalizer.Normalizer;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;



/**
 * <p>WeightRater is abstract class. Its subclasses are supposed to atribute to each column a certain constant weight. 
 * These weight are stored in values field.</p>
 * <p>Copyright (c) 2006</p>
 * @author Alan Eckhardt
 * @version 1.0
 */
public abstract class WeightRater implements Rater {
	// Weights of fields
	protected double weights[];
	
	/** 
	 * If the weights are used during variance computing. 
	 * The number of objects with the attribute value represents the weights. The more objects the higher weight the value gets when computing the variance over all the attribute.
	 */
	boolean useWeights = true;

	// Method of assigning weights
	protected String methodName = "VARIANCE";
	
	public WeightRater() {
	}

	public void init(double values[]) {
		this.weights = (double[]) values.clone();
	}

	public WeightRater(double values[]) {
		init( values);
	}


    /**
     * 
     * Estimates the similarity of the given rater. It is often reasonable to compare only the raters of the same class.
     * @param n Other rater to compare with.
     * @return The degree of similarity between 0 and 1.
     */
    public double compareTo(Rater n){

		if(!(n instanceof WeightRater))
			return 0;
		WeightRater n2 = (WeightRater)n;
		double dist = 0;
		int i = 3;
		double max=0;
		for (; i < weights.length && i < n2.weights.length; i++) {
			dist+=Math.abs(weights[i]-n2.weights[i]);
			if(weights[i]>max)
				max = weights[i];
			if(n2.weights[i]>max)
				max = n2.weights[i];
		}
		return 1-(dist/(i*max));
    }
	public double[] getWeights() {
		return weights;
	}

	public void setWeights(double[] weights) {
		this.weights = weights;
	}
	
	public double getVarianceNoWeight(ContentDataSource data, ContentBased method, int attributeIndex){
		double err = 0;
		Normalizer n = method.getNormalizers()[attributeIndex];
		if(n == null)
			return 0;
		int div = 0;
		data.restart();
		while(data.hasNext()){
			Rating r = (Rating) data.next();
			Double d = n.normalize(r);
			if(d == null)
				continue;
			err+=Math.abs(d-Utils.objectToDouble(r.getRating()));
			div++;
		}
		return err/div;
	}

	/*public double getVariance(ContentDataSource data, int attributeIndex){
		double err = 0;
		Normalizer n = data.getNormalizers()[attributeIndex];
		int div = 0;
		for( AttributeValue val : attr.getValues()){
			int numRecords = val.getRecords().size();
			for (Rating rec : val.getRecords()) {
				Double r = n.normalize(rec);
				if(r == null)
					continue;
				err+=numRecords*Math.abs(r-CommonUtils.objectToDouble(rec.get(2)));		
				div+=numRecords*val.getRecords().size();
			}
		}
		return err/div;
	}*/
	public void init(ContentDataSource data, ContentBased method) {
		weights = new double[data.getInstances().numAttributes()];
		if ("ALL_1".equals(methodName)) {
			for (int i = 0; i < weights.length; i++)
				weights[i] = 1;
		} else if ("VARIANCE".equals(methodName)) {			
			for (int i = 0; i < weights.length; i++){
				if(i == data.getClassAttributeIndex())
					continue;
				double var = 0;
				/*if(useWeights)
					var = getVariance(data, i);
				else*/
					var = getVarianceNoWeight(data, method, i);
				if ( var != 0)
					weights[i] = 1 / var;
				else
					weights[i] = -1.0;
			}
			//Filling unknown values with minimal weight
			double min = Double.MAX_VALUE;
			for (int i = 0; i < weights.length; i++){
				if ( min > weights[i] && weights[i] > 0)
					min = weights[i];
			}
			if(min == Double.MAX_VALUE)
				min = 1;

			for (int i = 0; i < weights.length; i++){
				if ( -1 == weights[i])
					weights[i] = min/2;
			}
			
		} else if ("INCREASING".equals(methodName)) {
			for (int i = 0; i < weights.length; i++)
				weights[i] = i*5 + 1;
		}

	}

	public boolean isUseWeights() {
		return useWeights;
	}

	public void setUseWeights(boolean useWeights) {
		this.useWeights = useWeights;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	
}
