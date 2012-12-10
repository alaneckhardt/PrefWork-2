package prefwork.rating.method;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Method;
import prefwork.core.Utils;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.normalizer.Normalizer;
import prefwork.rating.method.rater.Rater;
import prefwork.rating.method.representant.AvgRepresentant;
import prefwork.rating.method.representant.Representant;

public abstract class ContentBased implements Method {

	protected Normalizer[] normalizers;
	
	protected Integer userId;

	// Attributes, containing all attribute values.
	protected Instances attributes;
	protected ContentDataSource data;
	// Normalizer for nominal attributes
	String nominalNormName = "prefwork.rating.method.normalizer.RepresentantNormalizer";
	
	Normalizer nominalNorm;

	// Normalizer for numerical attributes
	String numericalNormName = "prefwork.rating.method.normalizer.Linear";
	Normalizer numericalNorm;

	// Normalizer for list attributes
	String listNormName = "prefwork.rating.method.normalizer.ListNormalizer";
	Normalizer listNorm;
	/*
	// Normalizer for nominal attributes
	String colorNormName = "prefwork.rating.method.normalizer.ColorNormalizer";
	Normalizer colorNorm;
	
	// Normalizer for text attributes
	String textNormName = "prefwork.rating.method.normalizer.TextNormalizer";
	Normalizer textNorm;*/
	

	String reprName = "prefwork.rating.method.representant.AvgRepresentant";
	prefwork.rating.method.representant.Representant representant = new prefwork.rating.method.representant.AvgRepresentant();

	String raterName = "prefwork.rating.method.rater.WeightAverage";
	Rater rater;

	public ContentBased(){

		//Testing null values

		if (numericalNorm == null)
			numericalNorm = (Normalizer) Utils
					.getInstance(numericalNormName);

		if (nominalNorm == null)
			nominalNorm = (Normalizer) Utils
					.getInstance(nominalNormName);

		if (listNorm == null)
			listNorm = (Normalizer) Utils.getInstance(listNormName);
		/*
		if (colorNorm == null)
			colorNorm = (Normalizer) Utils.getInstance(colorNormName);

		if (textNorm == null)
			textNorm = (Normalizer) Utils.getInstance(textNormName);
*/
		if (representant == null)
			representant = (Representant) Utils.getInstance(reprName);

		if (rater == null)
			rater = (Rater) Utils.getInstance(raterName);

	}
	/**
	 * Computes the local preferences. For each attribute, one normalizer is
	 * constructed. Then the variance is computed - it may be used by rater as
	 * weights.
	 */
	protected void computeLocalPreferences() {
		Normalizer[] norms = new Normalizer[attributes.numAttributes()];
		setNormalizers(norms);
		for (int i = 0; i < attributes.numAttributes(); i++) {
			Attribute attr = attributes.attribute(i);
			Normalizer norm = null;
			if (attr.isString()) {
				norm = nominalNorm.clone();
			} else if (attr.isNumeric()) {
				norm = numericalNorm.clone();
			} else if (attr.isRelationValued()) {
				norm = listNorm.clone();
			} /*else if (attr.isString()) {
				norm = textNorm.clone();
			}*/
			//computeVariances(attr);
			norms[i]=norm;			
			norm.init(data, this, i);
		}
		data.restart();
		while(data.hasNext()){
			Rating r = (Rating) data.next();
			for (int i = 0; i < r.getRecord().numAttributes(); i++) {
				norms[i].addValue(r);
			}
			
		}
		for (int i = 0; i < attributes.numAttributes(); i++) {
			norms[i].process();
		}
		data.restart();
	}

	/**
	 * Adds attribute value to this.attributes. If the value is already present,
	 * only the rating is added.
	 * 
	 * @param attribute
	 *            Index of attribute.
	 * @param value
	 *            Value to be inserted.
	 * @param rating
	 *            Rating of that value.
	 */
	/*protected void addAttributeValue(int attribute, Rating record,
			Double rating) {
		// TODO tohle s nullem chce vymyslet lepe. Do it better.
		if (record.get(attribute) == null) {
			return;
		}
		for (Attribute attr : attributes) {
			if (attr.getIndex() == attribute) {
				AttributeValue attrValue = attr.getValue(record.get(attribute));
				if (attrValue == null) {
					attrValue = new AttributeValue(attr, record.get(attribute));
					attr.addValue(attrValue);
				}
				attrValue.addRating(rating);
				attrValue.addRecord(record);
			}
		}
	}*/

