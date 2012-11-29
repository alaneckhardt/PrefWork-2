package prefwork.rating.method.rater.fuzzy;

public class SugenoWeber implements Family {
	public  double implicator(double x, double y, double lambda){
		if (lambda == -1.0)
			return BaseFunctions.IP(x, y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.ID(x, y);
		return Math.min(1, (x+y-lambda*x*y));
	}
	public  double conjunctor(double x, double y, double lambda){	
		if (lambda == -1.0)
			return BaseFunctions.TD(x, y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.TP(x, y);
		
		return Math.max(0, (x+y-1+lambda*x*y)/(1+lambda));
		
	}	
	public double getMostSpecific() {
		return getMaxLambda();
	}			
	public String toString() {
		return "Sugeno-Weber"+getMaxLambda();
	}
	@Override
	public double getMaxLambda() {
		return 1e50;
	}
	@Override
	public double getMinLambda() {
		return -1;
	}			
	public double getEpsilon(double l) {
		return BaseFunctions.Epsilon;
	}			
}
