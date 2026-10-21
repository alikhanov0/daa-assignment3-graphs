package tests;

import algorithms.StronglyConnectedComponents;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class SccTest {
    static boolean[] reachable(Graph graph, int source) {
        boolean[] visited = new boolean[graph.n()]; ArrayDeque<Integer> queue = new ArrayDeque<>();
        visited[source] = true; queue.add(source);
        while (!queue.isEmpty()) for (var edge : graph.outgoing(queue.remove()))
            if (!visited[edge.to()]) { visited[edge.to()] = true; queue.add(edge.to()); }
        return visited;
    }
    static void check(Graph graph) {
        Metrics k = new Metrics(), t = new Metrics();
        var kosaraju = StronglyConnectedComponents.kosaraju(graph, k);
        var tarjan = StronglyConnectedComponents.tarjan(graph, t);
        assertTrue(StronglyConnectedComponents.samePartition(kosaraju, tarjan));
        assertEquals(2L * graph.m(), k.edgeScans); assertEquals(graph.m(), t.edgeScans);
        assertEquals(graph.m(), k.preparationEdgeScans);
        Graph reverse = graph.reversed(); int[] ids = kosaraju.component();
        for (int v = 0; v < graph.n(); v++) {
            boolean[] forward = reachable(graph, v), backward = reachable(reverse, v);
            for (int u = 0; u < graph.n(); u++) assertEquals(forward[u] && backward[u], ids[v] == ids[u]);
        }
    }
    @Test void hundredRandomGraphsAgainstBfsInBothDirections() {
        Random random = new Random(920);
        for (int trial = 0; trial < 100; trial++) {
            Graph g = new Graph(1 + random.nextInt(200), true);
            int m = random.nextInt(6 * g.n());
            for (int i = 0; i < m; i++) g.addEdge(random.nextInt(g.n()), random.nextInt(g.n()), 1);
            check(g);
        }
    }
    @Test void fixturesAndEdgeCases() throws Exception {
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt")); check(cyclic);
        assertEquals(4, StronglyConnectedComponents.kosaraju(cyclic, new Metrics()).count());
        check(Graph.read(Path.of("data/dag.txt"))); check(new Graph(0, true)); check(new Graph(8, true));
        Graph loop = new Graph(1, true); loop.addEdge(0, 0, 1); check(loop);
    }
    @Test void hundredThousandVertexPathAndCycleAreStackSafe() {
        Graph graph = new Graph(100_000, true);
        for (int v = 1; v < graph.n(); v++) graph.addEdge(v - 1, v, 1);
        Metrics k = new Metrics(), t = new Metrics();
        var a = StronglyConnectedComponents.kosaraju(graph, k); var b = StronglyConnectedComponents.tarjan(graph, t);
        assertEquals(graph.n(), a.count()); assertTrue(StronglyConnectedComponents.samePartition(a, b));
        assertEquals(graph.n(), k.maxDepth); assertEquals(graph.n(), t.maxDepth);
        graph.addEdge(graph.n() - 1, 0, 1);
        a = StronglyConnectedComponents.kosaraju(graph, new Metrics()); b = StronglyConnectedComponents.tarjan(graph, new Metrics());
        assertEquals(1, a.count()); assertTrue(StronglyConnectedComponents.samePartition(a, b));
    }
}
