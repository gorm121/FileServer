import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoriesInit {
    public static void init(){
        Path path = Paths.get("data" + File.separator + "logs" + File.separator);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            System.out.println("Не удалось создать дирректорию:" + e.getMessage());
        }
        path = Paths.get("data" + File.separator + "received_files" + File.separator);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            System.out.println("Не удалось создать дирректорию: " + e.getMessage());
        }
    }
}
