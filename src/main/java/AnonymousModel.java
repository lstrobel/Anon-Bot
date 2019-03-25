import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class AnonymousModel {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(AnonymousModel.class);
    
    private static AnonymousModel ourInstance = new AnonymousModel();
    
    // Singleton instance
    public static AnonymousModel getInstance() {
        return ourInstance;
    }
    
    private static final String DATABASE_URL = "jdbc:sqlite:server_database.db";
    
    private AnonymousModel() {
        try {
            LOGGER.info("Starting database...");
            
            Connection con = DriverManager.getConnection(DATABASE_URL);
            
            Statement stmt = con.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS \"ServerConfiguration\"(\n" +
                    "\"server_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\"can_choose_id\" Boolean NOT NULL DEFAULT 0,\n" +
                    "\"id_cooldown_milliseconds\" Integer NOT NULL DEFAULT 0,\n" +
                    "\"previous_id_capacity\" Integer NOT NULL DEFAULT 10,\n" +
                    "\"automatic_blacklist\" Boolean NOT NULL DEFAULT 0,\n" +
                    "\"default_timeout_milliseconds\" Integer NOT NULL DEFAULT 60000 );");
            
            stmt.execute("  CREATE TABLE IF NOT EXISTS \"AnonymousEnabledChannels\"(\n" +
                    "\"channel_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\"server_id\" Text NOT NULL,\n" +
                    "CONSTRAINT \"unique_server_id\" UNIQUE ( \"server_id\" ) );");
            
            LOGGER.info("Database up");
            
        } catch (SQLException e) {
            LOGGER.error("SQL Exception on db initialization", e);
        }
    }
    
    //TODO: Implement
    public Mono<TextChannel> getChannelForUser(User user) {
        return user
                .getClient()
                .getGuildById(Snowflake.of("556690884121591838"))// Grab this from db - current guild
                .flatMap(guild -> guild.getChannelById(Snowflake.of("556690884121591841")))// Grab this from db - current channel
                // Ensure that the stored channel is a text channel - it should be.
                .filter(guildChannel -> guildChannel.getType().equals(Type.GUILD_TEXT))
                // Cast to a TextChannel to send messages - we ensured this is okay above
                .map(guildChannel -> (TextChannel) guildChannel);
    }
    
    //TODO: Implement
    public String getIDForUser(User user) {
        throw new RuntimeException("not implemented");
        //return CommandHandler.generateName();
    }
    
    //TODO: Implement
    public Mono<Void> initGuildConfig(Guild guild){
        throw new RuntimeException("not implemented");
    }
}
