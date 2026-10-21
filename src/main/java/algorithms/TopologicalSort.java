package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.ArrayDeque;
import java.util.Arrays;

public final class TopologicalSort {
    private TopologicalSort() { }
    public static final class CycleException extends IllegalArgumentException {
        public CycleException(int processed, int total) { super("Directed cycle: processed " + processed + " of " + total + " vertices"); }
    }
    public static int[] kahn(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int[] indegree = graph.indegrees(), order = new int[graph.n()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < graph.n(); v++) if (indegree[v] == 0) queue.add(v);
        int size = 0;
        while (!queue.isEmpty()) {
            int v = queue.remove(); order[size++] = v;
            for (var edge : graph.outgoing(v)) {
                metrics.edgeScans++;
                if (--indegree[edge.to()] == 0) queue.add(edge.to());
            }
        }
        if (size != graph.n()) throw new CycleException(size, graph.n());
        return order;
    }
    public static boolean isValid(Graph graph, int[] order) {
        if (!graph.directed() || order.length != graph.n()) return false;
        int[] position = new int[graph.n()]; Arrays.fill(position, -1);
        for (int i = 0; i < order.length; i++) {
            int v = order[i];
            if (v < 0 || v >= graph.n() || position[v] != -1) return false;
            position[v] = i;
        }
        for (var edge : graph.edges()) if (position[edge.from()] >= position[edge.to()]) return false;
        return true;
    }
}
