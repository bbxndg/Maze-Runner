package th.ac.kmutt.cpe.algorithm.maze.method;

import java.util.ArrayDeque;
import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;
import th.ac.kmutt.cpe.algorithm.maze.structure.Position;

/**
 * Breadth-first search. Explores evenly in all directions, so it finds the route
 * with the fewest steps. Cell weights are ignored, which is why it reports no cost.
 */
public class BFS extends AbstractSolver {

    public BFS(MazeData data, SolverListener listener) {
        super(data, listener);
    }

    @Override
    public void solve() {
        ArrayDeque<Position> queue = new ArrayDeque<>();
        Position entrance = new Position(data.getEntranceX(), data.getEntranceY(), null);
        queue.add(entrance);
        if (data.inArea(entrance.x, entrance.y)) data.visited[entrance.x][entrance.y] = true;

        boolean isSolved = false;
        int visitedCount = 0;
        long t0 = System.nanoTime();
        Position end = null;

        while (!queue.isEmpty()) {
            Position cur = queue.poll();
            visitedCount++;
            animateStep(cur.x, cur.y);
            if (cur.x == data.getExitX() && cur.y == data.getExitY()) { isSolved = true; end = cur; break; }
            for (int[] d : DIRECTIONS) {
                int nx = cur.x + d[0], ny = cur.y + d[1];
                if (data.inArea(nx, ny) && !data.visited[nx][ny] && data.getMazeChar(nx,ny)==MazeData.ROAD) {
                    data.visited[nx][ny] = true;
                    queue.add(new Position(nx, ny, cur));
                }
            }
        }

        long t1 = System.nanoTime();
        if (isSolved && end != null) {
            int steps = markPath(end);
            long ms = (t1 - t0) / 1_000_000L;
            listener.updateMetrics(null, steps, visitedCount, ms, "BFS");
        } else {
            listener.updateMetrics(null, null, visitedCount, (System.nanoTime()-t0)/1_000_000L, "BFS");
        }
        animateStep(-1, -1, false);
    }
}
