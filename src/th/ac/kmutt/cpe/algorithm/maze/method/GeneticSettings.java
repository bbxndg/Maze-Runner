package th.ac.kmutt.cpe.algorithm.maze.method;

/**
 * The genetic algorithm parameters exposed by the UI. Kept separate from
 * {@link SolverListener} because only the two genetic algorithms need them.
 */
public interface GeneticSettings {

    int getGaPopulation();

    int getGaGenerations();

    /** Mutation probability per gene, already normalized to 0.0 - 1.0. */
    double getGaMutationRate();

    /** Probability of overriding a move with a goal-directed one, 0.0 - 1.0. */
    double getGaGoalBias();

    int getGaElitismCount();
}
