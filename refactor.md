# Refactoring notes

What changed, why, and what was deliberately left alone.

**Nothing is committed.** All changes are sitting in the working tree on `ben-refactor`,
untracked new files included, so you can review everything with `git status` and
`git diff` before you decide what to stage.

---

## Ground rules I worked under

| Rule | Consequence |
| --- | --- |
| **Strictly behaviour-preserving** | No bug fixes. Every algorithm must produce the same cost/steps/visited as before. Known bugs are documented below, not fixed. |
| **No package renames** | `method` and `structure` keep their names. |
| **`Node`/`Position` may move out of `method`** | Done. They now live in `structure`. |
| **Genetic algorithm logic untouched** | The guided GA and PureGA keep their exact search logic: same evaluation, same penalties, same seeding, same RNG call order, same stopping rules. |
| **Tests without dependencies** | No JUnit, no build system — a plain Java program you compile and run. |
| **No commits, no pushes** | Left entirely to you. |

The plan I proposed earlier had seven phases. Everything behaviour-preserving is done;
the phases that would change visible behaviour are listed under "Deliberately not changed".

---

## The problem this addressed

All 7 files in `method/` used to import and call `MazeFrame` (15 references). A solver
could not run without opening a window: mid-search it called `frame.render(data)` and
`frame.updateMetrics(...)`. That made the algorithms untestable, and it forced every one
of them to carry the same plumbing:

```java
MazeData data;
MazeFrame frame;
Position pos;
private volatile boolean cancelled = false;   // never assigned anywhere in the project
```

`Run` then wired those fields by hand: `bfs.data = data; bfs.frame = frame; ...`.

**Result after this refactor: zero UI imports remain in `method/` and `structure/`.**
A full solve now runs with `-Djava.awt.headless=true`, which the test suite relies on.

---

## How I proved behaviour did not change

This was the priority, so I did it *before* touching any code.

1. **Baseline capture.** I wrote a throwaway harness (kept outside the repo, in `/tmp`)
   that drove the *original* code through its real path (`new Run(data, frame)` +
   `runWithAlgorithm(...)`) with animation disabled, and recorded cost/steps/visited for
   every algorithm on every maze.
2. **Refactor.**
3. **Regression test.** The same numbers are embedded in `test/.../SolverTest.java` as
   expected values. All of them still match, exactly, including the Genetic results —
   which are reproducible because that algorithm seeds its RNG with `new Random(42)`.
4. **Sensitivity check.** A test that always passes is worthless, so I mutated one
   expected value (`ASTAR_VISITED` 111 → 999) and confirmed the suite fails loudly and
   reports `1 CHECK(S) FAILED`, then reverted it.

Expected values for the three smallest mazes (the full 13-maze table is in the test):

| Maze | Dijkstra cost/steps/visited | A\* cost/steps/visited | BFS steps/visited | Genetic cost/steps/visited |
| --- | --- | --- | --- | --- |
| `m15_15` | 125 / 25 / 112 | 125 / 25 / 111 | 25 / 114 | 134 / 25 / 25 |
| `m24_20` | 188 / 41 / 274 | 188 / 41 / 260 | 41 / 283 | 219 / 41 / 41 |
| `m30_30` | 270 / 57 / 492 | 270 / 57 / 467 | 57 / 509 | 291 / 57 / 57 |

`PureGA` is the exception: it seeds its RNG randomly, so it genuinely cannot be
snapshotted (I measured 127, 133 and 125 for the same maze on three consecutive runs).
It gets a smoke test with a time limit instead.

---

## Changes, part by part

### 1. Solvers no longer know about the UI

Two new interfaces in `method/`:

```java
public interface SolverListener {
    void render();
    void setTitle(String title);
    void updateMetrics(Integer cost, Integer steps, Integer visited, Long timeMs, String algoName);
    void pause();                 // sleep for one animation step
}

public interface GeneticSettings { /* population, generations, mutation, goal bias, elitism */ }
```

`MazeFrame implements SolverListener, GeneticSettings` — its `updateMetrics`, inherited
`setTitle`, plus two small new methods `render()` (repaint) and `pause()` (delegate to
`MazeUtil.pause(getDelayMs())`). The sleep deliberately still goes through
`MazeUtil.pause`, so the interrupt handling is byte-for-byte the same as before.

New `AbstractSolver` holds what every algorithm shares: `MazeData`, the listener, the
`DIRECTIONS` constant, and three helpers that used to be copy-pasted:

- `animateStep(x, y)` — replaces `pos.setData(x, y, true)`. Marks the cell, renders, pauses.
  Out-of-area coordinates still render and pause without marking, because the algorithms
  use `(-1, -1)` as a final "step finished" signal.
