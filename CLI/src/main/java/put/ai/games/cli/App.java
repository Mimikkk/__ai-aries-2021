package put.ai.games.cli;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.GameEngine;
import put.ai.games.engine.impl.GameEngineImpl;
import put.ai.games.engine.loaders.MetaPlayerLoader;
import put.ai.games.engine.loaders.PlayerLoader;
import put.ai.games.engine.loaders.PlayerLoadingException;
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

    private static void play() throws Exception {
        var threadGroup = new ThreadGroup("players");

        var first = new Wrapper(firstPlayerLoader.newInstance(), 1);
        var second = new Wrapper(secondPlayerLoader.newInstance(), 2);

        new Thread(threadGroup, first).start();
        new Thread(threadGroup, second).start();

        var engine = new GameEngineImpl(factory) {{
            setTimeout(timeout);
            addPlayer(first);
            addPlayer(second);
        }};

        String error = "";
        Color winner;
        System.out.println("Starting game");

        try {
            winner = engine.play((color, board, move) -> System.out.printf("%s move %s\n", color, move));
        } catch (RuleViolationException reason) {
            winner = Player.getOpponent(reason.getGuilty());
            error = reason.toString();
        }

        var result = Stream.of(first, second, winner, error)
            .map(App::formatted).collect(joining(";"));
        System.out.println(result);
    }

    public static void main(String[] args) throws Exception {
        Arrays.stream(args).forEach(System.out::println);
        firstPlayerLoader = INSTANCE.load(args[0]).getDeclaredConstructor();
        secondPlayerLoader = INSTANCE.load(args[1]).getDeclaredConstructor();
        factory = RulesProvider.INSTANCE.getRulesByName(args[2]);
        factory.configure(new Hashtable<>() {{put(BoardFactory.BOARD_SIZE, boardSize);}});
        if (args.length > 3) timeout = Integer.parseInt(args[3]);
        if (args.length > 4) boardSize = Integer.parseInt(args[4]);


        for (var i = 0; i < 5; ++i) play();
        System.exit(0);
    }
}
