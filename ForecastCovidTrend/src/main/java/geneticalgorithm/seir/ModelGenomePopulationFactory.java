package geneticalgorithm.seir;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import geneticalgorithm.Genome;
import geneticalgorithm.InitialPopulationFactory;

public class ModelGenomePopulationFactory implements InitialPopulationFactory {
	private static final int GENOME_SIZE = 7;

	@Override
	public List<Genome> create(int size) {
		
		return IntStream.range(0,  size).
						parallel().mapToObj(i->{
							byte[] bytes = new byte[GENOME_SIZE];
							new Random().nextBytes(bytes);
							return new ModelParameterGenome(bytes);
						}).
						collect(Collectors.toCollection(ArrayList::new));
	}

}
