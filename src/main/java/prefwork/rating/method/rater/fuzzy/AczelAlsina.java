package prefwork.rating.method.rater.fuzzy;

public class AczelAlsina implements Family {
	public  double implicator(double x, double y, double lambda){
		if (lambda == 0.0)
			return BaseFunctions.ID(x, y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.IM(x, y);
		return 1-Math.pow(Math.E, -Math.pow(( Math.pow(-Math.log(1-x),lambda)+Math.pow(-Math.log(1-y),lambda) ), 1/lambda));
	}
	public  double conjunctor(double x, double y, double lambda){	
		if (lambda == 0.0)
			return BaseFunctions.TD(x, y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.TM(x, y);
		
		return Math.pow(Math.E, -Math.pow(( Math.pow(-Math.log(x),lambda)+Math.pow(-Math.log(y),lambda) ), 1/lambda));
		
	}	
	public double getMostSpecific() {
		return getMaxLambda();
	}			
	public String toString() {
		return "AczelAlsina";
	}
	@Override
	public double getMaxLambda() {
		return 50;
	}
	@Override
	public double getMinLambda() {
		return 0;
	}								
	public double getEpsilon(double l) {
		return BaseFunctions.Epsilon;
	}	
}
