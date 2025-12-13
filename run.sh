#!/bin/bash

echo "=== Maze Solver UI ==="
echo "Compiling Java files..."

# Compile all Java files
javac -d bin -sourcepath src src/th/ac/kmutt/cpe/algorithm/maze/**/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Starting Maze Solver UI..."
    echo ""
    echo "Available maze files:"
    ls MAZE/*.txt 2>/dev/null | head -5
    echo ""
    echo "Use the GUI to select algorithms: Dijkstra, BFS, A*, Genetic"
    
    # Run the application
    java -cp bin th.ac.kmutt.cpe.algorithm.maze.Main
else
    echo "Compilation failed!"
    exit 1
fi
