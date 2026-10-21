package tests;

import algorithms.DisjointSetUnion;
import algorithms.MinimumSpanningTree;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    static void validateForest(Graph g, MinimumSpanningTree.Result r) {
        DisjointSetUnion sets = new DisjointSetUnion(g.n()); long sum = 0;
        for (Graph.Edge e : r.edges()) {
            assertTrue(sets.union(e.from(), e.to()), "Forest contains a cycle"); sum += e.weight();
            assertTrue(g.edges().contains(e) || g.edges().contains(new Graph.Edge(e.to(), e.from(), e.weight())));
        }
        assertEquals(sum, r.weight()); assertEquals(g.n() - r.components(), r.edges().size());
        assertEquals(r.components(), sets.components());
        for (Graph.Edge e : g.edges()) assertEquals(sets.find(e.from()), sets.find(e.to()));
    }
    @Test void hundredRandomConnectedGraphs() {
        Random random = new Random(82);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(99); Graph g = new Graph(n, false);
            for (int v = 1; v < n; v++) g.addEdge(v, random.nextInt(v), random.nextInt(2001) - 1000);
            for (int e = 0; e < 5 * n; e++) g.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(2001) - 1000);
            var k = MinimumSpanningTree.kruskal(g, new Metrics()); var p = MinimumSpanningTree.prim(g, new Metrics());
            assertEquals(k.weight(), p.weight()); assertEquals(n - 1, k.edges().size()); assertEquals(n - 1, p.edges().size());
            validateForest(g, k); validateForest(g, p);
        }
    }
    @Test void tinyGraphsAgainstExhaustiveSpanningForest() {
        Random random = new Random(700);
        for (int trial = 0; trial < 100; trial++) {
            Graph g = new Graph(1 + random.nextInt(6), false);
            for (int e = 0; e < 9; e++) g.addEdge(random.nextInt(g.n()), random.nextInt(g.n()), random.nextInt(21) - 10);
            DisjointSetUnion all = new DisjointSetUnion(g.n());
            for (var e : g.edges()) all.union(e.from(), e.to());
            int needed = g.n() - all.components(); long optimum = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << g.m()); mask++) {
                if (Integer.bitCount(mask) != needed) continue;
                DisjointSetUnion sets = new DisjointSetUnion(g.n()); long sum = 0; boolean acyclic = true;
                for (int i = 0; i < g.m(); i++) if ((mask & (1 << i)) != 0) {
                    var e = g.edges().get(i); acyclic &= sets.union(e.from(), e.to()); sum += e.weight();
                }
                if (acyclic && sets.components() == all.components()) optimum = Math.min(optimum, sum);
            }
            var k = MinimumSpanningTree.kruskal(g, new Metrics()); var p = MinimumSpanningTree.prim(g, new Metrics());
            assertEquals(optimum, k.weight()); assertEquals(optimum, p.weight()); validateForest(g, k); validateForest(g, p);
        }
    }
    @Test void fixturesEmptyIsolatedAndOverflow() throws Exception {
        Graph disconnected = Graph.read(Path.of("data/disconnected.txt"));
        for (Graph g : new Graph[]{disconnected, Graph.read(Path.of("data/connected.txt")), new Graph(0, false), new Graph(4, false)}) {
            var k = MinimumSpanningTree.kruskal(g, new Metrics()); var p = MinimumSpanningTree.prim(g, new Metrics());
            assertEquals(k.weight(), p.weight()); validateForest(g, k); validateForest(g, p);
        }
        var r = MinimumSpanningTree.kruskal(disconnected, new Metrics()); assertEquals(4, r.weight()); assertEquals(4, r.components());
        Graph big = new Graph(3, false); big.addEdge(0, 1, Integer.MAX_VALUE); big.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, MinimumSpanningTree.kruskal(big, new Metrics()).weight());
        assertEquals(2L * Integer.MAX_VALUE, MinimumSpanningTree.prim(big, new Metrics()).weight());
        assertThrows(IllegalArgumentException.class, () -> MinimumSpanningTree.prim(new Graph(1, true), new Metrics()));
    }
}
