import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Holds the commands that may get invoked from Main. Is a singleton
 */
public class CommandHandler {
    
    private static CommandHandler ourInstance = new CommandHandler();
    
    public static CommandHandler getInstance() {
        return ourInstance;
    }
    
    private final Map<String, Command> commandMap; // Maps string inputs to Command objects
    
    private CommandHandler() {
        commandMap = new HashMap<>();
        
        commandMap.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("pong"))
                .then());
        
        commandMap.put("name", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(generateName()))
                .then());
        
        commandMap.put("test", event -> event.getMessage().getChannel()
                .filter(channel -> channel.getType().equals(Type.DM))
                .flatMap(messageChannel -> messageChannel.createMessage("youre in a dm!"))
                .then());
    }
    
    
    public Mono<Void> handleCommand(String prefix, MessageCreateEvent messageEvent) {
        // Filter out bot users
        Mono<MessageCreateEvent> messageEventMono = Mono.just(messageEvent);
        return messageEventMono
                // Remove messages from bot users
                .filter(event -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent()))
                .flatMap(content -> Flux.fromIterable(commandMap.entrySet())
                        .filter(entry -> content.equals(prefix + entry.getKey()))
                        .flatMap(entry -> entry.getValue().execute(messageEvent))
                        .next()
                );
    }
    
    
    private static String generateName() {
        return generateName(new Random().nextInt());
    }
    
    private static String generateName(long seed) {
        try {
            Random random = new Random(seed);
            List<String> animals = Files.readAllLines(Paths.get("res/animals.txt"));
            List<String> colors = Files.readAllLines(Paths.get("res/colors.txt"));
            return colors.get(random.nextInt(colors.size())) + "_" +
                    animals.get(random.nextInt(animals.size()));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
