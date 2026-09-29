package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIn;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;
import ch.bbw.m450.tictactoe.players.HumanPlayer;

// End-to-end tests of the console game using JUnit Pioneer's @StdIo.
// @StdIo replaces System.in with the given lines (each followed by a line break)
// and, if a StdOut parameter is declared, captures everything written to System.out.
// The human player (X) always starts, the greedy computer player (O) fills the
// first free field from the top-left.
class TicTacToeStdIoTest {

    // Human plays 3, 4, 5 while the greedy player takes 0 and 1 -> middle row for X.
    @Test
    @StdIo({ "3", "4", "5" })
    void humanWinsWithMiddleRow(StdIn in, StdOut out) {
        TicTacToeMain.main(new String[0]);

        assertThat(out.capturedString()).endsWith("...and the winner is: CROSS" + System.lineSeparator());
        assertThat(in.capturedLines()).containsExactly("3", "4", "5");
    }

    // Human plays 8, 7, 5 while the greedy player takes 0, 1, 2 -> top row for O.
    @Test
    @StdIo({ "8", "7", "5" })
    void computerWinsWithTopRow(StdOut out) {
        Stone winner = TicTacToeMain.play(new HumanPlayer(), new GreedyPlayer());

        assertThat(winner).isEqualTo(Stone.CIRCLE);
        assertThat(out.capturedString()).contains("...and the winner is: CIRCLE");
    }

    // The human is prompted once per move, the computer's moves print nothing.
    @Test
    @StdIo({ "3", "4", "5" })
    void promptsHumanForEveryMove(StdOut out) {
        TicTacToeMain.main(new String[0]);

        assertThat(out.capturedLines())
                .filteredOn(line -> line.startsWith("where to"))
                .containsExactly(
                        "where to to put the next CROSS? (0-8): ",
                        "where to to put the next CROSS? (0-8): ",
                        "where to to put the next CROSS? (0-8): ");
    }

    // The first prompt shows the empty board with all field numbers.
    @Test
    @StdIo({ "3", "4", "5" })
    void showsEmptyBoardBeforeFirstMove(StdOut out) {
        TicTacToeMain.main(new String[0]);

        String[] firstBoardRows = stripAnsi(out.capturedString()).lines().limit(3).toArray(String[]::new);
        assertThat(firstBoardRows).containsExactly("0  1  2  ", "3  4  5  ", "6  7  8  ");
    }

    // A move outside the board aborts the game.
    @Test
    @StdIo({ "9" })
    void rejectsMoveOutsideBoard(StdOut out) {
        assertThatThrownBy(() -> TicTacToeMain.main(new String[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot play to position 9");
    }

    // Playing onto a field the computer already occupies aborts the game.
    @Test
    @StdIo({ "4", "0" })
    void rejectsMoveOntoOccupiedField(StdOut out) {
        assertThatThrownBy(() -> TicTacToeMain.main(new String[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot play to position 0");
    }

    // Input that is not a number is not handled gracefully.
    @Test
    @StdIo({ "hello" })
    void failsOnNonNumericInput(StdOut out) {
        assertThatThrownBy(() -> TicTacToeMain.main(new String[0]))
                .isInstanceOf(NumberFormatException.class);
    }

    private static String stripAnsi(String text) {
        return text.replaceAll("\033\\[[0-9;]*m", "");
    }
}
