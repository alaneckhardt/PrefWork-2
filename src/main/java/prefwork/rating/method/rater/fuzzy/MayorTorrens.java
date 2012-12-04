package prefwork.rating.method.rater.fuzzy;

public class MayorTorrens implements Family{

	public  double implicator(double x, double y, double lambda){
		if (lambda > 0.0 && lambda <= 1 &&
				x >= 1-lambda && y >= 1-lambda &&
				x <= 1 && y <= 1 
				)
			return Math.min(x+y+lambda-1,1);
		
		return Math.max(x,y);
	}
	public  double conjunctor(double x, double y, double lambda){	
		if (lambda > 0.0 && lambda <= 1 &&
				x >= 0 && y >= 0 &&
				x <= lambda && y <= lambda 
				)
			return Math.max(x+y-lambda,0);
		
		return Math.min(x,y);
	}	
	public double getMostSpecific() {
		return getMinLambda();
	}			
	public String toString() {
		return "MayorTorrens"+getMaxLambda();
	}
	@Override
	public double getMaxLambda() {
		return 2;
	}
	@Override
	public double getMinLambda() {
		return 0.0;
	}			
	public double getEpsilon(double l) {
		return Math.pow(10, -35);
	}		
}
