package prefwork.rating.method.rater.fuzzy;

public class Hamacher implements  Family{

	public  double implicator(double x, double y, double lambda){
		if (lambda == 0.0 && x== 1.0 && y == 1.0)
			return 1.0;
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.ID(x, y);
		
		return (x+y-x*y-(1-lambda)*x*y)/(1-(1-lambda)*x*y);
	}
	public  double conjunctor(double x, double y, double lambda){	
		if (lambda == 0.0 && x== 0.0 && y == 0.0)
			return 0.0;
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.TD(x, y);
		
		return (x*y)/(lambda+(1-lambda)*(x+y-x*y));
		
	}	
	public double getMostSpecific() {
		return getMinLambda();
	}			
	public String toString() {
		return "Hamacher50";
	}
	@Override
	public double getMaxLambda() {
		return 50;
	}
	@Override
	public double getMinLambda() {
		return 0.0;
	}		
	public double getEpsilon(double l) {
		return BaseFunctions.Epsilon;
	}			
}