- `markPath(pathNode)` — walks a route back to the entrance, marks `result`, returns the
  cell count. This replaces the two near-identical `Position.findPath(Node)` /
  `findPath(Position)` overloads, now unified by the `PathNode` interface.
- `resetState()` — clears the grid, retitles, repaints.

Each solver is now a small class with a constructor and a `solve()` method:

```java
public class BFS extends AbstractSolver {
    public BFS(MazeData data, SolverListener listener) { super(data, listener); }

    @Override
    public void solve() { ... unchanged search ... }
}
```

### 2. `Node` and `Position` moved to `structure`, and `Position` lost its UI duties

Both moved out of `method/` (via deletion + re-add, so `git status` shows them as
deleted and untracked).

`Position` used to hold `MazeData data`, `MazeFrame frame` and a `setData(...)` method —
it was simultaneously a path node *and* the step animator. The animation now lives in
`AbstractSolver.animateStep`, so `Position` is a pure `(x, y, prev)` node with no UI
knowledge. That is what allowed it to move to `structure` without an illegal dependency
from `structure` back into the UI package.

A small `PathNode` interface (`getX`, `getY`, `getPrev`) is implemented by both `Node`
and `Position`, which is what lets `markPath` accept either. Because the classes changed
package, the fields and constructor that were package-private had to become `public`.

Note: their fields are still accessed directly (`cur.x`, `new Position(nx, ny, cur)`), so
`Node` and `Position` remain very similar classes. Merging them into one is listed as
deferred work below.

### 3. The two genetic algorithms share a base class

`GeneticAlgorithm` and `PureGA` used to be 172 byte-identical lines. Seven private helper
methods existed twice, character for character: `estimateShortestSteps`, `computeRouteCost`,
`countUnique`, `clearTransientMarks`, `renderTravellingPath`, `resetState`, `getMazeLabel`.

They now live once, in a new `AbstractGeneticAlgorithm`, moved verbatim apart from the
`frame` → `listener` / `pos.setData` → `animateStep` rename. The two subclasses keep their
own search logic, exactly as you designed it:

```
GeneticAlgorithm  443 → 344 lines
PureGA            323 → 207 lines
identical lines between them: 172 → 97
```

The remaining ~97 lines are the GA skeleton itself (the `evaluate` lambda, the `Candidate`
class, the sort comparators, the elitism and crossover loops). I left it duplicated on
purpose, since collapsing it into a template method would restructure the logic you asked
me not to change. See the deferred list.

### 4. `MazeData` owns state clearing, and labels

Six copies of the same triple loop (clearing `visited`/`path`/`result`) existed across
`Main`, `Run`, and both GAs. Now:

- `clearSearchState()` — clears all three arrays (replaces all six copies)
- `clearPathMarks()` — clears only `path` (used by the GA animation)
- `labelOf(data)` — static, replaces three copies of `getMazeLabel()`

⚠️ **`labelOf` on purpose still returns the object hash.** `MazeData` neither overrides
`toString()` nor keeps its file name, so the original `getMazeLabel()` always fell through
to `data.toString()`, i.e. the window title shows something like
`Maze Solver - th.ac.kmutt.cpe.algorithm.maze.structure.MazeData@1b6d3586`. Fixing that
means storing the file name and adding a real `toString()`, which **changes the visible
title**, so it is out of scope for a behaviour-preserving refactor. There is a comment
saying exactly this at the method.

### 5. `Run` and `Main` got simpler

`Run` no longer holds one instance of each solver, and no longer injects fields into them.
It builds a fresh solver per run (the algorithms keep no state between runs — everything
lives in `MazeData`) and its constructor now takes the two interfaces instead of a
`MazeFrame`, which is what makes headless testing possible:

```java
public Run(MazeData data, SolverListener listener, GeneticSettings geneticSettings)
```

`Main` lost its duplicated `getMazeLabel()` and the reset loop, and the dead
`BLOCK_SIZE` comment. Its threading, cancel attempt and import handling are untouched.

### 6. Dead code removed

| Removed | Where | Why safe |
| --- | --- | --- |
| `private volatile boolean cancelled` | all 5 algorithms | Never assigned `true` anywhere in the project, so `!cancelled` was always true |
| Unreachable `if (pos == null)` fallback in `renderTravellingPath` | `PureGA` | `Run` always supplied a `Position`, so the branch never ran |
| `resetGaParametersToDefaults()` | `MazeFrame` | No callers |
| Commented-out `BLOCK_SIZE` | `Main` | Commented-out constant |

`getBlockSize()` / `resizeToBlock()` were kept: `Main` calls both during import.

