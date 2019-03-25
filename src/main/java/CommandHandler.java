import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
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
    
    // Singleton instance
    public static CommandHandler getInstance() {
        return ourInstance;
    }
    
    private final Map<String, Command> commandMap; // Maps string inputs to Command objects
    
    /**
     * Creates the handler, and adds all of the commands to the command map.
     */
    private CommandHandler() {
        commandMap = new HashMap<>();
        
        commandMap.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("pong"))
                .then());
        
        //TODO: Remove these test commands ----
        commandMap.put("name", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(generateName()))
                .then());
        
        commandMap.put("testAnon", event -> event.getMessage().getChannel()
                // Only allow this command from DMs
                .filter(channel -> channel.getType().equals(Type.DM))
                // Map to user's currently-set anonymous channel
                .flatMap(message -> event
                        .getMessage()
                        .getAuthor()
                        .orElseThrow(RuntimeException::new)
                        .getClient()
                        .getGuildById(Snowflake.of("556690884121591838"))// Grab this from db - current guild
                        .flatMap(guild -> guild.getChannelById(Snowflake.of("556690884121591841")))// Grab this from db - current channel
                )
                // Ensure that the stored channel is a text channel - it should be.
                .filter(guildChannel -> guildChannel.getType().equals(Type.GUILD_TEXT))
                // Cast to a TextChannel to send messages - we ensured this is okay above
                .map(guildChannel -> (TextChannel) guildChannel)
                // Send the message
                .flatMap(channel -> channel.createMessage(
                        event.getMessage().getAuthor().orElseThrow(RuntimeException::new).getUsername() + " just sent an anonymous message")
                )
                .then());
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
    public Mono<Void> handleCommand(String prefix, MessageCreateEvent messageEvent) {
        // Filter out bot users
        Mono<MessageCreateEvent> messageEventMono = Mono.just(messageEvent);
        return messageEventMono
                // Remove messages from bot users
                .filter(event -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                // Map to Mono<String> representing the content
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent()))
                // Search through commandMap and execute matching commands
                .flatMap(content -> Flux.fromIterable(commandMap.entrySet())
                        .filter(entry -> content.equals(prefix + entry.getKey()))
                        .flatMap(entry -> entry.getValue().execute(messageEvent))
                        .next()
                );
    }
    
    // TODO: Javadoc
    private static String generateName() {
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
            e.printStackTrace();
        }
        return null;
    }
}
