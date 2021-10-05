package seir;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class SEIRResult {
	private final double[] susceptible;
	private final double[] exposed;
	private final double[] infectious;
	private final double[] recovered;
	private final double[] death;
	private final double[] newDeath;
	
	public SEIRResult(double[] susceptible, double[] exposed, double[] infectious, double[] recovered, double[] death,
			double[] newDeath) {
		super();
		this.susceptible = susceptible;
		this.exposed = exposed;
		this.infectious = infectious;
		this.recovered = recovered;
		this.death = death;
		this.newDeath = newDeath;
	}
	public double[] getSusceptible() {
		return susceptible;
	}
	public double[] getExposed() {
		return exposed;
	}
	public double[] getInfectious() {
		return infectious;
	}
	public double[] getRecovered() {
		return recovered;
	}
	public double[] getDeath() {
		return death;
	}
	public double[] getNewDeath() {
		return newDeath;
	}
}
