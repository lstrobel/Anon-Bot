import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler {
    
    private static final String DATABASE_URL = "jdbc:sqlite:server_database.db";
    
    /**
     * Perform the initialization of the database
     */
    static void initializeDatabase() {
        try {
            Connection con = DriverManager.getConnection(DATABASE_URL);
            
            Statement stmt = con.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS \"ServerConfiguration\"(\n" +
                    "\t\"server_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\t\"can_choose_id\" Boolean NOT NULL DEFAULT 0,\n" +
                    "\t\"automatic_blacklist\" Boolean NOT NULL DEFAULT 0,\n" +
                    "\t\"previous_id_capacity\" Integer NOT NULL DEFAULT 10,\n" +
                    "\t\"default_timeout_milliseconds\" Integer NOT NULL DEFAULT 60000 );");
            
            stmt.execute("  CREATE TABLE IF NOT EXISTS \"AnonymousEnabledChannels\"(\n" +
                    "\t\"channel_id\" Text NOT NULL PRIMARY KEY,\n" +
                    "\t\"server_id\" Text NOT NULL,\n" +
                    "CONSTRAINT \"unique_server_id\" UNIQUE ( \"server_id\" ) );");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
