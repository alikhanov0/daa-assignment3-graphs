package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mutable adjacency-list multigraph. Algorithms do not mutate their input. */
public final class Graph {
    public record Edge(int from, int to, int weight) { }
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges = new ArrayList<>();
    private final int[] indegree;

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.directed = directed;
        adjacency = new ArrayList<>(n);
        indegree = new int[n];
        for (int v = 0; v < n; v++) adjacency.add(new ArrayList<>());
    }
    public int n() { return adjacency.size(); }
    public int m() { return edges.size(); }
    public boolean directed() { return directed; }
    public void checkVertex(int v) {
        if (v < 0 || v >= n()) throw new IllegalArgumentException("Invalid vertex: " + v);
    }
    public void addEdge(int u, int v, int weight) {
        checkVertex(u); checkVertex(v);
        Edge e = new Edge(u, v, weight);
        edges.add(e);
        adjacency.get(u).add(e);
        indegree[v]++;
        if (!directed) {
            adjacency.get(v).add(new Edge(v, u, weight));
            indegree[u]++;
        }
    }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> outgoing(int v) { checkVertex(v); return Collections.unmodifiableList(adjacency.get(v)); }
    public int[] indegrees() { return indegree.clone(); }
    public Graph reversed() {
        Graph reverse = new Graph(n(), directed);
        for (Edge e : edges) reverse.addEdge(e.to(), e.from(), e.weight());
        return reverse;
    }
    public void requireDirected() {
        if (!directed) throw new IllegalArgumentException("A directed graph is required");
    }
    public void requireUndirected() {
        if (directed) throw new IllegalArgumentException("An undirected graph is required");
    }

    /** Strict format: one header, exactly m edge lines, no silently ignored data. */
    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = reader.readLine();
            if (header == null) throw new IllegalArgumentException("Missing graph header");
            String[] fields = header.trim().split("\\s+");
            if (fields.length != 3 || !(fields[2].equals("directed") || fields[2].equals("undirected")))
                throw new IllegalArgumentException("Expected: n m directed|undirected");
            int n = Integer.parseInt(fields[0]), m = Integer.parseInt(fields[1]);
            if (m < 0) throw new IllegalArgumentException("Negative edge count");
            Graph g = new Graph(n, fields[2].equals("directed"));
            for (int i = 0; i < m; i++) {
                String line = reader.readLine();
                if (line == null) throw new IllegalArgumentException("Missing edge " + (i + 1));
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 3) throw new IllegalArgumentException("Malformed edge " + (i + 1));
                g.addEdge(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
            String line;
            while ((line = reader.readLine()) != null)
                if (!line.isBlank()) throw new IllegalArgumentException("Extra data after declared edges");
            return g;
        }
    }
}