---

## Running the tests

```bash
./run-tests.sh
```

It compiles the app plus the test into `bin-test/` and runs it with
`-Djava.awt.headless=true`. No dependencies, no display, no build system. It exits
non-zero on failure. Run it from the project root (the mazes are loaded by relative path).
`bin-test/` was added to `.gitignore`.

`test/th/ac/kmutt/cpe/algorithm/maze/test/SolverTest.java` replaces `MazeFrame` with a
`RecordingListener` that captures metrics instead of drawing. **228 checks:**

1. **Exact snapshots** — cost/steps/visited for Dijkstra, A\* and BFS on all 13 mazes,
   plus Genetic on the 3 smallest. This is the behaviour-preservation net.
2. **Route validity** — for all 13 mazes × all 4 algorithms, the cells marked in
   `result` must all be walkable and must form a connected route from `S` to `G`.
3. **Cross-checks** — A\* cost equals Dijkstra cost on every maze, A\* visits no more
   cells than Dijkstra, BFS never uses more steps than Dijkstra. (The first is an
   observed property, not a guarantee: A\*'s heuristic ignores weights and so can
   overestimate. Noted in a comment in the test.)
4. **Listener wiring** — the solver actually rendered and paused, reported its own name,
   and an unknown algorithm name still falls back to Dijkstra.
5. **PureGA smoke test** — randomized and unbounded, so it runs on a daemon thread with a
   60s limit; a timeout is reported as a failure rather than hanging the suite.

Current output:

```
224 checks passed   (before the PureGA test was added)
228/228 checks passed
ALL TESTS PASSED
```

---

## Deliberately not changed (deferred backlog)

Each of these is a *behaviour* change, so it needs its own decision and its own before/after.

| # | Item | Why it was left | Impact if you fix it |
| --- | --- | --- | --- |
| 1 | `MazeFrame.paint` draws `S`/`G` with swapped row/column accessors | Bug fix | Goal marker appears on non-square mazes (it currently never does). Root cause is the `entranceX` = row / `entranceY` = column naming. |
| 2 | Window title shows the object hash | Bug fix | Needs a file-name field + real `toString()` on `MazeData`. |
| 3 | `Reset` does not stop a running solve | Bug fix | `Main` sets its own `cancelled` and interrupts the thread, but nothing propagates that into the algorithms. Needs a real token passed to `AbstractSolver`. I removed the dead per-solver flags so this gap is now visible rather than implied. |
| 4 | `PureGA` never terminates on an unsolvable maze | Bug fix | It has no generation cap before the goal is reached; with the flags removed its loop reads `while (true)`. Needs a cap or a working cancel. |
| 5 | Metrics/repaints happen on the worker thread | Bug fix | Swing is not thread-safe; renders should be marshalled with `SwingUtilities.invokeLater`. |
| 6 | `MazeFrame` is still a 313-line class doing window + controls + GA params + metrics + canvas + painting | Would restructure the UI | Split into `MazeFrame` / `ControlPanel` / `MazeCanvas` / `MazeRenderer` + a settings record. Needs visual checking, which I cannot do from here. |
| 7 | `method` package name, and `entranceX/Y` coordinate naming | Asked not to rename | Clearer names, but churns every import / every call site. |
| 8 | Full GA template-method extraction (~97 duplicated lines left) | Asked not to change GA logic | One `AbstractGeneticAlgorithm` skeleton with `evaluate`/`mutate`/`shouldStop` hooks. Riskiest item here: the RNG call order must stay identical or results change. |

---

## For your review

- `git status` — new files are untracked: `run-tests.sh`, `test/`, `AbstractSolver.java`,
  `AbstractGeneticAlgorithm.java`, `SolverListener.java`, `GeneticSettings.java`,
  `structure/PathNode.java`, and the moved `structure/Node.java` / `structure/Position.java`
  (their old copies show as deleted from `method/`).
- `git diff` — 233 insertions, 481 deletions across the modified files. The biggest
  concerns are the two GA files; I checked their diffs line by line and the only changes
  are `frame` → `listener`/`settings`, `directions` → `DIRECTIONS`, `pos.setData` →
  `animateStep`, the removal of the always-false `cancelled` guards, and deletion of the
  helpers that moved to the base class. No constant, comparison or loop order changed.
- Worth doing before you trust it: run the GUI yourself (`run.bat` or the two `javac`/`java`
  commands in the README) and click through all five algorithms, including Import and
  Reset. My verification of the Swing path was limited to "it compiles and the window
  opens without crashing" — I could not see or click the window.
- `README.md` was updated to match the new file layout and test command.
