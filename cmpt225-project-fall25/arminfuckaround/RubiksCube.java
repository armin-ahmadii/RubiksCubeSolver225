package rubikscube;

import java.io.*;
import java.util.*;

public class RubiksCube {
    char[] front = new char[9];
    char[] back = new char[9];
    char[] right = new char[9];
    char[] left = new char[9];
    char[] up = new char[9];
    char[] down = new char[9];

    public RubiksCube(RubiksCube other) {
    this.front = other.front.clone();
    this.back = other.back.clone();
    this.right = other.right.clone();
    this.left = other.left.clone();
    this.up = other.up.clone();
    this.down = other.down.clone();
    }

    public String getStateString() {
        return new String(front) + new String(back) + 
            new String(right) + new String(left) + 
            new String(up) + new String(down);
    }

    /**
     * default constructor
     * Creates a Rubik's Cube in an initial state:
     *    OOO
     *    OOO
     *    OOO
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     *    RRR
     *    RRR
     *    RRR
     */
    // default solved cube
    public RubiksCube() {
        Arrays.fill(front, 'W');
        Arrays.fill(back, 'Y');
        Arrays.fill(right, 'B');
        Arrays.fill(left, 'G');
        Arrays.fill(up, 'O');
        Arrays.fill(down, 'R');
    }

