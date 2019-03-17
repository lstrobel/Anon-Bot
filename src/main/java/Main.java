import discord4j.core.DiscordClientBuilder;

import java.io.*;
import java.util.Properties;

public class Main {
    
    public static void main(String[] args) {
        
        Properties properties = handleConfigFile();
        System.out.println(properties.get("api_token"));
        DiscordClientBuilder builder = new DiscordClientBuilder("TOKEN HERE");
        
    }
    
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
