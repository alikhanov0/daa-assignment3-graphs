package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DagPaths {
    private DagPaths() { }
    public record Result(long[] distance, int[] parent, int[] parentWeight) {
        public Result { distance = distance.clone(); parent = parent.clone(); parentWeight = parentWeight.clone(); }
        @Override public long[] distance() { return distance.clone(); }
        @Override public int[] parent() { return parent.clone(); }
        @Override public int[] parentWeight() { return parentWeight.clone(); }
        public long sum() { long sum = 0; for (long d : distance) sum = Math.addExact(sum, d); return sum; }
        public List<Integer> pathTo(int vertex) {
            if (vertex < 0 || vertex >= parent.length) throw new IllegalArgumentException("Invalid vertex");
            List<Integer> path = new ArrayList<>();
            for (int v = vertex; v != -1; v = parent[v]) path.add(v);
            Collections.reverse(path); return List.copyOf(path);
        }
    }
    public static Result shortest(Graph graph, Metrics metrics) { return solve(graph, metrics, false); }
    public static Result longest(Graph graph, Metrics metrics) { return solve(graph, metrics, true); }
    private static Result solve(Graph graph, Metrics metrics, boolean longest) {
        Metrics preparation = new Metrics();
        int[] order = TopologicalSort.kahn(graph, preparation);
        metrics.preparationEdgeScans += preparation.edgeScans;
        long sentinel = longest ? Long.MIN_VALUE : Long.MAX_VALUE;
        long[] distance = new long[graph.n()]; Arrays.fill(distance, sentinel);
        int[] parent = new int[graph.n()], parentWeight = new int[graph.n()], indegree = graph.indegrees();
        Arrays.fill(parent, -1);
        for (int v = 0; v < graph.n(); v++) if (indegree[v] == 0) distance[v] = 0;
        for (int v : order) for (var edge : graph.outgoing(v)) {
            metrics.edgeScans++;
            if (distance[v] == sentinel) continue;
            long candidate = distance[v] + (long) edge.weight();
            metrics.comparisons++;
            if (longest ? candidate > distance[edge.to()] : candidate < distance[edge.to()]) {
                distance[edge.to()] = candidate; parent[edge.to()] = v; parentWeight[edge.to()] = edge.weight();
            }
        }
        return new Result(distance, parent, parentWeight);
    }
}
