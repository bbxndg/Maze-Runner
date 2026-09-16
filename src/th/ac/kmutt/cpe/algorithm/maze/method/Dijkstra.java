package th.ac.kmutt.cpe.algorithm.maze.method;

import java.util.Comparator;
import java.util.PriorityQueue;
import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;
import th.ac.kmutt.cpe.algorithm.maze.structure.Node;

/**
 * Dijkstra's algorithm over the weighted grid: the cheapest reached cell is always
 * expanded next, so the route found has the minimum total cost.
 */
public class Dijkstra extends AbstractSolver {

    public Dijkstra(MazeData data, SolverListener listener) {
        super(data, listener);
    }

    @Override
    public void solve() {
        // Dijkstra's algorithm on grid with per-cell weights
        int rows = data.N();
        int cols = data.M();
        int[][] dist = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) dist[i][j] = Integer.MAX_VALUE;
        }

        Node start = new Node(data.getEntranceX(), data.getEntranceY(), 0, null);
        if (data.inArea(start.x, start.y)) {
            dist[start.x][start.y] = 0;
        }

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        pq.add(start);

        boolean isSolved = false;
        int visitedCount = 0;
        long t0 = System.nanoTime();
        Node endNode = null;

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (data.visited[cur.x][cur.y]) continue; // finalized already
            data.visited[cur.x][cur.y] = true;
            visitedCount++;

            animateStep(cur.x, cur.y); // visualize exploration

            if (cur.x == data.getExitX() && cur.y == data.getExitY()) {
                isSolved = true;
                endNode = cur;
                break;
            }

            for (int[] d : DIRECTIONS) {
                int nx = cur.x + d[0];
                int ny = cur.y + d[1];
                if (!data.inArea(nx, ny)) continue;
                if (data.getMazeChar(nx, ny) != MazeData.ROAD) continue; // skip walls
                if (data.visited[nx][ny]) continue;
                int stepCost = 1;
                if (data.weight != null) {
                    int w = data.weight[nx][ny];
                    stepCost = (w > 0 ? w : 1);
                }
                int newCost = (cur.cost == Integer.MAX_VALUE ? Integer.MAX_VALUE : cur.cost + stepCost);
                if (newCost < dist[nx][ny]) {
                    dist[nx][ny] = newCost;
                    pq.add(new Node(nx, ny, newCost, cur));
                }
            }
        }

        long t1 = System.nanoTime();

        if (isSolved && endNode != null) {
            int steps = markPath(endNode); // mark result path and count steps
            int totalCost = endNode.cost;
            long ms = (t1 - t0) / 1_000_000L;
            listener.updateMetrics(totalCost, steps, visitedCount, ms, "Dijkstra");
        } else {
            listener.updateMetrics(null, null, visitedCount, (System.nanoTime()-t0)/1_000_000L, "Dijkstra");
            System.out.println("The maze has NO solution!");
        }
        animateStep(-1, -1, false);
    }
}
