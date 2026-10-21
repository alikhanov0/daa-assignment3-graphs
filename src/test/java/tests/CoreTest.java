package tests;

import algorithms.DisjointSetUnion;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @Test void randomDsuAgainstLabelReference() {
        Random random = new Random(19);
        int n = 150;
        DisjointSetUnion dsu = DisjointSetUnion.makeSet(n);
        int[] label = new int[n];
        for (int i = 0; i < n; i++) label[i] = i;
        for (int step = 0; step < 1000; step++) {
            int a = random.nextInt(n), b = random.nextInt(n);
            int from = label[a], to = label[b];
            assertEquals(from != to, dsu.union(a, b));
            for (int i = 0; i < n; i++) if (label[i] == from) label[i] = to;
            assertEquals(Arrays.stream(label).distinct().count(), dsu.components());
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
                assertEquals(label[i] == label[j], dsu.find(i) == dsu.find(j));
        }
    }
    @Test void dsuCountersAndBounds() {
        Metrics m = new Metrics();
        DisjointSetUnion d = new DisjointSetUnion(3, m);
        assertTrue(d.union(0, 1)); assertFalse(d.union(1, 0)); d.find(1);
        assertEquals(5, m.findCalls); assertEquals(1, m.successfulUnions); assertEquals(6, m.dsuOps());
        assertEquals(2, d.components()); assertEquals(0, DisjointSetUnion.makeSet(0).components());
        assertThrows(IllegalArgumentException.class, () -> d.find(3));
        assertThrows(IllegalArgumentException.class, () -> new DisjointSetUnion(-1));
    }
    @Test void graphFixturesAndReverse() throws Exception {
        Graph g = Graph.read(Path.of("data/disconnected.txt"));
        assertEquals(7, g.n()); assertEquals(6, g.m()); assertFalse(g.directed());
        assertEquals(12, java.util.stream.IntStream.range(0, g.n()).map(v -> g.outgoing(v).size()).sum());
        assertTrue(g.outgoing(6).isEmpty());
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        assertEquals(cyclic.edges(), cyclic.reversed().reversed().edges());
        assertThrows(UnsupportedOperationException.class, () -> g.edges().clear());
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(-1, 0, 0));
    }
    @Test void malformedFilesAreRejected(@TempDir Path dir) throws Exception {
        String[] invalid = {"", "2 1 mixed\n0 1 3\n", "2 -1 directed\n", "-1 0 directed\n",
            "2 1 directed\n", "2 1 directed\n0 2 3\n", "2 0 directed\n0 1 2\n", "2 1 directed\n0 1\n"};
        for (String text : invalid) {
            Path file = dir.resolve("bad.txt"); Files.writeString(file, text);
            assertThrows(IllegalArgumentException.class, () -> Graph.read(file), text);
        }
        Path file = dir.resolve("empty.txt"); Files.writeString(file, "0 0 directed\n");
        assertEquals(0, Graph.read(file).n());
    }
}
