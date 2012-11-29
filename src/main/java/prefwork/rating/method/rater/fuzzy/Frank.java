package prefwork.rating.method.rater.fuzzy;

public class Frank implements Family{

	public  double implicator(double x, double y, double lambda){
		if (lambda == 0.0)
			return BaseFunctions.IM(x, y);
		if (lambda == 1.0)
			return BaseFunctions.IP(x, y);
		if (lambda == getMaxLambda())
			return BaseFunctions.IL(x, y);
		
		return 1-BaseFunctions.log((1.0 + (((Math.pow(lambda, 1-x) - 1) * (Math
					.pow(lambda, 1-y) - 1)) / (lambda - 1))), lambda);
	}
	public  double conjunctor(double x, double y, double lambda){	
		if (lambda == 0.0)
			return BaseFunctions.TM(x, y);
		if (lambda == 1.0)
			return BaseFunctions.TP(x, y);
		if (lambda == getMaxLambda())
			return BaseFunctions.TL(x, y);
		
		return BaseFunctions.log((1.0 + (((Math.pow(lambda, x) - 1) * (Math
					.pow(lambda, y) - 1)) / (lambda - 1))), lambda);
	
	}		
	public double getMostSpecific() {
		return 0.0;
	}		
	public String toString() {
		return "Frank1e10";
	}
	@Override
	public double getMaxLambda() {
		return 1e10;
	}
	@Override
	public double getMinLambda() {
		return 0;
	}		
	public double getEpsilon(double l) {
		return l*0.0000001;
	}	
}
