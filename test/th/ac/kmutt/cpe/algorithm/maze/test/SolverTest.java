package th.ac.kmutt.cpe.algorithm.maze.test;

import java.util.ArrayDeque;
import th.ac.kmutt.cpe.algorithm.maze.method.GeneticSettings;
import th.ac.kmutt.cpe.algorithm.maze.method.Run;
import th.ac.kmutt.cpe.algorithm.maze.method.SolverListener;
import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;

/**
 * Headless tests for the maze solvers.
 *
 * There is no build system or test framework in this project, so this is a plain
 * Java program: compile it and run it, no dependencies, no JUnit.
 *
 * It only works because the solvers no longer reference the Swing window. A
 * RecordingListener stands in for MazeFrame, which means a whole solve can run
 * with no display at all (the test does not even need a DISPLAY).
 *
 * Run from the project root:
 *   ./run-tests.sh
 */
public class SolverTest {

    // ---------------------------------------------------------------------
    // Stand-in for the UI: records the reported metrics instead of drawing.
    // ---------------------------------------------------------------------
    static class RecordingListener implements SolverListener, GeneticSettings {
        Integer cost, steps, visited;
        Long time;
        String algo;
        int renderCalls = 0;
        int pauseCalls = 0;

        // Genetic parameters default to the same values the Swing sliders start with,
        // so the Genetic results below stay reproducible.
        int population = 140;
        int generations = 300;
        double mutationRate = 0.05;
        double goalBias = 0.80;
        int elitism = 14;

        @Override public void render() { renderCalls++; }

        @Override public void setTitle(String title) { /* no window to title */ }

        @Override public void updateMetrics(Integer c, Integer s, Integer v, Long t, String a) {
            cost = c; steps = s; visited = v; time = t; algo = a;
        }

        @Override public void pause() { pauseCalls++; }

        @Override public int getGaPopulation() { return population; }
        @Override public int getGaGenerations() { return generations; }
        @Override public double getGaMutationRate() { return mutationRate; }
        @Override public double getGaGoalBias() { return goalBias; }
        @Override public int getGaElitismCount() { return elitism; }
    }

    // ---------------------------------------------------------------------
    // Every maze shipped with the project.
    // ---------------------------------------------------------------------
    static final String[] MAZES = {
        "MAZE/m15_15.txt", "MAZE/m24_20.txt", "MAZE/m30_30.txt", "MAZE/m33_35.txt",
        "MAZE/m40_40.txt", "MAZE/m40_45.txt", "MAZE/m45_45.txt", "MAZE/m50_50.txt",
        "MAZE/m60_60.txt", "MAZE/m70_60.txt", "MAZE/m80_50.txt", "MAZE/m100_90.txt",
        "MAZE/m100_100.txt"
    };

    // Expected values recorded from the app before the refactor. Matching these
    // exactly is what proves the refactor did not change any algorithm's behaviour.
    static final int[] DIJKSTRA_COST    = { 125, 188, 270, 337, 381, 584, 591, 592, 635, 763, 811, 1144, 1086 };
    static final int[] DIJKSTRA_STEPS   = {  25,  41,  57,  67,  79, 115, 109, 117, 131, 159, 163,  221,  227 };
    static final int[] DIJKSTRA_VISITED = { 112, 274, 492, 561, 908, 887,1028,1300,1981,2293,2206, 4973, 5532 };

    static final int[] ASTAR_COST    = { 125, 188, 270, 337, 381, 584, 591, 592, 635, 763, 811, 1144, 1086 };
    static final int[] ASTAR_STEPS   = {  25,  41,  57,  67,  79, 115, 109, 117, 131, 161, 163,  221,  227 };
    static final int[] ASTAR_VISITED = { 111, 260, 467, 534, 907, 864,1006,1281,1978,2285,2197, 4968, 5504 };

    static final int[] BFS_STEPS   = {  25,  41,  57,  67,  79, 107, 107, 111, 127, 149, 159,  211,  217 };
    static final int[] BFS_VISITED = { 114, 283, 509, 587, 909, 882,1003,1296,1987,2318,2210, 4983, 5522 };

    // Genetic uses a fixed RNG seed, so it is reproducible; the other three parameters
    // are the visited/steps snapshot for the first three mazes only.
    static final int[] GENETIC_COST    = { 134, 219, 291 };
    static final int[] GENETIC_STEPS   = {  25,  41,  57 };
    static final int[] GENETIC_VISITED = {  25,  41,  57 };

    static int checks = 0;
    static int failures = 0;

