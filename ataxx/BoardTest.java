package ataxx;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author
 */
public class BoardTest {

    private static final String[]
        GAME1 = { "a7-b7", "a1-a2",
                  "a7-a6", "a2-a3",
                  "a6-a5", "a3-a4" };

    private static final String[]
        GAME2 = { "g1-g2", "a1-c3",
                  "g2-f3", "c3-c4",
                  "f3-f4", "c4-c5",
                  "f4-e5" };


    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        Board b4 = new Board();
        Board b3 = new Board(b4);
        makeMoves(b3, GAME2);
        makeMoves(b0, GAME1);
        for (int i = 0; i < GAME2.length; i += 1) {
            b3.undo();
        }
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals("failed to return to start", b1, b0);
        assertEquals("failed to return to start for game 2", b4, b3);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

}
