package geneticalgorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class GeneticAlgorithm {
	
	private static Logger logger = LoggerFactory.getLogger(GeneticAlgorithm.class);

	private final int populationSize;
	private final int maxGenerations;
	private final int maxGenerationWithSameGenome;
	private final double crossoverRate;
	private final double mutationRate;
	private final int tournamentSize;
	private final int eliteGroupSize;
	private final InitialPopulationFactory initPopulationFactory;
	
	private static final Comparator<? super Genome> genomeComparator = Comparator.comparing(Genome::getFitness);

	private final List<GeneticAlgorithmEventListener> eventListeners = new ArrayList<>();
	
	public GeneticAlgorithm(int populationSize, int maxGenerations, int maxGenerationWithSameGenome,
			double crossoverRate, double mutationRate, int tournamentSize, int eliteGroupSize,
			InitialPopulationFactory initPopulationFactory) {
		super();
		this.populationSize = populationSize;
		this.maxGenerations = maxGenerations;
		this.maxGenerationWithSameGenome = maxGenerationWithSameGenome;
		this.crossoverRate = crossoverRate;
		this.mutationRate = mutationRate;
		this.tournamentSize = tournamentSize;
		this.eliteGroupSize = eliteGroupSize;
		this.initPopulationFactory = initPopulationFactory;
	}
	
	public void addEventListener(GeneticAlgorithmEventListener eventListener) {
		this.eventListeners.add(eventListener);
	}
	
	public void removeEventListener(GeneticAlgorithmEventListener eventListner) {
		this.eventListeners.remove(eventListner);
	}
	
	public Genome run(Environment env) {
		
		logger.debug("Population size={}, Max generations={}, Crossover rate={}, Mutation rate={}, "
				+ "Tournament size={}, Elite group size={}", 
				this.populationSize, this.maxGenerations, this.crossoverRate, 
				this.mutationRate, this.tournamentSize, this.eliteGroupSize);

		List<Genome> population = initPopulationFactory.create(populationSize);
		this.calculateFitness(population, env);
		population.sort(null);
		
		int generation = 0;
		int numIterationsWithSameGenome = 0;
		Genome currentBestGenome = population.get(0);

		for (var eventListener: eventListeners) 
			eventListener.onNewGeneration(generation, population);

		while (generation < maxGenerations) {
			
			if (logger.isTraceEnabled()) {
				if ( generation%100 == 0)
					logger.trace("Generation#: {}. Best fitness={}. Average={}", generation, population.get(0).getFitness(),
							population.parallelStream().mapToDouble(Genome::getFitness).average().getAsDouble());
			}
			
			List<Genome> eliteGroup = eliteGroupSize > 0 ? population.subList(0, eliteGroupSize) : new ArrayList<>();

			population = this.buildNewPopulation(population);
			this.calculateFitness(population, env);
			for (Genome elite: eliteGroup) {
				if (!population.contains(elite))
					population.add(elite);
			}
			
			population.sort(null);
			if (population.size() > populationSize)
				population = population.subList(0, populationSize);
			
			generation++;
			
			for (var eventListener: eventListeners) 
				eventListener.onNewGeneration(generation, population);
			
			if (population.get(0).equals(currentBestGenome)) {
				if ( ++numIterationsWithSameGenome > maxGenerationWithSameGenome) {
					logger.debug("Early exit because fitness does not improve after {} iterations", numIterationsWithSameGenome);
					break;
				}
			}
			else {
//				for (var eventListener: eventListeners) 
//					eventListener.onNewGeneration(generation, population);

				numIterationsWithSameGenome = 0;
				currentBestGenome = population.get(0);
			}
		}
		logger.debug("Best genome is {}", population.get(0));
		
		return population.get(0);
	}
	
	private void calculateFitness(List<Genome> population, Environment env) {
		population.parallelStream().forEach(genome->{
			genome.calculateFitness(env);
		});
	}

	private List<Genome> buildNewPopulation(List<Genome> population){
		List<Genome> population1 = select(population);		
		List<Genome> population2 = crossover(population1);
		return mutate(population2);
	}

	private List<Genome> select(List<Genome> population) {
		return IntStream.range(0,  this.populationSize).
						parallel().
						mapToObj(i->this.select(population, this.tournamentSize)).
						collect(Collectors.toCollection(ArrayList::new));
	}
	
	private Genome select( List<Genome> list, int numElements){
		var random = new Random();
		return IntStream.generate(() -> random.nextInt(list.size()))
						.distinct()
						.limit(numElements)
						.mapToObj(list::get)
						.min(genomeComparator).get();
	}

	private List<Genome> crossover(List<Genome> population1) {
		List<Genome> newPopulation = new ArrayList<>();
		Random random = new Random();
		for ( int i = 0 ; i < population1.size() && i+1 < population1.size() ; i+=2) {
			Genome parent0 = population1.get(i);
			Genome parent1 = population1.get(i+1);
			if ( !parent0.equals(parent1) && random.nextDouble() <= this.crossoverRate) {
				var children = parent0.crossover(parent1);
				newPopulation.add(children[0]);
				newPopulation.add(children[1]);
			}
			else {
				newPopulation.add(parent0);
				newPopulation.add(parent1);
			}
		}
		return newPopulation;
	}

	private List<Genome> mutate(List<Genome> population2) {
		Random random = new Random();
		for ( int i = 0 ; i < population2.size() ; i++) {
			if (random.nextDouble() <= this.mutationRate ) {
				population2.set(i, population2.get(i).mutate());
			}
		}
		return population2;		
	}

//	private Genome select(List<Genome> population) {
//		List<Genome> genomes = pickRandomElements(population, this.tournamentSize);
//		Collections.sort(genomes);
//		return genomes.get(0);
//	}
	
//////////////////////////////////////
// Unused 
//////////////////////////////////////

	@SuppressWarnings("unused")
	private List<Genome> select0(List<Genome> population) {
		List<Genome> population1 = new ArrayList<>();
		for (int i = 0 ; i < this.populationSize ; i++) {
			population1.add(this.select(population, this.tournamentSize));
		}
		return population1;
	}
	
	@SuppressWarnings("unused")
	private List<Genome> crossover_stream(List<Genome> population1){
		Random random = new Random();
		List<Genome> population2 = new ArrayList<>();// Collections.synchronizedList(new ArrayList<>());
		IntStream.range(0,  population1.size()/2).
//				parallel().
				forEach(i->{
					Genome parent0 = population1.get(2*i);
					Genome parent1 = population1.get(2*i+1);
					if ( !parent0.equals(parent1) && random.nextDouble() <= this.crossoverRate) {
						var children = parent0.crossover(parent1);
						population2.add(children[0]);
						population2.add(children[1]);
					}
					else {
						population2.add(parent0);
						population2.add(parent1);
					}

				});
				
		return population2;
		
	}

	
	@SuppressWarnings("unused")
	private List<Genome> mutate0(List<Genome> population2) {
		Random random = new Random();
		List<Genome> population3 = new ArrayList<>();
		for ( Genome genome: population2) {
			population3.add(random.nextDouble() <= this.mutationRate ? genome.mutate() : genome);
		}
		return population3;
	}


	@SuppressWarnings("unused")
	private List<Genome> mutate2(List<Genome> population2) {
		Random random = new Random();
		return population2.parallelStream().
							map(genome-> (random.nextDouble() <= this.mutationRate) ? genome.mutate() : genome).
							collect(Collectors.toCollection(ArrayList::new));
	}

	@SuppressWarnings("unused")
	private List<Genome> mutate3(List<Genome> population2) {
		Random random = new Random();
		return population2.stream().
							map(genome-> (random.nextDouble() <= this.mutationRate) ? genome.mutate() : genome).
							collect(Collectors.toCollection(ArrayList::new));
	}


	@SuppressWarnings("unused")
	private <T> List<T> pickRandomElements( List<T> list, int numElements){
		
		var random = new Random();
		return IntStream.generate(() -> random.nextInt(list.size()))
						.distinct()
						.limit(numElements)
						.mapToObj(list::get)
						.collect(Collectors.toList());
		
	}
	
	
}
