package rubikscube;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Cubie-level model of the Rubik's Cube.
 *
 * Corner indices (0..7):
 * 0: URF
 * 1: UFL
 * 2: ULB
 * 3: UBR
 * 4: DFR
 * 5: DLF
 * 6: DBL
 * 7: DRB
 *
 * Edge indices (0..11):
 * 0: UF
 * 1: UL
 * 2: UB
 * 3: UR
 * 4: FR
 * 5: FL
 * 6: BL
 * 7: BR
 * 8: DF
 * 9: DL
 * 10: DB
 * 11: DR
 *
 * Faces/colors for your assignment:
 * U = O, F = W, R = B, L = G, B = Y, D = R.
 *
 * File format (9 lines):
 *    UUU
 *    UUU
 *    UUU
 * LLLFFF RRRBBB
 * LLLFFF RRRBBB
 * LLLFFF RRRBBB
 *    DDD
 *    DDD
 *    DDD
 */
public class RubiksCube {

    // Corner permutation and orientation
    // cp[pos] = which corner cubie is at corner position 'pos'
    // co[pos] = orientation (0,1,2) of that cubie at that position
    public int[] cp = new int[8];
    public int[] co = new int[8];

    // Edge permutation and orientation
    // ep[pos] = which edge cubie is at edge position 'pos'
    // eo[pos] = orientation (0,1) of that cubie at that position
    public int[] ep = new int[12];
    public int[] eo = new int[12];

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    /** Solved cube constructor. */
    public RubiksCube() {
        setSolved();
    }

    /** Copy constructor. */
    public RubiksCube(RubiksCube other) {
        System.arraycopy(other.cp, 0, cp, 0, 8);
        System.arraycopy(other.co, 0, co, 0, 8);
        System.arraycopy(other.ep, 0, ep, 0, 12);
        System.arraycopy(other.eo, 0, eo, 0, 12);
    }

    /** Set this cube to solved state. */
    public final void setSolved() {
        for (int i = 0; i < 8; i++) {
            cp[i] = i;
            co[i] = 0;
        }
        for (int i = 0; i < 12; i++) {
            ep[i] = i;
            eo[i] = 0;
        }
    }

    /** True if cubie state is solved. */
    public boolean isSolved() {
        for (int i = 0; i < 8; i++) {
            if (cp[i] != i || co[i] != 0) return false;
        }
        for (int i = 0; i < 12; i++) {
            if (ep[i] != i || eo[i] != 0) return false;
        }
        return true;
    }

    // ------------------------------------------------------------
    // File constructor: read facelet net and convert to cubies
    // ------------------------------------------------------------

    /**
     * Construct cube from 9-line net file using the assignment layout.
     */
    public RubiksCube(String fileName) throws IOException {
        // Read 9 lines
        String[] lines = new String[9];
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for (int i = 0; i < 9; i++) {
                String line = br.readLine();
                if (line == null) throw new IOException("Bad file: less than 9 lines");
                lines[i] = line;
            }
        }

        // Temporary facelet arrays (3x3 each), row-major
        char[] U = new char[9];
        char[] F = new char[9];
        char[] R = new char[9];
        char[] L = new char[9];
        char[] B = new char[9];
        char[] D = new char[9];

        // Same mapping you had in your old facelet-based RubiksCube
        // Up: lines[0..2], cols 3..5
        for (int i = 0; i < 3; i++) {
            String row = lines[i];
            U[i * 3 + 0] = row.charAt(3);
            U[i * 3 + 1] = row.charAt(4);
            U[i * 3 + 2] = row.charAt(5);
        }

        // Middle band: lines[3..5], four faces L,F,R,B in order
        for (int i = 0; i < 3; i++) {
            String row = lines[3 + i];
            for (int c = 0; c < 3; c++) L[i * 3 + c] = row.charAt(0 + c);
            for (int c = 0; c < 3; c++) F[i * 3 + c] = row.charAt(3 + c);
            for (int c = 0; c < 3; c++) R[i * 3 + c] = row.charAt(6 + c);
            for (int c = 0; c < 3; c++) B[i * 3 + c] = row.charAt(9 + c);
        }

