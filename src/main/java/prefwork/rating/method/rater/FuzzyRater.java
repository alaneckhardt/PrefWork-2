package prefwork.rating.method.rater;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import prefwork.rating.method.ContentBased;
import prefwork.rating.method.normalizer.Normalizer;
import prefwork.rating.method.rater.fuzzy.Family;
import prefwork.rating.method.rater.fuzzy.FindLambda;
import prefwork.rating.method.rater.fuzzy.Lambdas;
import prefwork.rating.method.rater.fuzzy.Yager;

/**
 * <p>
 * WeightAverage computes rating as x*wx+y*wy+../wx+wy+... It is weighed
 * average.
 * </p>
 * <p>
 * Copyright (c) 2006
 * </p>
 * 
 * @author Alan Eckhardt
 * @version 1.0
 */
public class FuzzyRater implements Rater {
	Family f = new Yager();
	Lambdas l = new Lambdas();
	String familyName;
	FindLambda find;
	double precision =0.5;
	
	public FuzzyRater() {
	}


	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		familyName = Utils.getFromConfIfNotNull(methodConf,"familyName",familyName);
		f = (Family) Utils.getInstance(familyName);
	}

	public String toString(){
		return "FuzzyNew"+f.toString()+precision;
	}

	@Override
	public void init(ContentDataSource data, ContentBased method) {
		Normalizer[] n = method.getNormalizers();
		data.restart();
		List<Double[]> l = Utils.getList();
		find = new FindLambda();
		while(data.hasNext()){
			Rating r = (Rating) data.next();
			Double[] normR = new Double[r.getRecord().numAttributes()];
			for (int i = 0; i < normR.length; i++) {
				if(i == r.getRecord().classIndex())
					normR[i] = r.getRating()/5.0;
				normR[i] = n[i].normalize(r)/5.0;		
				if(normR[i] == null)
					normR[i] = 0.0;
			}
			l.add(normR);
		}
		find.setData(l);
		
		try {
			this.l = find.searchOptimumWithError(f, precision);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	@Override
	public double compareTo(Rater n) {
		return 0;
	}


	@Override
	public Double getRating(Double[] ratings, Rating record) {
		for (int i = 0; i < ratings.length; i++) {
			if(ratings[i] == null)
				ratings[i] = 0.0;
			else
				ratings[i] /= 5.0;
				
		}
		double[] b = find.getBounds(ratings,f,l);
		//return (b[0]+b[1])/2;
		return b[1];
	}

}

