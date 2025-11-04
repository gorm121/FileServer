import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static Path pathFiles = Paths.get("./data/logs/fileLogs.txt");
    private static Path pathUsers = Paths.get("./data/logs/userLogs.txt");

    public static void writeFileLog(String log){
        String str = "[" + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]" + log;
        try {

            Path parentDir = pathFiles.getParent();
            Files.createDirectories(parentDir);

            if (!Files.exists(pathFiles)) {
                Files.createFile(pathFiles);
            }

            Files.write(pathFiles, str.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            System.out.println("Произошла ошибка с файлом: " + e.getMessage());
        }
    }

    public static void writeUserLog(String log){
        String str = "[" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]" + log;        try {
            Path parentDir = pathUsers.getParent();
            Files.createDirectories(parentDir);

            if (!Files.exists(pathUsers)) {
                Files.createFile(pathUsers);
            }

            Files.write(pathUsers,str.getBytes(),StandardOpenOption.APPEND);
        }catch (IOException e) {
            System.out.println("Произошла ошибка с файлом: " + e.getMessage());
        }
    }
}
