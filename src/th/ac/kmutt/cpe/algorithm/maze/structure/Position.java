package th.ac.kmutt.cpe.algorithm.maze.structure;

/**
 * A plain cell linked back to the cell it was reached from. Used as a queue
 * element and as a walked route by BFS and the genetic algorithms.
 *
 * This class used to also own the step animation (it held a reference to the
 * window and repainted itself). That responsibility now lives in
 * AbstractSolver.animateStep, so this is a pure path node with no UI knowledge.
 */
public class Position implements PathNode {
    public int x, y;
    public Position prev;

    public Position(int x, int y, Position prev) {
        this.x = x;
        this.y = y;
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