    /**
     * @param fileName
     * @throws IOException
     * @throws IncorrectFormatException
     * Creates a Rubik's Cube from the description in fileName
     */
    // file reader
    public RubiksCube(String fileName) throws IOException, IncorrectFormatException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }
        if (lines.size() != 9) throw new IncorrectFormatException("bad file");

        for (int i = 0; i < 3; i++) {
            String row = lines.get(i);
            up[i*3] = row.charAt(3);
            up[i*3+1] = row.charAt(4);
            up[i*3+2] = row.charAt(5);
        }
        for (int i = 0; i < 3; i++) {
            String row = lines.get(3+i);
            for (int c = 0; c < 3; c++) left[i*3+c] = row.charAt(c);
            for (int c = 0; c < 3; c++) front[i*3+c] = row.charAt(3+c);
            for (int c = 0; c < 3; c++) right[i*3+c] = row.charAt(6+c);
            for (int c = 0; c < 3; c++) back[i*3+c] = row.charAt(9+c);
        }
        for (int i = 0; i < 3; i++) {
            String row = lines.get(6+i);
            down[i*3] = row.charAt(3);
            down[i*3+1] = row.charAt(4);
            down[i*3+2] = row.charAt(5);
        }
    }

    /**
     * @param moves
     * Applies the sequence of moves on the Rubik's Cube
     */
    public void applyMoves(String moves) {
        if (moves == null) return;
        for (char m : moves.toCharArray()) {
            switch(m){
                case 'F': moveF(); break;
                case 'B': moveB(); break;
                case 'R': moveR(); break;
                case 'L': moveL(); break;
                case 'U': moveU(); break;
                case 'D': moveD(); break;
            }
        }
    }

    /**
     * returns true if the current state of the Cube is solved,
     * i.e., it is in this state:
     *    OOO
     *    OOO
     *    OOO
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     *    RRR
     *    RRR
     *    RRR
     */
    public boolean isSolved(){
        return solved(front)&&solved(back)&&solved(right)&&solved(left)&&solved(up)&&solved(down);
    }

    @Override
    public String toString(){
        StringBuilder s=new StringBuilder();
        // up
        for(int i=0;i<3;i++){s.append("   ").append(up[i*3]).append(up[i*3+1]).append(up[i*3+2]).append("\n");}
        // middle
        for(int i=0;i<3;i++){
            s.append(left[i*3]).append(left[i*3+1]).append(left[i*3+2]);
            s.append(front[i*3]).append(front[i*3+1]).append(front[i*3+2]);
            s.append(right[i*3]).append(right[i*3+1]).append(right[i*3+2]);
            s.append(back[i*3]).append(back[i*3+1]).append(back[i*3+2]).append("\n");
        }
        // down
        for(int i=0;i<3;i++){s.append("   ").append(down[i*3]).append(down[i*3+1]).append(down[i*3+2]).append("\n");}
        return s.toString();
    }

    /**
     *
     * @param moves
     * @return the order of the sequence of moves
     */
    public static int order(String moves){
        RubiksCube c=new RubiksCube();
        String start=c.toString();
        int n=0;
        do{c.applyMoves(moves);n++;}while(!c.toString().equals(start));
        return n;
    }

    // helpers
    private boolean solved(char[] arr){
        for(char ch:arr) if(ch!=arr[0]) return false;
        return true;
    }
    private void rot(char[] x){
        char t0=x[0],t1=x[1],t2=x[2],t3=x[3],t5=x[5],t6=x[6],t7=x[7],t8=x[8];
        x[0]=t6;x[1]=t3;x[2]=t0;x[3]=t7;x[5]=t1;x[6]=t8;x[7]=t5;x[8]=t2;
    }

    public void moveF(){
        rot(front);
        char a=up[6],b0=up[7],c=up[8];
        up[6]=left[8];up[7]=left[5];up[8]=left[2];
        left[8]=down[2];left[5]=down[1];left[2]=down[0];
        down[2]=right[0];down[1]=right[3];down[0]=right[6];
        right[0]=a;right[3]=b0;right[6]=c;
    }
    public void moveB(){
        rot(back);
        char a=up[0],b0=up[1],c=up[2];
        up[0]=right[2];up[1]=right[5];up[2]=right[8];
        right[2]=down[8];right[5]=down[7];right[8]=down[6];
        down[8]=left[6];down[7]=left[3];down[6]=left[0];
        left[6]=a;left[3]=b0;left[0]=c;
    }
    public void moveR(){
        rot(right);
        char a=up[2],b0=up[5],c=up[8];
        up[2]=front[2];up[5]=front[5];up[8]=front[8];
        front[2]=down[2];front[5]=down[5];front[8]=down[8];
        down[2]=back[6];down[5]=back[3];down[8]=back[0];
        back[6]=a;back[3]=b0;back[0]=c;
    }
    public void moveL(){
        rot(left);
        char a=up[0],b0=up[3],c=up[6];
        up[0]=back[8];up[3]=back[5];up[6]=back[2];
        back[8]=down[6];back[5]=down[3];back[2]=down[0];
        down[6]=front[0];down[3]=front[3];down[0]=front[6];
        front[0]=a;front[3]=b0;front[6]=c;
    }
    public void moveU(){
        rot(up);
        char a=front[0],b0=front[1],c=front[2];
        front[0]=right[0];front[1]=right[1];front[2]=right[2];
        right[0]=back[0];right[1]=back[1];right[2]=back[2];
        back[0]=left[0];back[1]=left[1];back[2]=left[2];
        left[0]=a;left[1]=b0;left[2]=c;
    }
    public void moveD(){
        rot(down);
        char a=front[6],b0=front[7],c=front[8];
        front[6]=left[6];front[7]=left[7];front[8]=left[8];
        left[6]=back[6];left[7]=back[7];left[8]=back[8];
        back[6]=right[6];back[7]=right[7];back[8]=right[8];
        right[6]=a;right[7]=b0;right[8]=c;
    }


    // heuristic
    public int countSolvedCorners() {
    int count = 0;
    
    // There are 8 corners on a Rubik's cube
    // Each corner has 3 colors
    
    // Corner 1: Front-Up-Right (WBR in solved state)
    if (front[2] == 'W' && up[8] == 'O' && right[0] == 'B') count++;
    
    // Corner 2: Front-Up-Left (WGR in solved state)
    if (front[0] == 'W' && up[6] == 'O' && left[2] == 'G') count++;
    
    // Corner 3: Front-Down-Right (WBR in solved state)
    if (front[8] == 'W' && down[2] == 'R' && right[6] == 'B') count++;
    
    // Corner 4: Front-Down-Left (WGR in solved state)
    if (front[6] == 'W' && down[0] == 'R' && left[8] == 'G') count++;
    
    // Corner 5: Back-Up-Right (YBO in solved state)
    if (back[0] == 'Y' && up[2] == 'O' && right[2] == 'B') count++;
    
    // Corner 6: Back-Up-Left (YGO in solved state)
    if (back[2] == 'Y' && up[0] == 'O' && left[0] == 'G') count++;
    
    // Corner 7: Back-Down-Right (YBR in solved state)
    if (back[6] == 'Y' && down[8] == 'R' && right[8] == 'B') count++;
    
    // Corner 8: Back-Down-Left (YGR in solved state)
    if (back[8] == 'Y' && down[6] == 'R' && left[6] == 'G') count++;
    
    return count;
    }   

    public int countSolvedEdges() {
        int count = 0;
        
        // There are 12 edges on a Rubik's cube
        // Each edge has 2 colors
        
        // Front face edges
        if (front[1] == 'W' && up[7] == 'O') count++;      // Front-Up
        if (front[3] == 'W' && left[5] == 'G') count++;    // Front-Left
        if (front[5] == 'W' && right[3] == 'B') count++;   // Front-Right
        if (front[7] == 'W' && down[1] == 'R') count++;    // Front-Down
        
        // Back face edges
        if (back[1] == 'Y' && up[1] == 'O') count++;       // Back-Up
        if (back[3] == 'Y' && right[5] == 'B') count++;    // Back-Right
        if (back[5] == 'Y' && left[3] == 'G') count++;     // Back-Left
        if (back[7] == 'Y' && down[7] == 'R') count++;     // Back-Down
        
        // Middle layer edges (between front and back)
        if (up[3] == 'O' && left[1] == 'G') count++;       // Up-Left
        if (up[5] == 'O' && right[1] == 'B') count++;      // Up-Right
        if (down[3] == 'R' && left[7] == 'G') count++;     // Down-Left
        if (down[5] == 'R' && right[7] == 'B') count++;    // Down-Right
        
        return count;
    }

    public void moveF2() { moveF(); moveF(); }
    public void moveF3() { moveF(); moveF(); moveF(); }
    public void moveB2() { moveB(); moveB(); }
    public void moveB3() { moveB(); moveB(); moveB(); }
    public void moveR2() { moveR(); moveR(); }
    public void moveR3() { moveR(); moveR(); moveR(); }
    public void moveL2() { moveL(); moveL(); }
    public void moveL3() { moveL(); moveL(); moveL(); }
    public void moveU2() { moveU(); moveU(); }
    public void moveU3() { moveU(); moveU(); moveU(); }
    public void moveD2() { moveD(); moveD(); }
    public void moveD3() { moveD(); moveD(); moveD(); }

}