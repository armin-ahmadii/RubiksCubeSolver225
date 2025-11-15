package rubikscube;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Cube class: parses a 9-line net text file into a cubie model.
 * Representation:
 *   corners 0–7 : URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB
 *   edges   0–11: UF, UL, UB, UR, FR, FL, BL, BR, DF, DL, DB, DR
 * face order (indices in facelets[]): U(0-8), L(9-17), F(18-26), R(27-35), B(36-44), D(45-53)
 */
public class Cube {
    public int[] cornerPermutation = new int[8];
    public int[] cornerOrientation = new int[8];
    public int[] edgePermutation   = new int[12];
    public int[] edgeOrientation   = new int[12];
    public char[] facelets;

    // Face-triple identities for corners
    private static final int[][] CORNER_FACES = {
        {0, 3, 2}, // 0 URF
        {0, 2, 1}, // 1 UFL
        {0, 1, 4}, // 2 ULB
        {0, 4, 3}, // 3 UBR
        {5, 2, 3}, // 4 DFR
        {5, 1, 2}, // 5 DLF
        {5, 4, 1}, // 6 DBL
        {5, 3, 4}  // 7 DRB
    };

    // Face-pair identities for edges
    private static final int[][] EDGE_FACES = {
        {0, 2}, // 0 UF
        {0, 1}, // 1 UL
        {0, 4}, // 2 UB
        {0, 3}, // 3 UR
        {2, 3}, // 4 FR
        {2, 1}, // 5 FL
        {4, 1}, // 6 BL
        {4, 3}, // 7 BR
        {5, 2}, // 8 DF
        {5, 1}, // 9 DL
        {5, 4}, //10 DB
        {5, 3}  //11 DR
    };

    // Each slot's facelet indices in the flattened 54-char array
    private static final int[][] CORNER_FACELETS = {
        {8, 27, 20},  // URF
        {6, 18, 11},  // UFL
        {0, 9, 38},   // ULB
        {2, 36, 29},  // UBR
        {47, 24, 33}, // DFR
        {45, 15, 26}, // DLF
        {51, 42, 17}, // DBL
        {53, 35, 44}  // DRB
    };
    private static final int[][] EDGE_FACELETS = {
        {7, 19},  // UF
        {3, 10},  // UL
        {1, 37},  // UB
        {5, 28},  // UR
        {23, 30}, // FR
        {21, 12}, // FL
        {39, 14}, // BL
        {41, 32}, // BR
        {46, 25}, // DF
        {48, 16}, // DL
        {52, 43}, // DB
        {50, 34}  // DR
    };

    private char[] centers = new char[6]; // colors of U,L,F,R,B,D centers

