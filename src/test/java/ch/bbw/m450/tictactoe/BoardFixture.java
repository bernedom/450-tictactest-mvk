package ch.bbw.m450.tictactoe;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Shared test fixture for building TicTacToe boards from a compact string encoding,
 * so multiple test classes can reuse the same board-building logic instead of each
 * defining their own private helper.
 */
public final class BoardFixture {

    private BoardFixture() {
    }

    // Board encoding: X = CROSS, O = CIRCLE, _ = empty field.
    public static Stone[] parseBoard(String layout) {
        if (layout.length() != TicTacToeMain.BOARD_SIZE) {
            throw new IllegalArgumentException("layout must have exactly " + TicTacToeMain.BOARD_SIZE + " chars");
        }

        Stone[] board = new Stone[TicTacToeMain.BOARD_SIZE];
        for (int i = 0; i < layout.length(); i++) {
            char c = layout.charAt(i);
            if (c == 'X') {
                board[i] = Stone.CROSS;
            } else if (c == 'O') {
                board[i] = Stone.CIRCLE;
            } else if (c == '_') {
                board[i] = null;
            } else {
                throw new IllegalArgumentException("invalid board character: " + c);
            }
        }
        return board;
    }
}
