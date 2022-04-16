package geneticalgorithm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class HistoricalWinnersKeeper implements GeneticAlgorithmEventListener {
	
	private List<Pair<Integer,Genome>> historicalWinners = new ArrayList<>();
	private Genome previousWinner = null;

	@Override
	public void onNewGeneration(int generation, List<Genome> genomes) {
		Genome currentWinner = genomes.get(0);
		if (!currentWinner.equals(previousWinner)) {		
			historicalWinners.add( new ImmutablePair<Integer, Genome>(generation, currentWinner));
			previousWinner = currentWinner;
		}
	}

	public List<Pair<Integer, Genome>> getHistoricalWinners() {
		return historicalWinners;
	}
}
