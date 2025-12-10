package ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

public class MazeData {
    private int N, M;
    private char[][] maze;

    public int getN(){
        return N;
    }
    public int getM(){
        return M;
    }

    public MazeData(String fileName) {
        Scanner sc = null;
        try {
            File file = new File(fileName);
            if (!file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("Invalid file path: " + fileName);
            }

            FileInputStream fis = new FileInputStream(file);
            sc = new Scanner(new BufferedInputStream(fis), "UTF-8");

            if (!sc.hasNextLine()) {
                throw new IllegalArgumentException("File is empty: " + fileName);
            }

            String nmLine = sc.nextLine();
            String[] nm = nmLine.split("\\s+");
            if (nm.length != 2) {
                throw new IllegalArgumentException("Invalid maze dimensions in file: " + fileName);
            }

            N = Integer.parseInt(nm[0]);
            M = Integer.parseInt(nm[1]);
            maze = new char[N][M];

            for (int i = 0; i < N; i++) {
                if (!sc.hasNextLine()) {
                    throw new IllegalArgumentException("Incomplete maze data in file: " + fileName);
                }
                String line = sc.nextLine();
                if (line.length() != M) {
                    throw new IllegalArgumentException("Invalid maze row length at line " + (i + 2) + " in file: " + fileName);
                }
                for (int j = 0; j < M; j++) {
                    maze[i][j] = line.charAt(j);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading maze data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sc != null) {
                sc.close();
            }
        }
    }
}
