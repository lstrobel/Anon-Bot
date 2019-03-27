import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Message;
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
    private static final DatabaseModel MODEL = DatabaseModel.getInstance();
    
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
        
        COMMAND_MAP.put("anon ", event -> event.getMessage().getChannel()
                // Only allow this command from DMs
                .filter(channel -> channel.getType().equals(Type.DM))
                // Map to user's currently-set anonymous channel
                .flatMap(message -> MODEL.getChannelForUser(
                        event.getMessage().getAuthor().orElseThrow(RuntimeException::new)
                        )
                )
                // Send the message
                .flatMap(channel -> channel.createMessage(buildAnonMessage(event.getMessage())))
                .then());
        // ------------------------------------
    }
    
    /**
     * Will construct an anonymous message for the bot to post, given the appropriate Message
     * containing the original command by the user.
     *
     * @param message The Message containing the original command by the user
     * @return The String that the bot should post
     */
    private static String buildAnonMessage(Message message) {
        String content = message.getContent().orElse("");
        
        StringBuilder sb =
                new StringBuilder();
        sb.append('`');
        sb.append(MODEL.getIDForUser(message.getAuthor().orElseThrow(RuntimeException::new)));
        sb.append("` ");
        
        if (content.substring(1).startsWith("anon")) {
            sb.append(content.substring(5).trim());
        } else { // Preparation for when users dont have to enter the command to speak
            sb.append(content.trim());
        }
        
        return sb.toString();
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
}