        // Down: lines[6..8], cols 3..5
        for (int i = 0; i < 3; i++) {
            String row = lines[6 + i];
            D[i * 3 + 0] = row.charAt(3);
            D[i * 3 + 1] = row.charAt(4);
            D[i * 3 + 2] = row.charAt(5);
        }

        // Get center colors (robust even if the scheme changes)
        char colU = U[4];
        char colF = F[4];
        char colR = R[4];
        char colL = L[4];
        char colB = B[4];
        char colD = D[4];

        // Define solved corner color triples (in [U/D, other1, other2] order)
        char[][] cornerColors = new char[8][3];
        // 0: URF
        cornerColors[0][0] = colU; cornerColors[0][1] = colR; cornerColors[0][2] = colF;
        // 1: UFL
        cornerColors[1][0] = colU; cornerColors[1][1] = colF; cornerColors[1][2] = colL;
        // 2: ULB
        cornerColors[2][0] = colU; cornerColors[2][1] = colL; cornerColors[2][2] = colB;
        // 3: UBR
        cornerColors[3][0] = colU; cornerColors[3][1] = colB; cornerColors[3][2] = colR;
        // 4: DFR
        cornerColors[4][0] = colD; cornerColors[4][1] = colF; cornerColors[4][2] = colR;
        // 5: DLF
        cornerColors[5][0] = colD; cornerColors[5][1] = colL; cornerColors[5][2] = colF;
        // 6: DBL
        cornerColors[6][0] = colD; cornerColors[6][1] = colB; cornerColors[6][2] = colL;
        // 7: DRB
        cornerColors[7][0] = colD; cornerColors[7][1] = colR; cornerColors[7][2] = colB;

        // Define solved edge color pairs [primary, secondary]
        char[][] edgeColors = new char[12][2];
        // 0: UF
        edgeColors[0][0] = colU; edgeColors[0][1] = colF;
        // 1: UL
        edgeColors[1][0] = colU; edgeColors[1][1] = colL;
        // 2: UB
        edgeColors[2][0] = colU; edgeColors[2][1] = colB;
        // 3: UR
        edgeColors[3][0] = colU; edgeColors[3][1] = colR;
        // 4: FR
        edgeColors[4][0] = colF; edgeColors[4][1] = colR;
        // 5: FL
        edgeColors[5][0] = colF; edgeColors[5][1] = colL;
        // 6: BL
        edgeColors[6][0] = colB; edgeColors[6][1] = colL;
        // 7: BR
        edgeColors[7][0] = colB; edgeColors[7][1] = colR;
        // 8: DF
        edgeColors[8][0] = colD; edgeColors[8][1] = colF;
        // 9: DL
        edgeColors[9][0] = colD; edgeColors[9][1] = colL;
        // 10: DB
        edgeColors[10][0] = colD; edgeColors[10][1] = colB;
        // 11: DR
        edgeColors[11][0] = colD; edgeColors[11][1] = colR;

