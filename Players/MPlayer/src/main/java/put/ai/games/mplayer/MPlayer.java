package put.ai.games.mplayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

import static java.lang.Math.*;
import static put.ai.games.mplayer.GameState.*;
import static put.ai.games.mplayer.Strategy.*;

public class MPlayer extends Player {
  @Override
  public Move nextMove(Board board) {
    InitializeRound(board);
    return FindColorMoveAtDepth(Agent, MaxDepth, Max).Move;
  }

  private Field AgentHome(Color color) {
    return Home(color);
  }
  private Field EnemyHome(Color color) {
    return Home(getOpponent(color));
  }
  private Field Home(Color color) {
    if (!HomeFields.containsKey(color)) HomeFields.put(color, FindHomeField(color));
    return HomeFields.get(color);
  }
  private Field FindHomeField(Color color) {
    if (Board.getState(0, 0) == color) return new Field(0, 0);
    return new Field(BoardSize - 1, BoardSize - 1);
  }

  private MinMaxMove FindColorMoveAtDepth(Color color, int depth, Strategy strategy) {
    if (depth == 0) return FindBestMove(color);

    MinMaxMove bestMove = new MinMaxMove(null, -1, strategy);
    for (Move move : Board.getMovesFor(color)) {
      MinMaxMove trial;
      Board.doMove(move);
      try {
        switch (IsGameOver(color)) {
          case Victory:
            return new MinMaxMove(move, 0, strategy.Opposite());
          case Defeat:
            trial = new MinMaxMove(move, 0, strategy); break;
          case Undecided:
            trial = FindColorMoveAtDepth(getOpponent(color), depth - 1, strategy.Opposite()); break;
          default:
            throw new RuntimeException("Unknown game state");
        }
      } finally {Board.undoMove(move);} if (trial.IsBetter(bestMove, strategy)) {
        bestMove = new MinMaxMove(move, trial.Score, trial.Strategy);
      }
    } return bestMove;
  }
  private GameState IsGameOver(Color color) {
    if (Board.getWinner(color) == color) return Victory;
    Color enemy = getOpponent(color);
    if (Board.getWinner(enemy) == enemy) return Defeat;
    return Undecided;
  }
  private MinMaxMove FindBestMove(Color color) {
    Field home = AgentHome(color);
    Field goal = EnemyHome(color);

    Field agentBestPawn = FindBestPawn(color);
    int goalDistance = FindWinDistance(agentBestPawn, goal);

    Field enemyBestPawn = FindBestPawn(getOpponent(color));
    int enemyDistance = FindWinDistance(enemyBestPawn, home);

    List<Move> moves = Board.getMovesFor(color);
    Move bestMove = moves.get(Random.nextInt(moves.size()));
    int agentScore = goalDistance + (InDanger(color) ? BoardSize : 0);
    int enemyScore = enemyDistance;

    boolean shouldDefend = enemyDistance < BoardSize / 2;

    for (Move move : moves) {
      Board.doMove(move);
      if (shouldDefend) {
        Field newEnemy = FindBestPawn(getOpponent(color));
        int newScore = FindWinDistance(newEnemy, home);

        if (newScore > enemyScore) {
          enemyScore = newScore;
          bestMove = move;
        }
      } else {
        Field newPawn = FindBestPawn(color);
        int newScore = FindWinDistance(newPawn, goal) + (InDanger(color) ? BoardSize : 0);

        if (newScore < agentScore) {
          agentScore = newScore;
          bestMove = move;
        }
      } Board.undoMove(move);
    }

    Strategy strategy = shouldDefend ? Max : Min;
    int score = shouldDefend ? enemyScore : agentScore;
    return new MinMaxMove(bestMove, score, strategy);
  }
  private Field FindBestPawn(Color color) {
    Field goal = EnemyHome(color);

    Field pawn = null;
    for (int x = 0; x < BoardSize; x++) {
      for (int y = 0; y < BoardSize; y++) {
        if (Board.getState(x, y) == color) {
          Field field = new Field(x, y);
          if (pawn == null || FindWinDistance(field, goal) < FindWinDistance(pawn, goal)) {
            pawn = field;
          }
        }
      }
    } return pawn;
  }
  private int FindWinDistance(Field first, Field second) {
    int distance = abs(first.X - second.X) + abs(first.Y - second.Y);
    if (distance > 0 && IsFreeWay(first, second)) distance = 1;
    return distance;
  }

