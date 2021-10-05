package seir;

import java.util.Optional;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class ModelParameter implements Cloneable {
	
	private static Optional<Integer> population = Optional.empty();

	private double alpha;
	private double beta;
	private double epsilon;
	private double gamma;
	
	private double exposed;
	private double infectious;
	private double recovered;

	private double death;
	
	public ModelParameter(double alpha, double beta, double epsilon, double gamma,
			double exposed, double infectious, double recovered, double death) {

		super();
		this.alpha = alpha;
		this.beta = beta;
		this.epsilon = epsilon;
		this.gamma = gamma;
		
		this.exposed = exposed;
		this.infectious = infectious;
		this.recovered = recovered;
		this.death = death;
	}

	public double getAlpha() {
		return alpha;
	}

	public double getBeta() {
		return beta;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public double getGamma() {
		return gamma;
	}
	
	public double getSusceptible() {
		return population.get() - this.exposed - this.infectious - this.recovered - this.death;
	}

	public double getExposed() {
		return exposed;
	}

	public double getInfectious() {
		return infectious;
	}

	public double getRecovered() {
		return recovered;
	}

	public double getDeath() {
		return death;
	}

	public static int getPopulation() {
		return population.get();
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}
	
	public void setExposed(double exposed) {
		this.exposed = exposed;
	}

	public void setInfectious(double infectious) {
		this.infectious = infectious;
	}

	public void setRecovered(double recovered) {
		this.recovered = recovered;
	}

	public void setDeath(double death) {
		this.death = death;
	}
	
	public static void setPopulation(int _population) {
		if (population.isEmpty())
			population = Optional.of(_population);
		else
			throw new IllegalStateException("The value is already set");
	}

//	public void setNewDeath(double newDeath) {
//		this.newDeath = newDeath;
//	}
	
	public double getR0() {
		return beta/(gamma+alpha);
	}
	
	public double getIFR() {
		return alpha/gamma;
	}

	@Override
	public ModelParameter clone()  {
		try {
			return (ModelParameter)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String toString() {
		return String.format("𝛼=%f, 𝜷=%f, 𝜺=%f, 𝜸=%f, r0=%.4f, IFR=%.4f%%, "
				+ "exposed=%,.0f, infectious=%,.0f, recovered=%,.0f, "
				+ "death=%,.0f, susceptibles=%,.0f", 
				alpha, beta, epsilon, gamma, getR0(), getIFR()*100, exposed, infectious, recovered,
				death, this.getSusceptible());
	}
	public String shortString() {
		return String.format("R0=%.2f, IFR=%.3f%%", getR0(), getIFR()*100);
	}
	
}