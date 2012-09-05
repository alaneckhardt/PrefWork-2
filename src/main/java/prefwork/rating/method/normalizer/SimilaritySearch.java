package prefwork.rating.method.normalizer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;

public class SimilaritySearch implements Normalizer {

	TreeMap<String, Double> map = new TreeMap<String, Double>();
	List<String> al = Utils.getList();
	int nnCount = 10;
	int index;
	ContentDataSource data;
	ContentBased method;
	public Double normalize(List<Object> o) {
		if(o.get(index) == null)
			return 0.0;
		String s = o.get(index).toString();
		if (!map.containsKey(s)) {
			
			
			
			/*
			 * String nn[]=new String[nnCount]; int nnWeight[]=new int[nnCount];
			 * for(Map.Entry<String, Double> entry :map.entrySet()) { String s =
			 * entry.getKey(); String s2 = o.toString(); int i=0;
			 * while(s.length()<i&&s2.length()<i&&s.charAt(i)==s2.charAt(i))
			 * i++; if(i>0&&i>nnWeight[nnCount-1]){ for(int k=0;k<nnCount;k++){
			 * if(i>nnWeight[k]){ int j = i; i = nnWeight[k]; nnWeight[k] = j;
			 * String js = s; s = nn[k]; nn[k] = js; } } } } Double res= 0.0;
			 * Double div= 0.0; for(int i=0;i<nnCount;i++){ res +=
			 * nnWeight[i]*(map.get(nn[i])==null?0.0:map.get(nn[i])); div +=
			 * nnWeight[i]; } if(div == 0.0) return 0.0; return res/div;
			 */
			double d1 = 0.0, d2 = 0.0;
			int index = -al.indexOf(s);
			// Object directly above the place where o would be.
			if (index >= 0 && index < map.size()
					/*&& map.get(al.get(index)) != null*/) {
				d1 = map.get(al.get(index));

			} else if (index == map.size()) {
				d1 = 0.0;
			}

			// Object directly bellow the place where o would be.
			if (index > 0 && index <= map.size()
					/*&& map.get(al.get(index - 1)) != null*/) {
				d2 = map.get(al.get(index - 1));

			} else if (index == map.size()) {
				d2 = 0.0;
			}
			double div = 2.0;
			if (d1 == 0.0)
				div--;
			if (d2 == 0.0)
				div--;
			if (div == 0.0)
				return 0.0;
			return ((d1 == 0.0 ? 0.0 : d1) + (d2 == 0.0 ? 0.0 : d2)) / div;
		} else
			return map.get(s);
	}

	public int compare(List<Object> arg0, List<Object> arg1) {
		double a = normalize(arg0);
		double b = normalize(arg1);
		if (a < b)
			return -1;
		if (a > b)
			return 1;
		return 0;
	}

	@Override
	public void addValue(Rating r) {
		Object o = r.getRecord().value(index);
		if(map.containsKey(o))
			return;
		map.put(o.toString(), method.getNormalizers()[index].normalize(r));
		al.add(o.toString());
			
	}
	@Override
	public void process() {
		/*
		 * for(Double r : attrVal.getRatings())
		 * if(!r.isInfinite()&&!r.isNaN()) {
		 * map.put(attrVal.getValue().toString(), r);
		 * al.add(attrVal.getValue().toString()); Collections.sort(al); }
		 */		
		Collections.sort(al, new AlphaComparator());		
	}
	public void init(ContentDataSource data, ContentBased method, int attributeIndex) {
		this.data = data;
		index = attributeIndex;
		map = new TreeMap<String, Double>();
		al = Utils.getList(data.getInstances().attribute(attributeIndex).numValues());
	}

	public Normalizer clone() {
		Normalizer n = new SimilaritySearch();
		return n;
	}

	public void configClassifier(XMLConfiguration config, String section) {
		// TODO Auto-generated method stub
		map = new TreeMap<String, Double>();
		al = Utils.getList();
	}

	@Override
	public double compareTo(Normalizer n) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int compare(Rating arg0, Rating arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Double normalize(Rating record) {
		// TODO Auto-generated method stub
		return null;
	}

}

class AlphaComparator implements Comparator<Object> {

	public int compare(Object arg0, Object arg1) {
		if (arg0 == null && arg1 == null)
			return 0;
		if (arg0 == null)
			return 1;
		if (arg1 == null)
			return -1;
		return arg0.toString().compareTo(arg1.toString());
	}

}