  private boolean IsFreeWay(Field first, Field second) {
    switch (Collinearity.From(first, second)) {
      case X:
        if (first.Y < second.Y) for (int i = first.Y + 1; i < second.Y; i++) {
          if (Board.getState(first.X, i) == Agent || Board.getState(first.X, i) == Enemy) return false;
        }
        else for (int i = second.Y + 1; i < first.Y; i++) {
          if (Board.getState(first.X, i) == Agent || Board.getState(first.X, i) == Enemy) return false;
        } break;
      case Y:
        if (first.X < second.X) for (int i = first.X + 1; i < second.X; i++) {
          if (Board.getState(i, first.Y) == Agent || Board.getState(i, first.Y) == Enemy) return false;
        }
        else for (int i = second.X + 1; i < first.X; i++) {
          if (Board.getState(i, first.Y) == Agent || Board.getState(i, first.Y) == Enemy) return false;
        }
        break;
      default:
        return false;
    } return true;
  }
  private boolean IsConquerable(Field field, Color color) {
    Color enemy = getOpponent(color);

    if (IsOnEdge(field)) for (int i = 0; i < BoardSize; i++) {
      if (Board.getState(field.X, i) == enemy || Board.getState(i, field.Y) == enemy) return true;
    }
    else return (FindNeighbour(field, Direction.Up) == enemy && FindNeighbour(field, Direction.Down) == enemy)
      || (FindNeighbour(field, Direction.Right) == enemy && FindNeighbour(field, Direction.Left) == enemy);
    return false;
  }
  private boolean IsOnEdge(Field field) {
    return field.X == 0 || field.X == BoardSize - 1 || field.Y == 0 || field.Y == BoardSize - 1;
  }
  private Color FindNeighbour(Field field, Direction direction) {
    switch (direction) {
      case Up:
        for (int i = field.Y - 1; i >= 0; i--)
          if (Board.getState(field.X, i) != Color.EMPTY) return Board.getState(field.X, i);
        break;
      case Down:
        for (int i = field.Y + 1; i < BoardSize; i++)
          if (Board.getState(field.X, i) != Color.EMPTY) return Board.getState(field.X, i);
        break;
      case Right:
        for (int i = field.X + 1; i < BoardSize; i++)
          if (Board.getState(i, field.Y) != Color.EMPTY) return Board.getState(i, field.Y);
        break;
      case Left:
        for (int i = field.X - 1; i >= 0; i--)
          if (Board.getState(i, field.Y) != Color.EMPTY) return Board.getState(i, field.Y);
        break;
      default:
        return Color.EMPTY;
    }
    return null;
  }
  private boolean InDanger(Color color) {
    for (int x = 0; x < BoardSize; x++)
      for (int y = 0; y < BoardSize; y++) {
        if (Board.getState(x, y) == getColor() && IsConquerable(new Field(x, y), color)) return true;
      }
    return false;
  }

  private void InitializeRound(Board board) {
    Random.setSeed(System.currentTimeMillis());
    Board = board;
    BoardSize = Board.getSize();
    Agent = getColor();
    Enemy = getOpponent(Agent);
  }

  private final Random Random = new Random(System.currentTimeMillis());
  private final Map<Color, Field> HomeFields = new HashMap<>();
  private Board Board;
  private Color Agent;
  private Color Enemy;
  private int BoardSize;

  @Override
  public String getName() {
    return "Daniel Zdancewicz 145317 Monika Zielińska 143719";
  }

  private static final int MaxDepth = 2;
}

class Field {
  public final int X;
  public final int Y;

  public Field(int x, int y) {
    X = x;
    Y = y;
  }
}
class MinMaxMove implements Comparable<MinMaxMove> {
  public final Move Move;
  public final int Score;
  public final Strategy Strategy;

  public boolean IsBetter(MinMaxMove other, Strategy strategy) {
    return strategy == Max ? compareTo(other) > 0 : compareTo(other) < 0;
  }

  public MinMaxMove(Move move, int score, Strategy minmax) {
    Move = move;
    Score = score;
    Strategy = minmax;
  }

  @Override
  public int compareTo(MinMaxMove other) {
    return Strategy == other.Strategy
      ? Strategy == Min ? other.Score - Score : Score - other.Score
      : Strategy == Min ? 1 : -1;
  }
}

enum Strategy {
  Max, Min;
  public Strategy Opposite() {return this == Max ? Min : Max;}
}
enum Collinearity {
  X, Y, None;
  public static Collinearity From(Field first, Field second) {
    if (first.X == second.X) return Collinearity.X;
    else if (first.Y == second.Y) return Collinearity.Y;
    return Collinearity.None;
  }
}
enum Direction {Up, Down, Right, Left}
enum GameState {Victory, Defeat, Undecided}
