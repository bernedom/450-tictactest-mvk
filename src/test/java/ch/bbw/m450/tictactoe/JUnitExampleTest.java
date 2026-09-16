package ch.bbw.m450.tictactoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JUnitExampleTest {

    @Test
    void dummyJUnitTest() {
        String value = "TicTacToe";

        assertEquals("TicTacToe", value);
        assertTrue(value.startsWith("Tic"));
    }
}
