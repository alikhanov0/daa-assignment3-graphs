package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import static benchmark.ReferenceValues.require;

public final class Benchmark {
    private Benchmark() { }
    private record Measurement<T>(T value, Metrics metrics, double milliseconds, long result) { }
    private static <T> Measurement<T> measure(Function<Metrics,T> algorithm, ToLongFunction<T> summarize) {
        // One unrecorded warm-up, followed by five complete invocations.
        algorithm.apply(new Metrics());
        long[] times = new long[5]; T last = null; Metrics lastMetrics = null;
        String expected = null; long result = 0;
        for (int run = 0; run < 5; run++) {
            Metrics metrics = new Metrics(); long start = System.nanoTime();
            T value = algorithm.apply(metrics); times[run] = System.nanoTime() - start;
            long current = summarize.applyAsLong(value);
            String signature = metrics.signature() + "," + metrics.preparationEdgeScans + "," + current;
            if (expected == null) expected = signature;
            else require(expected.equals(signature), "Non-reproducible counters/result between runs");
            last = value; lastMetrics = metrics; result = current;
        }
        Arrays.sort(times);
        return new Measurement<>(last, lastMetrics, times[2] / 1_000_000.0, result);
    }
    private static String row(String algorithm, String kind, int n, int m, Measurement<?> measurement) {
        return String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%s,%d", algorithm, kind, n, m,
                measurement.milliseconds(), measurement.metrics().signature(), measurement.result());
    }
    public static void run(Path output, boolean quick) throws IOException {
        List<String> rows = new ArrayList<>(), bonus = new ArrayList<>();
        String header = "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
        rows.add(header); bonus.add(header);
        System.out.println("n,m,MST total weight,SCC count,Sum of DAG distances");
        int sizeCount = quick ? 1 : ReferenceValues.SIZES.length;
        for (int s = 0; s < sizeCount; s++) for (int density = 0; density < 2; density++) {
            int n = ReferenceValues.SIZES[s], m = n * ReferenceValues.DENSITIES[density];
            String suffix = density == 0 ? "sparse" : "dense";
            Graph undirected = GraphGenerator.generate(n, m, GraphGenerator.Kind.UNDIRECTED);
            var kruskal = measure(x -> MinimumSpanningTree.kruskal(undirected, x), MinimumSpanningTree.Result::weight);
            var prim = measure(x -> MinimumSpanningTree.prim(undirected, x), MinimumSpanningTree.Result::weight);
            require(kruskal.result() == prim.result(), "Kruskal/Prim mismatch at n=" + n + ", m=" + m);
            require(kruskal.result() == ReferenceValues.mst(s, density), "MST reference at " + n + "/" + m + ": got " + kruskal.result());
            require(kruskal.value().edges().size() == n - 1 && prim.value().edges().size() == n - 1, "Wrong MST edge count");
            require(kruskal.value().components() == 1 && prim.value().components() == 1, "Generated graph disconnected");
            require(prim.metrics().edgeScans == 2L * m, "Prim must scan both adjacency entries");
            rows.add(row("Kruskal", "undirected_" + suffix, n, m, kruskal));
            rows.add(row("Prim", "undirected_" + suffix, n, m, prim));
            Graph directed = GraphGenerator.generate(n, m, GraphGenerator.Kind.DIRECTED);
            var kosaraju = measure(x -> StronglyConnectedComponents.kosaraju(directed, x), StronglyConnectedComponents.Result::count);
            var tarjan = measure(x -> StronglyConnectedComponents.tarjan(directed, x), StronglyConnectedComponents.Result::count);
            require(kosaraju.result() == ReferenceValues.scc(s, density), "SCC reference at " + n + "/" + m + ": got " + kosaraju.result());
            require(StronglyConnectedComponents.samePartition(kosaraju.value(), tarjan.value()), "Tarjan partition mismatch");
            require(kosaraju.metrics().edgeScans == 2L * m && tarjan.metrics().edgeScans == m, "SCC edge scan counters");
            rows.add(row("Kosaraju", "directed_" + suffix, n, m, kosaraju));
            bonus.add(row("Tarjan", "directed_" + suffix, n, m, tarjan));
            Graph dag = GraphGenerator.generate(n, m, GraphGenerator.Kind.DAG);
            var kahn = measure(x -> TopologicalSort.kahn(dag, x), order -> order.length);
            require(TopologicalSort.isValid(dag, kahn.value()), "Invalid topological order");
            var paths = measure(x -> DagPaths.shortest(dag, x), DagPaths.Result::sum);
            require(paths.result() == ReferenceValues.distances(s, density), "DAG reference at " + n + "/" + m + ": got " + paths.result());
            require(kahn.metrics().edgeScans == m && paths.metrics().edgeScans == m, "Kahn/DAG edge scan counters");
            rows.add(row("Kahn", "dag_" + suffix, n, m, kahn));
            rows.add(row("DagShortestPath", "dag_" + suffix, n, m, paths));
            System.out.printf(Locale.ROOT, "%d,%d,%d,%d,%d%n", n, m, kruskal.result(), kosaraju.result(), paths.result());
        }
        require(rows.size() == 1 + sizeCount * 10, "Wrong CSV row count");
        // Never replace an earlier valid CSV with a partially failed benchmark.
        Files.createDirectories(output);
        writeComplete(output.resolve("results.csv"), rows);
        writeComplete(output.resolve("bonus_tarjan.csv"), bonus);
        Files.writeString(output.resolve("environment.txt"), "Java: " + System.getProperty("java.version")
                + "\nVM: " + System.getProperty("java.vm.name") + "\nOS: " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch") + "\nAvailable processors: " + Runtime.getRuntime().availableProcessors()
                + "\nMax heap bytes: " + Runtime.getRuntime().maxMemory() + "\nSeed: 42 per graph\nWarmups: 1\nMeasured runs: 5\n");
        System.out.println("PASS: all references, counters and cross-checks; wrote " + (rows.size() - 1) + " main rows.");
    }
    private static void writeComplete(Path destination, List<String> lines) throws IOException {
        Path temp = Files.createTempFile(destination.getParent(), "benchmark-", ".csv.tmp");
        try {
            Files.write(temp, lines);
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
