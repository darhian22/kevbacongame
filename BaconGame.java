import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Interactive Kevin Bacon game.
 *
 * @author Darhian Francisco
 * Dartmouth CS 10, Winter 2026
 */
public class BaconGame {
    // Full costar graph (all actors + costar relationships)
    private Graph<String, Set<String>> graph;

    // BFS shortest-path tree rooted at current center
    private Graph<String, Set<String>> bfsTree;

    // Current center of universe
    private String center;

    /**
     * Constructor builds graph and sets initial center.
     */
    public BaconGame(String actorsFile,
                     String moviesFile,
                     String movieActorsFile,
                     String initialCenter) throws IOException {
        // Build costar graph from input files
        graph = BaconGraphBuilder.buildGraph(
                actorsFile, moviesFile, movieActorsFile);

        // Initialize center and compute BFS tree
        setCenter(initialCenter);
    }

    /**
     * Changes the center of the universe and rebuilds BFS tree.
     */
    private void setCenter(String newCenter) {
        // Ensure actor exists in graph
        if (!graph.hasVertex(newCenter)) {
            System.out.println("Actor not found.");
            return;
        }

        center = newCenter;

        // Recompute shortest-path tree from new center
        bfsTree = GraphLibrary.bfs(graph, center);

        // Number of reachable actors
        int connected = bfsTree.numVertices();

        // Total actors in graph
        int total = graph.numVertices();

        // Compute average separation
        double avg =
                GraphLibrary.averageSeparation(bfsTree, center);

        // Print statistics
        System.out.println(center
                + " is now the center of the universe, connected to "
                + connected + "/" + total
                + " actors with average separation "
                + String.format("%.3f", avg));
    }

    /**
     * Prints shortest path from actor to current center.
     */
    private void printPath(String actor) {
        // If actor is not reachable from center
        if (!bfsTree.hasVertex(actor)) {
            System.out.println(actor
                    + " has infinite separation.");
            return;
        }

        // Retrieve path from actor back to center
        List<String> path =
                GraphLibrary.getPath(bfsTree, actor);

        // Separation number equals path length minus root
        System.out.println(actor
                + "'s number is "
                + (path.size() - 1));

        // Print each edge along the path
        for (int i = 0; i < path.size() - 1; i++) {
            String a = path.get(i);        // current actor
            String b = path.get(i + 1);    // parent in BFS tree

            // Retrieve set of movies connecting actors
            Set<String> movies =
                    graph.getLabel(a, b);

            System.out.println(a
                    + " appeared in "
                    + movies
                    + " with "
                    + b);
        }
    }

    /**
     * Prints actors not reachable from current center.
     */
    private void printInfinite() {
        // Determine vertices not in BFS tree
        Set<String> missing =
                GraphLibrary.missingVertices(
                        graph, bfsTree);

        System.out.println(
                "Actors with infinite separation:");

        // Print each unreachable actor
        for (String actor : missing) {
            System.out.println(actor);
        }
    }

    /**
     * Prints actors whose degree is within given range.
     */
    private void printDegreeRange(int low, int high) {
        for (String actor : graph.vertices()) {
            // Degree = number of costars
            int degree = graph.outDegree(actor);

            // Print actor if degree falls in range
            if (degree >= low && degree <= high) {
                System.out.println(actor
                        + " (" + degree + ")");
            }
        }
    }

    /**
     * Prints actors whose separation is within range.
     */
    private void printSeparationRange(int low, int high) {
        for (String actor : bfsTree.vertices()) {
            // Separation equals shortest path length minus root
            int sep =
                    GraphLibrary.getPath(bfsTree, actor)
                            .size() - 1;

            if (sep >= low && sep <= high) {
                System.out.println(actor
                        + " (" + sep + ")");
            }
        }
    }

    /**
     * Lists top or bottom centers of the universe
     * ranked by average separation.
     *
     * Only considers actors in the current
     * center's connected component.
     *
     * Positive number → best centers (lowest average)
     * Negative number → worst centers (highest average)
     */
    private void printCenters(int number) {
        // Map to store actor → average separation
        Map<String, Double> averages = new HashMap<>();

        // Only consider actors reachable from current center
        // (prevents tiny disconnected components from dominating)
        for (String actor : bfsTree.vertices()) {
            // Build BFS tree rooted at this actor
            Graph<String, Set<String>> tree =
                    GraphLibrary.bfs(graph, actor);

            // Ignore trivial trees (isolated actors)
            if (tree.numVertices() <= 1) continue;

            // Compute average separation for this actor
            double avg =
                    GraphLibrary.averageSeparation(tree, actor);

            // Store result
            averages.put(actor, avg);
        }

        // Convert map entries into list for sorting
        List<Map.Entry<String, Double>> list =
                new ArrayList<>(averages.entrySet());

        // Sort actors by average separation (ascending)
        list.sort(Map.Entry.comparingByValue());

        if (number > 0) {
            // Print best centers (smallest averages)
            for (int i = 0;
                 i < Math.min(number, list.size());
                 i++) {
                System.out.println(
                        list.get(i).getKey()
                                + " ("
                                + String.format("%.3f",
                                list.get(i).getValue())
                                + ")");
            }
        } else {
            // Print worst centers (largest averages)
            for (int i = list.size() - 1;
                 i >= Math.max(list.size() + number, 0);
                 i--) {
                System.out.println(
                        list.get(i).getKey()
                                + " ("
                                + String.format("%.3f",
                                list.get(i).getValue())
                                + ")");
            }
        }
    }

    /**
     * Runs interactive command loop.
     */
    public void play() {
        Scanner input = new Scanner(System.in);

        while (true) {
            // Display command prompt
            System.out.print(center + " game > ");

            String line = input.nextLine();

            // Ignore empty input
            if (line.trim().isEmpty()) continue;

            // Quit program
            if (line.equals("q")) break;

            String[] parts = line.split(" ");

            try {
                switch (parts[0]) {
                    case "c":   // best/worst centers
                        printCenters(Integer.parseInt(parts[1]));
                        break;

                    case "u":   // change center
                        setCenter(line.substring(2));
                        break;

                    case "p":   // print path
                        printPath(line.substring(2));
                        break;

                    case "i":   // infinite separation
                        printInfinite();
                        break;

                    case "d":   // degree range
                        printDegreeRange(
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]));
                        break;

                    case "s":   // separation range
                        printSeparationRange(
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]));
                        break;

                    default:
                        System.out.println("Unknown command.");
                }
            } catch (Exception e) {
                // Handle malformed input
                System.out.println("Invalid command format.");
            }
        }

        input.close();
    }

    /**
     * Program entry point.
     */
    public static void main(String[] args)
            throws IOException {
        if (args.length != 0 && args.length != 3) {
            System.err.println(
                    "Usage: java BaconGame [actors.txt movies.txt movie-actors.txt]");
            return;
        }

        String actorsFile =
                args.length == 3 ? args[0] : "demo-data/actors.txt";
        String moviesFile =
                args.length == 3 ? args[1] : "demo-data/movies.txt";
        String movieActorsFile =
                args.length == 3 ? args[2] : "demo-data/movie-actors.txt";

        BaconGame game = new BaconGame(
                actorsFile,
                moviesFile,
                movieActorsFile,
                "Kevin Bacon");

        game.play();
    }
}
