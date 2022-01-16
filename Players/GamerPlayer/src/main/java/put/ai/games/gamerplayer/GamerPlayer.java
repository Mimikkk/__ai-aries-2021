package put.ai.games.gamerplayer;

import java.util.List;
import java.util.Random;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class GamerPlayer extends Player {
    private final Random random = new Random(0xfeebdeeb);


    @Override
    public String getName() {
        return "Daniel Zdancewicz 145317 Monika Zielińska 143719";
    }


    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());

        return moves.get(random.nextInt(moves.size()));
    }
}