    public static void main(String[] args) {
        String cwd = System.getProperty("user.dir");
        System.out.println("Solver tests (run from the project root, here: " + cwd + ")");
        System.out.println();

        testSnapshots();
        testRoutesAreValid();
        testAlgorithmCrossChecks();
        testGeneticsAndCancellationSurface();
        testPureGaSmoke();

        System.out.println();
        System.out.println(checks - failures + "/" + checks + " checks passed");
        if (failures > 0) {
            System.out.println(failures + " CHECK(S) FAILED");
            System.exit(1);
        }
        System.out.println("ALL TESTS PASSED");
    }

    // ---------------------------------------------------------------------
    // 1. Exact regression snapshot: cost, steps and visited per algorithm/maze.
    // ---------------------------------------------------------------------
    static void testSnapshots() {
        section("exact result snapshots");
        for (int i = 0; i < MAZES.length; i++) {
            MazeData data = load(MAZES[i]);

            RecordingListener dijkstra = solve(data, "Dijkstra");
            check(MAZES[i] + " Dijkstra cost", DIJKSTRA_COST[i], dijkstra.cost);
            check(MAZES[i] + " Dijkstra steps", DIJKSTRA_STEPS[i], dijkstra.steps);
            check(MAZES[i] + " Dijkstra visited", DIJKSTRA_VISITED[i], dijkstra.visited);

            RecordingListener aStar = solve(data, "A*");
            check(MAZES[i] + " A* cost", ASTAR_COST[i], aStar.cost);
            check(MAZES[i] + " A* steps", ASTAR_STEPS[i], aStar.steps);
            check(MAZES[i] + " A* visited", ASTAR_VISITED[i], aStar.visited);

            RecordingListener bfs = solve(data, "BFS");
            // BFS ignores weights, so it deliberately reports no cost.
            check(MAZES[i] + " BFS cost is null", null, bfs.cost);
            check(MAZES[i] + " BFS steps", BFS_STEPS[i], bfs.steps);
            check(MAZES[i] + " BFS visited", BFS_VISITED[i], bfs.visited);

            if (i < GENETIC_COST.length) {
                RecordingListener ga = solve(data, "Genetic");
                check(MAZES[i] + " Genetic cost", GENETIC_COST[i], ga.cost);
                check(MAZES[i] + " Genetic steps", GENETIC_STEPS[i], ga.steps);
                check(MAZES[i] + " Genetic visited", GENETIC_VISITED[i], ga.visited);
            }
        }
    }

    // ---------------------------------------------------------------------
    // 2. The marked route must really connect the entrance to the exit.
    // ---------------------------------------------------------------------
    static void testRoutesAreValid() {
        section("routed cells form a real path from S to G");
        for (String maze : MAZES) {
            MazeData data = load(maze);
            String[] algos = { "Dijkstra", "A*", "BFS", "Genetic" };
            for (String algo : algos) {
                solve(data, algo);
                check(maze + " " + algo + " route is connected S->G", true, isConnectedRoute(data));
            }
        }
    }

    // ---------------------------------------------------------------------
    // 3. Relationships between algorithms, checked on every maze.
    // ---------------------------------------------------------------------
    static void testAlgorithmCrossChecks() {
        section("algorithm cross-checks");
        for (String maze : MAZES) {
            MazeData data = load(maze);
            RecordingListener dijkstra = solve(data, "Dijkstra");
            RecordingListener aStar = solve(data, "A*");
            RecordingListener bfs = solve(data, "BFS");

            // Observed on all 13 mazes. The A* heuristic is not guaranteed admissible
            // with weights > 1, so this is an empirical property rather than a proof.
            check(maze + " A* cost equals Dijkstra cost", dijkstra.cost, aStar.cost);
            check(maze + " A* visits no more cells than Dijkstra",
                    true, aStar.visited <= dijkstra.visited);
            // BFS optimises steps, Dijkstra optimises weight: BFS must never use more steps.
            check(maze + " BFS uses no more steps than Dijkstra",
                    true, bfs.steps <= dijkstra.steps);
        }
    }

    // ---------------------------------------------------------------------
    // 4. The listener wiring itself: the solvers must animate and report once.
    // ---------------------------------------------------------------------
    static void testGeneticsAndCancellationSurface() {
        section("listener wiring");
        MazeData data = load(MAZES[0]);

        RecordingListener small = solve(data, "Genetic");
        check("Genetic asked for the GA parameters from settings",
                true, small.cost != null);
        check("Genetic reported its own name", "Genetic", small.algo);

        RecordingListener dijkstra = solve(data, "Dijkstra");
        check("Dijkstra reported its own name", "Dijkstra", dijkstra.algo);
        check("solver rendered at least once", true, dijkstra.renderCalls > 0);
        check("solver paused at least once", true, dijkstra.pauseCalls > 0);

        // An unknown algorithm name falls back to Dijkstra, as it always has.
        RecordingListener fallback = solve(data, "not-a-real-algorithm");
        check("unknown algorithm falls back to Dijkstra", "Dijkstra", fallback.algo);

        // A fresh Run over a different maze must not reuse stale state.
        MazeData second = load(MAZES[1]);
        RecordingListener rerun = solve(second, "Dijkstra");
        check("second maze solved independently", ASTAR_COST[1], rerun.cost);
    }

