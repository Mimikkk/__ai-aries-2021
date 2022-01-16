package put.ai.games.cli;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.GameEngine;
import put.ai.games.engine.impl.GameEngineImpl;
import put.ai.games.engine.loaders.MetaPlayerLoader;
import put.ai.games.game.Player;
import put.ai.games.game.Player.Color;
import put.ai.games.game.exceptions.RuleViolationException;
import put.ai.games.rulesprovider.RulesProvider;

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

    public static void main(String[] args) throws Exception {
        var arguments = List.of(args);
        arguments.forEach(System.err::println);

        var boardSize = args.length > 3 ? Integer.parseInt(args[3]) : 8;
        var timeout = args.length > 4 ? Integer.parseInt(args[4]) : 20000;
        var factory = RulesProvider.INSTANCE.getRulesByName(args[2]);
        factory.configure(new Hashtable<>() {{put(BoardFactory.BOARD_SIZE, boardSize);}});

        var engine = new GameEngineImpl(factory) {{setTimeout(timeout);}};

        var result = new StringBuilder();
        var threadGroup = new ThreadGroup("players");
        for (int i = 0; i < 2; ++i) {
            var player = new Wrapper(MetaPlayerLoader.INSTANCE.load(args[i]).getDeclaredConstructor().newInstance(), i);
            new Thread(threadGroup, player).start();

            engine.addPlayer(player);
            result.append(formatted(player)).append(";");
        }

        String error = "";
        Color winner = Color.EMPTY;

        try {
            winner = engine.play((color, board, move) -> System.out.printf("%s move %s\n", color, move));
        } catch (RuleViolationException reason) {
            winner = Player.getOpponent(reason.getGuilty());
            error = reason.toString();
        } finally {
            result.append(String.format("%s;%s;", formatted(winner), formatted(error)));
            System.out.println(result);
        }

        System.exit(0);
    }
}