        // --------------------------------------------------------
        // Map facelets to corner positions (0..7)
        // Using standard mapping for this net:
        // 0 URF: (U8, R0, F2)
        // 1 UFL: (U6, F0, L2)
        // 2 ULB: (U0, L0, B2)
        // 3 UBR: (U2, B0, R2)
        // 4 DFR: (D2, F8, R6)
        // 5 DLF: (D0, L8, F6)
        // 6 DBL: (D6, B8, L6)
        // 7 DRB: (D8, R8, B6)
        // --------------------------------------------------------
        char[][] cornerFacelets = new char[8][3];
        // pos 0 URF
        cornerFacelets[0][0] = U[8];
        cornerFacelets[0][1] = R[0];
        cornerFacelets[0][2] = F[2];
        // pos 1 UFL
        cornerFacelets[1][0] = U[6];
        cornerFacelets[1][1] = F[0];
        cornerFacelets[1][2] = L[2];
        // pos 2 ULB
        cornerFacelets[2][0] = U[0];
        cornerFacelets[2][1] = L[0];
        cornerFacelets[2][2] = B[2];
        // pos 3 UBR
        cornerFacelets[3][0] = U[2];
        cornerFacelets[3][1] = B[0];
        cornerFacelets[3][2] = R[2];
        // pos 4 DFR
        cornerFacelets[4][0] = D[2];
        cornerFacelets[4][1] = F[8];
        cornerFacelets[4][2] = R[6];
        // pos 5 DLF
        cornerFacelets[5][0] = D[0];
        cornerFacelets[5][1] = L[8];
        cornerFacelets[5][2] = F[6];
        // pos 6 DBL
        cornerFacelets[6][0] = D[6];
        cornerFacelets[6][1] = B[8];
        cornerFacelets[6][2] = L[6];
        // pos 7 DRB
        cornerFacelets[7][0] = D[8];
        cornerFacelets[7][1] = R[8];
        cornerFacelets[7][2] = B[6];

        // --------------------------------------------------------
        // Map facelets to edge positions (0..11)
        // 0 UF: (U7, F1)
        // 1 UL: (U3, L1)
        // 2 UB: (U1, B1)
        // 3 UR: (U5, R1)
        // 4 FR: (F5, R3)
        // 5 FL: (F3, L5)
        // 6 BL: (B5, L3)
        // 7 BR: (B3, R5)
        // 8 DF: (D1, F7)
        // 9 DL: (D3, L7)
        // 10 DB: (D7, B7)
        // 11 DR: (D5, R7)
        // --------------------------------------------------------
        char[][] edgeFacelets = new char[12][2];
        // 0 UF
        edgeFacelets[0][0] = U[7];
        edgeFacelets[0][1] = F[1];
        // 1 UL
        edgeFacelets[1][0] = U[3];
        edgeFacelets[1][1] = L[1];
        // 2 UB
        edgeFacelets[2][0] = U[1];
        edgeFacelets[2][1] = B[1];
        // 3 UR
        edgeFacelets[3][0] = U[5];
        edgeFacelets[3][1] = R[1];
        // 4 FR
        edgeFacelets[4][0] = F[5];
        edgeFacelets[4][1] = R[3];
        // 5 FL
        edgeFacelets[5][0] = F[3];
        edgeFacelets[5][1] = L[5];
        // 6 BL
        edgeFacelets[6][0] = B[5];
        edgeFacelets[6][1] = L[3];
        // 7 BR
        edgeFacelets[7][0] = B[3];
        edgeFacelets[7][1] = R[5];
        // 8 DF
        edgeFacelets[8][0] = D[1];
        edgeFacelets[8][1] = F[7];
        // 9 DL
        edgeFacelets[9][0] = D[3];
        edgeFacelets[9][1] = L[7];
        // 10 DB
        edgeFacelets[10][0] = D[7];
        edgeFacelets[10][1] = B[7];
        // 11 DR
        edgeFacelets[11][0] = D[5];
        edgeFacelets[11][1] = R[7];

        // --------------------------------------------------------
        // Infer cp, co from corner facelets
        // --------------------------------------------------------
        for (int pos = 0; pos < 8; pos++) {
            char c0 = cornerFacelets[pos][0];
            char c1 = cornerFacelets[pos][1];
            char c2 = cornerFacelets[pos][2];

            // Try match against each corner type
            boolean found = false;
            for (int cubie = 0; cubie < 8 && !found; cubie++) {
                char s0 = cornerColors[cubie][0];
                char s1 = cornerColors[cubie][1];
                char s2 = cornerColors[cubie][2];

                // Same multiset of 3 colors?
                if (!sameSet3(c0, c1, c2, s0, s1, s2)) continue;

                cp[pos] = cubie;

                // Orientation = index (0..2) where the U/D color appears
                char upDownColor = (cubie < 4) ? colU : colD;
                int ori;
                if (c0 == upDownColor) ori = 0;
                else if (c1 == upDownColor) ori = 1;
                else ori = 2;
                co[pos] = ori;
                found = true;
            }
            if (!found) {
                throw new IllegalStateException("Invalid corner at position " + pos);
            }
        }

