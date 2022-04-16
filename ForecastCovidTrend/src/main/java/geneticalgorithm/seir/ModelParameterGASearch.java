package geneticalgorithm.seir;

import java.time.LocalDate;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import geneticalgorithm.GeneticAlgorithm;
import geneticalgorithm.GeneticAlgorithmEventListener;
import seir.ModelParameter;
import seir.ModelParameterSearch;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class ModelParameterGASearch implements ModelParameterSearch {

	private final GeneticAlgorithm ga;
	
	public ModelParameterGASearch(int populationSize, int maxGeneration, int maxGenerationWithSameGenome, 
			double crossoverRate, double mutationRate, int tournamentSize, int eliteGroupSize) {
		
		super();

		this.ga = new GeneticAlgorithm(populationSize, maxGeneration, maxGenerationWithSameGenome, crossoverRate, mutationRate, 
				tournamentSize, eliteGroupSize, new ModelGenomePopulationFactory());
	}
	
	public void addEventListener(GeneticAlgorithmEventListener eventListerner) {
		this.ga.addEventListener(eventListerner);
	}

	public void removeEventListner(GeneticAlgorithmEventListener evenListener) {
		this.ga.removeEventListener(evenListener);
	}
	
	@Override
	public Pair<ModelParameter, Double> search(LocalDate initDate, 
			double minInitExposed, double maxInitExposed, 
			double minInitInfectious, double maxInitInfectious,
			double minInitRecovered, double maxInitRecovered,
			double initDeaths, double[] observedDeaths) {
		
		ModelEnvironment env = new ModelEnvironment(
									minInitExposed, maxInitExposed,
									minInitInfectious, maxInitInfectious, 
									minInitRecovered, maxInitRecovered, 
									initDeaths,
									observedDeaths);
		
		var genome = (ModelParameterGenome)ga.run(env);
		return new ImmutablePair<ModelParameter, Double>(genome.getParameter(), genome.getFitness());
	}
}
