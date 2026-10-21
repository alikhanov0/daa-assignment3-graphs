package metrics;

/** Fresh per invocation; incremented where the corresponding operation occurs. */
public final class Metrics {
    public long findCalls;
    public long successfulUnions;
    public long edgeScans;
    public long comparisons;
    public int maxDepth;
    // Separate setup work: transpose construction or topological preparation.
    public long preparationEdgeScans;
    public long dsuOps() { return findCalls + successfulUnions; }
    public void depth(int depth) { maxDepth = Math.max(maxDepth, depth); }
    public String signature() {
        return dsuOps() + "," + edgeScans + "," + comparisons + "," + maxDepth;
    }
}
