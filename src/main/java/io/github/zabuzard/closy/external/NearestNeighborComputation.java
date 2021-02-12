package io.github.zabuzard.closy.external;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface for algorithms that solve nearest neighbor computation problems.
 *
 * @param <E> Type of the points the space consists of
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public interface NearestNeighborComputation<E> {
	/**
	 * Gets the {@code k} neighbors nearest to the given point. That are the
	 * {@code k} elements closest to the given point.
	 *
	 * @param point The point in question
	 * @param k     The amount of neighbors to get, a value of {@code 0} yields
	 *              an empty result
	 *
	 * @return The {@code k} neighbors nearest to the given point, ascending in
	 * distance to the point
	 */
	@SuppressWarnings("unused")
	Collection<E> getKNearestNeighbors(E point, int k);

	/**
	 * Gets the neighbor nearest to the given point. That is the element closest
	 * to the given point.
	 *
	 * @param point The point in question
	 *
	 * @return The neighbor nearest to the given point or {@code empty} if there
	 * is no
	 */
	@SuppressWarnings("unused")
	Optional<E> getNearestNeighbor(E point);

	/**
	 * Gets the neighborhood of the given point with the given range. That are all
	 * elements within the given range to the given point. I.e. all elements
	 * inside the ball around the point with the given range as radius.
	 *
	 * @param point The point in question
	 * @param range The range around the point, inclusive
	 *
	 * @return All elements inside the ball around the point with the given range
	 * as radius
	 */
	@SuppressWarnings("unused")
	Collection<E> getNeighborhood(E point, double range);

	/**
	 * Adds the given element to the space.
	 *
	 * @param element The element to add
	 *
	 * @return Whether the element was added, i.e. not contained already
	 */
	boolean add(E element);

	/**
	 * The current size of the space, i.e. the amount of elements contained.
	 *
	 * @return The size of the space
	 */
	int size();
}
