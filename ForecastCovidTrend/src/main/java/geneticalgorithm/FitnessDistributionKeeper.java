package geneticalgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class FitnessDistributionKeeper implements GeneticAlgorithmEventListener {

	private final int maxGeneration;
	private List<Pair<Integer,List<Double>>> fitnessDistributions = new ArrayList<>();

	public FitnessDistributionKeeper(int maxGeneration) {
		super();
		this.maxGeneration = maxGeneration;
	}

	@Override
	public void onNewGeneration(int generation, List<Genome> genomes) {
		if ( generation <= maxGeneration) {
			this.fitnessDistributions.add( 
					new ImmutablePair<>(generation, 
							genomes.stream().map(Genome::getFitness).collect(Collectors.toList())));
		}
	}

	public List<Pair<Integer,List<Double>>> getFitnessesDistributions() {
		return fitnessDistributions;
	}
}
