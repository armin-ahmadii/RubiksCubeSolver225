package rubikscube;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;

/**
 * Two-phase Rubik's Cube solver using the cubie-level RubiksCube model.
 *
 * Phase 1 heuristic:
 *   - cornerOrientationIndex  (3^7 states)
 *   - edgeOrientationIndex    (2^11 states)
 *   - sliceEdgeIndex          (C(12,4) states)
 *
 * Phase 2 heuristic:
 *   - corner permutation index (8! states)
 *   - U/D edge permutation index for edges {0,1,2,3,8,9,10,11} (8! states)
 *
 * Search:
 *   - Phase 1: IDDFS in full move set {U,R,F,D,L,B} into the subgroup
 *     (all orientations solved + UD-slice edges in middle layer).
 *   - Phase 2: IDDFS in restricted move set {U, U2, U', D, D2, D', R2, L2, F2, B2}
 *     to solved. Internally we use compound moves, but we always expand them
 *     to pure quarter-turns (U,R,F,D,L,B) in the output string.
 */
public class Solver {

    // ------------------------------------------------------------
    // Time limit (for the SEARCH part). Table building is done first.
    // ------------------------------------------------------------
    private static final long TIME_LIMIT_MS = 120000; // 120 seconds
    private static long deadlineMs;

    // Phase 1 move set: 6 quarter-turns
    private static final char[] PHASE1_MOVES = { 'U', 'R', 'F', 'D', 'L', 'B' };

    // Phase 2 move set encoded as 0..9; each expands to quarter-turns:
    // 0: U,  1: U2, 2: U'
    // 3: D,  4: D2, 5: D'
    // 6: R2, 7: L2, 8: F2, 9: B2
    private static final int[] PHASE2_MOVES = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

    // ------------------------------------------------------------
    // Phase 1 pruning tables (CO, EO, Slice)
    // ------------------------------------------------------------
    private static final int N_CO    = 2187; // 3^7
    private static final int N_EO    = 2048; // 2^11
    private static final int N_SLICE = 495;  // C(12,4)
    private static final int N_COEO  = N_CO * N_EO; // joint orientation space

    private static byte[] phase1CornerDist = new byte[N_CO];
    private static byte[] phase1EdgeDist   = new byte[N_EO];
    private static byte[] phase1SliceDist  = new byte[N_SLICE];
    private static byte[] phase1OriDist    = new byte[N_COEO];

    private static boolean phase1Built = false;

    // ------------------------------------------------------------
    // Phase 2 pruning tables (CP, U/D edges)
    // ------------------------------------------------------------
    private static final int N_CP = 40320;  // 8!
    private static final int N_UD = 40320;  // 8!

    private static byte[] phase2CornerDist = new byte[N_CP];
    private static byte[] phase2UDEdgeDist = new byte[N_UD];

    private static boolean phase2Built = false;

    // U/D edge cubies in label order: UF, UL, UB, UR, DF, DL, DB, DR
    // Correspond to edge indices {0,1,2,3,8,9,10,11}
    private static final int[] UDEDGE_LABELS    = { 0, 1, 2, 3, 8, 9, 10, 11 };
    private static final int[] UDEDGE_POSITIONS = { 0, 1, 2, 3, 8, 9, 10, 11 };

    // Visited transposition caches (store shallowest g/f-cost seen)
    private static final java.util.HashMap<Long, Integer> phase1Visited = new java.util.HashMap<>();