        // --------------------------------------------------------
        // Infer ep, eo from edge facelets
        // --------------------------------------------------------
        for (int pos = 0; pos < 12; pos++) {
            char e0 = edgeFacelets[pos][0];
            char e1 = edgeFacelets[pos][1];

            boolean found = false;
            for (int cubie = 0; cubie < 12 && !found; cubie++) {
                char s0 = edgeColors[cubie][0];
                char s1 = edgeColors[cubie][1];

                // Two possibilities: [s0,s1] or [s1,s0]
                if (e0 == s0 && e1 == s1) {
                    ep[pos] = cubie;
                    eo[pos] = 0; // unflipped
                    found = true;
                } else if (e0 == s1 && e1 == s0) {
                    ep[pos] = cubie;
                    eo[pos] = 1; // flipped
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalStateException("Invalid edge at position " + pos);
            }
        }
    }

    // Helper: compare two unordered triples of colors
    private static boolean sameSet3(char a0, char a1, char a2, char b0, char b1, char b2) {
        char[] A = { a0, a1, a2 };
        char[] B = { b0, b1, b2 };
        Arrays.sort(A);
        Arrays.sort(B);
        return A[0] == B[0] && A[1] == B[1] && A[2] == B[2];
    }

    // ------------------------------------------------------------
    // Phase-1 indices (used by Solver)
    // ------------------------------------------------------------

    // Small combination table C(n,k) for 0 <= n <= 12, 0 <= k <= 4
    private static final int[][] COMB = new int[13][5];
    static {
        for (int n = 0; n <= 12; n++) {
            COMB[n][0] = 1;
            for (int k = 1; k <= 4 && k <= n; k++) {
                if (k == n) {
                    COMB[n][k] = 1;
                } else {
                    COMB[n][k] = COMB[n - 1][k - 1] + COMB[n - 1][k];
                }
            }
        }
    }

    /** Corner orientation coordinate (Phase 1). */
    public int cornerOrientationIndex() {
        int idx = 0;
        for (int i = 0; i < 7; i++) {
            int o = co[i];
            if (o < 0) o += 3;
            o %= 3;
            idx = idx * 3 + o;
        }
        return idx;
    }

    /** Edge orientation coordinate (Phase 1). */
    public int edgeOrientationIndex() {
        int idx = 0;
        for (int i = 0; i < 11; i++) {
            int o = eo[i] & 1;
            idx = (idx << 1) | o;
        }
        return idx;
    }

    /** UD-slice edge coordinate (Phase 1). */
    public int sliceEdgeIndex() {
        int idx = 0;
        int r = 4; // need 4 slice edges in positions

        for (int pos = 0; pos < 12; pos++) {
            boolean isSlice =
                    (ep[pos] == 4 || ep[pos] == 5 || ep[pos] == 6 || ep[pos] == 7);

            if (isSlice) {
                r--;
                if (r == 0) break;
            } else {
                if (r > 0) {
                    idx += COMB[11 - pos][r - 1];
                }
            }
        }
        return idx;
    }

    // ------------------------------------------------------------
    // Low-level helpers for moves
    // ------------------------------------------------------------

    // cycle 4 positions in a permutation array
    private static void cycle4(int[] arr, int a, int b, int c, int d) {
        int tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = arr[c];
        arr[c] = arr[d];
        arr[d] = tmp;
    }

