package algorithms;

import graph.Graph;
import metrics.Metrics;

public final class Condensation {
    private Condensation() { }
    public record Result(Graph graph, StronglyConnectedComponents.Result scc) { }
    /** Keep parallel inter-component edges and original weights; drop internal edges.
     * This is an acyclic multigraph, so no arbitrary aggregation of weights is needed.
     */
    public static Result build(Graph original) {
        var scc = StronglyConnectedComponents.kosaraju(original, new Metrics());
        int[] id = scc.component(); Graph condensed = new Graph(scc.count(), true);
        for (var edge : original.edges()) if (id[edge.from()] != id[edge.to()])
            condensed.addEdge(id[edge.from()], id[edge.to()], edge.weight());
        return new Result(condensed, scc);
    }
    public static DagPaths.Result criticalPath(Result condensation, Metrics metrics) {
        return DagPaths.longest(condensation.graph(), metrics);
    }
}
