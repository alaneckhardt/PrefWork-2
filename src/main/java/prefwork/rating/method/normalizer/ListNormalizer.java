package prefwork.rating.method.normalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;
import prefwork.rating.method.representant.AvgRepresentant;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;


public class ListNormalizer implements Normalizer{

	Normalizer repr = new RepresentantNormalizer();	
	/**Map of object of list and the rating of the list.**/
	HashMap<Object,Double> mapOfRatings = new HashMap<Object,Double>();
	int index;

	HashMap<Object,List<Double>> listOfRatings = new HashMap<Object,List<Double>>();
	
	
	public ListNormalizer(){	
	}
	

	private Double average(List<Double> list){
		double sum = 0, count = 0;
		for(Double r : list){
			if(r == null)
				continue;
			count ++;
			sum += r;			
		}
		if(count == 0)
			return null;
		return sum/count;
	}
	
	/**
	 * Finds all list containing the specified value
	 * @param val Value to search for
	 * @return List of all lists that contain val.
	 */
	/*@SuppressWarnings({ "unchecked", "unused" })
	private List<AttributeValue> findValues(Object val){
		if(val == null)
			return null;
		List<AttributeValue> l = CommonUtils.getList(3);		
		for(AttributeValue tempVal:attr.getValues()){
			for(Object o :(List<Object>)tempVal.getValue()){
				if(o == null)
					return null;
				if(o.equals(val))
					l.add(tempVal);
			}
		}
		return l;
	}*/
	
	@SuppressWarnings("unchecked")
	public Double normalize(Rating r) {
		Instances l = r.getRecord().relationalValue(index);
		if(l == null || l.numInstances() == 0)
			return null;
		double rating = 0;
		int count = 0;
		for(int i=0; i< l.numInstances();i++){
			Object val = l.instance(i);
			Double d = mapOfRatings.get(val);
			if(d == null)
				continue;
			rating+=d;
			count++;
		}
		if(count == 0)
			return 0.0;
		return rating/count;		
	}

	public void addValue(Rating r){
		Instances l = r.getRecord().relationalValue(index);
		for (int i = 0; i < l.numInstances(); i++) {
			Instance inst = l.instance(i);
			Object value = inst.value(0);
			if(!listOfRatings.containsKey(value)){
				listOfRatings.put(value, new ArrayList());
			}
			listOfRatings.get(value).add(r.getRating());
		}
	}
	public void process(){
		for (Object o : listOfRatings.keySet()) {
			this.mapOfRatings.put(o, average(listOfRatings.get(o)));
		}
	}
	
	@SuppressWarnings("unchecked")
	public void init(ContentDataSource data, ContentBased method, int attributeIndex) {
		//virtualAttr = new Attribute();
		//this.attr = data.getInstances().attribute(attributeIndex);		
		index = attributeIndex;
	}

	public Normalizer clone() {
		ListNormalizer l = new ListNormalizer();
		l.repr = this.repr.clone();
		return l;
	}

	public int compare(Rating arg0, Rating arg1) {
		if(normalize(arg0)>normalize(arg1))
			return 1;
		else if(normalize(arg0)<normalize(arg1))
			return -1;
		else
			return 0;
	}
	public String toString(){
		return "L"+repr.toString();
	}

	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		if (methodConf.containsKey("normalizer")) {
			String normalizerName = methodConf.getString("normalizer");
			repr = (Normalizer) Utils.getInstance(normalizerName);
			repr.configClassifier(config, section+".normalizer");
		}
		else {
			repr = new RepresentantNormalizer();
			((RepresentantNormalizer)repr).representant =  new AvgRepresentant();
		}
	}

	@Override
	public double compareTo(Normalizer n) {
		if(!(n instanceof ListNormalizer))
			return 0;
		ListNormalizer n2 = (ListNormalizer)n;
		Set<Object> values = mapOfRatings.keySet();
		Set<Object> n2values =  n2.mapOfRatings.keySet();
		double diff=0;
		int count =0;
		for (Object av1 : values) {
			if(n2values.contains(av1)){
				count++;
				diff+=Math.abs(mapOfRatings.get(av1)-n2.mapOfRatings.get(av1));				
			}
		}
		if(count==0)
			return 0;
		return 1-diff/(5*count);
	}


}
