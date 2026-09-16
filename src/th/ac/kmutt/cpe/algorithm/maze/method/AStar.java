package th.ac.kmutt.cpe.algorithm.maze.method;

import java.util.Comparator;
import java.util.PriorityQueue;
import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;
import th.ac.kmutt.cpe.algorithm.maze.structure.Node;

/**
 * A* search: Dijkstra ordered by accumulated cost plus a Manhattan-distance
 * estimate of the remaining cost.
 *
 * Note that the estimate ignores cell weights while weights can be larger than 1,
 * so it is not guaranteed to be admissible and the route found is not strictly
 * guaranteed to be the cheapest one. Dijkstra on the same maze is the reference
 * for the minimum weighted cost.
 */
public class AStar extends AbstractSolver {

    public AStar(MazeData data, SolverListener listener) {
        super(data, listener);
    }

    @Override
    public void solve() {
        int rows = data.N(), cols = data.M();
        int[][] dist = new int[rows][cols];
        for (int i=0;i<rows;i++) for(int j=0;j<cols;j++) dist[i][j]=Integer.MAX_VALUE;
        Node start = new Node(data.getEntranceX(), data.getEntranceY(), 0, null);
        Node goal = new Node(data.getExitX(), data.getExitY(), 0, null);
        dist[start.x][start.y] = 0;

        Comparator<Node> cmp = (a,b) -> Integer.compare(a.cost + heuristic(a, goal), b.cost + heuristic(b, goal));
        PriorityQueue<Node> open = new PriorityQueue<>(cmp);
        open.add(start);

        boolean isSolved=false; int visitedCount=0; long t0=System.nanoTime(); Node end=null;
        while(!open.isEmpty()){
            Node cur = open.poll();
            if (data.visited[cur.x][cur.y]) continue;
            data.visited[cur.x][cur.y] = true; visitedCount++;
            animateStep(cur.x, cur.y);
            if (cur.x==goal.x && cur.y==goal.y){ isSolved=true; end=cur; break; }
            for(int[] d:DIRECTIONS){
                int nx=cur.x+d[0], ny=cur.y+d[1];
                if(!data.inArea(nx,ny) || data.getMazeChar(nx,ny)!=MazeData.ROAD || data.visited[nx][ny]) continue;
                int stepCost = data.weight!=null && data.weight[nx][ny]>0 ? data.weight[nx][ny] : 1;
                int newCost = cur.cost + stepCost;
                if(newCost < dist[nx][ny]){ dist[nx][ny]=newCost; open.add(new Node(nx,ny,newCost,cur)); }
            }
        }
        long t1=System.nanoTime();
        if(isSolved && end!=null){ int steps=markPath(end); long ms=(t1-t0)/1_000_000L; listener.updateMetrics(end.cost, steps, visitedCount, ms, "A*"); }
        else { listener.updateMetrics(null, null, visitedCount, (System.nanoTime()-t0)/1_000_000L, "A*"); }
        animateStep(-1,-1,false);
    }

    private int heuristic(Node a, Node goal){
        return Math.abs(a.x - goal.x) + Math.abs(a.y - goal.y);
    }
}
