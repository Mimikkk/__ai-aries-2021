package put.ai.games.cli;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;

import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.impl.GameEngineImpl;
import put.ai.games.game.Player;
import put.ai.games.game.Player.Color;
import put.ai.games.game.exceptions.RuleViolationException;
import put.ai.games.rulesprovider.RulesProvider;

import static java.util.stream.Collectors.*;
import static put.ai.games.engine.loaders.MetaPlayerLoader.*;

public class App {

    public static String escape(String string) {
        return string.replaceAll("[^\\p{Graph}\\p{Blank}]", "_").replace("\"", "\"\"");
    }

    public static String formatted(Object string) {
        return "\"" + escape(string.toString()) + "\"";
    }

    public static String formatted(Player player) {
        return formatted(player.getName());
    }

    private static BoardFactory factory = null;
    private static int timeout = 20000;
    private static int boardSize = 8;

    private static Constructor<? extends Player> firstPlayerLoader = null;
    private static Constructor<? extends Player> secondPlayerLoader = null;

    private static int firstWins = 0;
    private static int secondWins = 0;
    private static int draws = 0;

    private static void play() throws Exception {
        var threadGroup = new ThreadGroup("players");

        var first = new Wrapper(firstPlayerLoader.newInstance());
        var second = new Wrapper(secondPlayerLoader.newInstance());

        new Thread(threadGroup, first).start();
        new Thread(threadGroup, second).start();

        var engine = new GameEngineImpl(factory) {{
            setTimeout(timeout);
            addPlayer(first);
            addPlayer(second);
        }};

        Color winner;
        try {
            winner = engine.play((color, board, move) -> {});
        } catch (RuleViolationException reason) {
            winner = Player.getOpponent(reason.getGuilty());
        }

        firstWins += winner == Color.PLAYER1 ? 1 : 0;
        secondWins += winner == Color.PLAYER2 ? 1 : 0;
        draws += winner == Color.EMPTY ? 1 : 0;
    }

    public static void main(String[] args) throws Exception {
        Arrays.stream(args).forEach(System.out::println);
        firstPlayerLoader = INSTANCE.load(args[0]).getDeclaredConstructor();
        secondPlayerLoader = INSTANCE.load(args[1]).getDeclaredConstructor();
        factory = RulesProvider.INSTANCE.getRulesByName(args[2]);
        factory.configure(new Hashtable<>() {{put(BoardFactory.BOARD_SIZE, boardSize);}});
        if (args.length > 3) timeout = Integer.parseInt(args[3]);
        if (args.length > 4) boardSize = Integer.parseInt(args[4]);


        for (var i = 0; i < 101; play()) {
            if (++i % 5 != 0) continue;
            System.out.printf("Iteration %d/%d\n", i, 100);
            System.out.println("First wins: " + firstWins);
            System.out.println("Second wins: " + secondWins);
            System.out.println("Draws: " + draws);
        }
        System.exit(0);
    }
}