	/**
	 * Initializes this.attributes with names and indexes.
	 * 
	 * @param trainingDataset
	 *            The dataset the attribute names are taken from.
	 */
	//protected void loadAttributes(BasicDataSource trainingDataset) {
		/*
		 * String[] attributeNames = trainingDataset.getAttributesNames();
		 * attributes = new Attribute[attributeNames.length]; for (int i = 0; i
		 * < attributeNames.length; i++) { attributes[i] = new Attribute();
		 * attributes[i].setName(attributeNames[i]); attributes[i].setIndex(i);
		 * }
		 */

		  /*  Attribute[] dAttr = trainingDataset.getAttributes();
			attributes = new Attribute[dAttr.length];
			for (int i = 0; i < attributes.length; i++) {
				attributes[i]=dAttr[i].clone();
			}
		
	}*/

	/**
	 * The weights of attributes are computed based on the variance of the
	 * attribute values ratings.
	 * 
	 * @return The weights of attributes.
	 */
	protected double[] getWeights() {
		double[] weights = new double[attributes.numAttributes()];
		for (int i = 0; i < weights.length; i++)
			if (attributes.attribute(i) != null && attributes.attribute(i).weight() != -1)
				weights[i] = attributes.attribute(i).weight();
			else
				weights[i] = 1.0;
		return weights;
	}

	/**
	 * Computes the variance of ratings of given attribute value.
	 * 
	 * @param attrValue
	 *            Value, for which the variance is computed.
	 */
	/*protected void computeVariance(AttributeValue attrValue) {

		List<Double> ratings = attrValue.getRatings();
		double avg = 0;
		// Computing the average rating
		for (Double r : ratings)
			avg += r;
		avg /= ratings.size();
		double diff = 0;
		// Computing the variance from average rating
		for (Double r : ratings)
			diff += Math.abs(avg - r);
		attrValue.setVariance(diff / ratings.size());
	}*/

	/**
	 * Compute the overall variance of given attribute.
	 * 
	 * @param attr
	 *            Attribute, for which the variance is computed.
	 */
	/*protected void computeVariances(Attribute attr) {
		double var = 0;
		for (AttributeValue val : attr.getValues()) {
			computeVariance(val);
			var += val.getVariance();
		}
		attr.setVariance(var / attr.getValues().size());
	}*/

	/**
	 * Configures the methods normalizers and rater
	 */
	public void configClassifier(XMLConfiguration config, String section) {

		try {
			Configuration methodConf = config.configurationAt(section);
			// Setting all normalizers
			if (methodConf.containsKey("numericalNormalizer")) {
				numericalNormName = methodConf.getString("numericalNormalizer");
				numericalNorm = (Normalizer) Utils
						.getInstance(numericalNormName);
				numericalNorm.configClassifier(config, section
						+ ".numericalNormalizer");
			}

			if (methodConf.containsKey("nominalNormalizer")) {
				nominalNormName = methodConf.getString("nominalNormalizer");
				nominalNorm = (Normalizer) Utils
						.getInstance(nominalNormName);
				nominalNorm.configClassifier(config, section
						+ ".nominalNormalizer");
			}

			if (methodConf.containsKey("listNormalizer")) {
				listNormName = methodConf.getString("listNormalizer");
				listNorm = (Normalizer) Utils
						.getInstance(listNormName);
				listNorm.configClassifier(config, section
						+ ".listNormalizer");
			}
		

			if (methodConf.containsKey("representant.class")) {
				// Setting the representant
				reprName = methodConf.getString("representant.class");
				representant = (Representant) Utils.getInstance(reprName);
				representant
						.configClassifier(config, section + ".representant");
			}

			if (methodConf.containsKey("rater.class")) {
				// Setting the rater
				raterName = Utils.getFromConfIfNotNull(methodConf, "rater.class", raterName);
				//if (rater == null)
					rater = (Rater) Utils.getInstance(raterName);
				rater.configClassifier(config, section + ".rater");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Normalizer getNominalNorm() {
		return nominalNorm;
	}

	public void setNominalNorm(Normalizer nominalNorm) {
		this.nominalNorm = nominalNorm;
	}

	public Normalizer getNumericalNorm() {
		return numericalNorm;
	}

	public void setNumericalNorm(Normalizer numericalNorm) {
		this.numericalNorm = numericalNorm;
	}

	/*public Normalizer getListNorm() {
		return listNorm;
	}

	public void setListNorm(Normalizer listNorm) {
		this.listNorm = listNorm;
	}

	public Normalizer getColorNorm() {
		return colorNorm;
	}

	public void setColorNorm(Normalizer colorNorm) {
		this.colorNorm = colorNorm;
	}*/
/*
	public Attribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(Attribute[] attributes) {
		this.attributes = attributes;
	}*/

	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Normalizer[] getNormalizers() {
		return normalizers;
	}
	public void setNormalizers(Normalizer[] normalizers) {
		this.normalizers = normalizers;
	}
}
