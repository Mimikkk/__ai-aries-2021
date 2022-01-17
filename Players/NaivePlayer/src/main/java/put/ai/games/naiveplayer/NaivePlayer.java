package put.ai.games.naiveplayer;

import java.util.Collections;
import java.util.List;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class NaivePlayer extends Player {

    private long startTime = 0;
    private long timeLimit = this.getTime();

    private class MyMove {
        private Move move;
        private double score;
        public MyMove(Move move, double score) {
            this.move = move;
            this.score = score;
        }
    }

    @Override
    public String getName() {
        return "Mikołaj Skrzypczak 145398 Piotr Baryczkowski 145314";
    }

    double score(Board board, Color color) {
        if (board.getWinner(color) == color) return Double.MAX_VALUE;
        int result = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board.getState(i, j) == color) result += 1;
                else if (board.getState(i, j) != Color.EMPTY) result -= 1;
            }
        }
        return result;
    }

    boolean timeout() {
        return System.currentTimeMillis() - this.startTime >= this.timeLimit;
    }

    MyMove negMaxAlphaBeta( Board board, Color color, int turnMultiplier, int depth, double alpha, double beta) {
        Color nextColor = (color == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;

        List<Move> nextMoves = board.getMovesFor(color);
        Collections.shuffle(nextMoves);

        if (depth == 0 || nextMoves.isEmpty() || timeout())
            return new MyMove(null, turnMultiplier * score(board, color));

        double value = Double.MIN_VALUE;
        Move chosenMove = null;
        for (Move move : nextMoves) {
            board.doMove(move);
            MyMove newMoveAndEval = negMaxAlphaBeta(board, nextColor, -turnMultiplier, depth - 1, -beta, -alpha);
            board.undoMove(move);

            chosenMove = move;
            value = Math.max(value, -newMoveAndEval.score);
            alpha = Math.max(value, alpha);

            if (alpha >= beta) {
                break;
            }
        }
        return new MyMove(chosenMove, alpha);
    }

    Move getMyMove(Board board, Color color, int depthLimit) {
        return negMaxAlphaBeta(board, color, 1, depthLimit, Double.MIN_VALUE, Double.MAX_VALUE).move;
    }

    @Override
    public Move nextMove(Board board) {
        this.timeLimit = this.getTime() - 500;
        this.startTime = System.currentTimeMillis();
        return this.getMyMove(board, getColor(),  2);
    }
}