    // ------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------
    public static void main(String[] args) {
        if (args.length < 2 || (args.length % 2) != 0) {
            System.out.println("File names are not specified");
            System.out.println("usage: java " +
                    MethodHandles.lookup().lookupClass().getName() +
                    " input_file output_file [input2 output2 ...]");
            return;
        }

        try {
            // Build Phase 1 tables (unbounded time)
            if (!phase1Built) {
                System.out.println("Building Phase-1 orientation + slice tables...");
                buildPhase1Tables();
                phase1Built = true;
                System.out.println("Phase-1 CO table max depth    = "
                                   + maxDepth(phase1CornerDist));
                System.out.println("Phase-1 EO table max depth    = "
                                   + maxDepth(phase1EdgeDist));
                System.out.println("Phase-1 slice table max depth = "
                                   + maxDepth(phase1SliceDist));
                System.out.println("Phase-1 tables built.");
            }

            // Build Phase 2 tables (unbounded time)
            if (!phase2Built) {
                System.out.println("Building Phase-2 permutation tables...");
                buildPhase2Tables();
                phase2Built = true;
                System.out.println("Phase-2 CP table max depth       = "
                                   + maxDepth(phase2CornerDist));
                System.out.println("Phase-2 U/D edge table max depth = "
                                   + maxDepth(phase2UDEdgeDist));
                System.out.println("Phase-2 tables built.");
            }

            // Process each input/output pair inside this JVM (avoids rebuilds)
            for (int i = 0; i < args.length; i += 2) {
                String inFile = args[i];
                String outFile = args[i + 1];
                System.out.println("\n=== Solving " + inFile + " -> " + outFile + " ===");

                RubiksCube cube = new RubiksCube(inFile);

                // Set deadline AFTER tables are ready
                deadlineMs = System.currentTimeMillis() + TIME_LIMIT_MS;

                System.out.println("Starting two-phase search...");
                String solution = solveTwoPhase(cube);

                if (solution == null) {
                    System.out.println("No solution found within time limit.");
                    solution = ""; // output empty solution string
                } else {
                    System.out.println("Solution found: " + solution);
                    System.out.println("Length (quarter-turns): " + solution.length());
                }

                // Write solution to file
                try (FileWriter fw = new FileWriter(outFile)) {
                    fw.write(solution);
                }
            }

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // Utility: find max depth in a distance table
    // ------------------------------------------------------------
    private static int maxDepth(byte[] dist) {
        int max = 0;
        for (byte b : dist) {
            int d = b & 0xFF;
            if (d > max) max = d;
        }
        return max;
    }

    // ------------------------------------------------------------
    // Time check
    // ------------------------------------------------------------
    private static boolean isTimedOut() {
        return System.currentTimeMillis() > deadlineMs;
    }

    // ------------------------------------------------------------
    // Phase 1 pruning table build (CO, EO, Slice)
    // ------------------------------------------------------------
    private static void buildPhase1Tables() {
        for (int i = 0; i < N_CO; i++)    phase1CornerDist[i] = -1;
        for (int i = 0; i < N_EO; i++)    phase1EdgeDist[i]   = -1;
        for (int i = 0; i < N_SLICE; i++) phase1SliceDist[i]  = -1;
        for (int i = 0; i < N_COEO; i++)  phase1OriDist[i]    = -1;

        ArrayDeque<RubiksCube> queue = new ArrayDeque<>();
        RubiksCube solved = new RubiksCube();

        int startCo    = solved.cornerOrientationIndex();
        int startEo    = solved.edgeOrientationIndex();
        int startSlice = solved.sliceEdgeIndex();
        int startOri   = startCo * N_EO + startEo;

        phase1CornerDist[startCo]   = 0;
        phase1EdgeDist[startEo]     = 0;
        phase1SliceDist[startSlice] = 0;
        phase1OriDist[startOri]     = 0;

        queue.add(solved);

        while (!queue.isEmpty()) {
            RubiksCube cur = queue.poll();

            int curCoIdx = cur.cornerOrientationIndex();
            int curDepth = phase1CornerDist[curCoIdx] & 0xFF;

            for (char move : PHASE1_MOVES) {
                RubiksCube next = new RubiksCube(cur);
                applyPhase1Move(next, move);

                int nCo    = next.cornerOrientationIndex();
                int nEo    = next.edgeOrientationIndex();
                int nSlice = next.sliceEdgeIndex();
                int nOri   = nCo * N_EO + nEo;

                boolean added = false;

                if (phase1CornerDist[nCo] == -1) {
                    phase1CornerDist[nCo] = (byte) (curDepth + 1);
                    added = true;
                }

                if (phase1EdgeDist[nEo] == -1) {
                    phase1EdgeDist[nEo] = (byte) (curDepth + 1);
                    added = true;
                }

                if (phase1SliceDist[nSlice] == -1) {
                    phase1SliceDist[nSlice] = (byte) (curDepth + 1);
                    added = true;
                }
                if (phase1OriDist[nOri] == -1) {
                    phase1OriDist[nOri] = (byte) (curDepth + 1);
                    added = true;
                }

                if (added) {
                    queue.add(next);
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Phase 2 pruning table build (CP, U/D edges) using phase-2 moves
    // ------------------------------------------------------------
    private static void buildPhase2Tables() {
        for (int i = 0; i < N_CP; i++) phase2CornerDist[i] = -1;
        for (int i = 0; i < N_UD; i++) phase2UDEdgeDist[i] = -1;

        ArrayDeque<RubiksCube> queue = new ArrayDeque<>();
        RubiksCube solved = new RubiksCube();

        int startCP = cornerPermIndex(solved);
        int startUD = udEdgePermIndex(solved);

        phase2CornerDist[startCP] = 0;
        phase2UDEdgeDist[startUD] = 0;

        queue.add(solved);

        while (!queue.isEmpty()) {
            RubiksCube cur = queue.poll();
            int curDepth = phase2CornerDist[cornerPermIndex(cur)] & 0xFF;

            for (int code : PHASE2_MOVES) {
                RubiksCube next = new RubiksCube(cur);
                // We don't care about the returned string here, just the cubie state:
                applyPhase2Move(next, code);

                int cIdx = cornerPermIndex(next);
                int uIdx = udEdgePermIndex(next);

                boolean added = false;

                if (phase2CornerDist[cIdx] == -1) {
                    phase2CornerDist[cIdx] = (byte) (curDepth + 1);
                    added = true;
                }

                if (phase2UDEdgeDist[uIdx] == -1) {
                    phase2UDEdgeDist[uIdx] = (byte) (curDepth + 1);
                    added = true;
                }

                if (added) {
                    queue.add(next);
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Top-level two-phase solve
    // ------------------------------------------------------------
    private static String solveTwoPhase(RubiksCube start) {
        if (start.isSolved()) {
            return "";
        }

        // Total depth limit (in phase-2 atomic moves, not quarter-turns).
        // Bumped to 40 to cover harder scrambles where this two-phase search
        // needs more headroom than the 20 qtm optimal bound.
        final int MAX_TOTAL_DEPTH = 40;

        Phase2GlobalResult globalResult = new Phase2GlobalResult();

        // Clear per-search caches
        phase1Visited.clear();

        int initialH1 = heuristicPhase1(start);
        int minPhase1Depth = initialH1;
        if (minPhase1Depth < 0) minPhase1Depth = 0;

        for (int depth1Limit = minPhase1Depth;
             depth1Limit <= MAX_TOTAL_DEPTH;
             depth1Limit++) {

            if (isTimedOut()) break;
            System.out.println("Searching Phase 1 with depth limit " + depth1Limit);

            // Per-iteration clear avoids over-pruning across deeper limits
            phase1Visited.clear();
            phase1DFS(start, 0, depth1Limit, "",
                      '\0', MAX_TOTAL_DEPTH, globalResult);

            if (globalResult.found) {
                return globalResult.solutionPath;
            }
        }

        return null;
    }

    // ------------------------------------------------------------
    // Check whether cube is in Phase-1 goal subgroup:
    //  - all orientations solved
    //  - slice edges (FR, FL, BL, BR) are in middle layer positions (4..7)
    // ------------------------------------------------------------
    private static boolean isPhase1Goal(RubiksCube c) {
    // 1. All corner orientations must be 0
    for (int i = 0; i < 8; i++) {
        if (c.co[i] != 0) return false;
    }

    // 2. All edge orientations must be 0
    for (int i = 0; i < 12; i++) {
        if (c.eo[i] != 0) return false;
    }

    // 3. The four slice edges must be in positions 4..7 (FR,FL,BL,BR)
    //    BUT their *order* does not matter.
    int sliceCount = 0;
    for (int pos = 4; pos <= 7; pos++) {
        int e = c.ep[pos];
        if (e == 4 || e == 5 || e == 6 || e == 7) sliceCount++;
    }

    return sliceCount == 4;
}


    // ------------------------------------------------------------
    // Phase 1 heuristic (orientation + slice tables)
    // ------------------------------------------------------------
    private static int heuristicPhase1(RubiksCube cube) {
        int coIdx = cube.cornerOrientationIndex();
        int eoIdx = cube.edgeOrientationIndex();
        int slIdx = cube.sliceEdgeIndex();
        int oriIdx = coIdx * N_EO + eoIdx;

        int dCo = phase1CornerDist[coIdx] & 0xFF;
        int dEo = phase1EdgeDist[eoIdx]   & 0xFF;
        int dSl = phase1SliceDist[slIdx]  & 0xFF;
        int dOri = phase1OriDist[oriIdx]  & 0xFF;

        int h = dOri; // joint orientation often tighter
        if (dCo > h) h = dCo;
        if (dEo > h) h = dEo;
        if (dSl > h) h = dSl;
        return h;
    }

    // ------------------------------------------------------------
    // Phase 1 DFS with pruning; for every Phase-1 goal, run Phase 2
    // with remaining depth.
    // ------------------------------------------------------------
    private static void phase1DFS(RubiksCube cube,
                                  int depth1, int depth1Limit,
                                  String path1, char lastMove,
                                  int maxTotalDepth,
                                  Phase2GlobalResult globalResult) {
        if (globalResult.found) return;
        if (isTimedOut()) return;

        int h1 = heuristicPhase1(cube);
        int fScore = depth1 + h1;
        if (fScore > depth1Limit) {
            return;
        }

        long key = phase1Key(cube);
        Integer bestScore = phase1Visited.get(key);
        if (bestScore != null && bestScore <= fScore) {
            return; // already saw this state with equal/ better f-cost
        }
        phase1Visited.put(key, fScore);

        if (isPhase1Goal(cube)) {
            int remainingDepth = maxTotalDepth - depth1;
            System.out.println("Phase-1 solution at depth " + depth1 +
                    ", remaining depth for Phase 2: " + remainingDepth);

            String phase2Path = solvePhase2(cube, remainingDepth);
            if (phase2Path != null) {
                globalResult.found = true;
                globalResult.solutionPath = path1 + phase2Path;
            }
            return;
        }

        if (depth1 >= depth1Limit) return;

        // Move ordering by child heuristic (lower first) to reach goals sooner
        int m = PHASE1_MOVES.length;
        RubiksCube[] kids = new RubiksCube[m];
        int[] hVals = new int[m];
        int[] order = new int[m];
        for (int i = 0; i < m; i++) {
            RubiksCube next = new RubiksCube(cube);
            applyPhase1Move(next, PHASE1_MOVES[i]);
            kids[i] = next;
            hVals[i] = heuristicPhase1(next);
            order[i] = i;
        }
        // selection sort on indices by hVals
        for (int i = 0; i < m; i++) {
            int best = i;
            for (int j = i + 1; j < m; j++) {
                if (hVals[order[j]] < hVals[order[best]]) best = j;
            }
            int tmp = order[i]; order[i] = order[best]; order[best] = tmp;

            int idx = order[i];
            char move = PHASE1_MOVES[idx];
            RubiksCube next = kids[idx];

            // IMPORTANT: we do NOT prune opposite faces or repeated faces here,
            // to respect the requirement that all paths (like UUURRR...) remain allowed.
            if (depth1 + 1 + hVals[idx] > depth1Limit) {
                continue;
            }
            phase1DFS(next, depth1 + 1, depth1Limit,
                      path1 + move, move,
                      maxTotalDepth, globalResult);

            if (globalResult.found || isTimedOut()) return;
        }
    }

    // ------------------------------------------------------------
    // Phase 2: IDDFS limited by depth2Limit, using phase-2 moves
    // ------------------------------------------------------------
    private static String solvePhase2(RubiksCube start, int depth2Limit) {
        int h2 = heuristicPhase2(start);
        if (h2 > depth2Limit) return null;

        for (int bound = h2; bound <= depth2Limit; bound++) {
            if (isTimedOut()) return null;
            java.util.HashMap<Integer, Integer> visited = new java.util.HashMap<>();
            Phase2SearchResult res = phase2DFS(start, 0, bound, "", -1, visited);
            if (res.found) return res.path;
        }
        return null;
    }
    private static boolean samePhase2Face(int c1, int c2) {
        return phase2FaceId(c1) == phase2FaceId(c2);
    }
    private static int phase2FaceId(int code) {
        switch (code) {
            case 0: // U
            case 1: // U2
            case 2: // U'
                return 0; // U family
            case 3: // D
            case 4: // D2
            case 5: // D'
                return 1; // D family
            case 6: // R2
                return 2; // R
            case 7: // L2
                return 3; // L
            case 8: // F2
                return 4; // F
            case 9: // B2
                return 5; // B
            default:
                return -1;
        }
    }

    private static Phase2SearchResult phase2DFS(RubiksCube cube,
                                                int depth,
                                                int bound,
                                                String path,
                                                int lastMoveCode,
                                                java.util.HashMap<Integer, Integer> visited) {
        if (isTimedOut()) return new Phase2SearchResult(false, null);

        int h2 = heuristicPhase2(cube);
        int f = depth + h2;
        if (f > bound) {
            return new Phase2SearchResult(false, null);
        }

        int key = phase2Key(cube);
        Integer bestF = visited.get(key);
        if (bestF != null && bestF <= f) {
            return new Phase2SearchResult(false, null);
        }
        visited.put(key, f);

        if (cube.isSolved()) {
            return new Phase2SearchResult(true, path);
        }

        if (depth >= bound) {
            return new Phase2SearchResult(false, null);
        }

        int m = PHASE2_MOVES.length;
        RubiksCube[] kids = new RubiksCube[m];
        String[] moveStrs = new String[m];
        int[] hVals = new int[m];
        int[] order = new int[m];

        for (int i = 0; i < m; i++) {
            int code = PHASE2_MOVES[i];
            RubiksCube next = new RubiksCube(cube);
            moveStrs[i] = applyPhase2Move(next, code); // quarter-turn expansion
            kids[i] = next;
            hVals[i] = heuristicPhase2(next);
            order[i] = i;
        }

        // order children by heuristic (ascending)
        for (int i = 0; i < m; i++) {
            int best = i;
            for (int j = i + 1; j < m; j++) {
                if (hVals[order[j]] < hVals[order[best]]) best = j;
            }
            int tmp = order[i]; order[i] = order[best]; order[best] = tmp;

            int idx = order[i];
            int code = PHASE2_MOVES[idx];
            RubiksCube next = kids[idx];
            String moveStr = moveStrs[idx];

            if (depth + 1 + hVals[idx] > bound) {
                continue;
            }
            Phase2SearchResult child = phase2DFS(next,
                                                 depth + 1,
                                                 bound,
                                                 path + moveStr,
                                                 code,
                                                 visited);
            if (child.found) return child;
        }

        return new Phase2SearchResult(false, null);
    }

    // ------------------------------------------------------------
    // Phase 2 heuristic using CP & U/D edge permutation tables
    // ------------------------------------------------------------
    private static int heuristicPhase2(RubiksCube cube) {
        int cpIdx = cornerPermIndex(cube);
        int udIdx = udEdgePermIndex(cube);

        int dCP = phase2CornerDist[cpIdx]  & 0xFF;
        int dUD = phase2UDEdgeDist[udIdx] & 0xFF;

        // Admissible joint bound: each move changes both permutation subsets,
        // so ceil((dCP + dUD)/2) is also a lower bound.
        int joint = (dCP + dUD + 1) >> 1;
        int h = dCP;
        if (dUD > h) h = dUD;
        if (joint > h) h = joint;
        return h;
    }

    // ------------------------------------------------------------
    // Apply Phase 1 move (single quarter-turn)
    // ------------------------------------------------------------
    private static void applyPhase1Move(RubiksCube cube, char move) {
        switch (move) {
            case 'U': cube.moveU(); break;
            case 'R': cube.moveR(); break;
            case 'F': cube.moveF(); break;
            case 'D': cube.moveD(); break;
            case 'L': cube.moveL(); break;
            case 'B': cube.moveB(); break;
            default:
                throw new IllegalArgumentException("Unknown phase-1 move: " + move);
        }
    }

    // ------------------------------------------------------------
    // Apply Phase 2 move code atomically; return its quarter-turn expansion
    // ------------------------------------------------------------
    private static String applyPhase2Move(RubiksCube cube, int code) {
        switch (code) {
            case 0: // U
                cube.moveU();
                return "U";
            case 1: // U2
                cube.moveU(); cube.moveU();
                return "UU";
            case 2: // U'
                cube.moveU(); cube.moveU(); cube.moveU();
                return "UUU";
            case 3: // D
                cube.moveD();
                return "D";
            case 4: // D2
                cube.moveD(); cube.moveD();
                return "DD";
            case 5: // D'
                cube.moveD(); cube.moveD(); cube.moveD();
                return "DDD";
            case 6: // R2
                cube.moveR(); cube.moveR();
                return "RR";
            case 7: // L2
                cube.moveL(); cube.moveL();
                return "LL";
            case 8: // F2
                cube.moveF(); cube.moveF();
                return "FF";
            case 9: // B2
                cube.moveB(); cube.moveB();
                return "BB";
            default:
                throw new IllegalArgumentException("Unknown phase-2 move code: " + code);
        }
    }

    // ------------------------------------------------------------
    // Permutation indexing (Lehmer code) for corners & U/D edges
    // ------------------------------------------------------------
    private static int cornerPermIndex(RubiksCube cube) {
        int[] perm = new int[8];
        System.arraycopy(cube.cp, 0, perm, 0, 8);
        return lehmerIndex(perm);
    }

    private static int udEdgePermIndex(RubiksCube cube) {
        int[] perm = new int[8];

        for (int slot = 0; slot < 8; slot++) {
            int pos = UDEDGE_POSITIONS[slot];    // physical position in ep[]
            int edgeCubie = cube.ep[pos];        // 0..11

            int idxInLabels = -1;
            for (int i = 0; i < 8; i++) {
                if (UDEDGE_LABELS[i] == edgeCubie) {
                    idxInLabels = i;
                    break;
                }
            }
            if (idxInLabels == -1) {
                throw new IllegalStateException("Non U/D edge in U/D slot");
            }
            perm[slot] = idxInLabels;
        }

        return lehmerIndex(perm);
    }

    // Packed keys for visited pruning
    private static long phase1Key(RubiksCube cube) {
        int coIdx = cube.cornerOrientationIndex(); // up to 2186
        int eoIdx = cube.edgeOrientationIndex();   // up to 2047
        int slIdx = cube.sliceEdgeIndex();         // up to 494
        // pack into 32 bits: [co:12][eo:11][slice:9]
        return (((long) coIdx) << 20) | (((long) eoIdx) << 9) | slIdx;
    }

    private static int phase2Key(RubiksCube cube) {
        int cpIdx = cornerPermIndex(cube);   // 0..40319
        int udIdx = udEdgePermIndex(cube);   // 0..40319
        return cpIdx * N_UD + udIdx;         // fits in signed int
    }

    // Compute Lehmer code index for a permutation of 0..n-1
    private static int lehmerIndex(int[] perm) {
        int n = perm.length;
        int idx = 0;
        for (int i = 0; i < n; i++) {
            int less = 0;
            for (int j = i + 1; j < n; j++) {
                if (perm[j] < perm[i]) less++;
            }
            idx = idx * (n - i) + less;
        }
        return idx;
    }

    // ------------------------------------------------------------
    // Helper result types
    // ------------------------------------------------------------
    private static class Phase2GlobalResult {
        boolean found = false;
        String  solutionPath = null;
    }

    private static class Phase2SearchResult {
        final boolean found;
        final String  path;

        Phase2SearchResult(boolean found, String path) {
            this.found = found;
            this.path = path;
        }
    }
}
