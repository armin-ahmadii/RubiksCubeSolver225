package rubikscube;

import java.io.*;
import java.util.*;

public class Solver {
    private static final long TIME_LIMIT = 9000; // 9 seconds
    private static long startTime;
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("File names are not specified");
            System.out.println("usage: java rubikscube.Solver input_file output_file");
            return;
        }
        
        try {
            startTime = System.currentTimeMillis();
            RubiksCube cube = new RubiksCube(args[0]);
            
            System.out.println("Initial cube:");
            System.out.println(cube);
            System.out.println("Solved corners: " + cube.countSolvedCorners() + "/8");
            System.out.println("Solved edges: " + cube.countSolvedEdges() + "/12");
            System.out.println();
            
            String solution = solve(cube);
            
            // Expand shorthand notation (R3 -> RRR, F2 -> FF, etc.)
            solution = expandSolution(solution);
            
            try (FileWriter writer = new FileWriter(args[1])) {
                writer.write(solution);
            }
            
            System.out.println("\nSolution: " + solution);
            System.out.println("Length: " + solution.length() + " moves");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String solve(RubiksCube cube) {
        if (cube.isSolved()) {
            System.out.println("Already solved!");
            return "";
        }
        
        // Try BFS first for shallow solutions
        System.out.println("=== Trying BFS (depth 6) ===");
        String result = solveBFS(cube, 6);
        if (result != null) {
            System.out.println("BFS found solution!");
            return result;
        }
        
        System.out.println("\n=== Trying Bidirectional BFS (depth 5+5) ===");
        result = solveBidirectional(cube, 5);
        if (result != null) {
            System.out.println("Bidirectional BFS found solution!");
            return result;
        }
        
        System.out.println("\n=== Trying BFS (depth 8) ===");
        result = solveBFS(cube, 8);
        if (result != null) {
            System.out.println("BFS found solution!");
            return result;
        }
        
        System.out.println("\n=== Trying Bidirectional BFS (depth 6+6) ===");
        result = solveBidirectional(cube, 6);
        if (result != null) {
            System.out.println("Bidirectional BFS found solution!");
            return result;
        }
        
        // Try IDA* for deeper solutions
        System.out.println("\n=== Trying IDA* ===");
        result = solveIDAStar(cube, false);
        if (result != null) {
            System.out.println("IDA* found solution!");
            return result;
        }
        
        System.out.println("\n=== Trying IDA* with 18 moves ===");
        result = solveIDAStar(cube, true);
        if (result != null) {
            System.out.println("IDA* (18 moves) found solution!");
            return result;
        }
        
        System.out.println("No solution found within time limit");
        return "";
    }
    
    private static String solveBFS(RubiksCube cube, int maxDepth) {
        Queue<CubeState> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.add(new CubeState(cube, ""));
        visited.add(cube.getStateString());
        
        String[] moves = {"F", "B", "L", "R", "U", "D"};
        int nodesExplored = 0;
        
        while (!queue.isEmpty()) {
            if (System.currentTimeMillis() - startTime > TIME_LIMIT) {
                System.out.println("BFS timeout (explored " + nodesExplored + " nodes)");
                return null;
            }
            
            CubeState current = queue.poll();
            nodesExplored++;
            
            if (current.movesPath.length() >= maxDepth) continue;
            
            for (String move : moves) {
                // Better pruning: allow up to 3 consecutive same-face moves
                // Don't allow 4+ (since X4 = identity)
                if (current.movesPath.length() >= 3) {
                    char last1 = current.movesPath.charAt(current.movesPath.length() - 1);
                    char last2 = current.movesPath.charAt(current.movesPath.length() - 2);
                    char last3 = current.movesPath.charAt(current.movesPath.length() - 3);
                    if (last1 == move.charAt(0) && last2 == move.charAt(0) && last3 == move.charAt(0)) {
                        continue; // Already have 3 of same move
                    }
                }
                
                // Don't do opposite faces consecutively (F then B is wasteful)
                if (current.movesPath.length() > 0) {
                    char lastMove = current.movesPath.charAt(current.movesPath.length() - 1);
                    if (areOppositeFaces(lastMove, move.charAt(0))) {
                        continue;
                    }
                }
                
                RubiksCube newCube = new RubiksCube(current.cube);
                applyMoveString(newCube, move);
                
                if (newCube.isSolved()) {
                    System.out.println("Explored " + nodesExplored + " nodes");
                    return current.movesPath + move;
                }
                
                String state = newCube.getStateString();
                if (!visited.contains(state)) {
                    visited.add(state);
                    queue.add(new CubeState(newCube, current.movesPath + move));
                }
            }
            
            if (nodesExplored % 10000 == 0) {
                System.out.println("Explored " + nodesExplored + " nodes...");
            }
        }
        
        System.out.println("BFS exhausted (explored " + nodesExplored + " nodes)");
        return null;
    }
    
    // Bidirectional BFS - search from both start and goal
    private static String solveBidirectional(RubiksCube startCube, int maxDepthEach) {
        // Forward search from scrambled state
        Map<String, String> forwardPaths = new HashMap<>();
        Queue<CubeState> forwardQueue = new LinkedList<>();
        
        // Backward search from solved state
        Map<String, String> backwardPaths = new HashMap<>();
        Queue<CubeState> backwardQueue = new LinkedList<>();
        
        RubiksCube solvedCube = new RubiksCube();
        
        forwardQueue.add(new CubeState(startCube, ""));
        forwardPaths.put(startCube.getStateString(), "");
        
        backwardQueue.add(new CubeState(solvedCube, ""));
        backwardPaths.put(solvedCube.getStateString(), "");
        
        String[] moves = {"F", "B", "L", "R", "U", "D"};
        int nodesExplored = 0;
        
        // Alternate between forward and backward search
        for (int depth = 0; depth <= maxDepthEach; depth++) {
            if (System.currentTimeMillis() - startTime > TIME_LIMIT) {
                System.out.println("Bidirectional timeout (explored " + nodesExplored + " nodes)");
                return null;
            }
            
            // Expand forward frontier
            int forwardSize = forwardQueue.size();
            for (int i = 0; i < forwardSize; i++) {
                CubeState current = forwardQueue.poll();
                if (current.movesPath.length() >= depth) {
                    forwardQueue.add(current);
                    continue;
                }
                
                for (String move : moves) {
                    RubiksCube newCube = new RubiksCube(current.cube);
                    applyMoveString(newCube, move);
                    String state = newCube.getStateString();
                    String newPath = current.movesPath + move;
                    
                    // Check if we've met in the middle
                    if (backwardPaths.containsKey(state)) {
                        String backPath = backwardPaths.get(state);
                        String solution = newPath + reverseSequence(backPath);
                        System.out.println("Met in middle! Explored " + nodesExplored + " nodes");
                        return solution;
                    }
                    
                    if (!forwardPaths.containsKey(state)) {
                        forwardPaths.put(state, newPath);
                        forwardQueue.add(new CubeState(newCube, newPath));
                        nodesExplored++;
                    }
                }
            }
            
            // Expand backward frontier
            int backwardSize = backwardQueue.size();
            for (int i = 0; i < backwardSize; i++) {
                CubeState current = backwardQueue.poll();
                if (current.movesPath.length() >= depth) {
                    backwardQueue.add(current);
                    continue;
                }
                
                for (String move : moves) {
                    RubiksCube newCube = new RubiksCube(current.cube);
                    applyMoveString(newCube, move);
                    String state = newCube.getStateString();
                    String newPath = current.movesPath + move;
                    
                    // Check if we've met in the middle
                    if (forwardPaths.containsKey(state)) {
                        String forwardPath = forwardPaths.get(state);
                        String solution = forwardPath + reverseSequence(newPath);
                        System.out.println("Met in middle! Explored " + nodesExplored + " nodes");
                        return solution;
                    }
                    
                    if (!backwardPaths.containsKey(state)) {
                        backwardPaths.put(state, newPath);
                        backwardQueue.add(new CubeState(newCube, newPath));
                        nodesExplored++;
                    }
                }
            }
            
            if (nodesExplored % 10000 == 0 && nodesExplored > 0) {
                System.out.println("Explored " + nodesExplored + " nodes (depth " + depth + ")...");
            }
        }
        
        System.out.println("Bidirectional exhausted (explored " + nodesExplored + " nodes)");
        return null;
    }
    
    // Reverse a move sequence (F -> F3, F2 -> F2, F3 -> F)
    private static String reverseSequence(String moves) {
        StringBuilder reversed = new StringBuilder();
        for (int i = moves.length() - 1; i >= 0; i--) {
            char move = moves.charAt(i);
            // Check if next char is a number
            if (i < moves.length() - 1 && Character.isDigit(moves.charAt(i + 1))) {
                continue; // Skip, we'll handle it below
            }
            
            // Check if current has a number after it
            if (i + 1 < moves.length() && moves.charAt(i + 1) == '2') {
                reversed.append(move).append('2');
            } else if (i + 1 < moves.length() && moves.charAt(i + 1) == '3') {
                reversed.append(move); // F3 reversed is F
            } else {
                reversed.append(move).append('3'); // F reversed is F3
            }
        }
        return reversed.toString();
    }
    
    // Expand shorthand notation: F2 -> FF, F3 -> FFF, etc.
    private static String expandSolution(String solution) {
        StringBuilder expanded = new StringBuilder();
        for (int i = 0; i < solution.length(); i++) {
            char move = solution.charAt(i);
            
            // Check if next character is a digit
            if (i + 1 < solution.length() && Character.isDigit(solution.charAt(i + 1))) {
                int count = Character.getNumericValue(solution.charAt(i + 1));
                for (int j = 0; j < count; j++) {
                    expanded.append(move);
                }
                i++; // Skip the digit
            } else {
                expanded.append(move);
            }
        }
        return expanded.toString();
    }
    
    private static String solveIDAStar(RubiksCube cube, boolean use18Moves) {
        int bound = betterHeuristic(cube);
        String path = "";
        
        while (bound < 25) {
            if (System.currentTimeMillis() - startTime > TIME_LIMIT) {
                System.out.println("IDA* timeout");
                return null;
            }
            
            System.out.println("IDA* searching with bound: " + bound);
            IDAResult result = idaSearch(cube, path, 0, bound, "", use18Moves);
            
            if (result.found) {
                return result.path;
            }
            if (result.newBound == Integer.MAX_VALUE) {
                return null;
            }
            bound = result.newBound;
        }
        
        return null;
    }
    
    private static IDAResult idaSearch(RubiksCube cube, String path, int g, int bound, String lastMove, boolean use18Moves) {
        int f = g + betterHeuristic(cube);
        
        if (f > bound) {
            return new IDAResult(false, "", f);
        }
        
        if (cube.isSolved()) {
            return new IDAResult(true, path, 0);
        }
        
        if (System.currentTimeMillis() - startTime > TIME_LIMIT) {
            return new IDAResult(false, "", Integer.MAX_VALUE);
        }
        
        int min = Integer.MAX_VALUE;
        String[] moves = use18Moves ? get18Moves() : get6Moves();
        
        for (String move : moves) {
            // Allow up to 3 consecutive same-face moves
            int consecutiveCount = 0;
            for (int i = path.length() - 1; i >= 0; i--) {
                if (path.charAt(i) == move.charAt(0)) {
                    consecutiveCount++;
                } else {
                    break;
                }
            }
            if (consecutiveCount >= 3) continue; // Already have 3 of same move
            
            // Skip opposite faces in sequence
            if (lastMove.length() > 0 && areOppositeFaces(lastMove.charAt(0), move.charAt(0))) {
                continue;
            }
            
            RubiksCube newCube = new RubiksCube(cube);
            applyMoveString(newCube, move);
            
            IDAResult result = idaSearch(newCube, path + move, g + 1, bound, move, use18Moves);
            
            if (result.found) {
                return result;
            }
            
            min = Math.min(min, result.newBound);
        }
        
        return new IDAResult(false, "", min);
    }
    
    private static int betterHeuristic(RubiksCube cube) {
        if (cube.isSolved()) return 0;
        
        int solvedCorners = cube.countSolvedCorners();
        int solvedEdges = cube.countSolvedEdges();
        
        int unsolvedCorners = 8 - solvedCorners;
        int unsolvedEdges = 12 - solvedEdges;
        
        // Stronger heuristic - each move solves at most 1-2 pieces typically
        // Use ceiling division to be more accurate
        int cornerEstimate = (unsolvedCorners + 1) / 2;
        int edgeEstimate = (unsolvedEdges + 1) / 2;
        
        return Math.max(cornerEstimate, edgeEstimate);
    }
    
    private static String[] get6Moves() {
        return new String[]{"F", "B", "L", "R", "U", "D"};
    }
    
    private static String[] get18Moves() {
        return new String[]{
            "F", "F2", "F3",
            "B", "B2", "B3",
            "L", "L2", "L3",
            "R", "R2", "R3",
            "U", "U2", "U3",
            "D", "D2", "D3"
        };
    }
    
    private static boolean areOppositeFaces(char f1, char f2) {
        return (f1 == 'F' && f2 == 'B') || (f1 == 'B' && f2 == 'F') ||
               (f1 == 'L' && f2 == 'R') || (f1 == 'R' && f2 == 'L') ||
               (f1 == 'U' && f2 == 'D') || (f1 == 'D' && f2 == 'U');
    }
    
    private static void applyMoveString(RubiksCube cube, String move) {
        switch(move) {
            case "F": cube.moveF(); break;
            case "F2": cube.moveF2(); break;
            case "F3": cube.moveF3(); break;
            case "B": cube.moveB(); break;
            case "B2": cube.moveB2(); break;
            case "B3": cube.moveB3(); break;
            case "L": cube.moveL(); break;
            case "L2": cube.moveL2(); break;
            case "L3": cube.moveL3(); break;
            case "R": cube.moveR(); break;
            case "R2": cube.moveR2(); break;
            case "R3": cube.moveR3(); break;
            case "U": cube.moveU(); break;
            case "U2": cube.moveU2(); break;
            case "U3": cube.moveU3(); break;
            case "D": cube.moveD(); break;
            case "D2": cube.moveD2(); break;
            case "D3": cube.moveD3(); break;
        }
    }
    
    static class CubeState {
        RubiksCube cube;
        String movesPath;
        
        CubeState(RubiksCube cube, String movesPath) {
            this.cube = cube;
            this.movesPath = movesPath;
        }
    }
    
    static class IDAResult {
        boolean found;
        String path;
        int newBound;
        
        IDAResult(boolean found, String path, int newBound) {
            this.found = found;
            this.path = path;
            this.newBound = newBound;
        }
    }
}