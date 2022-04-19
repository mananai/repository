package gridsearch;

import static java.util.Comparator.comparingDouble;

import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import executorservice.CommonExecutorService;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridSearch<T> {
	
	Logger logger = LoggerFactory.getLogger(GridSearch.class);

	
	private final static int DEQUE_CAPACITY = 100_000;

	public SortedSet<Pair<T, Double>> run(Grid<T> grid, Predicate<T> constraint,
			ToDoubleFunction<T> objective, int numResults){
		
		final int numConsumers = Runtime.getRuntime().availableProcessors()-2;
		int[] parameterCounts = {0};
	
		BlockingDeque<Optional<T>> deque = new LinkedBlockingDeque<>(DEQUE_CAPACITY);
		List<Future<?>> futures = new ArrayList<>();
		
		futures.add(CommonExecutorService.getExecutorService().submit(()->{
			grid.forEach(p->{
				if (constraint.test(p)) {
					try {
						deque.putLast(Optional.of(p));
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					parameterCounts[0]++;
				}
			});
			
			for ( int i=0 ; i< numConsumers ; i++ ){
				Optional<T> stop = Optional.empty();
				try {
					deque.putLast(stop);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			};
		}));
		
		for (int i = 0 ; i < numConsumers ; i++){
			futures.add( CommonExecutorService.getExecutorService().submit(()->{
				BoundTreeSet<Pair<T, Double>> boundSet = new BoundTreeSet<>(comparingDouble(Pair::getValue), numResults);
				
				int processedCount=0;
				while(true){
					Optional<T> optionalParams;
					try {
						optionalParams = deque.takeFirst();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					if (optionalParams.isPresent()) {
						T params = optionalParams.get();
						double cost = objective.applyAsDouble(params);
						boundSet.add(new ImmutablePair<T, Double>(params, cost));
					}
					else
						break;
					
				}
				logger.debug("Consumer has processed %,d parameters", processedCount);
				return boundSet;
			}));
		};
		
		TreeSet<Pair<T, Double>> combinedSet = new TreeSet<>(comparingDouble(Pair::getValue));
		futures.forEach(f->
		{
			Object result = null;
			try {
				result = f.get();
			} catch (InterruptedException | ExecutionException e1) {
				throw new RuntimeException(e1);
			}
			if (result instanceof TreeSet) {
				combinedSet.addAll((TreeSet<Pair<T, Double>>) result);
			}
		});
		TreeSet<Pair<T, Double>> resultSet = new TreeSet<>(comparingDouble(Pair::getValue));//e->e.getValue()));
		var iterator = combinedSet.iterator();
		for (int i=0 ; i<numResults ; i++) {
			if ( iterator.hasNext() )
				resultSet.add(iterator.next());
			else
				break;
		}

		return resultSet;
	}
}
