package benchmark;

import graph.Graph;
import java.util.Random;

public final class GraphGenerator {
    public enum Kind { UNDIRECTED, DIRECTED, DAG }
    private GraphGenerator() { }
    public static Graph generate(int n, int m, Kind kind) {
        if (n < 0 || m < 0 || n < 2 && m != 0 || kind == Kind.UNDIRECTED && m < Math.max(0, n - 1))
            throw new IllegalArgumentException("Invalid generator size/density");
        Random rnd = new Random(42); // A new RNG for EACH graph, never shared between kinds.
        Graph graph = new Graph(n, kind != Kind.UNDIRECTED);
        int[] permutation = new int[n];
        for (int i = 0; i < n; i++) permutation[i] = i;
        if (kind != Kind.DIRECTED) for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1), tmp = permutation[i]; permutation[i] = permutation[j]; permutation[j] = tmp;
        }
        if (kind == Kind.UNDIRECTED) for (int i = 1; i < n; i++) {
            int a = permutation[rnd.nextInt(i)], b = permutation[i];
            graph.addEdge(a, b, 1 + rnd.nextInt(1000));
        }
        while (graph.m() < m) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            while (b == a) b = rnd.nextInt(n);
            if (kind == Kind.DAG && permutation[a] > permutation[b]) { int tmp = a; a = b; b = tmp; }
            graph.addEdge(a, b, 1 + rnd.nextInt(1000));
        }
        return graph;
    }
}
