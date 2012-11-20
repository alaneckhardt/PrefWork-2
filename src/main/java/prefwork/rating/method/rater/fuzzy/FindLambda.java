package prefwork.rating.method.rater.fuzzy;

import java.util.List;

import org.apache.log4j.Logger;

import prefwork.rating.method.normalizer.Normalizer;

public class FindLambda {
	List<Double[]> data;
	private static Logger log = Logger.getLogger(FindLambda.class);
	String resultsFile = "res200.csv";
	// static String fileName = "dataProClanekDavid.csv";
	static String fileName = "test200.csv";
	// static double[] lambdas =
	// {1.05,1.01,0.95,0.99,1.005,0.995,1.001,0.999,1.0005,0.9995,1.0001,0.9999};
	// static double[] lambdas = {1000,10000,100000,1000000};
	// static double[] lambdas = {0.1,0.01,0.001,0.0001};
	double[] lambdas;
	Family[] families;
	// double testRatio = 0.816;
	double testRatio = 0.04;
	boolean stopOnError = false;
	// double maxLambda;
	// double minLambda;
	static String prefix = "noTransformation;";

	/**
	 * Return the difference of upper and lower bound given by a fuzzy tnorm and
	 * tconorm.
	 * 
	 * @param o1
	 * @param o2
	 * @param o3
	 * @param family
	 * @param lambda
	 * @return
	 * @throws Exception
	 */
	private static double getBound(double a, double b, Family family,
			double lC, double lI) throws Exception {
		double lower = getBoundC(a,b, family, lC, lI);
		double upper = getBoundI(a,b, family, lC, lI);
		return upper - lower;
	}

	/**
	 * Return the upper and lower bounds given by a fuzzy tnorm and tconorm.
	 * 
	 * @param o1
	 * @param o2
	 * @param o3
	 * @param family
	 * @param lambda
	 * @return
	 * @throws Exception
	 */
	private static double getBoundI(double a, double b, Family family,
			double lC, double lI) {
		double boundI;
		boundI = family.implicator(a, b, lI);
		// We get minimum of estimation of T-conorm, because t-conorms are not
		// symmetric.
		boundI = Math.min(boundI, family.implicator(b, a, lI));
		return boundI;
	}

	/**
	 * Return the upper and lower bounds given by a fuzzy tnorm and tconorm.
	 * 
	 * @param o1
	 * @param o2
	 * @param o3
	 * @param family
	 * @param lambda
	 * @return
	 * @throws Exception
	 */
	private static double getBoundC(double a, double b, Family family,
			double lC, double lI) {
		return family.conjunctor(a, b, lC);
	}

	/**
	 * Return the upper and lower bounds given by a fuzzy tnorm and tconorm.
	 * 
	 * @param o1
	 * @param o2
	 * @param o3
	 * @param family
	 * @param lambda
	 * @return
	 * @throws Exception
	 */
	protected static double[] getBounds(double a, double b, Family family,
			double lC, double lI) {
		double[] bounds = new double[2];
		bounds[0] = getBoundC(a,b, family, lC, lI);
		bounds[1] = getBoundI(a,b, family, lC, lI);
		return bounds;
	}
	/**
	 * Returns lower and upper bounds on one object.
	 * 0 is the class, rest are the attributes.
	 * @param d
	 * @param family
	 * @param l
	 * @return
	 */
	public double[] getBounds(Double[] d, Family family, Lambdas l){
		double[] bounds = {0,0};		
		bounds[0] += getBoundC(d[1],d[2], family, l.lC, l.lI);
		bounds[1] += getBoundI(d[1],d[2], family, l.lC, l.lI);
		//Iteratively go through the record, adding two attributes at a time
		for (int j = 3; j < d.length; j++) {
			bounds[0] = getBoundC(bounds[0],d[j], family, l.lC, l.lI);
			bounds[1] = getBoundI(bounds[1],d[j], family, l.lC, l.lI);
		}
		return bounds;
	}
	private double computeBounds(Family family, Lambdas l) throws Exception {
		double[] bounds;
		for (int i = 0; i < data.size(); i++) {
			Double[] d = data.get(i);
			bounds = getBounds(d,family,l);
			l.boundC += bounds[0];
			l.boundI += bounds[1];
		}
		return l.boundI - l.boundC;
	}
	
