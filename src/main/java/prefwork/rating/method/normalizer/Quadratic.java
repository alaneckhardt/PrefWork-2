package prefwork.rating.method.normalizer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;

import weka.classifiers.functions.LinearRegression;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class Quadratic extends Linear{


	/*weka.classifiers.functions.LinearRegression lg = new LinearRegression();
	Instances isTrainingSet;
	FastVector fvWekaAttributes;
	Attribute attr;
	int numberOfClusters = 0;
	int index;
	Instances representants;*/
	

	public Quadratic(){
	}
	
	public Quadratic(ContentDataSource data, ContentBased method, int attrIndex) {
		init(data, method, attrIndex);
	}
	protected void computeRepresentants(){
		SimpleKMeans cluster = new SimpleKMeans();
		try {
			if(numberOfClusters != 0){
				cluster.setNumClusters(numberOfClusters);
				cluster.buildClusterer(isTrainingSet);
				representants = cluster.getClusterCentroids();
				representants.setClassIndex(2);
				lg.buildClassifier(representants);
			}
			else
				lg.buildClassifier(isTrainingSet);

			coefficients = lg.coefficients();
			if(	coefficients[1] != 0.0 ){
//				System.out.println("X^2="+coefficients[1]+",X="+coefficients[0]+",C="+coefficients[3]);
			}
		} catch (weka.core.WekaException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public Double normalize(List<Object> record) {
		try {
			return coefficients[3]+coefficients[0]*Utils.objectToDouble(record.get(index))+
			coefficients[1]*Utils.objectToDouble(record.get(index))*Utils.objectToDouble(record.get(index));
		} catch (Exception e) {
			return 0.0;
		}
		/*
		Instance iExample = new Instance(3);
		iExample.setDataset(isTrainingSet);		
		iExample.setValue(
				(weka.core.Attribute) fvWekaAttributes.elementAt(0), 
				Double.parseDouble(o.get(index).toString()));

		iExample.setValue(
				(weka.core.Attribute) fvWekaAttributes.elementAt(1), 
				Math.pow(Double.parseDouble(o.get(index).toString()),2));
		
		double[] fDistribution;
		try {
			fDistribution = lg.distributionForInstance(iExample);		
		} catch (Exception e) {
			return 0.0;
		}
		double res=0.0;
		for(int i=0;i<fDistribution.length;i++)
			res+=fDistribution[i];
		return res;*/
	}
	

	public int compare(List<Object> arg0, List<Object> arg1) {
		if(normalize(arg0)>normalize(arg1))
			return 1;
		else if(normalize(arg0)<normalize(arg1))
			return -1;
		else
			return 0;
	}

	@Override
	public void addValue(Rating r) {
		Instance iExample = new SparseInstance(2);
		iExample.setDataset(isTrainingSet);
		iExample.setValue((weka.core.Attribute) fvWekaAttributes
				.elementAt(0), Utils.objectToDouble(r.getRecord().value(index)));
		iExample.setValue((weka.core.Attribute) fvWekaAttributes
				.elementAt(1), Utils.objectToDouble(r.getRecord().value(index))*Utils.objectToDouble(r.getRecord().value(index)));
		iExample.setValue((weka.core.Attribute) fvWekaAttributes
				.elementAt(2), r.getRating());
		isTrainingSet.add(iExample);		
	}
	
	public void init(ContentDataSource data, ContentBased method, int attrIndex) {
		coefficients = null;
		lg = new LinearRegression();
		index = attrIndex;
		fvWekaAttributes = new FastVector(3);
		fvWekaAttributes.addElement(new weka.core.Attribute("X"));
		fvWekaAttributes.addElement(new weka.core.Attribute("X^2"));
		fvWekaAttributes.addElement(new weka.core.Attribute("Rating"));
		
		
		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet.setClassIndex(2);			
	}

	@Override
	public void process() {
		computeRepresentants();
	}

	public String toString(){
		return "Quad";
	}

	public Normalizer clone() {
		return new Quadratic();
	}

	public void configClassifier(XMLConfiguration config, String section) {
		
	}
}
