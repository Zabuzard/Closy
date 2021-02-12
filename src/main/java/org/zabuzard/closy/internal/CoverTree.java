package org.zabuzard.closy.internal;

import org.zabuzard.closy.external.Metric;
import org.zabuzard.closy.external.NearestNeighborComputation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of a Cover-Tree (see
 * <a href="https://en.wikipedia.org/wiki/Cover_tree">Wikipedia</a>) which
 * solves nearest neighbor computation queries.<br>
 * <br>
 * The implementation is based on the paper:
 * <ul>
 * <li><a href="https://dl.acm.org/citation.cfm?id=1143857">Cover Trees for
 * Nearest Neighbor</a> - Beygelzimer et al. in {@code ICML '06}</li>
 * </ul>
 * Modified version from
 * <a href="https://github.com/loehndorf/covertree">GitHub: Loehndorf -
 * CoverTree</a>.
 *
 * @param <E> Type of the objects contained in the tree
 *
 * @author Nils Loehndorf
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
public final class CoverTree<E> implements NearestNeighborComputation<E> {
	/**
	 * The default base to use by the tree.
	 */
	private static final double DEFAULT_BASE = 1.2;
	/**
	 * The default maximal numbers of levels.
	 */
	private static final int DEFAULT_MAX_NUM_LEVELS = 500;
	/**
	 * The default minimum number of levels.
	 */
	private static final int DEFAULT_MIN_NUM_LEVELS = -500;

	/**
	 * Utility method to create a new list instance. Can be used to exchange the
	 * list type used by the tree.
	 *
	 * @param <T> Type of the elements contained in the list
	 *
	 * @return The created list instance
	 */
	private static <T> List<T> createList() {
		return new ArrayList<>();
	}

	/**
	 * Lock used for synchronization.
	 */
	private final Object lock = new Object();
	/**
	 * The base of the tree.
	 */
	private final double base;
	/**
	 * The metric to use for determining distance between elements.
	 */
	private final Metric<? super E> metric;
	/**
	 * The current number of levels of the tree.
	 */
	private final int[] numLevels;
	/**
	 * The current maximal level of the tree.
	 */
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private int maxLevel;
	/**
	 * The maximal minimum level of the tree.
	 */
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private int maxMinLevel;
	/**
	 * The maximal amount of levels of the tree.
	 */
	private int maxNumLevels = CoverTree.DEFAULT_MAX_NUM_LEVELS;
	/**
	 * The current minimal level of the tree.
	 */
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private int minLevel;
	/**
	 * The minimum number of levels of the tree.
	 */
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private int minNumLevels = CoverTree.DEFAULT_MIN_NUM_LEVELS;
	/**
	 * The root node.
	 */
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private Node<E> rootNode;

	/**
	 * Create an initially empty cover tree at level {@code 0} which
	 * automatically expands above and below.
	 *
	 * @param base   The base of the tree
	 * @param metric The metric to use for determining distance between elements
	 */
	@SuppressWarnings("WeakerAccess")
	public CoverTree(final double base, final Metric<? super E> metric) {
		this.metric = metric;
		maxMinLevel = Integer.MIN_VALUE;
		numLevels = new int[maxNumLevels - minNumLevels];
		this.base = base;
	}

	/**
	 * Create an initially empty cover tree which stops increasing the minimum
	 * level as soon as the given number of nodes is reached.
	 *
	 * @param base        The base of the tree
	 * @param maxMinLevel The maximal minimum level of the tree
	 * @param metric      The metric to use for determining distance between
	 *                    elements
	 */
	@SuppressWarnings("unused")
	public CoverTree(final double base, final int maxMinLevel, final Metric<? super E> metric) {
		this.metric = metric;
		this.base = base;
		this.maxMinLevel = maxMinLevel;
		if (maxMinLevel > 0) {
			maxLevel = maxMinLevel;
			minLevel = maxMinLevel;
		}
		numLevels = new int[maxNumLevels - minNumLevels];
	}

	/**
	 * Create an initially empty cover tree at level {@code 0} which
	 * automatically expands above and below.
	 *
	 * @param metric The metric to use for determining distance between elements
	 */
	public CoverTree(final Metric<? super E> metric) {
		this(CoverTree.DEFAULT_BASE, metric);
	}

	/**
	 * Get the cover of the given level. All points at this level are guaranteed
	 * to be {@code 2^level} apart from one another.
	 *
	 * @param level The level to get the cover of
	 *
	 * @return The cover at the given level
	 */
	@SuppressWarnings("unused")
	public List<E> getCover(final int level) {
		List<Node<E>> coverset = CoverTree.createList();
		coverset.add(rootNode);

		for (int currentLevel = maxLevel; currentLevel > level; currentLevel--) {
			final List<Node<E>> nextCoverset = CoverTree.createList();
			for (final Node<E> node : coverset) {
				nextCoverset.addAll(node.getChildren());
			}
			coverset = nextCoverset;
		}

		final List<E> cover = CoverTree.createList();
		for (final Node<E> node : coverset) {
			cover.add(node.getElement());
		}

		return cover;
	}

	/**
	 * Gets at least {@code numCenters} centers which are maximally apart from
	 * each other. All remaining centers are removed from the tree.<br>
	 * <br>
	 * This function only works as designed when the function
	 * {@link #add(Object, int)} has been used before to add elements to the
	 * tree. Otherwise, it will return the cover one level above the bottom most
	 * level of the tree.
	 *
	 * @param numCenters The number of centers to get
	 *
	 * @return At least {@code numCenters} centers which are maximally apart from
	 * each other
	 */
	@SuppressWarnings("unused")
	public List<E> getKCenters(final int numCenters) {
		final List<Node<E>> coverset = removeNodes(numCenters);
		// create cover
		final List<E> cover = CoverTree.createList();
		for (final Node<E> node : coverset) {
			cover.add(node.getElement());
		}
		return cover;

	}

	@SuppressWarnings("unused")
	@Override
	public Collection<E> getKNearestNeighbors(final E point, final int k) {
		synchronized (lock) {
			if (size() == 0 || k == 0) {
				return Collections.emptyList();
			}

			final PriorityQueue<Double> minDistances =
					new PriorityQueue<>(k, Comparator.<Double>naturalOrder().reversed());

			final List<Node<E>> candidates = CoverTree.createList();
			candidates.add(rootNode);
			rootNode.setDistance(distance(rootNode, point));
			minDistances.add(rootNode.getDistance());
			for (int level = maxLevel; level > minLevel; level--) {
				final List<Node<E>> nextCandidates = CoverTree.createList();
				for (final Node<E> candidate : candidates) {
					for (final Node<E> child : candidate.getChildren()) {
						// Do not compute distances twice
						if (areAtDifferentLocations(candidate, child)) {
							child.setDistance(distance(child, point));
							// Remember the element if not collected enough already or better
							// than current greatest minimal distance
							if (minDistances.size() < k) {
								minDistances.add(child.getDistance());
							} else if (child.getDistance() < Objects.requireNonNull(minDistances.peek())) {
								// Throw away current greatest minimal distance to make place for
								// the closer child
								minDistances.remove();
								minDistances.add(child.getDistance());
							}
						} else {
							child.setDistance(candidate.getDistance());
						}
						nextCandidates.add(child);
					}
				}

				candidates.clear();

				// Create a set of nearest neighbor candidates
				final double greatestMinDist = Objects.requireNonNull(minDistances.peek());
				for (final Node<E> nextCandidate : nextCandidates) {
					if (nextCandidate.getDistance() <= greatestMinDist + Math.pow(base, level)) {
						candidates.add(nextCandidate);
					}
				}
			}

			// Check the remaining candidates and transform to the elements
			final double greatestMinDist = Objects.requireNonNull(minDistances.peek());
			return candidates.stream()
			                 .filter(candidate -> candidate.getDistance() <= greatestMinDist)
			                 .sorted()
			                 .map(Node::getElement)
			                 .collect(Collectors.toList());
		}
	}

	@SuppressWarnings("unused")
	@Override
	public Optional<E> getNearestNeighbor(final E point) {
		synchronized (lock) {
			if (size() == 0) {
				return Optional.empty();
			}

			final List<Node<E>> candidates = CoverTree.createList();
			candidates.add(rootNode);
			double minDist = distance(rootNode, point);
			rootNode.setDistance(minDist);
			for (int level = maxLevel; level > minLevel; level--) {
				final List<Node<E>> nextCandidates = CoverTree.createList();
				for (final Node<E> candidate : candidates) {
					for (final Node<E> child : candidate.getChildren()) {
						// Do not compute distances twice
						if (areAtDifferentLocations(candidate, child)) {
							child.setDistance(distance(child, point));
							// The minimum distance can be recorded here
							if (child.getDistance() < minDist) {
								minDist = child.getDistance();
							}
						} else {
							child.setDistance(candidate.getDistance());
						}
						nextCandidates.add(child);
					}
				}

				candidates.clear();

				// Create a set of nearest neighbor candidates
				for (final Node<E> nextCandidate : nextCandidates) {
					if (nextCandidate.getDistance() <= minDist + Math.pow(base, level)) {
						candidates.add(nextCandidate);
					}
				}
			}

			for (final Node<E> candidate : candidates) {
				//noinspection FloatingPointEquality
				if (candidate.getDistance() == minDist) {
					return Optional.of(candidate.getElement());
				}
			}

			return Optional.empty();
		}
	}

	@SuppressWarnings("unused")
	@Override
	public Collection<E> getNeighborhood(final E point, final double range) {
		synchronized (lock) {
			if (size() == 0) {
				return Collections.emptyList();
			}

			final List<Node<E>> candidates = CoverTree.createList();
			candidates.add(rootNode);
			rootNode.setDistance(distance(rootNode, point));
			for (int level = maxLevel; level > minLevel; level--) {
				final List<Node<E>> nextCandidates = CoverTree.createList();
				for (final Node<E> candidate : candidates) {
					for (final Node<E> child : candidate.getChildren()) {
						// Do not compute distances twice
						if (areAtDifferentLocations(candidate, child)) {
							child.setDistance(distance(child, point));
						} else {
							child.setDistance(candidate.getDistance());
						}
						nextCandidates.add(child);
					}
				}

				candidates.clear();

				// Create a set of nearest neighbor candidates
				for (final Node<E> nextCandidate : nextCandidates) {
					if (nextCandidate.getDistance() <= range + Math.pow(base, level)) {
						candidates.add(nextCandidate);
					}
				}
			}

			// Check the remaining candidates and transform to the elements
			return candidates.stream()
			                 .filter(candidate -> candidate.getDistance() <= range)
			                 .map(Node::getElement)
			                 .collect(Collectors.toList());
		}
	}

	@Override
	public boolean add(final E element) {
		synchronized (lock) {
			// If this is the first node make it the root node
			if (rootNode == null) {
				rootNode = new Node<>(null, element);
				incNodes(maxLevel);
				return true;
			}

			// Do not add if the new node is identical to the root node
			rootNode.setDistance(distance(rootNode, element));
			if (rootNode.getDistance() == 0.0) {
				return false;
			}

			// If the node lies outside the cover of the root node and its descendants
			// then insert the node above the root node
			if (rootNode.getDistance() > Math.pow(base, maxLevel + 1)) {
				insertAtRoot(element);
				return true;
			}

			// Usually insertion begins here
			List<Node<E>> coverset = CoverTree.createList();
			// The initial cover-set contains only the root node
			coverset.add(rootNode);
			int level = maxLevel;
			// The root node does not have a parent
			Node<E> parent = null;
			int parentLevel = maxLevel;
			while (true) {
				boolean parentFound = true;
				final List<Node<E>> candidates = CoverTree.createList();
				for (final Node<E> node : coverset) {
					for (final Node<E> child : node.getChildren()) {
						if (areAtDifferentLocations(node, child)) {
							// Do not compute distance twice
							child.setDistance(distance(child, element));
							// Do not add if node is already contained in the tree
							if (child.getDistance() == 0.0) {
								return false;
							}
						} else {
							child.setDistance(node.getDistance());
						}

						if (child.getDistance() <= Math.pow(base, level)) {
							candidates.add(child);
							parentFound = false;
						}
					}
				}

				// If the children of the cover-set are further away the 2^level then an
				// element of the cover-set is the parent of the new node
				if (parentFound) {
					break;
				}

				// Select one node of the cover-set as the parent of the node
				for (final Node<E> node : coverset) {
					if (node.getDistance() <= Math.pow(base, level)) {
						parent = node;
						parentLevel = level;
						break;
					}
				}
				// Set all nodes as the new cover-set
				level--;
				coverset = candidates;
			}

			// If the point is a sibling of the root node, then the cover of the root
			// node is increased
			if (parent == null) {
				insertAtRoot(element);
				return true;
			}

			if (parentLevel - 1 < minLevel) {
				// If the maximum size is reached and this would only increase the depth
				// of the tree then stop
				if (parentLevel - 1 < maxMinLevel) {
					return false;
				}
				minLevel = parentLevel - 1;
			}

			// Otherwise add child to the tree
			final Node<E> newNode = new Node<>(parent, element);
			parent.addChild(newNode);
			// Record distance to parent node and add to the sorted set of nodes where
			// distance is used for sorting (needed for removal)
			incNodes(parentLevel - 1);
			return true;
		}
	}

	/**
	 * Returns the maximum level of this tree.
	 *
	 * @return The maximum level
	 */
	@SuppressWarnings("unused")
	public int getMaxLevel() {
		return maxLevel;
	}

	/**
	 * Returns the minimum level of this tree.
	 *
	 * @return The minimum level
	 */
	@SuppressWarnings("unused")
	public int getMinLevel() {
		return minLevel;
	}

	/**
	 * Set the maximal levels of the cover tree by defining the maximum exponent
	 * of the base.
	 *
	 * @param max The maximum exponent to set
	 */
	@SuppressWarnings("unused")
	public void setMaxNumLevels(final int max) {
		maxNumLevels = max;
	}

	/**
	 * Set the minimum levels of the cover tree by defining the minimum exponent
	 * of the base.
	 *
	 * @param min The minimum exponent to set
	 */
	@SuppressWarnings("unused")
	public void setMinNumLevels(final int min) {
		minNumLevels = min;
	}

	@Override
	public int size() {
		return size(minLevel);
	}

	/**
	 * Returns the size of the cover tree up to the given level (inclusive).
	 *
	 * @param level The level to get the size to
	 *
	 * @return The size of the tree up to the given level (inclusive)
	 */
	@SuppressWarnings("WeakerAccess")
	public int size(final int level) {
		int sum = 0;
		for (int i = maxLevel; i >= level; i--) {
			sum += numLevels[i - minNumLevels];
		}
		return sum;
	}

	/**
	 * Insert the given element into the tree.<br>
	 * <br>
	 * If the tree size is greater than {@code level} the lowest cover will be
	 * removed as long as it does not decrease tree size below {@code level}.
	 *
	 * @param element The element to insert
	 * @param level   The level
	 *
	 * @return If the element was added
	 */
	@SuppressWarnings("unused")
	private boolean add(final E element, final int level) {
		final boolean inserted = add(element);
		// only do this if there are more than two levels
		if (maxLevel - minLevel > 2) {
			// remove lowest cover if the cover before has a sufficient number of
			// nodes
			if (size(minLevel + 1) >= level) {
				removeLowestCover();
				// do not accept new nodes at the minimum level
				maxMinLevel = minLevel + 1;
			}
			// remove redundant nodes from the minimum level
			if (size(minLevel) >= 2 * level) {
				removeNodes(level);
			}
		}
		return inserted;
	}

	/**
	 * Gets the maximal level of the cover tree.
	 *
	 * @return The maximum exponent of the base
	 */
	private int getMaxNumLevels() {
		return maxNumLevels;
	}

	/**
	 * Gets the minimum level of the cover tree.
	 *
	 * @return The minimum exponent of the base
	 */
	private int getMinNumLevels() {
		return minNumLevels;
	}

	/**
	 * Returns whether two elements are at different locations.
	 *
	 * @param first  The first element
	 * @param second The second element
	 *
	 * @return {@code True} if both elements are at different locations,
	 * {@code false} otherwise
	 */
	private boolean areAtDifferentLocations(final E first, final E second) {
		return metric.distance(first, second) != 0.0;
	}

	/**
	 * Returns whether the elements contained in the two given nodes are at different
	 * locations.
	 *
	 * @param first  The node containing the first element
	 * @param second The node containing the first element
	 *
	 * @return {@code True} if both elements are at different locations,
	 * {@code false} otherwise
	 */
	private boolean areAtDifferentLocations(final Node<? extends E> first, final Node<? extends E> second) {
		return areAtDifferentLocations(first.getElement(), second.getElement());
	}

	/**
	 * Decreases the number of nodes at the given level.
	 *
	 * @param level The level to decrease nodes at
	 */
	private void decNodes(final int level) {
		numLevels[level - minNumLevels]--;
	}

	/**
	 * Computes the distance between the given elements using the set metric.
	 *
	 * @param first  The first element
	 * @param second The second element
	 *
	 * @return The distance between the given elements according to the set metric
	 */
	private double distance(final E first, final E second) {
		return metric.distance(first, second);
	}

	/**
	 * Computes the distance between the given elements using the set metric.
	 *
	 * @param first  The node containing the first element
	 * @param second The second element
	 *
	 * @return The distance between the given elements according to the set metric
	 */
	private double distance(final Node<? extends E> first, final E second) {
		return distance(first.getElement(), second);
	}

	/**
	 * Computes the distance between the elements contained in the given nodes
	 * using the set metric.
	 *
	 * @param first  The node containing the first element
	 * @param second The node containing the second element
	 *
	 * @return The distance between the given elements according to the set metric
	 */
	private double distance(final Node<? extends E> first, final Node<? extends E> second) {
		return distance(first.getElement(), second.getElement());
	}

	/**
	 * Increases the number of nodes at the given level.
	 *
	 * @param level The level to increase nodes at
	 */
	private void incNodes(final int level) {
		numLevels[level - minNumLevels]++;
	}

	/**
	 * Inserts the given element at the root node.
	 *
	 * @param element The element to insert
	 */
	private void insertAtRoot(final E element) {
		// Inserts the point above the root by successively increasing the cover of
		// the root node until it contains the new point, the old root is added as
		// child of the new root
		final Node<E> oldRoot = rootNode;
		final double dist = distance(oldRoot, element);
		while (dist > Math.pow(base, maxLevel)) {
			final Node<E> nextRoot = new Node<>(null, rootNode.getElement());
			rootNode.setParent(nextRoot);
			nextRoot.addChild(rootNode);
			rootNode = nextRoot;
			decNodes(maxLevel);
			maxLevel++;
			incNodes(maxLevel);
		}
		final Node<E> nextNode = new Node<>(rootNode, element);
		rootNode.addChild(nextNode);
		incNodes(maxLevel - 1);
	}

	/**
	 * Removes the the cover at the lowest level of the tree.
	 */
	private void removeLowestCover() {
		List<Node<E>> coverset = CoverTree.createList();
		coverset.add(rootNode);
		for (int level = maxLevel; level > minLevel + 1; level--) {
			final List<Node<E>> nextCoverset = CoverTree.createList();
			for (final Node<E> node : coverset) {
				nextCoverset.addAll(node.getChildren());
			}
			coverset = nextCoverset;
		}
		for (final Node<E> node : coverset) {
			node.removeChildren();
		}

		minLevel++;
	}

	/**
	 * Removes all but {@code numCenters} elements.
	 *
	 * @param numCenters The amount of elements to keep
	 *
	 * @return The cover-set
	 */
	private List<Node<E>> removeNodes(final int numCenters) {
		synchronized (lock) {
			List<Node<E>> coverset = CoverTree.createList();
			coverset.add(rootNode);
			for (int level = maxLevel; level > minLevel + 1; level--) {
				final List<Node<E>> nextCoverset = CoverTree.createList();
				for (final Node<E> node : coverset) {
					nextCoverset.addAll(node.getChildren());
				}
				coverset = nextCoverset;
			}

			final int missing = numCenters - coverset.size();
			if (missing < 0) {
				throw new AssertionError("Negative missing=" + missing + " in coverset");
			}

			// Successively pick the node with the largest distance to the cover-set and
			// add it to the cover-set
			final LinkedList<Node<E>> candidates = new LinkedList<>();
			for (final Node<E> node : coverset) {
				for (final Node<E> child : node.getChildren()) {
					if (areAtDifferentLocations(node, child)) {
						candidates.add(child);
					}
				}
			}

			// Only add candidates when the cover-set is yet smaller then the number of
			// desired centers
			if (coverset.size() < numCenters) {
				// Compute the distance of all candidates to their parents and uncles
				for (final Node<E> node : candidates) {
					double minDist = Double.POSITIVE_INFINITY;
					for (final Node<E> uncle : node.getParent()
					                               .getParent()
					                               .getChildren()) {
						final double dist = distance(node, uncle);
						if (dist < minDist) {
							minDist = dist;
						}
					}
					node.setDistance(minDist);
					if (minDist == Double.POSITIVE_INFINITY) {
						throw new AssertionError("Infinite distance in k centers computation");
					}
				}

				do {
					Collections.sort(candidates);
					final Node<E> nextNode = candidates.removeLast();
					coverset.add(nextNode);
					// Update the distance of all candidates in the neighborhood of
					// the new node
					for (final Node<E> uncle : nextNode.getParent()
					                                   .getParent()
					                                   .getChildren()) {
						if (uncle != nextNode) {
							final double dist = distance(nextNode, uncle);
							if (dist < nextNode.getDistance()) {
								nextNode.setDistance(dist);
							}
						}
					}
				} while (coverset.size() < numCenters);
			}

			// Finally remove all nodes that have not been selected from the tree to
			// avoid confusing the nearest neighbor computation
			for (final Node<E> node : candidates) {
				node.getParent()
				    .removeChild(node);
				decNodes(minLevel);
			}

			return coverset;
		}
	}

}