	protected static double[] getNextLambdaI(Family family, double lC,
			double lI, double lIMin, double lIMax, double a, double b) {
		double lastLi = -3.0;
		double lIMaxLast = lIMax, lIMinLast = lIMin;
		double bound = getBoundI(a,b, family, lC, lI);
		lI = (lIMin + lIMax) / 2;
		bound = getBoundI(a, b, family, lC, lI);
		int i = 0;
		while (Math.abs(lastLi - lI) > family.getEpsilon(lI) && i < 1000000) {
			i++;
			if (bound < b) {
				if (family.getMostSpecific() > lI) {
					lIMax = (lIMin + lIMax) / 2;
					lIMin = lIMinLast;
				} else {
					lIMin = (lIMin + lIMax) / 2;
					lIMax = lIMaxLast;
				}
			} else {
				if (family.getMostSpecific() > lI) {
					lIMinLast = lIMin;
					lIMin = (lIMin + lIMax) / 2;

				} else {
					lIMaxLast = lIMax;
					lIMax = (lIMin + lIMax) / 2;
				}
			}
			lastLi = lI;
			lI = (lIMin + lIMax) / 2;
			bound = getBoundI(a, b, family, lC, lI);
		}
		if (bound < b) {
			if (family.getMostSpecific() > lI) {
				lIMin = Math.max(lIMin - 2 * family.getEpsilon(lI), family
						.getMinLambda());
				lI = (lIMin + lIMax) / 2;
			} else {
				lIMax = Math.min(lIMax + 2 * family.getEpsilon(lI), family
						.getMaxLambda());
				lI = (lIMin + lIMax) / 2;
			}
			bound = getBoundI(a, b, family, lC, lI);
			if (bound < b)
				return null;
		}
		return new double[] { lI, lIMin, lIMax };
	}

	protected static double[] getNextLambdaC(Family family, double lC,
			double lI, double lCMin, double lCMax, double a, double b) {
		double lastLC = -3.0;
		double lCMaxLast = lCMax, lCMinLast = lCMin;
		double bound = getBoundC(a, b, family, lC, lI);
		lC = (lCMin + lCMax) / 2;
		bound = getBoundC(a, b, family, lC, lI);
		int i = 0;
		while (Math.abs(lastLC - lC) > family.getEpsilon(lC) && i < 1000000) {
			i++;
			if (bound > b) {// candidateFalse
				if (family.getMostSpecific() > lC) {
					lCMax = (lCMin + lCMax) / 2;
					lCMin = lCMinLast;
				} else {
					lCMin = (lCMin + lCMax) / 2;
					lCMax = lCMaxLast;
				}
			} else {
				if (family.getMostSpecific() > lC) {
					// We need to remember last lC in case we need to get back
					// to this state - this lC is correct
					lCMinLast = lCMin;
					lCMin = (lCMin + lCMax) / 2;
				} else {
					lCMaxLast = lCMax;
					lCMax = (lCMin + lCMax) / 2;
				}
			}
			lastLC = lC;

			lastLC = lC;
			lC = (lCMin + lCMax) / 2;
			bound = getBoundC(a, b, family, lC, lI);
		}
		if (bound > b) {
			if (family.getMostSpecific() > lC) {
				lCMin = Math.max(lCMin - 2 * family.getEpsilon(lC), family
						.getMinLambda());
				lC = (lCMin + lCMax) / 2;
			} else {
				lCMax = Math.min(lCMax + 2 * family.getEpsilon(lC), family
						.getMaxLambda());
				lC = (lCMin + lCMax) / 2;
			}
			bound = getBoundC(a, b, family, lC, lI);

			if (bound > b)
				return null;
		}
		return new double[] { lC, lCMin, lCMax };
	}


	/**
	 * Return errors for given object.
	 * @param d
	 * @param family
	 * @param l
	 * @return
	 */
	protected Lambdas getErrors(Double[] d, Family family, Lambdas l) {
		double[] bounds = getBounds(d, family, l);
		if (bounds[1] < d[0]) {
			l.errI++;
		} else {
			l.okI++;
		}
		if (bounds[0] > d[0]) {
			l.errC++;
		} else {
			l.okC++;
		}
		return l;
	}

	private void initLambdas(Family family, Lambdas l) {
		l.lIMin = family.getMinLambda();
		l.lIMinLast = family.getMinLambda();
		l.lIMax = family.getMaxLambda();
		l.lIMaxLast = family.getMaxLambda();
		l.lCMin = family.getMinLambda();
		l.lCMinLast = family.getMinLambda();
		l.lCMax = family.getMaxLambda();
		l.lCMaxLast = family.getMaxLambda();
		l.lI = family.getMostSpecific();
		l.lILast = family.getMostSpecific();
		l.lC = family.getMostSpecific();
		l.lCLast = family.getMostSpecific();
	}

