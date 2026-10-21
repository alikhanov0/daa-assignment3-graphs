package algorithms;

import metrics.Metrics;

/** Union by size and two-pass, iterative full path compression. */
public final class DisjointSetUnion {
    private final int[] parent, size;
    private int components;
    private final Metrics metrics;
    public DisjointSetUnion(int n) { this(n, new Metrics()); }
    public DisjointSetUnion(int n, Metrics metrics) {
        if (n < 0) throw new IllegalArgumentException("Negative set count");
        this.metrics = java.util.Objects.requireNonNull(metrics);
        parent = new int[n]; size = new int[n]; components = n;
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    }
    public static DisjointSetUnion makeSet(int n) { return new DisjointSetUnion(n); }
    public int find(int x) {
        if (x < 0 || x >= parent.length) throw new IllegalArgumentException("Invalid element: " + x);
        metrics.findCalls++;
        int root = x;
        while (root != parent[root]) root = parent[root];
        while (x != root) { int next = parent[x]; parent[x] = root; x = next; }
        return root;
    }
    public boolean union(int x, int y) {
        int a = find(x), b = find(y);
        if (a == b) return false;
        if (size[a] < size[b]) { int tmp = a; a = b; b = tmp; }
        parent[b] = a; size[a] += size[b]; components--; metrics.successfulUnions++;
        return true;
    }
    public int components() { return components; }
    public Metrics metrics() { return metrics; }
}