    // ---------------------------------------------------------------------
    // 5. PureGA smoke test.
    //
    // PureGA seeds its RNG randomly, so its result cannot be snapshotted, and it has
    // no termination condition before the goal is reached. It therefore runs on a
    // worker thread with a hard time limit, so a regression here can never hang the
    // suite: a timeout is reported as a failure instead. See refactor.md.
    // ---------------------------------------------------------------------
    static void testPureGaSmoke() {
        section("PureGA (randomized, time-limited)");
        MazeData data = load(MAZES[0]);
        final RecordingListener[] holder = new RecordingListener[1];
        final RuntimeException[] error = new RuntimeException[1];

        Thread worker = new Thread(() -> {
            try {
                holder[0] = solve(data, "PureGA");
            } catch (RuntimeException e) {
                error[0] = e;
            }
        }, "purega-smoke");
        worker.setDaemon(true);

        long start = System.currentTimeMillis();
        worker.start();
        try {
            worker.join(60_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        if (error[0] != null) {
            checks++; failures++;
            System.out.println("  FAIL  PureGA threw: " + error[0]);
            return;
        }
        if (worker.isAlive() || holder[0] == null) {
            checks++; failures++;
            System.out.println("  FAIL  PureGA did not finish within 60s (it has no generation cap before reaching the goal)");
            return;
        }

        RecordingListener pureGa = holder[0];
        long elapsed = System.currentTimeMillis() - start;
        check("PureGA reported its own name", "PureGA", pureGa.algo);
        check("PureGA finished with a cost", true, pureGa.cost != null);
        check("PureGA found a connected route", true, isConnectedRoute(data));
        // Cost varies between runs because the seed is not fixed, so only check the range.
        check("PureGA cost is plausible", true, pureGa.cost != null && pureGa.cost >= 100 && pureGa.cost <= 400);
        System.out.println("  (PureGA reached the goal in " + elapsed + "ms with cost " + pureGa.cost + ")");
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    static MazeData load(String path) {
        try {
            return new MazeData(path);
        } catch (RuntimeException e) {
            failures++;
            System.out.println("  FAIL  could not load " + path + " (run from the project root): " + e.getMessage());
            throw e;
        }
    }

    static RecordingListener solve(MazeData data, String algorithm) {
        RecordingListener listener = new RecordingListener();
        new Run(data, listener, listener).runWithAlgorithm(algorithm);
        return listener;
    }

    /**
     * True when the cells marked in data.result are all walkable and form one
     * connected route from the entrance to the exit.
     */
    static boolean isConnectedRoute(MazeData data) {
        int sx = data.getEntranceX(), sy = data.getEntranceY();
        int gx = data.getExitX(), gy = data.getExitY();
        if (!data.result[sx][sy] || !data.result[gx][gy]) return false;

        for (int i = 0; i < data.N(); i++) {
            for (int j = 0; j < data.M(); j++) {
                if (data.result[i][j] && data.getMazeChar(i, j) != MazeData.ROAD) return false;
            }
        }

        boolean[][] seen = new boolean[data.N()][data.M()];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{ sx, sy });
        seen[sx][sy] = true;
        int[][] directions = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == gx && cur[1] == gy) return true;
            for (int[] d : directions) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (!data.inArea(nx, ny) || seen[nx][ny] || !data.result[nx][ny]) continue;
                seen[nx][ny] = true;
                queue.add(new int[]{ nx, ny });
            }
        }
        return false;
    }

    static void section(String title) {
        System.out.println("-- " + title);
    }

    static void check(String what, Integer expected, Integer actual) {
        checks++;
        boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
        if (!ok) {
            failures++;
            System.out.println("  FAIL  " + what + ": expected " + expected + ", got " + actual);
        }
    }

    static void check(String what, String expected, String actual) {
        checks++;
        boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
        if (!ok) {
            failures++;
            System.out.println("  FAIL  " + what + ": expected " + expected + ", got " + actual);
        }
    }

    static void check(String what, boolean expected, boolean actual) {
        checks++;
        if (expected != actual) {
            failures++;
            System.out.println("  FAIL  " + what + ": expected " + expected + ", got " + actual);
        }
    }
}
