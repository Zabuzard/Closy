# Closy

Closy is a simple library for efficient **nearest neighbor computations**. It is designed generic and can be used
with **any class** that defines a [metric](https://en.wikipedia.org/wiki/Metric_(mathematics)).

It is able to compute the nearest neighbor to a given point, as well as retrieving the k-nearest neighbors and also all
neighbors within a given range. The corresponding methods are:

* `Optional<E> getNearestNeighbor(E point)`
* `Collection<E> getKNearestNeighbors(E point, int k)`
* `Collection<E> getNeighborhood(E point, double range)`

# Requirements

* Requires at least **Java 9**

# Download

Maven:

```xml
<dependency>
   <groupId>io.github.zabuzard.closy</groupId>
   <artifactId>closy</artifactId>
   <version>1.2</version>
</dependency>
```

Jar downloads are available from the [release section](https://github.com/ZabuzaW/Closy/releases).

# Documentation

* **API Javadoc**: Available from the [release section](https://github.com/ZabuzaW/Closy/releases)

# Getting started

1. Integrate **Closy** into your project.
2. Create an implementation of `Metric<E>` for your custom `E` objects
3. Create an algorithm using `NearestNeighborComputations#of(Metric<E>)`
4. Add your objects to the algorithm using `NearestNeighborComputation#add(E)`
5. Execute nearest neighbor computations using the methods offered by `NearestNeighborComputation`

# Example

Consider the following simple class for points in a 2-dimensional space

```java
class Point {
    private final int x;
    private final int y;

    // constructor, getter, equals, hashCode and toString omitted
}
```

As first step, we need to define a `Metric` that operates on `Point`. We decide for
the [Euclidean distance](https://en.wikipedia.org/wiki/Euclidean_distance):

```java
class EuclideanDistance implements Metric<Point> {
	@Override
	public double distance(final Point a, final Point b) {
		return Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2));
	}
}
```

Next, we create an algorithm using this metric and then add some points to it:

```java
var metric = new EuclideanDistance();
var algo = NearestNeighborComputations.of(metric);

algo.add(Point.of(1, 2));
algo.add(Point.of(5, 7));
algo.add(Point.of(-10, 4));
algo.add(Point.of(9, 8));
algo.add(Point.of(3, 3));
```

Finally, we use the methods provided by `NearestNeighborComputation`
to compute some nearest neighbors:

```java
var point = Point.of(4, 2);
Optional<Point> neighbor = algo.getNearestNeighbor(point);          // (3, 3)
Collection<Point> neighbors = algo.getKNearestNeighbors(point, 3);  // [(3, 3), (1, 2), (5, 7)]
Collection<Point> neighborhood = algo.getNeighborhood(point, 3.5);  // [(1, 2), (3, 3)]
```

***

# Background

The current implementation uses [Cover Trees](https://en.wikipedia.org/wiki/Cover_tree), based on the implementation
described in [Cover Trees for  Nearest Neighbor](https://dl.acm.org/citation.cfm?id=1143857) (_Beygelzimer et al. in
ICML '06_).

A detailed description and benchmark can be found
in [Multi-Modal Route Planning in Road and Transit Networks](https://arxiv.org/abs/1809.05481) (_Daniel Tischner '18_).

The following is a benchmark of [Closy version 1.0](https://github.com/Zabuzard/Cobweb). The experiment consists of
continuous insertion of nodes, for each of three road networks respectively, and then measuring random nearest neighbor
queries, i.e. the execution time of the algorithm. Measurements are done for tree sizes of 1, 10000 and then in steps of

10000. Each measurement is averaged over 1000 queries using randomly selected nodes.

![Close version 1.0 benchmark](https://i.imgur.com/8qWYBG7.png)