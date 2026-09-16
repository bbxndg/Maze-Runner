package th.ac.kmutt.cpe.algorithm.maze.structure;

/**
 * A cell plus the accumulated cost of reaching it, used as the priority-queue
 * element by Dijkstra and A*. {@code prev} links it back to the cell it was
 * reached from, which is how the final route is reconstructed.
 */
public class Node implements PathNode {
    public int x, y;
    public int cost;
    public Node prev;

    public Node(int x, int y, int cost, Node prev) {
        this.x = x;
        this.y = y;
        this.cost = cost;
        this.prev = prev;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public PathNode getPrev() {
        return prev;
    }
}
