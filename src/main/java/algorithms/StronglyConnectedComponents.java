package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.Arrays;

public final class StronglyConnectedComponents {
    private StronglyConnectedComponents() { }
    public record Result(int[] component, int count) {
        public Result { component = component.clone(); }
        @Override public int[] component() { return component.clone(); }
    }
    public static Result kosaraju(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int n = graph.n(), finished = 0;
        boolean[] visited = new boolean[n];
        int[] stack = new int[n], cursor = new int[n], order = new int[n];
        for (int root = 0; root < n; root++) {
            if (visited[root]) continue;
            int depth = 1; stack[0] = root; visited[root] = true; metrics.depth(depth);
            while (depth > 0) {
                int v = stack[depth - 1];
                if (cursor[v] < graph.outgoing(v).size()) {
                    int to = graph.outgoing(v).get(cursor[v]++).to(); metrics.edgeScans++;
                    if (!visited[to]) { visited[to] = true; stack[depth++] = to; metrics.depth(depth); }
                } else { order[finished++] = v; depth--; }
            }
        }
        Graph reverse = new Graph(n, true);
        for (var edge : graph.edges()) {
            metrics.preparationEdgeScans++;
            reverse.addEdge(edge.to(), edge.from(), edge.weight());
        }
        int[] component = new int[n]; Arrays.fill(component, -1); Arrays.fill(cursor, 0);
        int count = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = order[i];
            if (component[root] != -1) continue;
            int depth = 1; stack[0] = root; component[root] = count; metrics.depth(depth);
            while (depth > 0) {
                int v = stack[depth - 1];
                if (cursor[v] < reverse.outgoing(v).size()) {
                    int to = reverse.outgoing(v).get(cursor[v]++).to(); metrics.edgeScans++;
                    if (component[to] == -1) { component[to] = count; stack[depth++] = to; metrics.depth(depth); }
                } else depth--;
            }
            count++;
        }
        return new Result(component, count);
    }

    /** Iterative Tarjan: one DFS frame stack plus a separate stack of active vertices. */
    public static Result tarjan(Graph graph, Metrics metrics) {
        graph.requireDirected();
        int n = graph.n(), clock = 0, count = 0, activeSize = 0;
        int[] index = new int[n], low = new int[n], cursor = new int[n];
        int[] frames = new int[n], active = new int[n], component = new int[n];
        boolean[] onStack = new boolean[n];
        Arrays.fill(index, -1); Arrays.fill(component, -1);
        for (int root = 0; root < n; root++) {
            if (index[root] != -1) continue;
            int depth = 1; frames[0] = root;
            index[root] = low[root] = clock++; active[activeSize++] = root; onStack[root] = true;
            metrics.depth(Math.max(depth, activeSize));
            while (depth > 0) {
                int v = frames[depth - 1];
                if (cursor[v] < graph.outgoing(v).size()) {
                    int to = graph.outgoing(v).get(cursor[v]++).to(); metrics.edgeScans++;
                    if (index[to] == -1) {
                        frames[depth++] = to; index[to] = low[to] = clock++;
                        active[activeSize++] = to; onStack[to] = true;
                        metrics.depth(Math.max(depth, activeSize));
                    } else if (onStack[to]) low[v] = Math.min(low[v], index[to]);
                } else {
                    depth--;
                    if (depth > 0) { int parent = frames[depth - 1]; low[parent] = Math.min(low[parent], low[v]); }
                    if (low[v] == index[v]) {
                        int u;
                        do { u = active[--activeSize]; onStack[u] = false; component[u] = count; } while (u != v);
                        count++;
                    }
                }
            }
        }
        return new Result(component, count);
    }
    /** Compare partitions in linear time; component IDs themselves need not match. */
    public static boolean samePartition(Result a, Result b) {
        if (a.count != b.count || a.component.length != b.component.length) return false;
        int[] forward = new int[a.count], backward = new int[b.count];
        Arrays.fill(forward, -1); Arrays.fill(backward, -1);
        for (int v = 0; v < a.component.length; v++) {
            int x = a.component[v], y = b.component[v];
            if (forward[x] != -1 && forward[x] != y || backward[y] != -1 && backward[y] != x) return false;
            forward[x] = y; backward[y] = x;
        }
        return true;
    }
}
