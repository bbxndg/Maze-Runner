package th.ac.kmutt.cpe.algorithm.maze.method;

import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;

/**
 * Picks the algorithm for a run and clears the previous search first.
 *
 * A fresh solver is built for every run. The algorithms keep no state between runs
 * (all search state lives in MazeData), so this replaces the old approach of holding
 * one instance of each solver and injecting data/frame/Position into it by hand.
 *
 * The listener and the genetic settings are supplied by the caller, which is what
 * lets the same dispatch work for the Swing window and for headless tests.
 */
public class Run {

    private MazeData data;
    private final SolverListener listener;
    private final GeneticSettings geneticSettings;

    public Run(MazeData data, SolverListener listener, GeneticSettings geneticSettings) {
        this.data = data;
        this.listener = listener;
        this.geneticSettings = geneticSettings;
    }

    /** Point subsequent runs at a different maze (used after importing one). */
    public void setData(MazeData newData) {
        this.data = newData;
    }

    public void runWithAlgorithm(String algo) {
        data.clearSearchState();
        solverFor(algo).solve();
    }

    private AbstractSolver solverFor(String algo) {
        switch (algo) {
            case "BFS":
                return new BFS(data, listener);
            case "A*":
                return new AStar(data, listener);
            case "Genetic":
                return new GeneticAlgorithm(data, listener, geneticSettings);
            case "PureGA":
                return new PureGA(data, listener, geneticSettings);
            case "Dijkstra":
            default:
                return new Dijkstra(data, listener);
        }
    }
}
