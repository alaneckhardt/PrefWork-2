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

public class Linear implements Normalizer {

	weka.classifiers.functions.LinearRegression lg = new LinearRegression();
	//weka.classifiers.functions.SMOreg lg = new weka.classifiers.functions.SMOreg();
	Instances isTrainingSet;
	FastVector fvWekaAttributes;
	Attribute attr;
	Instances representants;
	double[] coefficients;
	public double[] getCoefficients() {
		return coefficients;
	}
	public void setCoefficients(double[] coefficients) {
		this.coefficients=coefficients;
	}

	int numberOfClusters = 0;
	boolean varNumberOfClusters = false;
	int index;
	
	public String toString() {
		return "LCoef"+numberOfClusters+varNumberOfClusters/*+lg.getClass().getSimpleName()*/;
	}
	
	public Linear() {
	}

	public Linear(ContentDataSource data, ContentBased method, int attrIndex) {
		init(data, method, attrIndex);
	}

	protected void computeRepresentants() {
		SimpleKMeans cluster = new SimpleKMeans();
		try {
			if(numberOfClusters != 0){
				cluster.setNumClusters(numberOfClusters);
				isTrainingSet.setClassIndex(-1);
				cluster.buildClusterer(isTrainingSet);
				representants = cluster.getClusterCentroids();
				representants.setClassIndex(1);
				lg.buildClassifier(representants);
			}
			else
				lg.buildClassifier(isTrainingSet);
			coefficients = lg.coefficients();
			/*if(representants.numInstances()<5)
				lg.setSampleSize(3);*/
			
		} catch (weka.core.WekaException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Double normalize(Rating record) {
		/*return 0.0;
		Instance iExample = new Instance(2);
		iExample.setDataset(isTrainingSet);
		iExample.setValue((weka.core.Attribute) fvWekaAttributes
				.elementAt(0), CommonUtils.objectToDouble(record.get(index)));*/

		try {
			//return lg.classifyInstance(iExample);
			return coefficients[2]+coefficients[0]*Utils.objectToDouble(record.getRecord().value(index));
		} catch (Exception e) {
			return null;
		}/*CommonUtils.objectToDouble(record.get(index))*/
	}

	public void computeCoefs(List<Double> l, List<Double> ratings) {

	}

	public int compare(Rating arg0,Rating arg1) {
		if (normalize(arg0) > normalize(arg1))
			return 1;
		else if (normalize(arg0) < normalize(arg1))
			return -1;
		else
			return 0;
	}

	public void init(ContentDataSource data, ContentBased method, int attrIndex) {
		coefficients = null;
		lg = new LinearRegression();
		index = attrIndex;
		fvWekaAttributes = new FastVector(2);
		fvWekaAttributes.addElement(new weka.core.Attribute("X"));
		fvWekaAttributes.addElement(new weka.core.Attribute("Rating"));

		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		isTrainingSet.setClassIndex(1);
	}

	@Override
	public void addValue(Rating r) {
		Instance iExample = new SparseInstance(2);
		iExample.setDataset(isTrainingSet);
		iExample.setValue((weka.core.Attribute) fvWekaAttributes.elementAt(0), Utils.objectToDouble(r.getRecord().value(index)));
		iExample.setValue((weka.core.Attribute) fvWekaAttributes.elementAt(1), r.getRating());
		isTrainingSet.add(iExample);		
	}
	@Override
	public void process() {
		computeRepresentants();
	}
	
	
	public Normalizer clone() {
		Linear l = new Linear();
		l.numberOfClusters = numberOfClusters;
		l.varNumberOfClusters = varNumberOfClusters;
		return l;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		numberOfClusters = Utils.getIntFromConfIfNotNull(config.configurationAt(section), "numberOfClusters", numberOfClusters);
		varNumberOfClusters = Utils.getBooleanFromConfIfNotNull(config.configurationAt(section), "varNumberOfClusters", varNumberOfClusters);
	}

	@Override
	public double compareTo(Normalizer n) {
		if(n == null || !(n instanceof Linear))
			return 0;
		Linear n2 = (Linear)n;
		
		double dist = 0;
		int i = 0;
		if(coefficients == null || n2.coefficients == null)
			return 0;
		
		double max=Double.MIN_VALUE;
		for (; i < coefficients.length && i < n2.coefficients.length; i++) {
			dist += Math.abs(coefficients[i] - n2.coefficients[i]);
			if(Math.abs(coefficients[i])>max)
				max =  Math.abs(coefficients[i]);
			if(Math.abs(n2.coefficients[i])>max)
				max = Math.abs(n2.coefficients[i]);
		}
		return 1.0-(dist/(i*max));
	}

}
