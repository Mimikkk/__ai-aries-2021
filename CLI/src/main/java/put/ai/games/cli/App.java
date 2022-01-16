package put.ai.games.cli;

import java.util.*;

import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.impl.GameEngineImpl;
import put.ai.games.game.Player;
import put.ai.games.game.Player.Color;
import put.ai.games.game.exceptions.RuleViolationException;
import put.ai.games.rulesprovider.RulesProvider;

import static put.ai.games.engine.loaders.MetaPlayerLoader.*;

public class App {
    private static GameEngineImpl engine = null;
    private static BoardFactory factory = null;
    private static int timeout = 20000;
    private static int boardSize = 8;
    private static int firstWins = 0;
    private static int secondWins = 0;
    private static int draws = 0;

    public static void main(String[] args) throws Exception {
        Arrays.stream(args).forEach(System.out::println);

        var firstLoader = INSTANCE.load(args[0]).getDeclaredConstructor();
        var secondLoader = INSTANCE.load(args[1]).getDeclaredConstructor();
        factory = RulesProvider.INSTANCE.getRulesByName(args[2]);
        if (args.length > 3) timeout = Integer.parseInt(args[3]);
        if (args.length > 4) boardSize = Integer.parseInt(args[4]);

        factory.configure(new Hashtable<>() {{put(BoardFactory.BOARD_SIZE, boardSize);}});

        var threadGroup = new ThreadGroup("players");
        var first = new Wrapper(firstLoader.newInstance());
        var second = new Wrapper(secondLoader.newInstance());
        new Thread(threadGroup, first).start();
        new Thread(threadGroup, second).start();
        engine = new GameEngineImpl(factory) {{
            setTimeout(timeout);
            addPlayer(first);
            addPlayer(second);
        }};

        for (var i = 0; i < 101; play()) {
            if (++i % 5 != 0) continue;
            System.out.printf("Iteration %d/%d\n", i, 100);
            System.out.println("First wins: " + firstWins);
            System.out.println("Second wins: " + secondWins);
            System.out.println("Draws: " + draws);
        }
        System.exit(0);
    }

    private static void play() {
        Color winner = run();
        firstWins += winner == Color.PLAYER1 ? 1 : 0;
        secondWins += winner == Color.PLAYER2 ? 1 : 0;
        draws += winner == Color.EMPTY ? 1 : 0;
    }

    private static Color run() {
        try {
            return engine.play((color, board, move) -> {});
        } catch (RuleViolationException reason) {
            return Player.getOpponent(reason.getGuilty());
        }
    }
}
