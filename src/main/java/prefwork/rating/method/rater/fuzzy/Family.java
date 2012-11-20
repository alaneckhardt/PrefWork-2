package prefwork.rating.method.rater.fuzzy;

/**
 * Family of fuzzy T-norms and T-conorms.
 * @author Alan
 *
 */
public interface Family {
	public String toString();
	/**Lowest difference. Depends on the family and the actual number.*/
	public double getEpsilon(double l);
	/**Minimal lambda for the family.*/
	public double getMinLambda();
	/**Maximal lambda for the family.*/
	public double getMaxLambda();
	/**Where the family is most strict, i.e. norm, lower bound, is the highest and conorm, the higher bound, the lowest.*/
	public double getMostSpecific();
	/**
	 * Conorm, or the higher bound.
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public double implicator(double x, double y, double lambda);
	/**
	 * Norm, or the lower bound.
	 * @param x
	 * @param y
	 * @param lambda
	 * @return
	 */
	public double conjunctor(double x, double y, double lambda);
}
