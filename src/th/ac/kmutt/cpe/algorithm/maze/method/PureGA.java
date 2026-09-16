package th.ac.kmutt.cpe.algorithm.maze.method;

import th.ac.kmutt.cpe.algorithm.maze.structure.MazeData;

/**
 * Pure genetic algorithm: travelling RANDOMLY ONLY.
 *
 * Deliberately has none of the guidance used by {@link GeneticAlgorithm}:
 * no goal-directed move selection, no distance map, no repair operators.
 * It still evolves with selection, elitism, crossover and mutation, which makes it
 * the raw baseline the guided variant is compared against.
 *
 * The search logic here is unchanged from the original implementation.
 */
public class PureGA extends AbstractGeneticAlgorithm {

    public PureGA(MazeData data, SolverListener listener, GeneticSettings settings) {
        super(data, listener, settings, "PureGA");
    }

    @Override
    public void solve() {
        final int populationSize = Math.max(10, settings.getGaPopulation());
        // Used only as a post-solution improvement budget; before reaching goal we keep running.
        final int improveGenerationsBudget = Math.max(1, settings.getGaGenerations());
        int area = data.N() * data.M();
        int estSteps = Math.max(data.N() + data.M(), estimateShortestSteps());
        // Random-only genomes need extra slack to have a chance to reach the goal.
        final int genomeLength = Math.max(estSteps * 4, Math.min(800, Math.max(200, area)));
        final double mutationRate = settings.getGaMutationRate();
        final java.util.Random rnd = new java.util.Random();

        // Helper to evaluate a genome
        // routeCost: true path cost (sum of entered cell weights, excluding start)
        // fitness: GA score used to guide search (routeCost + penalties)
        class EvalResult { int fitness; int routeCost; java.util.List<int[]> path; boolean reached; }
        java.util.function.Function<int[], EvalResult> evaluate = genome -> {
            // reset temp visited
            boolean[][] seen = new boolean[data.N()][data.M()];
            int x = data.getEntranceX(), y = data.getEntranceY();
            int fitness = 0;
            int routeCost = 0;
            java.util.ArrayList<int[]> path = new java.util.ArrayList<>();
            path.add(new int[]{x,y});
            seen[x][y] = true;
            for (int i=0;i<genome.length;i++) {
                int move = genome[i]%4;
                int[] d = DIRECTIONS[move];
                int nx = x + d[0], ny = y + d[1];
                if (!data.inArea(nx, ny) || data.getMazeChar(nx,ny)!=MazeData.ROAD) {
                    fitness += 50; // heavier penalty for invalid move
                    continue;
                }
                x = nx; y = ny;
                int w = data.weight!=null?data.weight[x][y]:1;
                int stepCost = w>0?w:1;
                routeCost += stepCost;
                fitness += stepCost;
                if (!seen[x][y]) {
                    seen[x][y]=true;
                } else {
                    fitness += 2; // small loop penalty
                }
                path.add(new int[]{x,y});
                if (x==data.getExitX() && y==data.getExitY()) break;
            }
            boolean reached = (x==data.getExitX() && y==data.getExitY());
            if (!reached) {
                // Random-only: penalize based on Manhattan distance to goal.
                int d = Math.abs(x - data.getExitX()) + Math.abs(y - data.getExitY());
                fitness += d * 200;
            }
            // Shorter solutions slightly preferred among ties
            fitness += path.size();
            EvalResult r = new EvalResult();
            r.fitness=fitness;
            r.routeCost=routeCost;
            r.path=path;
            r.reached=reached;
            return r;
        };

        class Candidate {
            final int[] genome;
            final EvalResult eval;
            Candidate(int[] genome, EvalResult eval) { this.genome = genome; this.eval = eval; }
        }

        // Initialize population (random only)
        java.util.List<int[]> pop = new java.util.ArrayList<>(populationSize);
        for (int i=0;i<populationSize;i++){
            int[] g=new int[genomeLength];
            for(int j=0;j<genomeLength;j++) g[j]=rnd.nextInt(4);
            pop.add(g);
        }

        int bestRouteCost = Integer.MAX_VALUE;
        int bestSteps = Integer.MAX_VALUE;
        java.util.List<int[]> bestPath=null;
        boolean bestReached=false;
        long t0 = System.nanoTime();
        int gen = 0;
        int stagnation = 0;
        int improveGen = 0;
        // Keep running until we reach the goal. After reaching, try to improve a bit.
        // NOTE: there is no generation cap before the goal is found, so an unsolvable
        // maze never terminates. See refactor.md.
        while (true) {
            // Evaluate with alignment
            java.util.List<Candidate> candidates = new java.util.ArrayList<>(populationSize);
            for (int[] g : pop) candidates.add(new Candidate(g, evaluate.apply(g)));
            // Sort by reached then shortest routeCost (and steps) for finished paths;
            // otherwise by fitness for unfinished paths.
            candidates.sort((a, b) -> {
                int c1 = (a.eval.reached ? 0 : 1);
                int c2 = (b.eval.reached ? 0 : 1);
                if (c1 != c2) return Integer.compare(c1, c2);
                if (a.eval.reached) {
                    int rc = Integer.compare(a.eval.routeCost, b.eval.routeCost);
                    if (rc != 0) return rc;
                    return Integer.compare(a.eval.path.size(), b.eval.path.size());
                }
                return Integer.compare(a.eval.fitness, b.eval.fitness);
            });
            // Elitism
            java.util.List<int[]> next = new java.util.ArrayList<>(populationSize);
            int eliteCount = Math.max(1, Math.min(settings.getGaElitismCount(), populationSize-1));
            for (int i=0;i<eliteCount;i++) {
                int[] elite = candidates.get(i).genome.clone();
                next.add(elite);
            }
            // Track best
            EvalResult br = candidates.get(0).eval;
            boolean improved = false;
            if (br.reached) {
                if (!bestReached
                    || br.routeCost < bestRouteCost
                    || (br.routeCost == bestRouteCost && br.path.size() < bestSteps)) {
                    bestReached = true;
                    bestRouteCost = br.routeCost;
                    bestSteps = br.path.size();
                    bestPath = br.path;
                    improved = true;
                }
            }

            if (improved) stagnation = 0; else stagnation++;

            // Animate occasionally to keep UI responsive
            if (br.path != null && gen % 5 == 0) {
                clearTransientMarks();
                renderTravellingPath(br.path);
            }
            // Before reaching the goal: never stop; if stuck, re-seed population and keep going.
            if (!bestReached && stagnation > 200) {
                pop = new java.util.ArrayList<>(populationSize);
                for (int i = 0; i < populationSize; i++) {
                    int[] g = new int[genomeLength];
                    for (int j = 0; j < genomeLength; j++) g[j] = rnd.nextInt(4);
                    pop.add(g);
                }
                stagnation = 0;
                gen++;
                continue;
            }
            // After reaching the goal: stop when we stop improving for a while or exceed improvement budget.
            if (bestReached) {
                improveGen++;
                if (stagnation > 120) break;
                if (improveGen >= improveGenerationsBudget) break;
            }
            // Crossover + mutation to refill
            while (next.size() < populationSize) {
                int parentPool = Math.max(eliteCount, Math.min(populationSize, 20));
                int[] p1 = candidates.get(rnd.nextInt(parentPool)).genome;
                int[] p2 = candidates.get(rnd.nextInt(parentPool)).genome;
                int[] child = new int[genomeLength];
                int cut = 1 + rnd.nextInt(genomeLength-1);
                System.arraycopy(p1, 0, child, 0, cut);
                System.arraycopy(p2, cut, child, cut, genomeLength-cut);
                // mutation
                for (int j=0;j<genomeLength;j++) {
                    if (rnd.nextDouble() < mutationRate) child[j] = rnd.nextInt(4);
                }
                next.add(child);
            }
            pop = next;
            gen++;
        }
        long t1 = System.nanoTime();
        // Render best path (paint finished route only once)
        resetState();
        if (bestReached && bestPath != null) {
            for (int[] cell : bestPath) {
                int bx = cell[0], by = cell[1];
                if (data.inArea(bx, by)) data.result[bx][by] = true;
            }
            listener.render();
        }
        // Final report: show only the best route metrics
        Integer finalCost = (bestReached && bestPath != null ? computeRouteCost(bestPath) : null);
        Integer finalSteps = (bestReached && bestPath != null ? bestPath.size() : null);
        Integer finalVisited = (bestReached && bestPath != null ? countUnique(bestPath) : null);
        listener.updateMetrics(finalCost, finalSteps, finalVisited, (t1-t0)/1_000_000L, algoName);
        animateStep(-1, -1, false);
    }
}
