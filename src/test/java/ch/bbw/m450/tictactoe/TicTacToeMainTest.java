package ch.bbw.m450.tictactoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeMainTest {

    @Test
    void detectsHorizontalWin() {
        Stone[] board = new Stone[9];
        board[0] = Stone.CROSS;
        board[1] = Stone.CROSS;
        board[2] = Stone.CROSS;

        assertTrue(TicTacToeMain.isWin(board, Stone.CROSS));
        assertFalse(TicTacToeMain.isWin(board, Stone.CIRCLE));
    }

    @Test
    void detectsDiagonalWin() {
        Stone[] board = new Stone[9];
        board[0] = Stone.CIRCLE;
        board[4] = Stone.CIRCLE;
        board[8] = Stone.CIRCLE;

        assertTrue(TicTacToeMain.isWin(board, Stone.CIRCLE));
    }

    @Test
    void noWinForEmptyBoard() {
        Stone[] board = new Stone[9];

        assertFalse(TicTacToeMain.isWin(board, Stone.CROSS));
        assertFalse(TicTacToeMain.isWin(board, Stone.CIRCLE));
    }

    @Test
    void boardStringIncludesPositionsForEmptyFields() {
        Stone[] board = new Stone[9];

        assertEquals(3, TicTacToeMain.toString(board).lines().count());
        assertTrue(TicTacToeMain.toString(board).contains("0"));
    }
}
