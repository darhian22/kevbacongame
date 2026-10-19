# Kevin Bacon Graph Explorer

This repository contains both an interactive browser-based actor connection
explorer and a Java command-line graph project. Both use the included
actor-and-film dataset to find shortest co-star paths.

## Start here

- **Try the live web explorer:** [Kevin Bacon Graph Explorer](https://darhian22.github.io/kevbacongame/)
- **Run it locally:** clone or download this repository, then follow the
  [web explorer instructions](#interactive-web-explorer). All code and data
  needed by the explorer are in this repository; no separate project is
  required.
- **Explore the Java project:** follow [Build and run](#build-and-run) below.

In the web explorer, choose two actor names from the suggestions to see their
shortest connection, the films shared along the route, and original abstract
film illustrations. The default example connects Kevin Bacon and Tom Hanks.

The browser interface was inspired by the visual approach of my separate
Connection Atlas prototype, but this explorer applies it to the actor-and-film
graph and dataset in this repository. The Java command-line game remains
available as a separate way to query the graph.

## Java graph project

The Java program models actors as vertices and connects two actors when they
appeared in the same movie. It can find shortest actor-to-actor paths, inspect
disconnected actors, and compare how centrally connected actors are.

## How it works

The project is split into three parts:

- **`BaconGraphBuilder`** reads actor, movie, and movie-cast records. It maps
  the input IDs to names, creates one vertex for every actor, and adds an
  undirected edge for each pair of actors who share a movie. Each edge is
  labeled with the set of movies shared by that pair.
- **`GraphLibrary`** provides graph algorithms independent of the movie
  domain. Its breadth-first search builds a shortest-path tree whose edges
  point from each discovered vertex to its parent. It also reconstructs paths,
  finds vertices missing from a connected subgraph, and calculates average
  separation from a root.
- **`BaconGame`** is the interactive command-line interface. It builds the
  graph, starts with Kevin Bacon as the center, and lets the user change the
  center, inspect paths and disconnected actors, and query degree, separation,
  and center rankings.

The BFS tree makes the shortest path from an actor to the current center
available by following parent edges. Average separation is the mean shortest
path length from a center to every other reachable actor; unreachable actors
are excluded from that calculation.

## Requirements

- Java 8 or newer
- The included `Graph.java` and `AdjacencyMapGraph.java` graph support classes
- The included small, fictional dataset in `demo-data/` for trying the game
- The larger CS 10 dataset at the project root for exploring the full graph

The graph support classes are from Dartmouth CS 10 and retain their original
attribution in the source files. The larger dataset is included for users who
are permitted to use and redistribute the course materials.

Each input file is pipe-delimited:

| File | Record format |
| --- | --- |
| `actors.txt` | `actorID|actorName` |
| `movies.txt` | `movieID|movieTitle` |
| `movie-actors.txt` | `movieID|actorID` |

## Build and run

From the project directory, compile and run with the included demo data:

```sh
javac Graph.java AdjacencyMapGraph.java BaconGraphBuilder.java GraphLibrary.java BaconGame.java
java BaconGame
```

The demo uses fictional actors and movie titles and starts with Kevin Bacon as
the center. To use the included full dataset, pass its three file paths in this
order:

```sh
java BaconGame actors.txt movies.txt movie-actors.txt
```

Only redistribute or reuse the course support code and dataset if permitted by
the applicable course policies.

## Interactive web explorer

The [live explorer](https://darhian22.github.io/kevbacongame/) is hosted with
GitHub Pages. To run the same app locally, start the server from the repository
root:

The web explorer includes a Kevin Bacon portrait credited under CC BY 2.0 and
original abstract SVG title-card illustrations for films on the selected path.
The illustrations are not movie posters or stills; see [`web/ASSETS.md`](web/ASSETS.md)
for attribution and asset details.

```sh
python3 -m http.server 8000
```

Then open <http://localhost:8000/web/>. The app loads `actors.txt`,
`movies.txt`, and `movie-actors.txt` from the repository root; use the
actor-name suggestions to select valid names.

## Commands

At the game prompt, enter one of the following:

| Command | Description |
| --- | --- |
| `p <actor name>` | Print the actor's shortest path to the current center and the movies shared along each step |
| `u <actor name>` | Make the named actor the new center and print connectivity and average-separation statistics |
| `i` | List actors unreachable from the current center |
| `d <low> <high>` | List actors whose number of costars is in the inclusive degree range |
| `s <low> <high>` | List reachable actors whose shortest-path separation is in the inclusive range |
| `c <n>` | List up to `n` best centers; use a negative number to list the worst centers |
| `q` | Quit |

For example, `p Tom Hanks` prints Tom Hanks's path to the current center, while
`u Tom Hanks` changes the center to Tom Hanks. The `c` ranking only considers
actors reachable from the current center's connected component.

## Complexity

Breadth-first search takes `O(V + E)` time for a graph with `V` actors and `E`
costar relationships. Building a movie's costar relationships considers every
pair of cast members, taking `O(k^2)` work for a movie with `k` listed actors.
The graph and its edge labels are held in memory.

## Screenshots

![Interactive Kevin Bacon game running against a small sample graph](screenshots/interactive-demo.png)

*This sample session demonstrates shortest paths, unreachable actors, center changes, center rankings, and clear responses to invalid commands.*

![Shortest path and center ranking results using the larger course dataset](screenshots/full-dataset-paths.png)

*Tom Hanks is one movie away from Kevin Bacon in this dataset; the session also shows changing the center and ranking actors by average separation.*

![Actors returned by the degree-range query](screenshots/degree-range.png)

*The degree query lists actors whose number of costars falls within the requested range.*

![Actors returned by the separation query and examples of unreachable actors](screenshots/separation-and-unreachable.png)

*The separation query lists actors within a chosen number of steps, while the final command displays actors disconnected from the current center.*

### Interactive web explorer

These captures show the live explorer using different actor pairs:

![Kevin Bacon to Tom Hanks: one shared-film connection](screenshots/web-one-hop.png)

*The explorer finds Tom Hanks one step from Kevin Bacon through Apollo 13 (1995).*

![Kevin Bacon to John Mills: a three-step co-star route](screenshots/web-multi-hop.png)

*The explorer traces a three-step route through Jim Cummings and Rowan Atkinson, with each shared film shown on the map and in the film cards.*
