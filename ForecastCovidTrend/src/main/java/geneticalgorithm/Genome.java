package geneticalgorithm;

public interface Genome extends Comparable<Genome> {
	
	Genome[] crossover(Genome theOther);
	Genome mutate();
	void calculateFitness(Environment e);
	double getFitness();
	
	@Override
	default int compareTo(Genome o) {
		return Double.valueOf(this.getFitness()).compareTo(Double.valueOf(o.getFitness()));
	}
}
