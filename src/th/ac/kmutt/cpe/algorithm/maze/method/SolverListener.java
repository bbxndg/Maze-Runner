package th.ac.kmutt.cpe.algorithm.maze.method;

/**
 * Everything a solver needs from the outside world: somewhere to draw progress,
 * somewhere to report results, and the current animation speed.
 *
 * Solvers talk to this interface instead of the Swing window directly, which is
 * what lets them run headlessly (see the tests under test/).
 */
public interface SolverListener {

    /** Repaint the maze in its current state (visited/path/result markings). */
    void render();

    /** Update the window title, e.g. to show the algorithm name. */
    void setTitle(String title);

    /**
     * Report the finished result.
     *
     * @param cost    total weighted cost of the route, or null when the algorithm
     *                does not compute one (BFS) or found no route
     * @param steps   cells on the route including the entrance, or null if unsolved
     * @param visited cells expanded while searching
     * @param timeMs  wall-clock time of the search in milliseconds
     * @param algoName display name of the algorithm that ran
     */
    void updateMetrics(Integer cost, Integer steps, Integer visited, Long timeMs, String algoName);

    /** Sleep for one animation step, so a search can be watched as it runs. */
    void pause();
}
