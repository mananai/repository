package geneticalgorithm;

import java.util.List;

public interface InitialPopulationFactory {
	List<Genome> create(int size);
}
