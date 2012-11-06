package prefwork.rating.method;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.UserEval;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaBridge implements Method {

	// Create a naive bayes classifier
	String classifierName = "weka.classifiers.bayes.NaiveBayes";
	Classifier cModel = (Classifier) new weka.classifiers.bayes.NaiveBayes();

	boolean wantsNumericClass = true;

	boolean onlyNumericAttributes = false;

	boolean onlyNominalAttributes = false;
	
	boolean noMissing = false;

	protected Instances attributes;
	protected ContentDataSource data;
	
	/**Indexes of numerical attributes.*/
	List<Integer> numerical;
	/**Indexes of nominal attributes.*/
	List<Integer> nominal;
	List<Integer> indexes;
	Integer targetAttribute;

	FastVector fvWekaAttributes;

	Instances isTrainingSet;

	public String toString() {
		return classifierName+(wantsNumericClass?"0":"1");
	}

	/**
	 * Creates a Weka Instance from a Rating. We need to transform the attributes, if the method supports only numerical, etc.
	 * @param rec
	 * @return
	 */
	protected Instance getWekaInstance(Rating rec) {
		Instance iExample;
		iExample = (Instance) rec.getRecord().copy();
		iExample.setDataset(isTrainingSet);
		return iExample;
	}


	/**
	 * Creates a Weka Instance from a Rating. We need to transform the attributes, if the method supports only numerical, etc.
	 * @param rec
	 * @return
	 */
	protected Instance getInstanceFromIndexes(Rating rec) {
		Instance iExample = null;
		iExample = new Instance(indexes.size());

		
		for (int i = 0; i < indexes.size(); i++) {
			try {
				if(rec.getRecord().isMissing(indexes.get(i)))
						continue;
				if (numerical.contains(indexes.get(i)))
					iExample.setValue(
							(Attribute) fvWekaAttributes.elementAt(i),rec.get(indexes.get(i)));
				else
					iExample.setValue(
							(Attribute) fvWekaAttributes.elementAt(i), 
							rec.getRecord().stringValue(indexes.get(i)));
			} catch (IllegalArgumentException e) {
				// e.printStackTrace();
				if (noMissing) {
					iExample.setValue(
							(Attribute) fvWekaAttributes.elementAt(i),
							((Attribute) fvWekaAttributes.elementAt(i))
									.value(0));
				}

				//System.err.print("" + rec.get(i).toString() + "," + i + "\n");
			}
		}
		//Fill the class. Class is always numeric, for the time being.
		try {
			if (wantsNumericClass)
				iExample.setValue((Attribute) fvWekaAttributes
						.elementAt(targetAttribute), rec.get(
								indexes.get(targetAttribute)));
			else
				iExample.setValue((Attribute) fvWekaAttributes
						.elementAt(targetAttribute), rec.get(
								indexes.get(targetAttribute)));
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			if (noMissing) {
				iExample.setValue((Attribute) fvWekaAttributes
						.elementAt(targetAttribute),
						((Attribute) fvWekaAttributes
								.elementAt(targetAttribute)).value(0));
			}
			/*System.err.print(""
					+ rec.get(indexes.get(targetAttribute)).toString() + ","
					+ targetAttribute + "\n");*/
		}

		return iExample;
	}

	protected void processAttribute(Rating rec, FastVector vec, int i) {
		if(rec.getRecord().isMissing(indexes.get(i)))
			return;
		if (!vec.contains(Double.toString(rec.getRecord().value(indexes.get(i)))))
			vec.addElement(Double.toString(rec.getRecord().value(indexes.get(i))));
	}

	protected void getAttributes(DataSource ds, Integer user) {
		ContentDataSource trainingDataset = (ContentDataSource)ds;
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		int size = indexes.size();
		String[] attributeNames = trainingDataset.getAttributesNames();		
		FastVector[] vec = new FastVector[size];
		for (int i = 0; i < vec.length; i++) {
			vec[i] = new FastVector();
		}

		Rating rec;
		if (!onlyNumericAttributes) {
			while ((rec = (Rating)trainingDataset.next()) != null) {
				for (int i = 0; i < indexes.size(); i++) {
					if(( i == targetAttribute && !wantsNumericClass) || nominal.contains(indexes.get(i)))
						processAttribute(rec, vec[i], i);
				}
			}
		}

		fvWekaAttributes = new FastVector(size);
		for (int i = 0; i < size; i++) {
			// Add a nominal attribute
			if ((vec[i].size() > 0 && !onlyNumericAttributes) || onlyNominalAttributes)
				fvWekaAttributes.addElement(new Attribute(
						ProgolBridge.transform(attributeNames[indexes.get(i)]), vec[i], i));
			// Add a numerical attribute
			else if (!onlyNominalAttributes || ( i == targetAttribute && wantsNumericClass))
				// fvWekaAttributes.addElement(new Attribute(attributeNames[i],
				// new FastVector()));
				fvWekaAttributes.addElement(new Attribute(
						ProgolBridge.transform(attributeNames[indexes.get(i)]), i));
		}
	}

	protected void clear(){
		nominal = Utils.getList();
		numerical = Utils.getList();
		indexes = Utils.getList();
		fvWekaAttributes = null;
		isTrainingSet = null;
		try {
			Class c = Class.forName(classifierName);
			Constructor[] a = c.getConstructors();
			cModel = (Classifier) a[0].newInstance();		
		}catch (Exception e) {}
	}
	
	/**
	 * Initializes the attributes indexes. We need to know which are nominal and which are numerical.
	 * @param ds
	 */
	protected void init(DataSource ds) {
		ContentDataSource trainingDataset = (ContentDataSource)ds;
		Instances attrs = trainingDataset.getInstances();
		for (int i = 0; i < attrs.numAttributes(); i++) {
			Attribute attr = attrs.attribute(i);
			if (attr.isNumeric())
				numerical.add(i);
			else if (attr.isNominal() || attr.isString())
				nominal.add(i);
		}
		if (onlyNumericAttributes)
			indexes.addAll(numerical);
		else if (onlyNominalAttributes)
			indexes.addAll(nominal);

		if (onlyNumericAttributes
				&& !numerical.contains(trainingDataset.getClassAttributeIndex())) {
			indexes.add(trainingDataset.getClassAttributeIndex());
		} else if (onlyNominalAttributes
				&& !nominal.contains(trainingDataset.getClassAttributeIndex())) {
			indexes.add(trainingDataset.getClassAttributeIndex());
		}
		
		else {
			List<Integer> all = Utils.getList();
			all.addAll(numerical);
			all.addAll(nominal);
			indexes = all;
		}
		targetAttribute = indexes.indexOf(trainingDataset.getClassAttributeIndex());
		
	}

	public int buildModel(DataSource ds, int user) {clear();
		ContentDataSource trainingDataset = (ContentDataSource)ds;
		data = trainingDataset;
		attributes = trainingDataset.getInstances();
		init(ds);
		getAttributes(trainingDataset, user);
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		if (!trainingDataset.hasNext())
			return 0;
		
		int count = 0;
		// Create an empty training set
		isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
		// Set class index
		isTrainingSet.setClassIndex(targetAttribute);
		while(trainingDataset.hasNext()){
			Rating r = (Rating) trainingDataset.next();
			isTrainingSet.add(getWekaInstance(r));

			count++;
		}
		try {
			cModel.buildClassifier(isTrainingSet);
		} catch (Exception e) {
			e.printStackTrace();
			cModel = null;
		}
		return count;
	}

	public Double classifyRecord(UserEval record) {
		Rating rating=(Rating)record;
		try {
			//The model wasn't built, we return null.
			if(cModel == null)
				return null;
			Instance inst = getWekaInstance(rating);
			//double r = cModel.classifyInstance(inst);
			double[] fDistribution = cModel.distributionForInstance(inst);
			double res=0.0;
			for(int i=0;i<fDistribution.length;i++)
				if(wantsNumericClass)
					res+=fDistribution[i];
				else
					res+=Utils.objectToDouble(((Attribute) fvWekaAttributes.elementAt(this.targetAttribute)).value(i))*fDistribution[i];
			return res;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		try {
			classifierName = Utils.getFromConfIfNotNull(methodConf, "classifier", classifierName);
			Class c = Class.forName(classifierName);
			Constructor[] a = c.getConstructors();
			cModel = (Classifier) a[0].newInstance();		
		}catch (Exception e) {
			e.printStackTrace();
		}
		try {
		    wantsNumericClass = methodConf.getBoolean("wantsNumericClass", wantsNumericClass);
		}catch (Exception e) {}
		try {
			onlyNominalAttributes = methodConf.getBoolean("onlyNominalAttributes", onlyNominalAttributes);
		}catch (Exception e) {}
		try {
			onlyNumericAttributes = methodConf.getBoolean("onlyNumericAttributes", onlyNumericAttributes);
		}catch (Exception e) {}
	}

}
