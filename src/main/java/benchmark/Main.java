package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.Path;
import java.util.Arrays;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java -Xmx2g -jar target/assignment3-graphs.jar benchmark [output-dir] [--quick]\n"
                    + "       java -jar target/assignment3-graphs.jar demo data/dag.txt"); return;
        }
        switch (args[0]) {
            case "benchmark" -> {
                boolean quick = false; Path output = null;
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equals("--quick")) quick = true;
                    else if (output == null && !args[i].startsWith("--")) output = Path.of(args[i]);
                    else throw new IllegalArgumentException("Unexpected argument: " + args[i]);
                }
                Benchmark.run(output != null ? output : Path.of(quick ? "tmp/quick-results" : "results"), quick);
            }
            case "demo" -> {
                if (args.length != 2) throw new IllegalArgumentException("demo needs one graph file");
                demo(Graph.read(Path.of(args[1])));
            }
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }
    private static void demo(Graph g) {
        System.out.printf("Graph: n=%d m=%d %s%n", g.n(), g.m(), g.directed() ? "directed" : "undirected");
        if (!g.directed()) {
            System.out.println("Kruskal: " + MinimumSpanningTree.kruskal(g, new Metrics()));
            System.out.println("Prim: " + MinimumSpanningTree.prim(g, new Metrics())); return;
        }
        var scc = StronglyConnectedComponents.kosaraju(g, new Metrics());
        System.out.println("SCC count=" + scc.count() + "; component[v]=" + Arrays.toString(scc.component()));
        try {
            System.out.println("Topological order: " + Arrays.toString(TopologicalSort.kahn(g, new Metrics())));
            var paths = DagPaths.shortest(g, new Metrics()); long[] distance = paths.distance();
            for (int v = 0; v < g.n(); v++) System.out.println("Vertex " + v + ": distance=" + distance[v] + ", path=" + paths.pathTo(v));
        } catch (TopologicalSort.CycleException e) { System.out.println(e.getMessage()); }
        var condensation = Condensation.build(g); var critical = Condensation.criticalPath(condensation, new Metrics());
        System.out.println("Condensation: " + condensation.graph().n() + " vertices, " + condensation.graph().m() + " edges");
        long[] lengths = critical.distance(); int endpoint = -1;
        for (int v = 0; v < lengths.length; v++) if (endpoint == -1 || lengths[v] > lengths[endpoint]) endpoint = v;
        System.out.println(endpoint == -1 ? "Critical path: empty" : "Critical path through SCC IDs: " + critical.pathTo(endpoint) + ", length=" + lengths[endpoint]);
    }
}
