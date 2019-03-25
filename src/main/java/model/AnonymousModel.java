package model;

import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.nio.channels.Channel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class AnonymousModel {
    
    private static AnonymousModel ourInstance = new AnonymousModel();
    
    public static AnonymousModel getInstance() {
        return ourInstance;
    }
    
    private static final String DATABASE_URL = "jdbc:sqlite:server_database.db";
    
    private AnonymousModel() {
        try {
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
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Mono<Channel> getCurrentAnonymousChannelForUser(Mono<User> user) {
        throw new RuntimeException("not implemented");
    }
}
