package org.zabuzard.closy.external;

/**
 * Interface for a metric defined on a given type of objects.
 *
 * @param <E> The type of objects the metric operates on
 *
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
@SuppressWarnings("InterfaceNeverImplemented")
@FunctionalInterface
public interface Metric<E> {
	/**
	 * Computes the distance between the two given objects accordingly to the
	 * implementing metric.
	 * <p>
	 * The distance must satisfy the following properties:
	 * <ul>
	 *     <li><b>non-negativity</b> - must be greater equals {@code 0}</li>
	 *     <li><b>identity-of-indiscernibles</b> - if the distance is {@code 0},
	 *     the elements must be equal, according to their {@link E#equals(Object)}</li>
	 *     <li><b>symmetry</b> - {@code distance(a, b)} must be equals to {@code distance(b, a)}</li>
	 *     <li><b>triangle inequality</b> - {@code distance(a, c)} must be less equals
	 *     {@code distance(x, b) + distance(b, c)}</li>
	 * </ul>
	 *
	 * @param first  The first object
	 * @param second The second object
	 *
	 * @return The distance between the two given objects
	 */
	double distance(E first, E second);
}