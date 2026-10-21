"""Generate three plots and the five-page report from actual benchmark CSVs."""
from pathlib import Path
import csv
import html
import math
import statistics
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "results"
PLOTS = RESULTS / "plots"
PLOTS.mkdir(parents=True, exist_ok=True)

def read_rows(filename):
    with (RESULTS / filename).open(encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    for row in rows:
        for key in ("n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"):
            row[key] = int(row[key])
        row["time_ms"] = float(row["time_ms"])
    return rows

rows = read_rows("results.csv")
bonus = read_rows("bonus_tarjan.csv")
environment = dict(line.split(": ", 1) for line in (RESULTS / "environment.txt").read_text(encoding="utf-8").splitlines() if ": " in line)
ALGORITHMS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
COLORS = ["#be3c34", "#2563a6", "#15806a", "#8d5bb4", "#d18a12"]
MARKERS = ["o", "s", "^", "D", "x"]
assert len(rows) == 50 and len(bonus) == 10
assert len({(r["algorithm"], r["n"], r["m"]) for r in rows}) == 50

def series(algorithm, density):
    return sorted((r for r in rows if r["algorithm"] == algorithm and r["graph"].endswith("_" + density)), key=lambda r: r["n"])

def operations(r):
    return r["dsu_ops"] + r["edge_scans"] + r["comparisons"]

def slope(data, counter):
    x = [math.log(r["n"]) for r in data]
    y = [math.log(counter(r)) for r in data]
    xm, ym = statistics.mean(x), statistics.mean(y)
    return sum((a-xm)*(b-ym) for a,b in zip(x,y)) / sum((a-xm)**2 for a in x)

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9, "axes.spines.top": False, "axes.spines.right": False})
def plot_panels(filename, ylabel, counter, logarithmic):
    fig, axes = plt.subplots(1, 2, figsize=(10, 3.25), sharey=True)
    for ax, density, factor in zip(axes, ("sparse", "dense"), (4,20)):
        for algorithm, color, marker in zip(ALGORITHMS, COLORS, MARKERS):
            data = series(algorithm, density)
            ax.plot([r["n"] for r in data], [counter(r) for r in data], label=algorithm,
                    color=color, marker=marker, markersize=5, fillstyle="none", linewidth=1.5,
                    linestyle="--" if algorithm == "DagShortestPath" else "-")
        ax.set_xscale("log"); ax.set_xlabel("Vertices n"); ax.set_title(f"{density.capitalize()}: m = {factor}n")
        if logarithmic: ax.set_yscale("log")
        else: ax.set_ylim(0, 2.2)
        ax.grid(True, which="major", alpha=.2)
    axes[0].set_ylabel(ylabel)
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="lower center", ncol=5, frameon=False, fontsize=8)
    fig.tight_layout(rect=(0,.12,1,1)); fig.savefig(PLOTS / filename, dpi=200); plt.close(fig)

plot_panels("operations_vs_n.png", "Recorded operations", operations, True)
plot_panels("scans_per_edge.png", "edgeScans / m", lambda r: r["edge_scans"] / r["m"], False)
fig, ax = plt.subplots(figsize=(10,3.25))
for density, color, marker in (("sparse",COLORS[0],"o"),("dense",COLORS[1],"s")):
    data=series("Kruskal",density)
    ax.plot([r["n"] for r in data],[r["edge_scans"]/r["m"] for r in data],color=color,marker=marker,label=density)
ax.set_xscale("log"); ax.set_ylim(0,1.08); ax.set_xlabel("Vertices n"); ax.set_ylabel("Kruskal edgeScans / m")
ax.set_title("Fraction examined after sorting (sorting still processes every edge)")
ax.grid(True,alpha=.2); ax.legend(frameon=False); fig.tight_layout(); fig.savefig(PLOTS/"kruskal_edge_share.png",dpi=200); plt.close(fig)

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="BodyCustom", fontName="Helvetica", fontSize=9, leading=12, spaceAfter=6, textColor=colors.HexColor("#263447")))
styles.add(ParagraphStyle(name="SmallCustom", fontName="Helvetica", fontSize=7.7, leading=10, spaceAfter=3))
styles.add(ParagraphStyle(name="HeadingCustom", fontName="Helvetica-Bold", fontSize=13, leading=16, spaceBefore=9, spaceAfter=7, textColor=colors.HexColor("#14556c")))
styles.add(ParagraphStyle(name="TitleCustom", fontName="Helvetica-Bold", fontSize=22, leading=25, spaceAfter=9, textColor=colors.HexColor("#13364a")))
story=[]; markdown=[]
def heading(text, title=False):
    story.append(Paragraph(html.escape(text), styles["TitleCustom" if title else "HeadingCustom"]))
    markdown.append(("# " if title else "## ")+text+"\n")
