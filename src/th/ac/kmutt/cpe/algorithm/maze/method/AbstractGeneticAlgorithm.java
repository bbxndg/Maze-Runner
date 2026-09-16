package th.ac.kmutt.cpe.algorithm.maze.method;

import java.util.List;
import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;
import th.ac.kmutt.cpe.algorithm.maze.structure.Position;

/**
 * Helpers shared by the two genetic algorithms.
 *
 * This class used to be duplicated line for line inside GeneticAlgorithm and
 * PureGA. The search logic itself (evaluation, selection, elitism, crossover,
 * mutation and stopping rules) deliberately stays in each subclass, because the
 * two variants are meant to differ.
 */
public abstract class AbstractGeneticAlgorithm extends AbstractSolver {

    protected final GeneticSettings settings;
    protected final String algoName;

    protected AbstractGeneticAlgorithm(MazeData data, SolverListener listener,
                                       GeneticSettings settings, String algoName) {
        super(data, listener);
        this.settings = settings;
        this.algoName = algoName;
    }

    /** Clear the transient exploration marks used by the travelling animation. */
    protected void clearTransientMarks() {
        data.clearPathMarks();
        listener.render();
    }

    /** Animate a candidate route as travelling steps, leaving it drawn afterwards. */
    protected void renderTravellingPath(List<int[]> path) {
        for (int[] cell : path) {
            animateStep(cell[0], cell[1]);
        }
        listener.render();
    }

    /** Estimate shortest steps from entrance to exit using unweighted BFS (ignores weights). */
    protected int estimateShortestSteps() {
        int n = data.N(), m = data.M();
        boolean[][] seen = new boolean[n][m];
        java.util.ArrayDeque<Position> q = new java.util.ArrayDeque<>();
        Position s = new Position(data.getEntranceX(), data.getEntranceY(), null);
        q.add(s);
        seen[s.x][s.y] = true;
        while (!q.isEmpty()) {
            Position cur = q.poll();
            if (cur.x == data.getExitX() && cur.y == data.getExitY()) {
                // count steps via backtracking
                int steps = 0;
                Position p = cur;
                while (p != null) { steps++; p = p.prev; }
                return steps;
            }
            for (int[] d : DIRECTIONS) {
                int nx = cur.x + d[0], ny = cur.y + d[1];
                if (data.inArea(nx, ny) && !seen[nx][ny] && data.getMazeChar(nx,ny)==MazeData.ROAD) {
                    seen[nx][ny] = true;
                    q.add(new Position(nx, ny, cur));
                }
            }
        }
        // fallback to Manhattan distance + padding if unreachable by BFS
        int md = Math.abs(data.getEntranceX()-data.getExitX()) + Math.abs(data.getEntranceY()-data.getExitY());
        return md + 20;
    }

    /** Total weighted cost of a route (sum of entered cell weights, excluding the start). */
    protected int computeRouteCost(List<int[]> path) {
        if (path == null || path.size() < 2) return 0;
        int cost = 0;
        for (int i = 1; i < path.size(); i++) {
            int x = path.get(i)[0];
            int y = path.get(i)[1];
            if (!data.inArea(x, y)) continue;
            int w = (data.weight != null ? data.weight[x][y] : 1);
            cost += (w > 0 ? w : 1);
        }
        return cost;
    }

    /** Number of distinct cells a route touches. */
    protected int countUnique(List<int[]> path) {
        if (path == null) return 0;
        boolean[][] seen = new boolean[data.N()][data.M()];
        int c = 0;
        for (int[] cell : path) {
            int x = cell[0], y = cell[1];
            if (!data.inArea(x, y)) continue;
            if (!seen[x][y]) {
                seen[x][y] = true;
                c++;
            }
        }
        return c;
    }
}
