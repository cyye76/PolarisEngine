package Utils;

import org.apache.commons.math3.distribution.NormalDistribution;

public class MathUtils {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println(""+normalIntegration(0,1, -1, 1));
		System.out.println(""+normalIntegration(0,1, -2, 2));
		System.out.println(""+normalIntegration(0,1, -1.5, 1.5));

	}
	
	public static double normalIntegration(double mean, double sd, double from, double to) {
		if(sd<=0) return 1;//only one value for failed events
		
		NormalDistribution normalDistributioin = new NormalDistribution(mean, sd);
		
		double s1 = normalDistributioin.cumulativeProbability(from);
		double s2 = normalDistributioin.cumulativeProbability(to);
		
		return s2-s1;
	}
 
}