    // twist corner orientation
    private static void twistCorner(int[] co, int idx, int amt) {
        co[idx] = (co[idx] + amt) % 3;
    }

    // flip edge orientation
    private static void flipEdge(int[] eo, int idx) {
        eo[idx] ^= 1;
    }

    // ------------------------------------------------------------
    // Face moves in cubie space (quarter-turns)
    // ------------------------------------------------------------

    /** U face 90° clockwise */
    public void moveU() {
        // corners: (0,1,2,3)
        cycle4(cp, 0, 3, 2, 1);
        cycle4(co, 0, 3, 2, 1); // orientation unchanged

        // edges: (0,3,2,1)
        cycle4(ep, 0, 3, 2, 1);
        cycle4(eo, 0, 3, 2, 1); // orientation unchanged
    }

    /** R face 90° clockwise */
    public void moveR() {
        // Twist affected corners before permuting
        twistCorner(co, 0, 1);
        twistCorner(co, 3, 2);
        twistCorner(co, 7, 1);
        twistCorner(co, 4, 2);

        // Permute corners (0,3,7,4) and carry orientations
        cycle4(cp, 0, 4, 7, 3);
        cycle4(co, 0, 4, 7, 3);

        // Edges around R face: (3,4,11,7)
        cycle4(ep, 3, 4, 11, 7);
        cycle4(eo, 3, 4, 11, 7);
    }

    /** F face 90° clockwise */
    public void moveF() {
        // Twist affected corners before permutation
        twistCorner(co, 0, 2);
        twistCorner(co, 1, 1);
        twistCorner(co, 5, 2);
        twistCorner(co, 4, 1);

        // Permute corners/orientations (0,1,5,4)
        cycle4(cp, 0, 1, 5, 4);
        cycle4(co, 0, 1, 5, 4);

        // Flip the four F-layer edges, then permute them
        flipEdge(eo, 0);
        flipEdge(eo, 4);
        flipEdge(eo, 8);
        flipEdge(eo, 5);
        cycle4(ep, 0, 5, 8, 4);
        cycle4(eo, 0, 5, 8, 4);
    }




    /** D face 90° clockwise */
    public void moveD() {
        // corners: (4,5,6,7)
        cycle4(cp, 4, 5, 6, 7);
        cycle4(co, 4, 5, 6, 7); // orientation unchanged

        // edges: (8,9,10,11)
        cycle4(ep, 8, 9, 10, 11);
        cycle4(eo, 8, 9, 10, 11); // unchanged
    }

    /** L face 90° clockwise */
    public void moveL() {
        // Twist corners on L layer
        twistCorner(co, 1, 2);
        twistCorner(co, 2, 1);
        twistCorner(co, 6, 2);
        twistCorner(co, 5, 1);

        // Permute corners/orientations (1,2,6,5)
        cycle4(cp, 1, 2, 6, 5);
        cycle4(co, 1, 2, 6, 5);

        // Edges around L: (1,6,9,5) with no flips
        cycle4(ep, 1, 6, 9, 5);
        cycle4(eo, 1, 6, 9, 5);
    }


    /** B face 90° clockwise */
    public void moveB() {
        // Twist corners on the B layer
        twistCorner(co, 2, 2);
        twistCorner(co, 3, 1);
        twistCorner(co, 7, 2);
        twistCorner(co, 6, 1);

        // Permute corners/orientations (2,3,7,6)
        cycle4(cp, 2, 3, 7, 6);
        cycle4(co, 2, 3, 7, 6);

        // Edges: (2,7,10,6) = UB, BR, DB, BL
        flipEdge(eo, 2);
        flipEdge(eo, 7);
        flipEdge(eo, 10);
        flipEdge(eo, 6);
        cycle4(ep, 2, 7, 10, 6);
        cycle4(eo, 2, 7, 10, 6);
    }




