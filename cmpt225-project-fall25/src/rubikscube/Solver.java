package rubikscube;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;

/**
 * Rubik's Cube Solver entry point.
 *
 * Usage:
 *   java rubikscube.Solver input_file output_file
 *
 * It:
 *   1. Parses the cube from input_file using Cube(String filename)
 *   2. Searches for a sequence of moves using iterative deepening DFS
 *   3. Writes the move sequence (e.g., "URFU...") as a single line to output_file
 */
public class Solver {

    // Allowed face turns (90° clockwise)
    private static final char[] MOVES = {'U', 'D', 'L', 'R', 'F', 'B'};

    // Safety limits – you can tweak these if needed
    private static final int MAX_DEPTH = 8;        // max search depth
    private static final long TIME_LIMIT_MS = 9000; // ~9 seconds, under the 10s spec

    // Will hold the solution once found
    private static String foundSolution = null;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("File names are not specified");
            System.out.println("usage: java " +
                    MethodHandles.lookup().lookupClass().getName() +
                    " input_file output_file");
            return;
        }

        String inputFileName = args[0];
        String outputFileName = args[1];

        // 1. Build initial cube from the scramble file
        Cube start = new Cube(inputFileName);

        // 2. Search for a solution sequence
        String solution = solve(start);

        // 3. Write the solution to the output file
        File outputFile = new File(outputFileName);
        try (PrintWriter out = new PrintWriter(outputFile)) {
            out.println(solution); // must end with newline
        } catch (IOException e) {
            System.err.println("Error writing solution file: " + e.getMessage());
        }
    }

    /**
     * High-level solve routine.
     * Returns a string of moves like "URUUURRR".
     */
    private static String solve(Cube start) {
        // If it's already solved, just return empty line (no moves needed)
        if (start.isSolved()) {
            return "";
        }

        long deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
        foundSolution = null;

        // Iterative deepening: try depth 1, 2, ..., MAX_DEPTH
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            StringBuilder path = new StringBuilder();
            if (dfs(start, 0, depth, path, deadline, '\0')) {
                break; // foundSolution is set inside dfs
            }
            if (System.currentTimeMillis() > deadline) {
                break; // ran out of time
            }
        }

        // If we never found a solution within the limits, return empty solution.
        // (The cube will not actually be solved in that case, but at least the
        // program respects the I/O contract.)
        if (foundSolution == null) {
            return "";
        }
        return foundSolution;
    }

    /**
     * Depth-limited DFS with backtracking.
     *
     * @param cube      current cube state (mutated in-place then undone)
     * @param depth     current depth in the search tree
     * @param maxDepth  maximum depth limit for this iteration
     * @param path      sequence of moves from the root to this node
     * @param deadline  absolute time (ms) when we should stop searching
     * @param lastMove  last face turned (for optional pruning)
     * @return true if a solution was found (foundSolution is set)
     */
    private static boolean dfs(Cube cube,
                               int depth,
                               int maxDepth,
                               StringBuilder path,
                               long deadline,
                               char lastMove) {

        // Stop if we run out of time
        if (System.currentTimeMillis() > deadline) {
            return false;
        }

        // Check goal
        if (cube.isSolved()) {
            foundSolution = path.toString();
            return true;
        }

        // Depth limit reached – stop expanding this branch
        if (depth == maxDepth) {
            return false;
        }

        // Try all face turns
        for (char m : MOVES) {
            // Simple pruning: avoid doing the exact same face 3 times in a row, etc.
            // (You can get fancier if you want, but this is optional.)
            // For now, we don't forbid repeating the same move – keeps it simple.

            // Apply move
            Moves.applyMove(cube, m);
            path.append(m);

            if (dfs(cube, depth + 1, maxDepth, path, deadline, m)) {
                return true; // foundSolution is set; bubble up
            }

            // Backtrack: undo the move and remove from path
            path.deleteCharAt(path.length() - 1);
            // To undo one clockwise quarter turn, apply it 3 more times
            Moves.applyMove(cube, m);
            Moves.applyMove(cube, m);
            Moves.applyMove(cube, m);
        }

        return false;
    }
}
