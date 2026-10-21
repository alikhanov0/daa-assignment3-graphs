package tests;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class PathsTest {
    static long[] bellmanFord(Graph graph, boolean longest) {
        long unreachable = longest ? Long.MIN_VALUE : Long.MAX_VALUE;
        long[] d = new long[graph.n()]; Arrays.fill(d, unreachable); int[] degrees = graph.indegrees();
        for (int v = 0; v < graph.n(); v++) if (degrees[v] == 0) d[v] = 0;
        for (int pass = 1; pass < graph.n(); pass++) {
            boolean changed = false;
            for (var e : graph.edges()) if (d[e.from()] != unreachable) {
                long candidate = d[e.from()] + e.weight();
                if (longest ? candidate > d[e.to()] : candidate < d[e.to()]) { d[e.to()] = candidate; changed = true; }
            }
            if (!changed) break;
        }
        return d;
    }
    static void verifyPaths(Graph g, DagPaths.Result r) {
        long[] d = r.distance(); int[] parent = r.parent(), weight = r.parentWeight(), degrees = g.indegrees();
        for (int v = 0; v < g.n(); v++) {
            var path = r.pathTo(v); assertEquals(v, path.get(path.size() - 1)); assertEquals(0, degrees[path.get(0)]);
            long total = 0;
            for (int i = 1; i < path.size(); i++) {
                int to = path.get(i), from = path.get(i - 1);
                assertEquals(from, parent[to]); assertTrue(g.edges().contains(new Graph.Edge(from, to, weight[to]))); total += weight[to];
            }
            assertEquals(d[v], total);
        }
    }
    @Test void randomDagsAgainstBellmanFordIncludingNegativeWeights() {
        Random random = new Random(9200);
        for (int trial = 0; trial < 150; trial++) {
            int n = 1 + random.nextInt(45); Graph g = new Graph(n, true); int[] perm = new int[n];
            for (int i = 0; i < n; i++) perm[i] = i;
            for (int i = n - 1; i > 0; i--) { int j = random.nextInt(i + 1), tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp; }
            for (int a = 0; a < n; a++) for (int b = a + 1; b < n; b++) if (random.nextInt(6) == 0) {
                g.addEdge(perm[a], perm[b], random.nextInt(201) - 100);
                if (random.nextInt(4) == 0) g.addEdge(perm[a], perm[b], random.nextInt(201) - 100);
            }
            Metrics k = new Metrics(), s = new Metrics();
            assertTrue(TopologicalSort.isValid(g, TopologicalSort.kahn(g, k))); assertEquals(g.m(), k.edgeScans);
            var shortest = DagPaths.shortest(g, s); var longest = DagPaths.longest(g, new Metrics());
            assertArrayEquals(bellmanFord(g, false), shortest.distance()); assertArrayEquals(bellmanFord(g, true), longest.distance());
            assertEquals(g.m(), s.edgeScans); assertEquals(g.m(), s.preparationEdgeScans);
            verifyPaths(g, shortest); verifyPaths(g, longest);
        }
    }
    @Test void fixturesCyclesAndEmptyGraph() throws Exception {
        Graph dag = Graph.read(Path.of("data/dag.txt")); var paths = DagPaths.shortest(dag, new Metrics());
        assertArrayEquals(new long[]{0, 0, 2, -2, 1, -4, 0}, paths.distance()); verifyPaths(dag, paths);
        assertEquals(java.util.List.of(1, 2, 3, 5), paths.pathTo(5));
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        assertThrows(TopologicalSort.CycleException.class, () -> TopologicalSort.kahn(cyclic, new Metrics()));
        assertThrows(TopologicalSort.CycleException.class, () -> DagPaths.shortest(cyclic, new Metrics()));
        Graph loop = new Graph(1, true); loop.addEdge(0, 0, 1);
        assertThrows(TopologicalSort.CycleException.class, () -> DagPaths.longest(loop, new Metrics()));
        assertEquals(0, DagPaths.shortest(new Graph(0, true), new Metrics()).sum());
        assertFalse(TopologicalSort.isValid(dag, new int[]{0, 0, 1, 2, 3, 4, 5}));
        Graph large = new Graph(3, true); large.addEdge(0, 1, Integer.MIN_VALUE); large.addEdge(1, 2, Integer.MIN_VALUE);
        assertEquals(2L * Integer.MIN_VALUE, DagPaths.shortest(large, new Metrics()).distance()[2]);
    }
    @Test void condensationIsAcyclicAndLongestPathIsVerified() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        var c = Condensation.build(graph); var path = Condensation.criticalPath(c, new Metrics());
        assertEquals(4, c.graph().n()); assertEquals(2, c.graph().m());
        assertTrue(TopologicalSort.isValid(c.graph(), TopologicalSort.kahn(c.graph(), new Metrics())));
        assertEquals(13, Arrays.stream(path.distance()).max().orElseThrow()); verifyPaths(c.graph(), path);
        Random random = new Random(51);
        for (int trial = 0; trial < 100; trial++) {
            Graph g = new Graph(25, true);
            for (int i = 0; i < trial; i++) g.addEdge(random.nextInt(25), random.nextInt(25), random.nextInt(41) - 20);
            var condensed = Condensation.build(g);
            assertTrue(TopologicalSort.isValid(condensed.graph(), TopologicalSort.kahn(condensed.graph(), new Metrics())));
            var actual = Condensation.criticalPath(condensed, new Metrics());
            assertArrayEquals(bellmanFord(condensed.graph(), true), actual.distance()); verifyPaths(condensed.graph(), actual);
        }
    }
}
