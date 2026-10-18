import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the actor costar graph from data files.
 * Vertices: actor names
 * Edge labels: Set<String> of movie names
 *
 * @author Darhian Francisco
 * Dartmouth CS 10, Winter 2026
 */
public class BaconGraphBuilder {
    /**
     * Builds the costar graph from given files.
     */
    public static Graph<String, Set<String>> buildGraph(
            String actorsFile,
            String moviesFile,
            String movieActorsFile) throws IOException {
        // Graph where vertices are actor names
        // and edges are labeled with movie title sets
        Graph<String, Set<String>> graph = new AdjacencyMapGraph<>();

        // Maps to translate IDs to names
        Map<String, String> actorIDtoName = new HashMap<>();
        Map<String, String> movieIDtoName = new HashMap<>();

        // Map from movieID to all actors in that movie
        Map<String, Set<String>> movieToActors = new HashMap<>();

        // ----- Read actors file -----
        BufferedReader actorsReader =
                new BufferedReader(new FileReader(actorsFile));
        String line;

        while ((line = actorsReader.readLine()) != null) {
            // Split line on pipe character
            String[] parts = line.split("\\|");

            String actorID = parts[0];
            String actorName = parts[1];

            // Store ID → name mapping
            actorIDtoName.put(actorID, actorName);

            // Add actor as vertex in graph
            graph.insertVertex(actorName);
        }
        actorsReader.close();

        // ----- Read movies file -----
        BufferedReader moviesReader =
                new BufferedReader(new FileReader(moviesFile));

        while ((line = moviesReader.readLine()) != null) {
            String[] parts = line.split("\\|");

            // Store movie ID → movie name
            movieIDtoName.put(parts[0], parts[1]);
        }
        moviesReader.close();

        // ----- Read movie-actors file -----
        BufferedReader maReader =
                new BufferedReader(new FileReader(movieActorsFile));

        while ((line = maReader.readLine()) != null) {
            String[] parts = line.split("\\|");

            String movieID = parts[0];
            String actorID = parts[1];

            // Ensure movie has a set initialized
            movieToActors.putIfAbsent(movieID, new HashSet<>());

            // Add actor name to that movie's actor set
            movieToActors.get(movieID)
                    .add(actorIDtoName.get(actorID));
        }
        maReader.close();

        // ----- Create costar edges -----
        for (String movieID : movieToActors.keySet()) {
            String movieName = movieIDtoName.get(movieID);

            // Convert set to list for pair iteration
            List<String> actors =
                    new ArrayList<>(movieToActors.get(movieID));

            // Connect every pair of actors in this movie
            for (int i = 0; i < actors.size(); i++) {
                for (int j = i + 1; j < actors.size(); j++) {
                    String actor1 = actors.get(i);
                    String actor2 = actors.get(j);

                    // If edge does not yet exist, create it
                    if (!graph.hasEdge(actor1, actor2)) {
                        graph.insertUndirected(actor1,
                                actor2,
                                new HashSet<>());
                    }

                    // Add movie title to both directions
                    graph.getLabel(actor1, actor2)
                            .add(movieName);
                    graph.getLabel(actor2, actor1)
                            .add(movieName);
                }
            }
        }

        return graph;
    }
}