    public String toNetString() {
    // Your fixed color scheme:
    // U = O, F = W, R = B, L = G, B = Y, D = R
    char colU = 'O';
    char colF = 'W';
    char colR = 'B';
    char colL = 'G';
    char colB = 'Y';
    char colD = 'R';

    // Facelet arrays
    char[] U = new char[9];
    char[] F = new char[9];
    char[] R = new char[9];
    char[] L = new char[9];
    char[] B = new char[9];
    char[] D = new char[9];

    // Fill with centers by default
    Arrays.fill(U, colU);
    Arrays.fill(F, colF);
    Arrays.fill(R, colR);
    Arrays.fill(L, colL);
    Arrays.fill(B, colB);
    Arrays.fill(D, colD);

    // Corner color triples (home orientation: [U/D, other1, other2])
    char[][] cornerColors = new char[8][3];
    // 0: URF
    cornerColors[0][0] = colU; cornerColors[0][1] = colR; cornerColors[0][2] = colF;
    // 1: UFL
    cornerColors[1][0] = colU; cornerColors[1][1] = colF; cornerColors[1][2] = colL;
    // 2: ULB
    cornerColors[2][0] = colU; cornerColors[2][1] = colL; cornerColors[2][2] = colB;
    // 3: UBR
    cornerColors[3][0] = colU; cornerColors[3][1] = colB; cornerColors[3][2] = colR;
    // 4: DFR
    cornerColors[4][0] = colD; cornerColors[4][1] = colF; cornerColors[4][2] = colR;
    // 5: DLF
    cornerColors[5][0] = colD; cornerColors[5][1] = colL; cornerColors[5][2] = colF;
    // 6: DBL
    cornerColors[6][0] = colD; cornerColors[6][1] = colB; cornerColors[6][2] = colL;
    // 7: DRB
    cornerColors[7][0] = colD; cornerColors[7][1] = colR; cornerColors[7][2] = colB;

    // Edge color pairs
    char[][] edgeColors = new char[12][2];
    // 0: UF
    edgeColors[0][0] = colU; edgeColors[0][1] = colF;
    // 1: UL
    edgeColors[1][0] = colU; edgeColors[1][1] = colL;
    // 2: UB
    edgeColors[2][0] = colU; edgeColors[2][1] = colB;
    // 3: UR
    edgeColors[3][0] = colU; edgeColors[3][1] = colR;
    // 4: FR
    edgeColors[4][0] = colF; edgeColors[4][1] = colR;
    // 5: FL
    edgeColors[5][0] = colF; edgeColors[5][1] = colL;
    // 6: BL
    edgeColors[6][0] = colB; edgeColors[6][1] = colL;
    // 7: BR
    edgeColors[7][0] = colB; edgeColors[7][1] = colR;
    // 8: DF
    edgeColors[8][0] = colD; edgeColors[8][1] = colF;
    // 9: DL
    edgeColors[9][0] = colD; edgeColors[9][1] = colL;
    // 10: DB
    edgeColors[10][0] = colD; edgeColors[10][1] = colB;
    // 11: DR
    edgeColors[11][0] = colD; edgeColors[11][1] = colR;

    // ---- Corners: positions -> facelets ----
    for (int pos = 0; pos < 8; pos++) {
        int cubie = cp[pos];
        int ori = co[pos];

        char c0 = cornerColors[cubie][0];
        char c1 = cornerColors[cubie][1];
        char c2 = cornerColors[cubie][2];

        // Orient triple so that up/down color sits at index = ori
        char oc0, oc1, oc2;
        if (ori == 0) {
            oc0 = c0; oc1 = c1; oc2 = c2;
        } else if (ori == 1) {
            // up/down color at index 1
            oc0 = c2; oc1 = c0; oc2 = c1;
        } else { // ori == 2
            // up/down color at index 2
            oc0 = c1; oc1 = c2; oc2 = c0;
        }

        switch (pos) {
            case 0: // URF: (U8,R0,F2)
                U[8] = oc0; R[0] = oc1; F[2] = oc2;
                break;
            case 1: // UFL: (U6,F0,L2)
                U[6] = oc0; F[0] = oc1; L[2] = oc2;
                break;
            case 2: // ULB: (U0,L0,B2)
                U[0] = oc0; L[0] = oc1; B[2] = oc2;
                break;
            case 3: // UBR: (U2,B0,R2)
                U[2] = oc0; B[0] = oc1; R[2] = oc2;
                break;
            case 4: // DFR: (D2,F8,R6)
                D[2] = oc0; F[8] = oc1; R[6] = oc2;
                break;
            case 5: // DLF: (D0,L8,F6)
                D[0] = oc0; L[8] = oc1; F[6] = oc2;
                break;
            case 6: // DBL: (D6,B8,L6)
                D[6] = oc0; B[8] = oc1; L[6] = oc2;
                break;
            case 7: // DRB: (D8,R8,B6)
                D[8] = oc0; R[8] = oc1; B[6] = oc2;
                break;
        }
    }

    // ---- Edges: positions -> facelets ----
    for (int pos = 0; pos < 12; pos++) {
        int cubie = ep[pos];
        int ori = eo[pos] & 1;

        char e0 = edgeColors[cubie][0];
        char e1 = edgeColors[cubie][1];

        char oc0 = (ori == 0) ? e0 : e1;
        char oc1 = (ori == 0) ? e1 : e0;

        switch (pos) {
            case 0: // UF: (U7,F1)
                U[7] = oc0; F[1] = oc1;
                break;
            case 1: // UL: (U3,L1)
                U[3] = oc0; L[1] = oc1;
                break;
            case 2: // UB: (U1,B1)
                U[1] = oc0; B[1] = oc1;
                break;
            case 3: // UR: (U5,R1)
                U[5] = oc0; R[1] = oc1;
                break;
            case 4: // FR: (F5,R3)
                F[5] = oc0; R[3] = oc1;
                break;
            case 5: // FL: (F3,L5)
                F[3] = oc0; L[5] = oc1;
                break;
            case 6: // BL: (B5,L3)
                B[5] = oc0; L[3] = oc1;
                break;
            case 7: // BR: (B3,R5)
                B[3] = oc0; R[5] = oc1;
                break;
            case 8: // DF: (D1,F7)
                D[1] = oc0; F[7] = oc1;
                break;
            case 9: // DL: (D3,L7)
                D[3] = oc0; L[7] = oc1;
                break;
            case 10: // DB: (D7,B7)
                D[7] = oc0; B[7] = oc1;
                break;
            case 11: // DR: (D5,R7)
                D[5] = oc0; R[7] = oc1;
                break;
        }
    }

    // ---- Build 9-line net ----
    StringBuilder sb = new StringBuilder();
    // Up
    sb.append("   ").append(U[0]).append(U[1]).append(U[2]).append("\n");
    sb.append("   ").append(U[3]).append(U[4]).append(U[5]).append("\n");
    sb.append("   ").append(U[6]).append(U[7]).append(U[8]).append("\n");
    // Middle (L F R B)
    for (int row = 0; row < 3; row++) {
        sb.append(L[row * 3 + 0]).append(L[row * 3 + 1]).append(L[row * 3 + 2]);
        sb.append(F[row * 3 + 0]).append(F[row * 3 + 1]).append(F[row * 3 + 2]);
        sb.append(R[row * 3 + 0]).append(R[row * 3 + 1]).append(R[row * 3 + 2]);
        sb.append(B[row * 3 + 0]).append(B[row * 3 + 1]).append(B[row * 3 + 2]);
        sb.append("\n");
    }
    // Down
    sb.append("   ").append(D[0]).append(D[1]).append(D[2]).append("\n");
    sb.append("   ").append(D[3]).append(D[4]).append(D[5]).append("\n");
    sb.append("   ").append(D[6]).append(D[7]).append(D[8]).append("\n");

    return sb.toString();
}

}
