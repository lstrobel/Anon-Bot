import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the connection to the database. Currently set up as a singleton, to share the
 * connection to the db, while still remaining somewhat "static"
 */
public class AnonymousModel {
    
    private static AnonymousModel ourInstance = new AnonymousModel();
    
    // Singleton instance
    public static AnonymousModel getInstance() {
        return ourInstance;
    }
    
    // For whatever reason, setting this to static breaks this class
    //TODO: Fix that
    private final Logger LOGGER = LoggerFactory.getLogger(AnonymousModel.class);
    
    private Connection connection; // Our database connection
    
    private AnonymousModel() {
        try {
            LOGGER.info("Starting database...");
            
            connection = DriverManager.getConnection("jdbc:sqlite:server_database.db");
            
            final Statement stmt = connection.createStatement();
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
            
            //TODO: Remove this test insert
            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO UserChannelSelections" +
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
    public Mono<TextChannel> getChannelForUser(@NonNull User user) {
        return Mono.just(user.getId())
                // Grab guild_id and channel_id
                .flatMap(this::queryChannelForUser)
                .flatMap(map ->
                        user.getClient()
                                .getGuildById(Snowflake.of(map.get("guild_id")))
                                .flatMap(guild ->
                                        guild.getChannelById(Snowflake.of(map.get("channel_id")))
                                )
                                // Ensure that the stored channel is a text channel - it should be.
                                .filter(guildChannel ->
                                        guildChannel.getType().equals(Type.GUILD_TEXT)
                                )
                                // Cast to a TextChannel to send messages - we ensured this is okay above
                                .map(guildChannel -> (TextChannel) guildChannel)
                );
    }
    
    /**
     * Takes the ID of a user and returns a map that maps "guild_id" and "channel_id" to the
     * appropriate ids that the given user has stored
     *
     * @param userID The Snowflake representing the ID of the user to query the data for
     * @return A map that maps "guild_id" and "channel_id" to the appropriate ids that the given
     * user has stored
     */
    private Mono<Map<String, String>> queryChannelForUser(@NonNull Snowflake userID) {
        try {
            PreparedStatement pstmt = connection.prepareStatement("SELECT user_id, guild_id, " +
                    "channel_id FROM UserChannelSelections WHERE user_id = " + userID.asString());
            ResultSet rs = pstmt.executeQuery();
            
            rs.next();
            Map<String, String> map = new HashMap<>();
            map.put("guild_id", rs.getString("guild_id"));
            map.put("channel_id", rs.getString("channel_id"));
            return Mono.just(Collections.unmodifiableMap(map));
            
        } catch (SQLException e) {
            LOGGER.error("Failure in SQL query for ChannelForUser", e);
        }
        return null;
    }
    
    //TODO: Implement
    public String getIDForUser(User user) {
        return "test/temp_user";
    }
    
    //TODO: Implement
    public Mono<Void> initGuildConfig(Guild guild) {
        throw new RuntimeException("not implemented");
    }
}
