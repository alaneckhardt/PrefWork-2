package prefwork.rating.method.rater.fuzzy;

public final class BaseFunctions {
	public static final double MAX_VALUE = 100;
	public static final double Epsilon = 0.000000001;
	public static double log(double d, double base) {
		return Math.log(d) / Math.log(base);
	}
	
	/**
	 * Computes TM conjunction of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double TM(double x, double y) {		
			return Math.min(x, y);
	}

	/**
	 * Computes TP conjunction of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double TP(double x, double y) {		
			return x*y;
	}

	/**
	 * Computes TD conjunction of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double TD(double x, double y) {		
		if(x == 1.0)
			return y;
		if(y == 1.0)
			return x;
		return 0.0;
	}

	/**
	 * Computes TL conjunction of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double TL(double x, double y) {		
		return Math.max(0, x+y-1);
	}


	/**
	 * Computes IM implication of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double IM(double x, double y) {		
		if(x>y)
			return y;
		return 1;
	}

	/**
	 * Computes IP implication of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double IP(double x, double y) {		
			if(x==0.0)
				return 1;
			return Math.min(y/x,1);
	}

	/**
	 * Computes ID implication of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double ID(double x, double y) {	
		if(x == 0.0)
			return y;
		if(y == 0.0)
			return x;
		return 1.0;
	}

	/**
	 * Computes IL implication of two given variables with given labmda	 * 
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public static double IL(double x, double y) {		
		return Math.min(1, 1-x+y);
	}

}
