import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.io.*;
import java.util.Properties;

public class Main {
    
    public static void main(String[] args) {
        
        Properties properties = handleConfigFile();
        DiscordClientBuilder builder =
                new DiscordClientBuilder(properties.getProperty("api_token"));
        DiscordClient client = builder.build();
        
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });
        
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().orElse("").equalsIgnoreCase("!ping"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Pong!"))
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
