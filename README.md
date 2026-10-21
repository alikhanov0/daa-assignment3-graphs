# DAA Assignment 3 - Graphs

**Alikhan Yerbol | SE-2517**

Repository: <https://github.com/alikhanov0/daa-assignment3-graphs>  
Submission branch: `main` | Release tag: `v1.0`

Java implementations of DSU, Kruskal, Prim, Kosaraju, Kahn and multisource DAG shortest paths, plus both bonuses: iterative Tarjan and condensation/critical path. All graph algorithms are implemented in this project; Java collections provide storage and the required priority queue. Kruskal uses an explicit stable merge sort.

## Build and test

Requirements: **JDK 17 or newer**, Maven 3.8+. Maven downloads JUnit 5 and build plugins on the first build. No third-party library is needed at runtime.

```sh
mvn clean verify
```

The 15 JUnit test methods exercise hundreds of graph instances, including independent reference algorithms and exhaustive spanning-forest enumeration. Surefire uses `-Xss256k` so the 100,000-vertex SCC tests demonstrate stack safety under a small JVM call stack. Test details are in `target/surefire-reports/`.

## Benchmark

```sh
java -Xmx2g -jar target/assignment3-graphs.jar benchmark
```

This runs **all five sizes x two densities x five required algorithms**, with one warm-up and five measured runs each, and saves:

- `results/results.csv`: exactly 50 data rows, prescribed columns and median times.
- `results/bonus_tarjan.csv`: 10 separate Tarjan rows, preserving the 50-row main format.
- `results/environment.txt`: JVM, OS, heap and measurement settings.

Each graph uses a fresh `Random(42)`. The benchmark fails on any reference mismatch, MST disagreement, incorrect tree size/component count, invalid topological order, Tarjan partition disagreement, incorrect required scan count, or non-reproducible counters/results between runs. Every one of the 30 numeric reference values matches the PDF. CSV files are written only after the entire run passes. Generation, summary and validation are outside the timed interval; algorithm preprocessing is included. Full execution can take several minutes and needs sufficient free memory for the 2 GiB heap.

For a fast demonstration (only n=1,000, both densities; writes to a separate directory):

```sh
java -Xmx2g -jar target/assignment3-graphs.jar benchmark --quick
```

You can specify an output directory before or after `--quick`. Report generation always reads the complete `results/` dataset.

## Regenerate plots and report

Python 3.10+ is needed only for the report and charts. Install the reporting packages once:

```sh
python -m pip install -r scripts/requirements.txt
```

Then reproduce tests, all measurements, charts and the report with one command:

```powershell
# Windows PowerShell, from the project root
./scripts/reproduce.ps1
```

```sh
# Linux / macOS, from the project root
sh scripts/reproduce.sh
```

To redraw from the already committed measurements without rerunning Java:

```sh
python scripts/report.py
```

Read [the five-page PDF report](output/pdf/REPORT.pdf), [REPORT.md](REPORT.md), or the [Russian defense notes](DEFENSE_RU.md). Figures are in `results/plots/`. Numerical fit results are in `results/analysis.csv`.

## Run the four input examples

```sh
java -jar target/assignment3-graphs.jar demo data/connected.txt
java -jar target/assignment3-graphs.jar demo data/disconnected.txt
java -jar target/assignment3-graphs.jar demo data/cyclic.txt
java -jar target/assignment3-graphs.jar demo data/dag.txt
```

The input format is exactly:

```text
n m directed|undirected
u v weight
... exactly m edge lines ...
```

Vertices are `0..n-1`; weights are signed 32-bit integers. Malformed input, missing/extra edges and invalid vertices produce clear exceptions. Empty graphs, self-loops, parallel edges and isolated vertices are supported. MST algorithms require undirected input and return a forest with `n-components` edges. Directed algorithms require directed input. Kahn and DAG paths explicitly reject cycles.

`data/disconnected.txt` has forest weight **4** and **4 components**. `data/cyclic.txt` has **4 SCCs**, a detected cycle, and condensed critical-path weight **13**. `data/dag.txt` has distances **[0, 0, 2, -2, 1, -4, 0]** and shortest path **[1, 2, 3, 5]** to vertex 5. Every zero-indegree vertex starts at zero, including isolated vertices.

## Metrics and interpretation

All counters are incremented during the relevant operation, never synthesized from formulas. Use a fresh `Metrics` object per invocation. `dsu_ops = findCalls + successfulUnions`; a failed union still performs two counted finds. Kruskal terminates when the DSU has one component, so its scan counter is a prefix of the sorted edges. Its comparisons still include sorting **all** edges. Prim uses a lazy `PriorityQueue`, skips already visited destinations and scans both adjacency entries of every undirected edge.

The assignment specifies **Kosaraju 2m, Kahn m and DAG relaxation m**. To preserve those exact definitions, CSV `edge_scans` counts the two DFS passes, Kahn traversal, or relaxation pass respectively. Transpose construction and the DAG algorithm's internal topological pass each count their own actual inspections in `preparationEdgeScans`, which is not a prescribed CSV column. Timings include these preparation steps. Indegrees are maintained by `Graph.addEdge` and copied for Kahn. Counting all graph visits would yield 3m for Kosaraju and 2m for the complete DAG shortest-path call; both remain linear.

`comparisons` counts sort/heap weight comparisons and candidate-distance comparisons during DAG relaxation; vertex-ID tie-breaking and ordinary index tests are excluded. `max_depth` is explicit DFS stack depth, not Java recursion depth or queue size. For Tarjan it is the maximum of its DFS and active-stack peaks.

The report explicitly resolves two inconsistencies in the assignment: the detailed specification requires 10 graph cases/50 rows despite a later mention of four graphs; and Kruskal's sorting cost appears in **comparisons**, not in its edge-scan count. The report fits and reports both kinds of slopes without forcing measured evidence to match an incorrect claim. Here `m=20n` is relatively dense, not asymptotically quadratic density.

## Layout and Git workflow

```text
src/main/java/graph/          shared adjacency-list graph and strict reader
src/main/java/algorithms/     algorithms and result types
src/main/java/metrics/        counters
src/main/java/benchmark/      generator, reference values, benchmark, CLI
src/test/java/tests/          JUnit 5 tests
data/                        four small graphs, all used by tests
results/                     CSV measurements, environment, fitted slopes
results/plots/               three PNG figures
scripts/                     report/reproduction/archive helpers
output/pdf/REPORT.pdf         five-page report
REPORT.md                    matching readable report source
DEFENSE_RU.md                 explanation and demo notes in Russian
```

Feature branches: `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, `feature/metrics`. Each feature contains implementation and validation commits and is merged into `main`. The release is tagged `v1.0`. `submission/GIT_HISTORY.txt` records the graph of commits, while `submission/repository.bundle` preserves the complete Git history, feature branches and release tag inside the ZIP (these two files are added by the archive script). The archive omits Maven output, caches and credentials.

```sh
python scripts/package_submission.py
```

This creates `DAA_Assignment3_Alikhan_Yerbol_SE-2517.zip`. Upload that archive to Moodle and include the repository link. The ZIP is created from tracked release files and a Git bundle, so build artifacts and local configuration are not included.

## AI-use acknowledgement

OpenAI Codex assisted in implementing the Java algorithms, writing reference-based tests, preparing the benchmark/report and organizing the Git history. The supplied test suite and full benchmark were executed, and results were checked against the assignment's reference table. This acknowledgement does not claim independent authorship or personal mastery by the student. The student should review the code and be prepared to explain and reproduce it during the defense.