def paragraph(text, small=False):
    story.append(Paragraph(html.escape(text),styles["SmallCustom" if small else "BodyCustom"]))
    markdown.append(text+"\n")
def table(headers, data, widths):
    all_rows=[headers]+[[str(c) for c in row] for row in data]
    cells=[[Paragraph(html.escape(str(c)),styles["SmallCustom"]) for c in row] for row in all_rows]
    t=Table(cells,colWidths=widths,repeatRows=1,hAlign="LEFT")
    t.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,0),colors.HexColor("#dceaf0")),("ROWBACKGROUNDS",(0,1),(-1,-1),[colors.white,colors.HexColor("#f3f6f8")]),("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),6),("RIGHTPADDING",(0,0),(-1,-1),6),("TOPPADDING",(0,0),(-1,-1),5),("BOTTOMPADDING",(0,0),(-1,-1),5),("LINEBELOW",(0,0),(-1,0),.6,colors.HexColor("#9db8c5"))]))
    story.extend([t,Spacer(1,6)])
    markdown.extend(["| "+" | ".join(row)+" |" for row in [list(headers),["---"]*len(headers),*all_rows[1:]]]); markdown.append("")
def figure(filename, caption):
    story.append(Image(str(PLOTS/filename),width=499,height=162.2))
    paragraph(caption,True); markdown.append(f"![{caption}](results/plots/{filename})\n")
def page():
    story.append(PageBreak()); markdown.append("\n---\n")
def spread(values):
    return f"{min(values):.3f} - {max(values):.3f}"

heading("City Network & Job Scheduler",True)
paragraph("DAA Assignment 3 | Alikhan Yerbol | SE-2517 | 21 October 2026")
paragraph("Repository: https://github.com/alikhanov0/daa-assignment3-graphs | branch main | release v1.0",True)
heading("1. Implementation and complexity")
paragraph("One adjacency-list Graph supports directed and undirected multigraphs. Logical undirected edges are stored once for Kruskal and twice in adjacency lists for Prim. Weights are int; totals and distances are long. All DFS traversals use explicit arrays as stacks. Let n=V, m=E and k be the edges Kruskal examines after sorting; logarithms below use max(2,m). Space excludes the input graph and includes auxiliary data and returned results.")
table(["Algorithm","Time","Extra space","Reason"],[
    ["DSU find / union","O(alpha(n)) amortized","O(n) structure; O(1) per call","Union by size + full path compression; makeSet is O(n). A single call can take O(log n)."],
    ["Kruskal","O(n + m log m)","O(n + m)","Stable merge sort; at most m unions, each with two finds; n-array initialization."],
    ["Prim (lazy heap)","O(n + m log m)","O(n + m)","Each adjacency entry scanned; up to m candidates enter the heap. Multigraphs need log m, not automatically log n."],
    ["Kosaraju","O(n + m)","O(n + m)","Two DFS passes and an explicit transpose; linear stacks and arrays."],
    ["Kahn","O(n + m)","O(n)","Copy cached indegrees, queue each vertex once, scan each outgoing edge once."],
    ["DAG shortest paths","O(n + m)","O(n)","Kahn order, then one relaxation pass; distance and parent arrays."],
],[86,96,88,229])
heading("2. Correctness arguments")
paragraph("Cut property (7 sentences). Consider a cut that respects the already chosen forest, and let e be a lightest edge crossing it. If an optimal spanning tree T omits e, adding e to T creates a cycle. That cycle contains another edge f crossing the same cut. Since w(e) <= w(f), replacing f with e preserves connectivity without increasing total weight. Thus some optimum contains the chosen forest together with e. Kruskal chooses the lightest edge joining distinct DSU components, so the cut around either component respects the forest and the exchange argument applies. Prim chooses a lightest edge leaving the visited set; restarting it in each unvisited connected component yields a minimum spanning forest, including isolated vertices.")
paragraph("Topological induction (7 sentences). Every source receives distance zero, equivalent to adding a virtual source with zero-weight edges to all original sources. Every vertex of a finite DAG is reachable from some source, since repeatedly following predecessors must terminate. Assume that all vertices preceding v in a topological order already have optimal distances. Any nonempty path ending at v ends with an edge u -> v, where u precedes v. Relaxing every such edge therefore considers every possible optimal predecessor and finalizes the minimum distance of v. The induction works for negative weights because an edge never returns to a finalized predecessor and no directed cycle exists. Recording the predecessor and its selected edge weight on each improvement reconstructs an actual optimal path, even with parallel edges.")

page(); heading("3. Reproducibility and reference checks",True)
paragraph("For each of the five sizes and two densities, three separate graphs are generated with a fresh java.util.Random(42). A Fisher-Yates permutation and an interleaved random weight are used exactly as specified; rejected self-loops redraw b, and the DAG compares rank[a] with rank[b]. Parallel edges are retained. The benchmark warms up each algorithm once, measures five complete invocations, reports the median, and checks that all five counter/result signatures agree. Graph generation, result summarization and validation are outside the timed region.")
reference=[]
for n in (1000,3162,10000,31623,100000):
    for density in ("sparse","dense"):
        a=next(r for r in series("Kruskal",density) if r["n"]==n)
        b=next(r for r in series("Kosaraju",density) if r["n"]==n)
        c=next(r for r in series("DagShortestPath",density) if r["n"]==n)
        reference.append([f'{n:,} / {a["m"]:,}',f'{a["result"]:,}',f'{b["result"]:,}',f'{c["result"]:,}'])
table(["n / m","MST weight (both)","SCC count","Sum of DAG distances"],reference,[135,120,95,149])
paragraph("All 30 reference values match the assignment, both MST algorithms agree, every Kahn order is validated edge by edge, and every Tarjan partition equals Kosaraju's. results/results.csv has exactly 50 data rows; bonus_tarjan.csv has 10 separate bonus rows. Any mismatch raises an exception before the final CSV is replaced.")
heading("4. Counters divided by their formulas")
ratios=[["DSU in Kruskal", "dsuOps / examined k", *[spread([r["dsu_ops"] / r["edge_scans"] for r in series("Kruskal", d)]) for d in ("sparse", "dense")]]]
for algorithm in ALGORITHMS:
    formula="ops / (m log2 m)" if algorithm in ("Kruskal","Prim") else "scans / (n + m)"
    values=[]
    for density in ("sparse","dense"):
        values.append(spread([operations(r)/(r["m"]*math.log2(r["m"])) if algorithm in ("Kruskal","Prim") else r["edge_scans"]/(r["n"]+r["m"]) for r in series(algorithm,density)]))
    ratios.append([algorithm,formula,*values])
table(["Algorithm","Normalization","Sparse min-max","Dense min-max"],ratios,[95,144,130,130])
paragraph("Here ops = dsu_ops + edge_scans + comparisons, a declared proxy rather than a literal CPU instruction count. The bounded MST ratios support the sorting/heap growth model; traversal ratios are exactly constant because m=4n or 20n. DSU uses two find calls per examined edge and one counted union only when successful, hence dsuOps = 2k + (n-c), where c is the final component count. dsuOps/k remains bounded; dsuOps/m need not stay constant when early stopping changes k/m. Counting calls alone cannot establish the inverse-Ackermann bound: that bound follows from the two DSU heuristics.")

page(); heading("5. Operation growth and slope checks",True)
figure("operations_vs_n.png","Figure 1. One series per algorithm and density on log-log axes. Kosaraju and DAG operations overlap at 2m; they count different operations.")
figure("scans_per_edge.png","Figure 2. Prim and Kosaraju overlap at 2 scans/edge; Kahn and DAG relaxation overlap at 1. Kruskal scans only its examined prefix.")
slope_rows=[]
for algorithm in ALGORITHMS:
    slope_rows.append([algorithm,*[f'{slope(series(algorithm,d),lambda r:r["edge_scans"]):.3f}' for d in ("sparse","dense")],*[f'{slope(series(algorithm,d),operations):.3f}' for d in ("sparse","dense")]])
table(["Algorithm","Scans slope sparse","Scans slope dense","Ops slope sparse","Ops slope dense"],slope_rows,[99,100,100,100,100])
paragraph("Slopes use ordinary least squares of ln(counter) on ln(n), independently for each density and all five sizes. Traversal edge-scan slopes are exactly 1.000. Kruskal's sorting-comparison slopes are " + ", ".join(f'{slope(series("Kruskal",d),lambda r:r["comparisons"]):.3f} ({d})' for d in ("sparse","dense")) + ". These finite-range slopes are slightly above one, consistent with m log m; they approach one asymptotically and are not fixed polynomial exponents.")
paragraph("Specification clarification: an E log E sort does not make Kruskal's edgeScans count E log E. Every sorted edge is considered at most once; comparisons measure the sorting cost separately. The scan slopes above reflect a changing examined fraction, not the logarithmic factor. Reporting comparisons and total recorded operations avoids claiming evidence that the scan counter cannot provide.",True)

page(); heading("6. Choosing an algorithm",True)
figure("kruskal_edge_share.png","Figure 3. Early termination saves DSU scans after a spanning tree is complete, but does not remove the initial sort.")
paragraph("At n=100,000, Kruskal examines " + "; ".join(f'{series("Kruskal",d)[-1]["edge_scans"]/series("Kruskal",d)[-1]["m"]:.2%} of {d} edges' for d in ("sparse","dense")) + ". Sparse graphs require nearly all sorted edges, while dense graphs offer many alternatives and a shorter relative prefix. Our lazy Prim scans 2m adjacency entries and drains its candidate heap, including stale entries. Kruskal performs fewer recorded operations for every supplied case; this is evidence about these implementations and graph families, not a universal winner.")
table(["Situation","Choice","Why / limitation"],[
    ["Sparse undirected graph; edge list","Kruskal","Sorting plus DSU is simple; no adjacency heap is required."],
    ["Undirected adjacency lists, connected or not","Prim or Kruskal","Prim grows locally; restart for disconnected components. Compare actual heap and sorting work."],
    ["Truly dense simple graph, m=Theta(n^2)","Matrix Prim (alternative)","O(n^2) array/matrix implementation avoids heap logs; our required version uses a PriorityQueue. m=20n is only relatively dense."],
    ["Directed graph, cycles possible","Kosaraju / Tarjan","Identify SCCs and deadlocks; Tarjan saves a pass and transpose storage."],
    ["Dependency order, cycle status unknown","Kahn","Returns a complete valid order or explicitly reports a cycle."],
    ["DAG, even with negative weights","DAG shortest paths","Topological relaxation is linear; all sources are initialized to zero."],
    ["General graph shortest routes","Dijkstra / Bellman-Ford","Nonnegative edges: Dijkstra. Negative edges: Bellman-Ford and negative-cycle handling (outside this project)."],
],[155,114,230])
heading("Measured time is secondary evidence")
times=[]
for a in ALGORITHMS:
    times.append(a+" "+"/".join(f'{series(a,d)[-1]["time_ms"]:.1f}' for d in ("sparse","dense")))
paragraph(f"On the recorded {environment['OS']} / Java {environment['Java']} run with a {int(environment['Max heap bytes']) / 2**30:.1f} GiB heap, the largest sparse/dense median times in milliseconds were: "+"; ".join(times)+". The two smallest sizes are vulnerable to JIT, allocation and scheduling noise; one warm-up is not a full JVM benchmarking harness. Five-run medians reduce outliers, but other hardware, JVMs, garbage collection and background load change the clock. Counter trends are the graded evidence; timing does not justify a universal claim that one algorithm is always faster.")

page(); heading("7. Bonus algorithms and validation",True)
heading("Tarjan: one DFS pass")
paragraph("The iterative Tarjan implementation maintains discovery index[], low[], a DFS frame stack and a stack of active vertices. A completed DFS child propagates its low-link value to its parent; an edge to an active discovered vertex uses that vertex's discovery index. A vertex with low[v]=index[v] closes exactly one SCC, popped from the active stack. Finished SCCs are ignored in later low-link updates. The algorithm takes O(n+m) time and O(n) auxiliary space, excluding the input and output. It performs exactly m traversal scans versus Kosaraju's 2m, saving 50% of traversal edge inspections and avoiding the transpose; counting transpose construction too changes 3m versus m. At n=100,000 and m=2,000,000 the measured counts are 2,000,000 and 4,000,000, respectively.")
heading("Condensation and critical path")
paragraph("Each SCC becomes one vertex. Every original inter-component edge keeps its weight; parallel inter-component edges are retained and intra-component edges are omitted. A directed cycle among condensed vertices would make those components mutually reachable, contradicting that they are different SCCs, so condensation is always a DAG. Longest paths use the same topological induction with maximum instead of minimum, avoiding int negation overflow; all zero-indegree components start at zero. This is equivalent to negating weights, solving shortest paths and negating distances, with appropriate long arithmetic. The cyclic fixture yields four components, two inter-component edges and a critical path of weight 13. This is a path through components, not a solution to longest simple paths inside an original SCC; internal durations need an explicit model.")
paragraph("In scheduling, a critical path determines the earliest possible completion time when edge weights model nonnegative activity durations and predecessor constraints must hold. Arbitrary negative weights remain mathematically valid in DAG path calculations, although they are not normal durations. In general graphs, unrestricted longest walks can be unbounded because of positive cycles, while longest simple path is NP-hard. Negative weights are likewise safe for DAG shortest paths because no negative cycle exists; in general graphs a reachable negative cycle destroys a finite shortest-walk optimum, and negative edges alone already invalidate Dijkstra's greedy rule.")
heading("Tests and counter accounting")
paragraph("JUnit 5 passes 15 test methods with no failures: 1,000 random DSU unions checked against set labels; 100 connected MST instances; 100 tiny forests checked by exhaustive edge subsets; 100 SCC instances up to 200 vertices checked by BFS in both directions; 150 random DAGs with negative and parallel edges checked against Bellman-Ford, for both shortest and longest paths; 100 random condensations; malformed files and four committed fixtures; overflow, empty, isolated, loop and cycle cases; 100,000-vertex path AND cycle with a 256 KiB JVM stack. Both SCC algorithms survive, and reconstructed paths are checked edge by edge with their selected weights.")
paragraph("Counter contract: findCalls counts public find invocations; successfulUnions counts only merges; comparisons counts merge-sort/heap weight comparisons and DAG candidate-distance comparisons, not vertex IDs. maxDepth is the maximum explicit DFS frame depth for Kosaraju; for Tarjan it is the larger of the frame and active-stack maxima, not their sum. Queue size is not DFS depth. CSV edge_scans records traversal/relaxation work: Kosaraju 2m, Kahn m, DAG relaxation m. Transpose construction and DAG's internal Kahn preparation are instrumented separately in preparationEdgeScans (m each), excluded from the prescribed CSV columns but included in timings. Cached indegrees are maintained when edges are added.")
heading("Reproduction and specification decisions")
paragraph("Run mvn clean verify, java -Xmx2g -jar target/assignment3-graphs.jar benchmark, then python scripts/report.py; scripts/reproduce.ps1 and scripts/reproduce.sh combine these steps. The report and charts are regenerated from the resulting CSV. Source requires JDK 17+; Python is needed only to regenerate the report. Follow the explicit ten size/density pairs and 50-row requirement; the deliverables' phrase 'four generated graphs' is inconsistent with those detailed instructions. README documents AI assistance, commands, repository and release. Git contains main, the five requested feature branches and v1.0, with incremental implementation/test commits.")

out=ROOT/"output"/"pdf"; out.mkdir(parents=True,exist_ok=True)
def decorate(canvas, doc):
    canvas.setStrokeColor(colors.HexColor("#b4c8d2")); canvas.line(48,43,A4[0]-48,43)
    canvas.setFont("Helvetica",8); canvas.setFillColor(colors.HexColor("#4e6674"))
    canvas.drawString(48,29,"Alikhan Yerbol | SE-2517 | DAA Assignment 3")
    canvas.drawRightString(A4[0]-48,29,f"{doc.page} / 5")
document=SimpleDocTemplate(str(out/"REPORT.pdf"),pagesize=A4,rightMargin=48,leftMargin=48,topMargin=38,bottomMargin=54,
    title="DAA Assignment 3 - City Network & Job Scheduler",author="Alikhan Yerbol, SE-2517")
document.build(story,onFirstPage=decorate,onLaterPages=decorate)
if document.page != 5:
    raise RuntimeError(f"Expected exactly five report pages, generated {document.page}")
(ROOT/"REPORT.md").write_text("\n".join(markdown),encoding="utf-8")
with (RESULTS/"analysis.csv").open("w",newline="",encoding="utf-8") as f:
    writer=csv.writer(f); writer.writerow(["algorithm","density","scans_slope","operations_slope","comparisons_slope"])
    for a in ALGORITHMS:
        for d in ("sparse","dense"):
            data=series(a,d)
            writer.writerow([a,d,f'{slope(data,lambda r:r["edge_scans"]):.6f}',f'{slope(data,operations):.6f}',f'{slope(data,lambda r:r["comparisons"]):.6f}' if all(r["comparisons"]>0 for r in data) else ""])
print("Generated three PNG plots, REPORT.md, output/pdf/REPORT.pdf and results/analysis.csv")
