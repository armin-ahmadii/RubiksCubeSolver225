package rubikscube;

/**
 * Move logic for the cubie model.
 *
 * Conventions (must match Cube.java):
 *  Corners (slots/pieces 0..7): URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB
 *  Edges   (slots/pieces 0..11): UF, UL, UB, UR, FR, FL, BL, BR, DF, DL, DB, DR
 *
 * Face turns are 90° clockwise as viewed from outside that face.
 * Orientation updates (UD-based edge orientation):
 *  - Corners: U,D: +0;  R,F: +1;  L,B: +2  (mod 3)
 *  - Edges:   F,B: +1 (flip); U,D,R,L: +0 (mod 2)
 */
public final class Moves {

    private Moves() {}

    // ---- Core public API ----------------------------------------------------

    /** Apply a single 90° clockwise face turn: one of 'F','B','R','L','U','D'. */
    public static void applyMove(Cube c, char move) {
        switch (move) {
            case 'U': turn(c, U_CORNER, U_EDGE, 0, 0); break;
            case 'D': turn(c, D_CORNER, D_EDGE, 0, 0); break;
            case 'R': turn(c, R_CORNER, R_EDGE, 1, 0); break; // corners +1
            case 'L': turn(c, L_CORNER, L_EDGE, 2, 0); break; // corners +2 (i.e., -1)
            case 'F': turn(c, F_CORNER, F_EDGE, 1, 1); break; // corners +1, edges flip
            case 'B': turn(c, B_CORNER, B_EDGE, 2, 1); break; // corners +2, edges flip
            default: throw new IllegalArgumentException("Unknown move: " + move);
        }
    }

    /**
     * Apply a sequence like "R U R' U' F2".
     * Supports spaces, primes (X'), and doubles (X2). Uppercase expected.
     */
    public static void applySequence(Cube c, String seq) {
        if (seq == null) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char m = t.charAt(0);
            if ("FBRLUD".indexOf(m) < 0)
                throw new IllegalArgumentException("Bad token: " + t);
            if (t.length() == 1) {
                applyMove(c, m);
            } else {
                char suf = t.charAt(1);
                if (suf == '\'') { // prime: 3 clockwise turns
                    applyMove(c, m); applyMove(c, m); applyMove(c, m);
                } else if (suf == '2') { // double
                    applyMove(c, m); applyMove(c, m);
                } else {
                    throw new IllegalArgumentException("Bad suffix in token: " + t);
                }
            }
        }
    }

    // ---- Internal turn implementation --------------------------------------

    /**
     * Rotate the 4-cycle on corners and edges.
     * Then add cornerDelta to those 4 corner ORIs (mod 3), and edgeDelta to edge ORIs (mod 2).
     */
    private static void turn(Cube c, int[] cornerCycle, int[] edgeCycle, int cornerDelta, int edgeDelta) {
        // rotate permutations
        rotate4(c.cornerPermutation, cornerCycle);
        rotate4(c.edgePermutation,   edgeCycle);
        // rotate orientations with their pieces
        rotate4(c.cornerOrientation, cornerCycle);
        rotate4(c.edgeOrientation,   edgeCycle);
        // apply deltas to the four affected slots
        for (int idx : cornerCycle) {
            c.cornerOrientation[idx] = (c.cornerOrientation[idx] + cornerDelta) % 3;
        }
        if (edgeDelta != 0) {
            for (int idx : edgeCycle) {
                c.edgeOrientation[idx] ^= 1; // flip (mod 2)
            }
        }
    }

    /** In-place 4-cycle rotate: a->b->c->d->a for the given index list. */
    private static void rotate4(int[] arr, int[] cyc) {
        int a = cyc[0], b = cyc[1], c2 = cyc[2], d = cyc[3];
        int tmp = arr[d];
        arr[d] = arr[c2];
        arr[c2] = arr[b];
        arr[b]  = arr[a];
        arr[a]  = tmp;
    }

    // ---- Cycles for each face (slot indices) --------------------------------
    // Indices chosen to match the cubie indexing in the header comment.
    // Corner cycles:
    private static final int[] U_CORNER = {0, 3, 2, 1}; // URF→UBR→ULB→UFL
    private static final int[] D_CORNER = {4, 5, 6, 7}; // DFR→DLF→DBL→DRB
    private static final int[] R_CORNER = {0, 4, 7, 3}; // URF→DFR→DRB→UBR
    private static final int[] L_CORNER = {1, 2, 6, 5}; // UFL→ULB→DBL→DLF
    private static final int[] F_CORNER = {1, 5, 4, 0}; // UFL→DLF→DFR→URF
    private static final int[] B_CORNER = {3, 7, 6, 2}; // UBR→DRB→DBL→ULB

    // Edge cycles:
    private static final int[] U_EDGE = {3, 2, 1, 0};   // UR→UB→UL→UF
    private static final int[] D_EDGE = {11,10, 9, 8};  // DR→DB→DL→DF
    private static final int[] R_EDGE = {3, 4,11, 7};   // UR→FR→DR→BR
    private static final int[] L_EDGE = {1, 6, 9, 5};   // UL→BL→DL→FL
    private static final int[] F_EDGE = {0, 5, 8, 4};   // UF→FL→DF→FR
    private static final int[] B_EDGE = {2, 7,10, 6};   // UB→BR→DB→BL
}
