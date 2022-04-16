package geneticalgorithm.seir;

import java.util.Arrays;

import geneticalgorithm.Environment;

public class ModelEnvironment implements Environment{
	private final double minInitExposed;
	private final double maxInitExposed;
	private final double minInitInfectious;
	private final double maxInitInfectious;
	private final double minInitRecovered;
	private final double maxInitRecovered;
	private final double initDeaths;
	private final double[] observedDeaths;
	
	public ModelEnvironment(double minInitExposed, double maxInitExposed, double minInitInfectious, double maxInitInfectious,
			double minInitRecovered, double maxInitRecovered, double initDeaths, double[] observedDeaths) {
		super();
		this.minInitExposed = minInitExposed;
		this.maxInitExposed = maxInitExposed;
		this.minInitInfectious = minInitInfectious;
		this.maxInitInfectious = maxInitInfectious;
		this.minInitRecovered = minInitRecovered;
		this.maxInitRecovered = maxInitRecovered;
		this.initDeaths = initDeaths;
		this.observedDeaths = observedDeaths;
	}
	public double[] getObservedDeaths() {
		return observedDeaths;
	}
	public double getMinInitExposed() {
		return minInitExposed;
	}
	public double getMaxInitExposed() {
		return maxInitExposed;
	}
	public double getMinInitInfectious() {
		return minInitInfectious;
	}
	public double getMaxInitInfectious() {
		return maxInitInfectious;
	}
	public double getMinInitRecovered() {
		return minInitRecovered;
	}
	public double getMaxInitRecovered() {
		return maxInitRecovered;
	}
	public double getInitDeaths() {
		return initDeaths;
	}
	@Override
	public String toString() {
		return "SEIREnvironment [minInitExposed=" + minInitExposed + ", maxInitExposed=" + maxInitExposed + ", minInitInfectious="
				+ minInitInfectious + ", maxInitInfectious=" + maxInitInfectious + ", minInitRecovered=" + minInitRecovered
				+ ", maxInitRecovered=" + maxInitRecovered + ", initDeaths=" + initDeaths + ", observedDeaths="
				+ Arrays.toString(observedDeaths) + "]";
	}
	
}
