import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitDatabase {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:boris_theatre.db");
             Statement stmt = conn.createStatement()) {
            String sqlScript = new String(Files.readAllBytes(Paths.get("database.sql")));
            for (String statement : sqlScript.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            System.out.println("SUCCESS: boris_theatre.db created!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}