	private void processLCs(Family family, Lambdas l, boolean moreGeneral) {
		if (moreGeneral) {
			if (family.getMostSpecific() > l.lC) {
				l.lCMax = (l.lCMin + l.lCMax) / 2;
				l.lCMin = l.lCMinLast;
			} else {
				l.lCMin = (l.lCMin + l.lCMax) / 2;
				l.lCMax = l.lCMaxLast;
			}
		} else {
			if (family.getMostSpecific() > l.lC) {
				// We need to remember last lC in case we need to get back
				// to this state - this lC is correct
				l.lCMinLast = l.lCMin;
				l.lCMin = (l.lCMin + l.lCMax) / 2;
			} else {
				l.lCMaxLast = l.lCMax;
				l.lCMax = (l.lCMin + l.lCMax) / 2;
			}
		}
		l.lCLast = l.lC;
		l.lC = (l.lCMin + l.lCMax) / 2;
		log.debug("change " + family + ",lC " + l.lC + ",lCErr " + l.errC  );
	}

	private void processLIs(Family family, Lambdas l, boolean moreGeneral) {
		if (moreGeneral) {
			if (family.getMostSpecific() > l.lI) {
				l.lIMax = (l.lIMin + l.lIMax) / 2;
				l.lIMin = l.lIMinLast;
			} else {
				l.lIMin = (l.lIMin + l.lIMax) / 2;
				l.lIMax = l.lIMaxLast;
			}
		} else {
			if (family.getMostSpecific() > l.lI) {
				// We need to remember last lI in case we need to get back
				// to this state - this lI is correct
				l.lIMinLast = l.lIMin;
				l.lIMin = (l.lIMin + l.lIMax) / 2;
			} else {
				l.lIMaxLast = l.lIMax;
				l.lIMax = (l.lIMin + l.lIMax) / 2;
			}
		}
		l.lILast = l.lI;
		l.lI = (l.lIMin + l.lIMax) / 2;
		log.debug("change " + family + ",lI " + l.lI + ",lIErr " + l.errI);
	}

	private Lambdas getErrors(Family family, Lambdas l){
		for (int i = 0; i < data.size(); i++) {
			Double[] d = data.get(i);
			double[] bounds = getBounds(d,family,l);
			l.boundC += bounds[0];
			l.boundI += bounds[1];
			l = getErrors(d, family, l);
		}
		return l;
	}	
	
	public Lambdas searchOptimumWithError(Family family, double errorRatio) throws Exception {
		Lambdas l = new Lambdas();
		l.errAllowed = errorRatio;
		initLambdas(family, l);
		l.errC = l.errI = l.okC = l.okI = 0;
		l.lI = (l.lIMin + l.lIMax) / 2;
		l.lC = (l.lCMin + l.lCMax) / 2;
		l = getErrors(family, l);

		int i = 0;
		while ((Math.abs(l.lCLast - l.lC) > family.getEpsilon(l.lC) && Math.abs(l.lILast - l.lI) > family.getEpsilon(l.lI)) 
				&& ((l.okC == 0 || errorRatio < ((double) (0.0 + l.errC) / (l.okC)))||
				     	  (l.okI == 0 || errorRatio < ((double) (0.0 + l.errI) / (l.okI))))
				&& i < 500) {
			i++;
			processLCs(	family,	l,
					(l.okC == 0 || errorRatio < ((double) (0.0 + l.errC) / (l.okC))));
			processLIs(					family,	l,
					(l.okI == 0 || errorRatio < ((double) (0.0 + l.errI) / (l.okI))));
			l.errC = l.errI = l.okC = l.okI = 0;
			l = getErrors(family, l);

		}
		i = 0;
		//If the allowed error was exceeded in the last execution of the loop above
		// we need to lower the lC and lI
		while(((l.okC == 0 || errorRatio < ((double) (0.0 + l.errC) / (l.okC)))||
	     	  (l.okI == 0 || errorRatio < ((double) (0.0 + l.errI) / (l.okI)))) &&  i < 400){
			i++;
			if(l.okC == 0 || errorRatio < ((double) (0.0 + l.errC) / (l.okC))){
				processLCs(family,l,
					(l.okC == 0 || errorRatio < ((double) (0.0 + l.errC) / (l.okC))));
			}
			if(l.okI == 0 || errorRatio < ((double) (0.0 + l.errI) / (l.okI))){
				processLIs(family,l,
					(l.okI == 0 || errorRatio < ((double) (0.0 + l.errI) / (l.okI))));
			}
			l.errC = l.errI = l.okC = l.okI = 0;
			l = getErrors(family, l);
		}
		
		return l;
	}
	
	public List<Double[]> getData() {
		return data;
	}

	public void setData(List<Double[]> data) {
		this.data = data;
	}

	public double[] getLambdas() {
		return lambdas;
	}

	public void setLambdas(double[] lambdas) {
		this.lambdas = lambdas;
	}

	public Family[] getFamilies() {
		return families;
	}

	public void setFamilies(Family[] families) {
		this.families = families;
	}
}

