package com.example.game.model;

import java.util.Random;

public class Game {
    private static final int SIZE = 4;
    private int[][] board = new int[SIZE][SIZE];
    private boolean gameOver = false;
    private int score = 0;

    public Game() {
        spawnNewTile();
        spawnNewTile();
    }

    public int[][] getBoard() { return board; }
    public boolean isGameOver() { return gameOver; }
    public int getScore() { return score; }

    public boolean move(String direction) {
        boolean moved = false;
        for (int i = 0; i < (direction.equals("left") || direction.equals("right") ? 1 : 4); i++) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int current = board[row][col];
                    if (current != 0) {
                        int[] target = getTargetPosition(row, col, direction);
                        if (target != null) {
                            int targetRow = target[0];
                            int targetCol = target[1];
                            if (board[targetRow][targetCol] == 0) {
                                board[targetRow][targetCol] = current;
                                board[row][col] = 0;
                                moved = true;
                            } else if (board[targetRow][targetCol] == current) {
                                board[targetRow][targetCol] *= 2;
                                score += board[targetRow][targetCol];
                                board[row][col] = 0;
                                moved = true;
                            }
                        }
                    }
                }
            }
            if (!moved && i > 0) break;
        }

        if (moved) {
            spawnNewTile();
            checkGameOver();
        }
        return moved;
    }

    private int[] getTargetPosition(int row, int col, String direction) {
        int[] result = new int[2];
        switch (direction) {
            case "up":
                for (int r = row - 1; r >= 0; r--) {
                    if (board[r][col] != 0) {
                        result[0] = board[r][col] == 0 ? r : r + 1;
                        result[1] = col;
                        return result;
                    }
                    result[0] = 0;
                    result[1] = col;
                }
                break;
            case "down":
                for (int r = row + 1; r < SIZE; r++) {
                    if (board[r][col] != 0) {
                        result[0] = board[r][col] == 0 ? r : r - 1;
                        result[1] = col;
                        return result;
                    }
                    result[0] = SIZE - 1;
                    result[1] = col;
                }
                break;
            case "left":
                for (int c = col - 1; c >= 0; c--) {
                    if (board[row][c] != 0) {
                        result[1] = board[row][c] == 0 ? c : c + 1;
                        result[0] = row;
                        return result;
                    }
                    result[0] = row;
                    result[1] = 0;
                }
                break;
            case "right":
                for (int c = col + 1; c < SIZE; c++) {
                    if (board[row][c] != 0) {
                        result[1] = board[row][c] == 0 ? c : c - 1;
                        result[0] = row;
                        return result;
                    }
                    result[0] = row;
                    result[1] = SIZE - 1;
                }
                break;
        }
        return null;
    }

    private void spawnNewTile() {
        Random random = new Random();
        int emptyCells = 0;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) emptyCells++;
            }
        }

        if (emptyCells > 0) {
            int index = random.nextInt(emptyCells);
            int count = 0;
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == 0) {
                        if (count == index) {
                            board[row][col] = random.nextBoolean() ? 2 : 4;
                            return;
                        }
                        count++;
                    }
                }
            }
        }
    }

    private void checkGameOver() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) return;

                if (col < SIZE - 1 && board[row][col] == board[row][col + 1]) return;
                if (row < SIZE - 1 && board[row][col] == board[row + 1][col]) return;
            }
        }
        gameOver = true;
    }

    public void reset() {
        board = new int[SIZE][SIZE];
        score = 0;
        gameOver = false;
        spawnNewTile();
        spawnNewTile();
    }
}
