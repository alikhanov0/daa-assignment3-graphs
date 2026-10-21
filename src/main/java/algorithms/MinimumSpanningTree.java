package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class MinimumSpanningTree {
    private MinimumSpanningTree() { }
    public record Result(List<Edge> edges, long weight, int components) {
        public Result { edges = List.copyOf(edges); }
    }
    public static Result kruskal(Graph graph, Metrics metrics) {
        graph.requireUndirected();
        Edge[] sorted = graph.edges().toArray(Edge[]::new);
        mergeSort(sorted, metrics);
        DisjointSetUnion dsu = new DisjointSetUnion(graph.n(), metrics);
        List<Edge> tree = new ArrayList<>();
        long total = 0;
        for (Edge edge : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            // union performs the two finds and links only different components.
            if (dsu.union(edge.from(), edge.to())) { tree.add(edge); total += edge.weight(); }
        }
        return new Result(tree, total, dsu.components());
    }
    /** Stable bottom-up mergesort: explicit comparisons, independent of JDK sort details. */
    private static void mergeSort(Edge[] edges, Metrics metrics) {
        Edge[] buffer = new Edge[edges.length];
        for (long width = 1; width < edges.length; width *= 2) {
            for (long start = 0; start < edges.length; start += 2 * width) {
                int left = (int) start, mid = (int) Math.min(start + width, edges.length);
                int right = (int) Math.min(start + 2 * width, edges.length);
                int a = left, b = mid;
                for (int k = left; k < right; k++) {
                    if (a == mid) buffer[k] = edges[b++];
                    else if (b == right) buffer[k] = edges[a++];
                    else {
                        metrics.comparisons++;
                        buffer[k] = edges[a].weight() <= edges[b].weight() ? edges[a++] : edges[b++];
                    }
                }
            }
            System.arraycopy(buffer, 0, edges, 0, edges.length);
        }
    }
    public static Result prim(Graph graph, Metrics metrics) {
        graph.requireUndirected();
        boolean[] visited = new boolean[graph.n()];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            int c = Integer.compare(a.weight(), b.weight());
            if (c == 0) c = Integer.compare(a.to(), b.to());
            if (c == 0) c = Integer.compare(a.from(), b.from());
            return c;
        });
        List<Edge> tree = new ArrayList<>();
        long total = 0; int components = 0;
        // Root 0 first; restart at each smallest unvisited vertex for a forest.
        for (int root = 0; root < graph.n(); root++) {
            if (visited[root]) continue;
            components++;
            queue.add(new Edge(-1, root, 0));
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                int v = edge.to();
                if (visited[v]) continue;
                visited[v] = true;
                if (edge.from() != -1) { tree.add(edge); total += edge.weight(); }
                for (Edge next : graph.outgoing(v)) {
                    metrics.edgeScans++;
                    if (!visited[next.to()]) queue.add(next);
                }
            }
        }
        return new Result(tree, total, components);
    }
}
