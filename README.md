# Kevin Bacon Graph

A command-line Kevin Bacon game implemented in Java for Dartmouth CS 10.
The program models actors as vertices and connects two actors when they
appeared in the same movie. It can find shortest actor-to-actor paths,
inspect disconnected actors, and compare how centrally connected actors are.

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
- The Dartmouth CS 10 graph support classes `Graph.java` and
  `AdjacencyMapGraph.java`, which this project uses but does not include
- Three compatible input files in the program's working directory:
  `actors.txt`, `movies.txt`, and `movie-actors.txt`

The data files and course support library are not included in this repository.
Obtain them from the corresponding course materials and keep the graph support
classes alongside these Java files when compiling.

Each input file is pipe-delimited:

| File | Record format |
| --- | --- |
| `actors.txt` | `actorID|actorName` |
| `movies.txt` | `movieID|movieTitle` |
| `movie-actors.txt` | `movieID|actorID` |

## Build and run

From the directory containing these files, the two graph support classes, and
the three data files, compile and run:

```sh
javac Graph.java AdjacencyMapGraph.java BaconGraphBuilder.java GraphLibrary.java BaconGame.java
java BaconGame
```

`BaconGame` loads the three files by their names from the current working
directory and initially sets Kevin Bacon as the center. If your dataset uses a
different default actor, change the `initialCenter` argument in `main`.

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
