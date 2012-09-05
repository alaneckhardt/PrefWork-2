package prefwork.rating.method.rater;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Method;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.datasource.THDataSource;
import prefwork.rating.method.ContentBased;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class MethodRater implements Rater {

	// Create a weka classifier
	ContentBased method = null;
	String methodName = "";
	Integer targetAttribute = 2;
	Instances attributes, newInstances;
	FastVector fvWekaAttributes;
	Instances isTrainingSet;
	int userId = -1;
	//double[] classes;
	public String toString(){		
		return "MethodRaterNoNull"+method.toString();
	}
	@SuppressWarnings("unchecked")
	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		try {
			methodName = Utils.getFromConfIfNotNull(methodConf, "method", methodName);
			Class c = Class.forName(methodName);
			Constructor[] a = c.getConstructors();
			method = (ContentBased) a[0].newInstance();
			method.configClassifier(config, section+".method");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public Double getRating(Double[] ratings, Rating record) {
		if(method == null)
			return null;
		Rating rec = new Rating(newInstances);
		rec.setObjectId(1);
		rec.setUserId(0);
		rec.setRecord(new SparseInstance(ratings.length));
		
		for (int i = 0; i < ratings.length; i++) {
			if(ratings[i] == null)
				//rec.getRecord().setMissing(i);
				rec.getRecord().setValue(i,0);
			else
				rec.getRecord().setValue(i,ratings[i]);
		}
		return (Double)method.classifyRecord(rec);			
	}

	/**
	 * Transforms existing data into [0,1]^N and return the transformed data.
	 * @param data
	 * @return Data transformed to [0,1]^N
	 */
	Rating[][] getUserRecords(ContentDataSource data, ContentBased method ) {
		Rating[][] userRecords;
		List<Rating> recs = Utils.getList();
		// this.classes = new double[];
		int i = 0;
		while (data.hasNext()) {
			Rating r = (Rating) data.next();

			if (userId == -1)
				userId = r.getUserId();
			
			Rating newR = new Rating(attributes);
			newR.setObjectId(r.getObjectId());
			newR.setUserId(r.getUserId());
			newR.setRecord(new SparseInstance(data.getInstances().numAttributes()));
			for (int j = 0; j < data.getInstances().numAttributes(); j++) {
				if(attributes.classIndex() == j)
					newR.getRecord().setValue(j, r.get(j));
				else
					newR.getRecord().setValue(j, method.getNormalizers()[j].normalize(r));
			}
			recs.add(newR);

			// this.classes[i]=Utils.objectToDouble(val.getValue());
			i++;
		}
		userRecords = new Rating[1][recs.size()];

		userRecords[0] = recs.toArray(userRecords[0]);
		return userRecords;
	}
	@Override	
	public void init(ContentDataSource data, ContentBased method) {
	//	this.method = method;
		this.attributes = data.getInstances();
		THDataSource source = new THDataSource();
		FastVector newAttributes = new FastVector(attributes.numAttributes());
		for (int i = 0; i < attributes.numAttributes(); i++) {
			newAttributes.addElement(new Attribute(attributes.attribute(i).name()));
		}		
		newInstances = new Instances("Inner",newAttributes,attributes.numInstances());
		newInstances.setClassIndex(attributes.classIndex());
		source.setInstances(newInstances);
		Rating[][] ratings = getUserRecords(data, method);
		source.setUserRecords(ratings);
		source.setFile("\\Test"+data.getName());
		source.loadClasses(ratings[0]);
		//source.setClasses(classes);
		this.method.buildModel(source, userId);
	}
	@Override
	public double compareTo(Rater n) {
		// TODO Auto-generated method stub
		return 0;
	}
}
