package prefwork.rating.method;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.UserEval;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.rater.Rater;
import prefwork.rating.method.rater.WeightAverage;
import prefwork.rating.method.representant.Representant;

public class Statistical extends ContentBased {
	Long normalizeNumTime = 0L;
	Long normalizeNomTime = 0L;
	Long raterTime = 0L;
	public Statistical() {
		rater = new WeightAverage();
	}
	public Statistical clone(){
		Statistical st = new Statistical();
		/*if(this.attributes!= null){
			st.attributes = new Attribute[this.attributes.length];
			for (int i = 0; i < st.attributes.length; i++) {
				st.attributes[i]=this.attributes[i].clone();
			}
		}
		st.colorNorm = this.colorNorm.clone();

		// Normalizer for nominal attributes
		st.nominalNormName = this.nominalNormName;
		st.nominalNorm = this.nominalNorm.clone();

		// Normalizer for numerical attributes
		st.numericalNormName = this.numericalNormName;
		st.numericalNorm = this.numericalNorm.clone();

		// Normalizer for list attributes
		st.listNormName = this.listNormName;
		st.listNorm = this.listNorm.clone();

		// Normalizer for nominal attributes
		st.colorNormName = this.colorNormName;
		 st.colorNorm = this.colorNorm.clone();

		 st.reprName = this.reprName;
		 st.representant = this.representant;

		 st.raterName = this.raterName;
		 st.rater = this.rater;*/
		
		return st;
		
	}

	/**
	 * Method for initialization of Statistical method.
	 */
	private void clear() {
		if(attributes == null)
			return;
		/*for(Attribute attr:attributes){
			attr.setValues(null);
			attr.setVariance(0.0);
		}*/
	}

	/**
	 * Method builds the model for evaluation. For nominal attributes, it is
	 * based on statistical distribution of ratings of objects with given
	 * attribute values. For numerical, linear or other regression is used.<br />
	 * 
	 * @param trainingDataset
	 *            The dataset with training data.
	 * @param splitValue
	 *            User id.
	 */
	public int buildModel(DataSource ds, int userId) {
		clear();
		ContentDataSource trainingDataset = (ContentDataSource)ds;
		this.data = trainingDataset;
		this.attributes = trainingDataset.getInstances();
		trainingDataset.setFixedUserId(userId);
		trainingDataset.restart();
		if (!trainingDataset.hasNext())
			return 0;
		//loadAttributes(trainingDataset);

		int count = 0;
		/*while ((rec = (Rating)trainingDataset.next()) != null) {
			// record[0] - uzivatelID
			// record[1] - itemID
			// record[2] - rating
			for (int i = 0; i < attributes.numAttributes(); i++) {
				addAttributeValue(i, rec, rec.getRating());
			}
			count++;
		}*/
		computeLocalPreferences();

		rater.init(trainingDataset, this);
		/*
		for (int i = 0; i < attributes.numAttributes(); i++) {
			for (AttributeValue val : attributes[i].getValues()) {
				CommonUtils.cleanAttributeValue(val);
			}
		}*/
		// ((WeightRater) rater).setWeights(weights);
		return count;
	}

	/**
	 * Computes the rating of given object. It is done in two steps - first
	 * attribute values are normalized using local preferences. Then, local
	 * preferences are aggregated using Rater to overall score of object.
	 * 
	 * @param record
	 *            The record to be classified.
	 * @param targetAttribute
	 *            Index, where rating is stored.
	 */
	public Double classifyRecord(UserEval record) {
		if(!(record instanceof Rating))
			return Double.NaN;
		Rating r = (Rating)record;
		if(attributes == null)
			return 0.0;
		// Ratings are the preferences over attribute values.
 		Double[] ratings = new Double[attributes.numAttributes()];
		for (int i = 0; i < ratings.length; i++) {
			/*AttributeValue val = attributes[i].getValue(record.get(i));
			if (val == null
					&& (attributes[i] == null
							|| attributes[i].getValues() == null
							|| attributes[i].getNorm() == null ))
				ratings[i] = null;
			else{*/
				ratings[i] = getNormalizers()[i].normalize(r);
			
		}
		// Final aggregation
		Double res=rater.getRating(ratings, r);
		return res;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		super.configClassifier(config,section);

	}

	public Representant getRepresentant() {
		return representant;
	}

	public void setRepresentant(Representant representant) {
		this.representant = representant;
	}

	public Rater getRater() {
		return rater;
	}

	public void setRater(Rater rater) {
		this.rater = rater;
	}

	public String toString() {
		return "StatisticalOptimized" + rater.toString() + ","
				+ representant.toString() + "," +/*textNorm.toString()+ */numericalNorm.toString()
				+ "," + nominalNorm.toString()
				+ "," /*+ listNorm.toString()*/;
	}

}
