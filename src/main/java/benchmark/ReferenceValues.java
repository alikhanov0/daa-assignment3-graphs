package benchmark;

public final class ReferenceValues {
    private ReferenceValues() { }
    public static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    public static final int[] DENSITIES = {4, 20};
    private static final long[][] MST = {{147970,30363},{468911,97589},{1486139,309550},{4715515,968517},{15012094,3056681}};
    private static final long[][] SCC = {{27,1},{137,1},{373,1},{1188,1},{3923,1}};
    private static final long[][] DISTANCES = {{396274,252411},{1271911,723157},{3941828,2381342},{12551891,7376612},{40096539,23512087}};
    public static long mst(int size, int density) { return MST[size][density]; }
    public static long scc(int size, int density) { return SCC[size][density]; }
    public static long distances(int size, int density) { return DISTANCES[size][density]; }
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("BENCHMARK VALIDATION FAILED: " + message);
    }
}
