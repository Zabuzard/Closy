package de.zabuza.closy.external;

import de.zabuza.closy.internal.CoverTree;

/**
 * Provides utility methods for nearest neighbor computations.
 * For example factory methods to create corresponding algorithms.
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class NearestNeighborComputations {
	/**
	 * Creates an object that can be used for efficient nearest neighbor computation.
	 * All operations of the object are thread-safe.
	 *
	 * @param metric The metric to operate on
	 * @param <E>    The type of elements contained in the space
	 *
	 * @return An object for nearest neighbor computation
	 */
	@SuppressWarnings("unused")
	public static <E> NearestNeighborComputation<E> of(final Metric<? super E> metric) {
		return new CoverTree<>(metric);
	}

	/**
	 * Utility class. No implementation.
	 */
	private NearestNeighborComputations() {
		throw new UnsupportedOperationException("Utility class. No implementation.");
	}
}
