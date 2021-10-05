package gridsearch;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * 
 * @author Mananai Saengsuwan
 *
 * @param <E>
 */
@SuppressWarnings("serial")
class BoundTreeSet<E> extends TreeSet<E> {
	private final Comparator<E> comparator;
	private final int maxSize;
	
	BoundTreeSet(Comparator<E> comparator, int maxSize) {
		super(comparator);
		this.comparator = comparator;
		this.maxSize = maxSize;
	}
	
	public boolean add(E e) {
		if ( this.size() >= maxSize) {
			if (this.comparator.compare(last(), e) < 0 ) {
				return false;
			} else {
				this.pollLast();
				return super.add(e);
			}
		}
		else {
			return super.add(e);
		}
	}
}
