package put.ai.games.mnplayer;

import java.util.Collections;
import java.util.List;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

import static put.ai.games.mnplayer.GameState.*;
import static put.ai.games.mnplayer.Strategy.*;

public class MNPlayer extends Player {
  @Override
  public Move nextMove(Board board) {
    InitializeRound(board);
    return FindBestMove(Agent);
  }

  private Move FindBestMove(Color color) {
    return FindBestScoredMove(color, MaxDepth, Strategy.Max).Move;
  }

  private ScoredMove FindBestScoredMove(Color color, int depth, Strategy strategy) {
    if (depth == 0) return new ScoredMove(null, Score());

    List<Move> moves = Board.getMovesFor(color);
    Collections.shuffle(moves);

    ScoredMove best = strategy.WorstMove;
    for (Move move : moves) {
      if (IsOutOfTime()) return best;
      ScoredMove next = null;

      try {
        Board.doMove(move);
        switch (IsGameOver(color)) {
          case Victory:
            return new ScoredMove(move, strategy.BestScore);
          case Defeat:
            next = new ScoredMove(move, strategy.WorstScore);
            break;
          case Undecided:
            next = FindBestScoredMove(getOpponent(color), depth - 1, strategy.Opposite());
        }
      } finally {Board.undoMove(move);}

      if (strategy.Compare(next, best)) best = new ScoredMove(move, next.Score);
    }

    return best;
  }

  private GameState IsGameOver(Color color) {
    if (Board.getWinner(color) == color) return Victory;
    Color enemy = getOpponent(color);
    if (Board.getWinner(enemy) == enemy) return Defeat;
    return Undecided;
  }
  private int Score() {
    return CountPawns();
  }
  private int CountPawns() {
    int count = 0;
    for (int x = 0; x < BoardSize; ++x) {
      for (int y = 0; y < BoardSize; ++y) {
        Color color = Board.getState(x, y);
        if (color == getColor()) {
          ++count;
        } else if (color == getOpponent(getColor())) {
          --count;
        }
      }
    }
    return count;
  }
  private boolean IsOutOfTime() {
    long timeBuffer = 200;
    return TimeLimit + Start - System.currentTimeMillis() < timeBuffer;
  }
  private void InitializeRound(Board board) {
    Board = board;
    Start = System.currentTimeMillis();
    if (BoardSize == 0) {
      TimeLimit = getTime();
      Agent = getColor();
      BoardSize = Board.getSize();
      Min.WorstScore = Integer.MAX_VALUE;
      Max.WorstScore = Integer.MIN_VALUE;
      Max.BestScore = Integer.MAX_VALUE;
      Min.BestScore = Integer.MIN_VALUE;
      Min.WorstMove = new ScoredMove(null, Integer.MAX_VALUE);
      Max.WorstMove = new ScoredMove(null, Integer.MIN_VALUE);
    }
  }

  private int BoardSize;
  private Board Board;
  private Color Agent;
  private long Start;
  private long TimeLimit;

  @Override
  public String getName() {
    return "Daniel Zdancewicz 145317 Monika Zielińska 143719";
  }

  private static final int MaxDepth = 8;
}

class ScoredMove {
  public final Move Move;
  public final int Score;

  public ScoredMove(Move move, int eval) {
    Move = move;
    Score = eval;
  }
}

enum Strategy {
  Max, Min;
  public int BestScore;
  public int WorstScore;
  public ScoredMove WorstMove;

  public boolean Compare(ScoredMove first, ScoredMove second) {
    if (this == Max) return first.Score > second.Score;
    return first.Score < second.Score;
  }
  public Strategy Opposite() {
    return this == Max ? Min : Max;
  }
}
enum GameState {Victory, Defeat, Undecided}