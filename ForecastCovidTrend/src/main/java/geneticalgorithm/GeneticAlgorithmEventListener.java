package geneticalgorithm;

import java.util.List;

public interface GeneticAlgorithmEventListener {
	void onNewGeneration(int generation, List<Genome> genomes);
}
