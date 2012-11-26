package prefwork.rating.method.rater.fuzzy;

public class SchweizerSklar implements Family{

	public  double implicator(double x, double y, double lambda){
		if (lambda == 0.0)
			return BaseFunctions.IP(x,y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.ID(x,y);
		if (lambda == Double.MIN_VALUE)
			return BaseFunctions.IM(x,y);
		
		return 1-(Math.pow(Math.max(0, Math.pow(1-x,lambda)+Math.pow(1-y,lambda)-1), 1/lambda));
	
	}
	public  double conjunctor(double x, double y, double lambda){				
		/*if (lambda == 0.0)
			return BaseFunctions.TP(x,y);
		if (lambda == BaseFunctions.MAX_VALUE)
			return BaseFunctions.TD(x,y);
		if (lambda == Double.MIN_VALUE)
			return BaseFunctions.TM(x,y);*/
		
		return Math.pow(Math.max(0, ((Math.pow(x,lambda)+Math.pow(y,lambda)-1))), 1/lambda);
	}	
	public double getMostSpecific() {
		return 0.1;
	}		
	public String toString() {
		return "SchweizerSklar";
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
