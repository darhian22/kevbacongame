import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Graph library containing BFS and related algorithms.
 *
 * Provides:
 *  - Breadth-first search
 *  - Shortest-path reconstruction
 *  - Missing vertex detection
 *  - Average separation calculation
 *
 * @author Darhian Francisco
 * Dartmouth CS 10, Winter 2026
 */
public class GraphLibrary {
    /**
     * Performs breadth-first search from source and
     * returns a shortest-path tree.
     *
     * Tree edges are directed child → parent.
     */
    public static <V, E> Graph<V, E> bfs(Graph<V, E> g, V source) {
        // If source is not in graph, return empty tree
        if (!g.hasVertex(source)) {
            return new AdjacencyMapGraph<>();
        }

        // Create directed graph to represent BFS tree
        Graph<V, E> tree = new AdjacencyMapGraph<>();

        // FIFO queue to manage frontier
        Queue<V> queue = new LinkedList<>();

        // Mark source as visited by inserting into tree
        tree.insertVertex(source);

        // Begin BFS from source
        queue.add(source);

        while (!queue.isEmpty()) {
            // Remove next vertex from frontier
            V u = queue.remove();

            // Explore all outgoing neighbors
            for (V v : g.outNeighbors(u)) {
                // If neighbor has not been visited
                if (!tree.hasVertex(v)) {
                    // Mark as visited
                    tree.insertVertex(v);

                    // Add edge child → parent
                    tree.insertDirected(v, u,
                            g.getLabel(u, v));

                    // Add neighbor to frontier
                    queue.add(v);
                }
            }
        }

        return tree;
    }

    /**
     * Reconstructs shortest path from vertex v
     * back to the root of the BFS tree.
     */
    public static <V, E> List<V> getPath(Graph<V, E> tree, V v) {
        List<V> path = new ArrayList<>();

        // If vertex not reachable, return empty list
        if (!tree.hasVertex(v)) {
            return path;
        }

        V current = v;

        // Add starting vertex
        path.add(current);

        // Follow parent pointers until root reached
        while (tree.outDegree(current) > 0) {
            // Each vertex has exactly one parent
            V parent =
                    tree.outNeighbors(current)
                            .iterator().next();

            path.add(parent);
            current = parent;
        }

        return path;
    }

    /**
     * Returns vertices in original graph
     * not present in subgraph (i.e., unreachable).
     */
    public static <V, E> Set<V> missingVertices(
            Graph<V, E> graph,
            Graph<V, E> subgraph) {
        Set<V> missing = new HashSet<>();

        // Check each vertex in full graph
        for (V v : graph.vertices()) {
            // If not visited in BFS tree
            if (!subgraph.hasVertex(v)) {
                missing.add(v);
            }
        }

        return missing;
    }

    /**
     * Computes average distance from root
     * in a BFS shortest-path tree.
     */
    public static <V, E> double averageSeparation(
            Graph<V, E> tree, V root) {
        // If only root exists, separation is 0
        if (tree.numVertices() <= 1) {
            return 0.0;
        }

        // Compute total depth recursively
        int totalDepth =
                totalDepth(tree, root, 0);

        // Divide by number of non-root vertices
        return (double) totalDepth
                / (tree.numVertices() - 1);
    }

    /**
     * Recursively computes total depth
     * of all vertices in tree.
     */
    private static <V, E> int totalDepth(
            Graph<V, E> tree,
            V v,
            int depth) {
        // Start with current depth
        int sum = depth;

        // Children are vertices pointing to v
        for (V child : tree.inNeighbors(v)) {
            // Recurse on each child
            sum += totalDepth(tree,
                    child,
                    depth + 1);
        }

        return sum;
    }
}
