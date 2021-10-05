package seir;


/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class SEIRModel {

	private final ModelParameter param;
	private static final int NUM_ITERATIONS_PER_DAY = 10; /* number of iterations per day*/
	
	public SEIRModel(ModelParameter modelParameter) {
		super();
		this.param = modelParameter;
	}
	
	public SEIRResult run( final int numDays) {
		double[] aSusceptible = new double[numDays];
		double[] aExposed = new double[numDays];
		double[] aInfectious = new double[numDays];
		double[] aRecovered = new double[numDays];
		double[] aDeath = new double[numDays];
		double[] aNewDeath = new double[numDays];
		
		final double dt = 1.0/NUM_ITERATIONS_PER_DAY;
		for (int i = 0 ; i < numDays ; i++ ) {
			
			double susceptible = i == 0 ?  param.getSusceptible() : aSusceptible[i-1];
			double exposed = i == 0 ? param.getExposed() : aExposed[i-1];
			double infectious = i == 0 ? param.getInfectious() : aInfectious[i-1];
			double recovered = i == 0 ? param.getRecovered() : aRecovered[i-1];
			double death = i == 0 ? param.getDeath() : aDeath[i-1];
			double newDeaths = 0;
			double population = susceptible + exposed + infectious + recovered;
			for ( int j = 0; j < NUM_ITERATIONS_PER_DAY ; j++) {
				
				double ds = dt*(-param.getBeta()*susceptible*infectious/population);
				double de = dt*(param.getBeta()*susceptible*infectious/population - param.getEpsilon()*exposed);
				double di = dt*(param.getEpsilon()*exposed - (param.getGamma() + param.getAlpha())*infectious);
				double dr = dt*(param.getGamma()*infectious);
				double dd = dt*(param.getAlpha()*infectious);
				
				susceptible += ds;
				exposed += de;
				infectious += di;
				recovered += dr;
				death += dd;
				newDeaths += dd;
			}
			aSusceptible[i] = susceptible;
			aExposed[i] = exposed;
			aInfectious[i] = infectious;
			aRecovered[i] =recovered;
			aDeath[i] = death;
			aNewDeath[i] = newDeaths;
		}
		return new SEIRResult(aSusceptible, aExposed, aInfectious, aRecovered, aDeath, aNewDeath);
	}
}
