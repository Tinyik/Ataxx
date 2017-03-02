package ataxx;

import static ataxx.PieceColor.*;
import static java.lang.Math.min;
import static java.lang.Math.max;
import java.util.List;
import java.util.Collections;
import java.util.Random;

/** A Player that computes its own moves.
 *  @author Tianyi Fang
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 5;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            System.out.println(_myColor.toString() + " passes.");
            return Move.pass();
        }
        Move move = findMove();
        String postion = (String.valueOf(move.col0())
             + String.valueOf(move.row0()) + '-' + String.valueOf(move.col1())
                                                + String.valueOf(move.row1()));
        System.out.println(_myColor.toString() + " moves " + postion + ".");
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY, false);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY, false);
        }
        return _lastFoundMove;
    }

    @Override
    void setSeed(Long seed) {
        _seed = seed;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. If SIMPLE, only use
     *  static value as a rough estimation.
     */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta, boolean simple) {
        if (simple) {
            if (board.gameOver() && board.leading() == RED) {
                return INFTY;
            }
            if (board.gameOver() && board.leading() == BLUE) {
                return -INFTY;
            }
        } else {
            if (depth == 0 || board.gameOver()) {
                return findMove(board, depth, false, sense, alpha, beta, true);
            }
        }
        int bestSoFar = (sense == 1) ? -INFTY : INFTY;
        List<Integer> positions = (sense == 1) ? board.allPositions(RED)
                                               : board.allPositions(BLUE);
        if (_seed != null) {
            Collections.shuffle(positions, new Random(_seed));
        } else {
            Collections.shuffle(positions);
        }
        for (Integer sq : positions) {
            List<Integer> neighbors = board.squareIndices(sq, 2);
            for (Integer nb : neighbors) {
                if (board().checkMovable(sq, nb)) {
                    char[] src = board.toChar(sq), dest = board.toChar(nb);
                    Move mv = Move.move(src[0], src[1], dest[0], dest[1]);
                    board.makeMove(mv);
                    int scoreNext = simple ? staticScore(board)
                                           : findMove(board, depth - 1, false,
                                                   -sense, alpha, beta, false);
                    if (sense == 1) {
                        if (scoreNext >= bestSoFar || board.gameOver()) {
                            if (saveMove) {
                                _lastFoundMove = mv;
                            }
                            bestSoFar = scoreNext;
                            alpha = max(alpha, scoreNext);
                            if (beta <= alpha) {
                                return bestSoFar;
                            }
                        }
                    } else if (sense == -1) {
                        if (scoreNext <= bestSoFar || board.gameOver()) {
                            if (saveMove) {
                                _lastFoundMove = mv;
                            }
                            bestSoFar = scoreNext;
                            beta = min(beta, scoreNext);
                            if (beta <= alpha) {
                                return bestSoFar;
                            }
                        }
                    }
                    board.undo();
                }
            }
        }
        return bestSoFar;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        return board.redPieces() - board.bluePieces();
    }

    /** AI seed. */
    private Long _seed;
}
