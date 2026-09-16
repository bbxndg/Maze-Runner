package th.ac.kmutt.cpe.algorithm.maze.structure;

/**
 * A cell that knows how it was reached, so a finished route can be walked
 * backwards from the exit to the entrance.
 *
 * Implemented by both node types used during a search:
 * {@link Node} (priority-queue nodes used by Dijkstra and A*) and
 * {@link Position} (plain queue/path nodes used by BFS and the genetic algorithms).
 */
public interface PathNode {

    int getX();

    int getY();

    /** The cell this one was reached from, or null at the entrance. */
    PathNode getPrev();
}
