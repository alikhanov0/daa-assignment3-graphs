package tests;

import algorithms.*;
import benchmark.*;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    @Test void smallestReferenceCasesAndExactCounters() {
        for (int density = 0; density < 2; density++) {
            int m = 1000 * ReferenceValues.DENSITIES[density];
            var g = GraphGenerator.generate(1000, m, GraphGenerator.Kind.UNDIRECTED);
            assertEquals(ReferenceValues.mst(0, density), MinimumSpanningTree.kruskal(g, new Metrics()).weight());
            assertEquals(ReferenceValues.mst(0, density), MinimumSpanningTree.prim(g, new Metrics()).weight());
            var directed = GraphGenerator.generate(1000, m, GraphGenerator.Kind.DIRECTED);
            Metrics scc = new Metrics();
            assertEquals(ReferenceValues.scc(0, density), StronglyConnectedComponents.kosaraju(directed, scc).count());
            assertEquals(2L * m, scc.edgeScans);
            var dag = GraphGenerator.generate(1000, m, GraphGenerator.Kind.DAG); Metrics paths = new Metrics();
            assertEquals(ReferenceValues.distances(0, density), DagPaths.shortest(dag, paths).sum()); assertEquals(m, paths.edgeScans);
            assertEquals(g.edges(), GraphGenerator.generate(1000, m, GraphGenerator.Kind.UNDIRECTED).edges());
        }
    }
    @Test void invalidGeneratorInputsCannotHang() {
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(1, 1, GraphGenerator.Kind.DAG));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(5, 2, GraphGenerator.Kind.UNDIRECTED));
        assertEquals(0, GraphGenerator.generate(0, 0, GraphGenerator.Kind.DIRECTED).m());
    }
}
