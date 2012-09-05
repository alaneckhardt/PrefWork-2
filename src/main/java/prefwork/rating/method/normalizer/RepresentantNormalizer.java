package prefwork.rating.method.normalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;
import prefwork.rating.method.representant.AvgRepresentant;
import prefwork.rating.method.representant.Representant;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

public class RepresentantNormalizer implements Normalizer{


	public static final int SIMILARITY = 1;
	public static final int MEAN = 2;
	public static final int NULL = 3;
	
	Map<Object,Double> map = new HashMap<Object,Double>();
	Map<Object,Integer> mapCounts = new HashMap<Object,Integer>();
	int count = 0;
	SimilaritySearch sim = new SimilaritySearch();
	int useSim = SIMILARITY;
	Representant representant = new AvgRepresentant();
	int index;
	Double mean;
	public RepresentantNormalizer(){
	}
	
	public RepresentantNormalizer(Attribute attr){
		//sim.init(attr);
	}
	
	public Double normalize(Rating o) {
		if(o == null)
			return null;
		
		/*List<AttributeValue> values = attr.getValues();
		if(values == null){
			if(useSim == MEAN)
				return mean;
			else if(useSim == SIMILARITY)
				return sim.normalize(o);
			else if(useSim == NULL)
				return null;
			return null;
		}*/
		/*for(AttributeValue attrVal : values){
			if(attrVal.getValue().equals(o.get(index)))
				return attrVal.getRepresentant();
		}*/
		if(map.containsKey(o.getRecord().value(index))){
			return map.get(o.getRecord().value(index));
		}
		if(useSim == MEAN)
			return mean;
		else if(useSim == SIMILARITY)
			return sim.normalize(o);
		else if(useSim == NULL)
			return null;
		return null;
	}

	public int compare(Rating arg0, Rating arg1) {
		if(normalize(arg0)>normalize(arg1))
			return 1;
		else if(normalize(arg0)<normalize(arg1))
			return -1;
		else
			return 0;
	}

	@Override
	public void addValue(Rating r) {
		Object o = r.getRecord().value(index);
		double rating = r.getRating();
		if(!mapCounts.containsKey(o))
			mapCounts.put(o, new Integer(0));
		if(!map.containsKey(o))
			map.put(o, 0D);
		Integer c = mapCounts.get(o);
		Double sum = map.get(o);
		c++;
		sum+=rating;
		mapCounts.put(o, c);
		map.put(o,sum);
		mean+=rating;
		count++;
	}
	@Override
	public void process() {
		if(count != 0)
			mean/=count;
		for (Object key : map.keySet()) {
			Double r = map.get(key);
			Integer c = mapCounts.get(key);
			r /= c;
			map.put(key, r);
		}
		/*if(useSim == SIMILARITY)
			sim.init(attr);		*/
		
	}
	public void init(ContentDataSource data, ContentBased method, int attributeIndex) {
		index = attributeIndex;
		mean = 0.0;
	}
	

	public Normalizer clone(){
		RepresentantNormalizer representantNormalizer = new RepresentantNormalizer();
		representantNormalizer.representant = representant;
		representantNormalizer.useSim = useSim;
		return representantNormalizer;
	}
	
	public String toString(){
		return "B"+representant.toString()+useSim;
	}

	public Representant getRepresentant() {
		return representant;
	}

	public void setRepresentant(Representant representant) {
		this.representant = representant;
	}
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		if (methodConf.containsKey("representant.class")) {
			String normalizerName = methodConf.getString("representant.class");
			representant = (Representant) Utils.getInstance(normalizerName);
			representant.configClassifier(config, section+".representant");
		}
		else 
			representant = new AvgRepresentant();
		if (methodConf.containsKey("useSim")) {
			useSim = methodConf.getInt("useSim");
		}
		
	}

	@Override
	public double compareTo(Normalizer n) {

		if(!(n instanceof RepresentantNormalizer))
			return 0;
		RepresentantNormalizer n2 = (RepresentantNormalizer)n;
		double diff=0;
		int count =0;
	/*	List<AttributeValue> values = attr.getValues();
		List<AttributeValue> n2values = n2.attr.getValues();
		for (AttributeValue av1 : values) {
			for (AttributeValue av2 : n2values) {
				if(av1.getValue().equals(av2.getValue())){
					count++;
					diff+=Math.abs(av1.getRepresentant()-av2.getRepresentant());
				}
			}
		}*/
		if(count==0)
			return 0;
		return 1-diff/(5*count);
	}

}
