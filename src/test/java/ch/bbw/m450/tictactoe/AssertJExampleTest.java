package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssertJExampleTest {

    @Test
    void dummyAssertJTest() {
        assertThat("TicTacToe").startsWith("Tic").contains("Toe").hasSize(9);
    }
}