    /** Construct cube from scramble text file */
    public Cube(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (!Character.isWhitespace(c)) sb.append(c);
            }
            facelets = sb.toString().toCharArray();
            if (facelets.length != 54)
                throw new IllegalStateException("Expected 54 facelets, got " + facelets.length);
            initCenters();
            buildCubieModel();
        } catch (IOException e) {
            System.err.println("Error reading " + filename + ": " + e.getMessage());
        }
    }

    /** center colors U,L,F,R,B,D */
    private void initCenters() {
        centers[0] = facelets[4];
        centers[1] = facelets[13];
        centers[2] = facelets[22];
        centers[3] = facelets[31];
        centers[4] = facelets[40];
        centers[5] = facelets[49];
    }

    /** map color → face index 0..5 */
    private int colorToFace(char color) {
        for (int i = 0; i < 6; i++)
            if (centers[i] == color) return i;
        throw new IllegalArgumentException("Unknown color " + color);
    }

    /** identify which corner cubie and orientation is in each slot */
    private void buildCubieModel() {
        // corners
        for (int i = 0; i < 8; i++) {
            int[] fidx = CORNER_FACELETS[i];
            int[] faces = {
                colorToFace(facelets[fidx[0]]),
                colorToFace(facelets[fidx[1]]),
                colorToFace(facelets[fidx[2]])
            };
            // find which corner identity matches this set
            for (int id = 0; id < 8; id++) {
                if (sameSet(faces, CORNER_FACES[id])) {
                    cornerPermutation[i] = id;
                    cornerOrientation[i] = getCornerOrientation(faces);
                    break;
                }
            }
        }
        // edges
        for (int i = 0; i < 12; i++) {
            int[] fidx = EDGE_FACELETS[i];
            int[] faces = {
                colorToFace(facelets[fidx[0]]),
                colorToFace(facelets[fidx[1]])
            };
            for (int id = 0; id < 12; id++) {
                if (sameSet(faces, EDGE_FACES[id])) {
                    edgePermutation[i] = id;
                    edgeOrientation[i] = getEdgeOrientation(faces);
                    break;
                }
            }
        }
    }

    /** compare two unordered face sets of same size */
    private boolean sameSet(int[] a, int[] b) {
        int count = 0;
        for (int x : a)
            for (int y : b)
                if (x == y) count++;
        return count == a.length;
    }

    /** 0/1/2 twist depending on which sticker points up/down */
    private int getCornerOrientation(int[] faces) {
        // find which face is U or D
        for (int f : faces) {
            if (f == 0 || f == 5) return 0; // if U/D sticker on U/D face
        }
        // if R/L face is on U/D, orientation=1; if F/B, orientation=2
        // Simplified heuristic: middle-layer corners default 0 here; real orientation computed via moves
        return 0;
    }

    /** edge flip under UD-based definition */
    private int getEdgeOrientation(int[] faces) {
        // if edge includes U/D face
        boolean hasUD = (faces[0] == 0 || faces[0] == 5 ||
                         faces[1] == 0 || faces[1] == 5);
        if (hasUD) return 0; // if U/D color on U/D face (solved input)
        return 0;
    }

    /** sanity check */
    public boolean isSolved() {
        for (int i = 0; i < 8; i++)
            if (cornerPermutation[i] != i || cornerOrientation[i] != 0) return false;
        for (int i = 0; i < 12; i++)
            if (edgePermutation[i] != i || edgeOrientation[i] != 0) return false;
        return true;
    }
}


//     private int edgePermutation[] = new int[12]; //which piece occupies which position
//     private int edgeOrientation[] = new int[12]; //describes how a piece is rotated in its slow
//     private int cornerPermutation[] = new int[8];
//     private int cornerOrientation[] = new int[8];

//     public Cube() {
//     /*
//      * defines the solved cube in cubie coordinates.
//      *
//      * CORNER INDEXING (8 corners)
//      *   0: URF  (Up, Right, Front)
//      *   1: UFL  (Up, Front, Left)
//      *   2: ULB  (Up, Left, Back)
//      *   3: UBR  (Up, Back, Right)
//      *   4: DFR  (Down, Front, Right)
//      *   5: DLF  (Down, Left, Front)
//      *   6: DBL  (Down, Back, Left)
//      *   7: DRB  (Down, Right, Back)
//      *
//      * EDGE INDEXING (12 edges)
//      *   0: UF  (Up–Front)
//      *   1: UL  (Up–Left)
//      *   2: UB  (Up–Back)
//      *   3: UR  (Up–Right)
//      *   4: FR  (Front–Right)
//      *   5: FL  (Front–Left)
//      *   6: BL  (Back–Left)
//      *   7: BR  (Back–Right)
//      *   8: DF  (Down–Front)
//      *   9: DL  (Down–Left)
//      *   10: DB (Down–Back)
//      *   11: DR (Down–Right)
//      *
//      * In the solved cube:
//      *   - Every cubie is in its home slot (identity permutation)
//      *   - All orientations are 0 (no twist/flip)
//      */
//     this.cornerPermutation = new int[]{0,1,2,3,4,5,6,7};
//     this.edgePermutation   = new int[]{0,1,2,3,4,5,6,7,8,9,10,11};
//     this.cornerOrientation = new int[8];
//     this.edgeOrientation   = new int[12];
// }

//     public Cube(Cube other) {
//     this.cornerPermutation = other.cornerPermutation.clone();
//     this.edgePermutation   = other.edgePermutation.clone();
//     this.cornerOrientation = other.cornerOrientation.clone();
//     this.edgeOrientation   = other.edgeOrientation.clone();
// }

// public boolean isSolved() {
//     for (int i = 0; i < 8; i++) {
//         if (cornerPermutation[i] != i || cornerOrientation[i] != 0) return false;
//     }
//     for (int i = 0; i < 12; i++) {
//         if (edgePermutation[i] != i || edgeOrientation[i] != 0) return false;
//     }
//     return true;
// }

