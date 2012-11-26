package prefwork.rating.method.rater.fuzzy;

public class Dombi implements Family {
	public  double implicator(double x, double y, double lambda){
		if (lambda == 0.0)
			return BaseFunctions.ID(x, y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.IM(x, y);
		
		return 1-1/(1+Math.pow((Math.pow(x/(1-x),lambda)+Math.pow(y/(1-y),lambda)),1/lambda));
	}
	public  double conjunctor(double x, double y, double lambda){

		if (lambda == 0.0)
			return BaseFunctions.TD(x, y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.TM(x, y);
		
		return 1/(1+Math.pow((Math.pow((1-x)/x,lambda)+Math.pow((1-y)/y,lambda)),1/lambda));			
	}

	public double getMostSpecific() {
		return getMaxLambda();
	}	
	public String toString() {
		return "Dombi";
	}
	@Override
	public double getMaxLambda() {
		return BaseFunctions.MAX_VALUE;
	}
	@Override
	public double getMinLambda() {
		return 0;
	}			
	public double getEpsilon(double l) {
		return BaseFunctions.Epsilon;
	}	
}
