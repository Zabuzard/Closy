package de.zabuza.closy.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Node class for a {@link CoverTree}.
 *
 * @param <E> Type of the elements contained by the node
 *
 * @author Nils Loehndorf
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 */
final class Node<E> implements Comparable<Node<E>> {
	/**
	 * The children of the node.
	 */
	private final List<Node<E>> children;
	/**
	 * The element contained in the node.
	 */
	private final E element;
	/**
	 * The current distance of the node.
	 */
	private double distance;
	/**
	 * The parent of the node.
	 */
	private Node<E> parent;

	/**
	 * Creates a new node with a given parent and element.
	 *
	 * @param parent  The parent of the node, <tt>null</tt> indicates a root node
	 * @param element The element to wrap around
	 */
	Node(final Node<E> parent, final E element) {
		this.parent = parent;
		this.element = element;
		children = new ArrayList<>();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Node<?> node = (Node<?>) obj;
		//noinspection ChainedMethodCall
		return Double.compare(node.getDistance(), getDistance()) == 0 && getChildren().equals(node.getChildren())
				&& Objects.equals(getElement(), node.getElement()) && Objects.equals(getParent(), node.getParent());
	}

	/**
	 * Compares nodes ascending based on their distance.
	 */
	@Override
	public int compareTo(final Node<E> o) {
		//noinspection CompareToUsesNonFinalVariable
		return Double.compare(distance, o.getDistance());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getChildren(), getDistance(), getElement(), getParent());
	}

	@Override
	public String toString() {
		return "Node{" + "children=" + children + ", distance=" + distance + ", element=" + element + ", parent="
				+ parent + '}';
	}

	/**
	 * Adds the given child to this node.
	 *
	 * @param child The child to add, not null
	 */
	void addChild(final Node<E> child) {
		Objects.requireNonNull(child);
		children.add(child);
	}

	/**
	 * Gets a list of all children of this node. If the node did not contain
	 * children already it will add a new child with the same element to
	 * itself.
	 *
	 * @return An unmodifiable list of all children of this node
	 */
	List<Node<E>> getChildren() {
		if (children.isEmpty()) {
			final Node<E> child = new Node<>(this, element);
			addChild(child);
		}
		return Collections.unmodifiableList(children);
	}

	/**
	 * Gets the distance of the node.
	 *
	 * @return The distance of the node
	 */
	double getDistance() {
		return distance;
	}

	/**
	 * Gets the element of the node.
	 *
	 * @return The element of the node
	 */
	E getElement() {
		return element;
	}

	/**
	 * Gets the parent of the node.
	 *
	 * @return The parent of the node, <tt>null</tt> indicates a root node
	 */
	Node<E> getParent() {
		return parent;
	}

	/**
	 * Removes the given child from the node.<br>
	 * <br>
	 * Note that this operation may take some time since the node maintains
	 * children in a list.
	 *
	 * @param child The child to remove
	 */
	void removeChild(final Node<E> child) {
		children.remove(child);
	}

	/**
	 * Removes all children of this node.
	 */
	void removeChildren() {
		children.clear();
	}

	/**
	 * Sets the distance of this node.
	 *
	 * @param distance The distance to set
	 */
	void setDistance(final double distance) {
		this.distance = distance;
	}

	/**
	 * Sets the parent of this node.
	 *
	 * @param parent The parent to set, <tt>null</tt> indicates a root node
	 */
	void setParent(final Node<E> parent) {
		this.parent = parent;
	}

}