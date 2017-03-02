package ataxx;

import static ataxx.PieceColor.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Tianyi Fang
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Command mvcmd = _game.getMoveCmnd(_myColor.toString() + ": ");
        if (mvcmd == null) {
            return null;
        }
        String[] operands = mvcmd.operands();
        Move mv = Move.move(operands[0].charAt(0), operands[1].charAt(0),
                            operands[2].charAt(0), operands[3].charAt(0));
        return mv;
    }

}
