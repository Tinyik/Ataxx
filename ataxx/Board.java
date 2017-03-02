package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Observable;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Tianyi Fang
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** ASCII. */
    static final int ASCIICONST = 47;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        clear();
    }

    /** A copy of B. */
    @SuppressWarnings("unchecked")
    Board(Board b) {
        _board = b._board.clone();
        _whoseMove = b._whoseMove;
        _jumpCount = b._jumpCount;
        _undoStack = (Stack<Board>) b._undoStack.clone();
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the charater representation of tile at sq. Assume in range.
     * @param   sq  liner index
     * @return      converted alphabetical index in char array
    */
    static char[] toChar(int sq) {
        char[] result = new char[2];
        result[1] = (char) ((int) (sq / EXTENDED_SIDE + ASCIICONST));
        result[0] = (char) ('a' - 2 + (sq % EXTENDED_SIDE));
        return result;
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Indices of tiles in a (2*OFFSET + 1)^2 square centers at SQ.
     * @return  a list of adjacent index within OFFSET centers at SQ
    */
    static List<Integer> squareIndices(int sq, int offset) {
        List<Integer> result = new ArrayList<Integer>();
        for (int dc = -offset; dc <= offset; dc++) {
            for (int dr = -offset; dr <= offset; dr++) {
                result.add(neighbor(sq, dc, dr));
            }
        }
        return result;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _whoseMove = RED;
        _jumpCount = 0;
        _undoStack = new Stack<Board>();
        for (int i = 0; i < _board.length; i++) {
            _board[i] = BLOCKED;
        }
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                _board[index((char) i, (char) j)] = EMPTY;
            }
        }
        _board[index('a', '7')] = RED;
        _board[index('g', '1')] = RED;
        _board[index('a', '1')] = BLUE;
        _board[index('g', '7')] = BLUE;
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        return (!canMove(_whoseMove) && !canMove(_whoseMove.opposite()))
                || numJumps() >= JUMP_LIMIT;
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        int count = 0;
        for (PieceColor p : _board) {
            if (p == color) {
                count += 1;
            }
        }
        return count;
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        _whoseMove = _whoseMove.opposite();
        _board[sq] = v;
    }

    /** Set square at C R to V (not undoable). */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        if (move.isPass()) {
            return (canMove(_whoseMove)) ? false : true;
        }
        if (_board[move.fromIndex()] != _whoseMove) {
            return false;
        }
        if (_board[move.toIndex()] != EMPTY) {
            return false;
        }
        return move.isExtend() || move.isJump();
    }

    /** Check wheter FROM and TO are available. Helper method for CANMOVE.
     * @ return     movable
    */
    boolean checkMovable(int from, int to) {
        if (_board[from] != _whoseMove) {
            return false;
        }
        if (_board[to] != EMPTY) {
            return false;
        }
        return true;
    }

 /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                int sq = index((char) i, (char) j);
                List<Integer> neighbors = squareIndices(sq, 2);
                for (Integer nb : neighbors) {
                    if (checkMovable(sq, nb)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _undoStack.size();
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _jumpCount;
    }

    /** Leading player on board.
     * @return      the leading player for this game
    */
    PieceColor leading() {
        return (redPieces() > bluePieces()) ? RED : BLUE;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        if (move == null) {
            throw error("That move is illegal.");
        }
        if (legalMove(move)) {
            _undoStack.push(new Board(this));
            if (move.isPass()) {
                pass();
                return;
            }
            if (move.isExtend()) {
                _board[move.toIndex()] = _whoseMove;
                _jumpCount = 0;
            }
            if (move.isJump()) {
                _board[move.toIndex()] = _whoseMove;
                _board[move.fromIndex()] = EMPTY;
                _jumpCount += 1;
            }
            for (Integer i : squareIndices(move.toIndex(), 1)) {
                if (_board[i] != BLOCKED && _board[i] != EMPTY) {
                    _board[i] = _whoseMove;
                }
            }
            _whoseMove = _whoseMove.opposite();
            setChanged();
            notifyObservers();
        } else {
            throw error("That move is illegal.");
        }

    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        assert !canMove(_whoseMove);
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        try {
            Board last = _undoStack.pop();
            _whoseMove = last._whoseMove;
            _jumpCount = last._jumpCount;
            _board = last._board;
        } catch (java.util.EmptyStackException excp) {
            System.out.println("Aborted. Attempt to pop empty stack.");
        }
        setChanged();
        notifyObservers();
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        try {
            return _board[index(c, r)] == EMPTY;
        } catch (java.lang.ArrayIndexOutOfBoundsException excp) {
            return false;
        }
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("Illegal block placement");
        }
        int li = index(c, r);
        int center = (EXTENDED_SIDE * EXTENDED_SIDE - 1) / 2;
        int indexsum = 2 * (li % (EXTENDED_SIDE))
                        + EXTENDED_SIDE * (EXTENDED_SIDE - 1);
        int index1 = center - (li - center);
        int index2 = indexsum - li;
        int index3 = center - (index2 - center);
        _board[li] = BLOCKED;
        _board[index1] = BLOCKED;
        _board[index2] = BLOCKED;
        _board[index3] = BLOCKED;
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return WHO's all legal moves. (Unused) */
    List<Move> allLegalMoves(PieceColor who) {
        ArrayList<Move> result = new ArrayList<Move>();
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                int sq = index((char) i, (char) j);
                if (get(sq) == who) {
                    List<Integer> neighbors = squareIndices(sq, 2);
                    for (Integer nb : neighbors) {
                        if (checkMovable(sq, nb)) {
                            char[] grid = toChar(nb);
                            Move mv = Move.move((char) i, (char) j,
                                                 grid[0], grid[1]);
                            result.add(mv);
                        }
                    }
                }
            }
        }
        return result;
    }

    /** Return a list of linear indices of all WHO's pieces. */
    List<Integer> allPositions(PieceColor who) {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                int sq = index((char) i, (char) j);
                if (get(sq) == who) {
                    result.add(sq);
                }
            }
        }
        return result;
    }


    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        String str = "";
        if (legend) {
            for (int i = 7; i >= 1; i--) {
                str = str + String.valueOf(i) + " %s %s %s %s %s %s %s\n";
            }
            str = str + "  a b c d e f g\n";
        } else {
            for (int i = 7; i >= 1; i--) {
                str = str + " %s %s %s %s %s %s %s\n";
            }
        }

        List<String> argList = new ArrayList<String>();

        for (int i = '7'; i >= '1'; i--) {
            for (int j = 'a'; j <= 'g'; j++) {
                int sq = Board.index((char) j, (char) i);
                String token = "";
                switch (_board[sq]) {
                case BLOCKED: token = "X"; break;
                case RED: token = "r"; break;
                case BLUE: token = "b"; break;
                case EMPTY: token = "-"; break;
                default: break;
                }
                argList.add(token);
            }
        }
        Object[] args = (Object[]) argList.toArray(new Object[argList.size()]);
        return String.format(str, args);
    }

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private PieceColor[] _board;

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Count the number of consecutive jumps. */
    private int _jumpCount;

    /** The undo stack of current game. */
    private Stack<Board> _undoStack;

}
