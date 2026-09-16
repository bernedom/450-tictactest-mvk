package ch.bbw.m450.tictactoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

/**
 * Same scenarios as {@link TicTacToeTestMainParameterized}, but using the board
 * parsing promoted to the shared {@link BoardFixture} instead of a private helper
 * method, so it can be reused across test classes.
 */
class TicTacToeTestMainParameterizedFixture {

    // Each row from greedyMoveCases provides: board layout, color to play, expected
    // chosen index.
    @ParameterizedTest(name = "board={0}, color={1} -> expected index {2}")
    @MethodSource("greedyMoveCases")
    void greedyPlayerAlwaysPlaysMostTopLeftFreePositionFixture(String boardLayout, Stone colorToPlay, int expectedIndex) {
        Stone[] board = BoardFixture.parseBoard(boardLayout);

        int chosenIndex = new GreedyPlayer().play(board, colorToPlay);

        assertEquals(expectedIndex, chosenIndex);
    }

    // Board encoding for fixtures: X = CROSS, O = CIRCLE, _ = empty field.
    // The expected index is always the first free cell when scanning from index 0
    // to 8 (top-left to bottom-right).
    private static Stream<Arguments> greedyMoveCases() {
        return Stream.of(
                Arguments.of("_________", Stone.CROSS, 0),
                Arguments.of("X________", Stone.CIRCLE, 1),
                Arguments.of("XO_______", Stone.CROSS, 2),
                Arguments.of("XOX______", Stone.CIRCLE, 3),
                Arguments.of("XOXOX____", Stone.CROSS, 5),
                Arguments.of("XOXOXO___", Stone.CIRCLE, 6),
                Arguments.of("XOXOXOXO_", Stone.CROSS, 8));
    }
}
