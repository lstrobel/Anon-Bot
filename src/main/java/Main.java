import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {
    
    private static final Map<String, Command> commands = new HashMap<>();
    
    public static void main(String[] args) {
        
        final Properties properties = handleConfigFile();
        final String command_id = properties.getProperty("command_identifier");
        DiscordClientBuilder builder =
                new DiscordClientBuilder(properties.getProperty("api_token"));
        final DiscordClient client = builder.build();
        
        // Login to Discord
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });
        
        //TODO: Remove this test command
        commands.put("ping", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("pong"))
                .then());
        
        // Attach listener to MessageCreateEvent, which runs corresponding commands
        client.getEventDispatcher().on(MessageCreateEvent.class)
                // Filter out bot users
                .filter(event -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                // Iterate through commands
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                // If the command matches, run it
                                .filter(entry -> content.startsWith(command_id + entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()
                        )
                )
                .subscribe();
        
        client.login().block();
    }
    
    /**
     * Will read from a config.properties file in the working dir and return a corresponding
     * Properties. If no such file exists, it will create one and then exit.
     *
     * @return The Properties read from config.properties
     */
    private static Properties handleConfigFile() {
        
        Properties properties = new Properties();
        
        InputStream input = null;
        FileOutputStream out = null;
        
        try {
            File configFile = new File("config.properties");
            
            if (!configFile.exists()) { // Create a config.properties if there isn't one, and quit
                System.out.println(
                        "Unable to find a config.properties file. Creating one for you.");
                System.out.println("Please fill out the config then restart.");
                
                out = new FileOutputStream("config.properties");
                properties.setProperty("api_token", "YOUR TOKEN HERE");
                properties.setProperty("command_identifier", ">");
                properties.store(out, null);
                
                System.exit(0);
            }
            
            input = new FileInputStream(configFile);
            properties.load(input);
            
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally { // Close streams
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return properties;
    }
    
}
