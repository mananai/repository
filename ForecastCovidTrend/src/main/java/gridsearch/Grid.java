package gridsearch;

import java.util.function.Consumer;

/**
 * 
 * @author Mananai Saengsuwan
 *
 * @param <T>
 */
@FunctionalInterface
public interface Grid<T> {
	void forEach(Consumer<T> consumer);
}
