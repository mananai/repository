package gridsearch;

import static java.lang.System.out;
import static java.util.Comparator.comparingDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

/**
 * 
 * @author Mananai Saengsuwan
 *
 * @param <T>
 */
public class GridSearch<T> {
	
	private final static int DEQUE_CAPACITY = 100_000;

	public SortedSet<Map.Entry<T, Double>> run(Grid<T> grid, Predicate<T> constraint,
			ToDoubleFunction<T> objective, int numResults){
		
		long start = System.currentTimeMillis();
		
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
				BoundTreeSet<Map.Entry<T, Double>> boundSet = new BoundTreeSet<>(comparingDouble(Map.Entry::getValue), numResults);
				
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
						boundSet.add(Map.entry(params, cost));
					}
					else
						break;
					
				}
				return boundSet;
			}));
		};
		
		TreeSet<Map.Entry<T, Double>> combinedSet = new TreeSet<>(comparingDouble(Map.Entry::getValue));
		futures.forEach(f->
		{
			Object result = null;
			try {
				result = f.get();
			} catch (InterruptedException | ExecutionException e1) {
				throw new RuntimeException(e1);
			}
			if (result instanceof TreeSet) {
				combinedSet.addAll((TreeSet<Map.Entry<T, Double>>) result);
			}
		});
		TreeSet<Map.Entry<T, Double>> resultSet = new TreeSet<>(comparingDouble(Map.Entry::getValue));//e->e.getValue()));
		var iterator = combinedSet.iterator();
		for (int i=0 ; i<numResults ; i++) {
			if ( iterator.hasNext() )
				resultSet.add(iterator.next());
			else
				break;
		}
		long end = System.currentTimeMillis();
		out.printf("Finished grid search. Took %d seconds to search %,d parameters. Found %,d result.\n", 
				(end-start)/1000, parameterCounts[0], resultSet.size());

		return resultSet;
	}
}
