package th.ac.kmutt.cpe.algorithm.maze.method;

import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;
import th.ac.kmutt.cpe.algorithm.maze.structure.PathNode;

/**
 * Shared behaviour for every maze solver: the grid being searched, the channel
 * used to report progress, and the small helpers that all algorithms need.
 *
 * Solvers no longer hold a reference to the Swing window. The animation that used
 * to live in Position is here instead, driven through {@link SolverListener}.
 */
public abstract class AbstractSolver {

    /** The 4-neighbourhood, in the order up, right, down, left. */
    protected static final int[][] DIRECTIONS = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

    protected final MazeData data;
    protected final SolverListener listener;

    protected AbstractSolver(MazeData data, SolverListener listener) {
        this.data = data;
        this.listener = listener;
    }

    /** Run the search, animating progress and reporting metrics when finished. */
    public abstract void solve();

    /** Mark a cell as currently explored, show it, and pause for one animation step. */
    protected void animateStep(int x, int y) {
        animateStep(x, y, true);
    }

    /**
     * Show one animation step. Out-of-area coordinates are ignored for marking but
     * still trigger a render and a pause; the algorithms use (-1, -1) as a final
     * "step finished" signal, so that behaviour is deliberate.
     */
    protected void animateStep(int x, int y, boolean isPath) {
        if (data.inArea(x, y)) {
            data.path[x][y] = isPath;
        }
        listener.render();
        listener.pause();
    }

    /**
     * Walk a finished route backwards from {@code end} to the entrance, marking every
     * cell and returning how many cells the route contains (including the entrance).
     */
    protected int markPath(PathNode end) {
        int steps = 0;
        for (PathNode cur = end; cur != null; cur = cur.getPrev()) {
            data.result[cur.getX()][cur.getY()] = true;
            steps++;
        }
        return steps;
    }

    /** Clear every search marking and repaint an empty maze. */
    protected void resetState() {
        data.clearSearchState();
        listener.setTitle("Maze Solver - " + MazeData.labelOf(data));
        listener.render();
    }
}
