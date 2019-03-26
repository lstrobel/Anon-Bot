import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.sql.*;

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
            
            final Connection con = DriverManager.getConnection(DATABASE_URL);
            
            final Statement stmt = con.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS \"ServerConfiguration\"(\n" +
                    "\"guild_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\"can_choose_id\" Boolean NOT NULL DEFAULT 0,\n" +
                    "\"id_cooldown_milliseconds\" Integer NOT NULL DEFAULT 0,\n" +
                    "\"previous_id_capacity\" Integer NOT NULL DEFAULT 10,\n" +
                    "\"automatic_blacklist\" Boolean NOT NULL DEFAULT 0,\n" +
                    "\"default_timeout_milliseconds\" Integer NOT NULL DEFAULT 60000 );");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS \"AnonymousEnabledChannels\"(\n" +
                    "\"channel_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\"guild_id\" Text NOT NULL,\n" +
                    "CONSTRAINT \"unique_server_id\" UNIQUE ( \"guild_id\" ) );");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS \"UserChannelSelections\"(\n" +
                    "\"user_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\"guild_id\" Text NOT NULL,\n" +
                    "\"channel_id\" Text NOT NULL );");
            
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO UserChannelSelections" +
                    "(user_id,guild_id,channel_id) VALUES(?,?,?)");
            pstmt.setString(1, "158687676587966465");
            pstmt.setString(2, "556690884121591838");
            pstmt.setString(3, "556690884121591841");
            pstmt.executeUpdate();
            
            LOGGER.info("Database up");
            
        } catch (SQLException e) {
            LOGGER.error("SQL Exception on db initialization", e);
        }
    }
    
    /**
     * Returns the current TextChannel that a user has set their anon to be speaking in.
     *
     * @param user The User to grab the information for
     * @return A Mono<TextChannel> containing the TextChannel the user has set.
     */
    public Mono<TextChannel> getChannelForUser(User user) {
        try {
            Connection con = DriverManager.getConnection(DATABASE_URL);
            PreparedStatement pstmt = con.prepareStatement("SELECT user_id, guild_id, " +
                    "channel_id FROM UserChannelSelections WHERE user_id = " + user.getId().asString());
            ResultSet rs = pstmt.executeQuery();
            
            rs.next();
            final String guildID = rs.getString("guild_id");
            final String channelID = rs.getString("channel_id");
            
            return user
                    .getClient()
                    .getGuildById(Snowflake.of(guildID))
                    .flatMap(guild -> guild.getChannelById(Snowflake.of(channelID)))
                    // Ensure that the stored channel is a text channel - it should be.
                    .filter(guildChannel -> guildChannel.getType().equals(Type.GUILD_TEXT))
                    // Cast to a TextChannel to send messages - we ensured this is okay above
                    .map(guildChannel -> (TextChannel) guildChannel);
        } catch (SQLException e) {
            LOGGER.error("SQL Exception on getChannelForUser", e);
        }
        throw new RuntimeException("try statment passed");
    }
    
    //TODO: Implement
    public String getIDForUser(User user) {
        //throw new RuntimeException("not implemented");
        return CommandHandler.generateName();
    }
    
    //TODO: Implement
    public Mono<Void> initGuildConfig(Guild guild) {
        throw new RuntimeException("not implemented");
    }
}
