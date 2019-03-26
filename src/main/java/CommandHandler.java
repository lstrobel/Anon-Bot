import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Holds the commands that may get invoked from Main
 */
public class CommandHandler {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    
    // Maps string inputs to Command objects
    private static final Map<String, Command> COMMAND_MAP = new HashMap<>();
    
    // The database handler
    private static final AnonymousModel MODEL = AnonymousModel.getInstance();
    
    /**
     * Creates the handler, and adds all of the commands to the command map.
     */
    public static void init() {
        COMMAND_MAP.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("pong"))
                .then());
        
        //TODO: Remove these test commands ----
        COMMAND_MAP.put("name", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(generateName()))
                .then());
        
        COMMAND_MAP.put("anon", event -> event.getMessage().getChannel()
                // Only allow this command from DMs
                .filter(channel -> channel.getType().equals(Type.DM))
                // Map to user's currently-set anonymous channel
                .flatMap(message -> MODEL.getChannelForUser(
                        event.getMessage().getAuthor().orElseThrow(RuntimeException::new)
                        )
                )
                // Send the message
                .flatMap(channel -> channel.createMessage(
                        "`" + MODEL.getIDForUser(event.getMessage().getAuthor().orElseThrow(RuntimeException::new)) + "` " +
                                event.getMessage().getContent().orElse("").split(" ")[1] //TODO: you need to bounds check here someways
                        
                        //event.getMessage().getAuthor().orElseThrow(RuntimeException::new)
                        // .getUsername() + " just sent an anonymous message")}
                ).then()));
        // ------------------------------------
    }
    
    /**
     * Takes a MessageCreateEvent and executes any relevant commands, if that message event
     * turns out to be a valid command.
     *
     * @param prefix       The prefix used to determine if a message is a command
     * @param messageEvent The MessageCreateEvent to parse
     * @return A Mono to subscribe to for reactive responses
     */
    public static Mono<Void> handleCommand(String prefix, MessageCreateEvent messageEvent) {
        return Mono.just(messageEvent)
                // Remove messages from bot users
                .filter(event -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                // Map to Mono<String> representing the content
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent()))
                // Search through COMMAND_MAP and execute matching commands
                .flatMap(content -> Flux.fromIterable(COMMAND_MAP.entrySet())
                        .filter(entry -> content.startsWith(prefix + entry.getKey()))
                        .flatMap(entry -> entry.getValue().execute(messageEvent))
                        .next()
                );
    }
    
    // TODO: Javadoc
    public static String generateName() {
        return generateName(new Random().nextInt());
    }
    
    // TODO: Javadoc
    // TODO: Make this faster by caching, maybe in singleton class or in the fields of this class?
    //  No need to prematurely optimize for now, though
    private static String generateName(long seed) {
        try {
            Random random = new Random(seed);
            List<String> animals = Files.readAllLines(Paths.get("res/animals.txt"));
            List<String> colors = Files.readAllLines(Paths.get("res/colors.txt"));
            return colors.get(random.nextInt(colors.size())) + "_" +
                    animals.get(random.nextInt(animals.size()));
            
        } catch (Exception e) {
            LOGGER.error("Error in generateName", e);
        }
        return null;
    }
}
