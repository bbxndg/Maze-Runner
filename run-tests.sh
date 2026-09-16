#!/bin/bash
# Compile and run the headless solver tests.
# No build system and no dependencies: just a JDK.
#
# Runs headless on purpose (-Djava.awt.headless=true). The solvers no longer touch
# Swing, so if a solver ever starts referencing the window again, this fails loudly.

cd "$(dirname "$0")" || exit 1

echo "=== Maze Solver tests ==="
echo "Compiling application sources and tests..."

rm -rf bin-test
javac -d bin-test -sourcepath src:test test/th/ac/kmutt/cpe/algorithm/maze/test/SolverTest.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Running tests..."
java -Djava.awt.headless=true -cp bin-test th.ac.kmutt.cpe.algorithm.maze.test.SolverTest
