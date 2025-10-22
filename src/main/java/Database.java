import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static final String USERS_FILE = "data/users.txt";
    private static final String USERS_DATA_FILE = "data/files.txt";
    private static Map<String, String> users = new HashMap<>();
    private static List<FileRecord> fileRecords = new ArrayList<>();

    public static void loadData(){
        loadUsersData();
        loadUsersFiles();
    }

    private static void loadUsersData(){
        try {
            List<String> lines = Files.readAllLines(Path.of(USERS_FILE));
            for (String line : lines){
                String[] parts = line.split(":",2);
                if (parts.length == 2) {
                    users.put(parts[0],parts[1]);
                }
            }
        } catch (IOException ignored) {
            System.out.println("Файл отсутствует");
        }
    }

    private static void loadUsersFiles(){
        try {
            List<String> lines = Files.readAllLines(Path.of(USERS_DATA_FILE));
            for (String line : lines){
                String[] parts = line.split(":",3);
                if (parts.length == 3) fileRecords.add(new FileRecord(parts[0],parts[1]));
            }
        } catch (IOException e) {
            System.out.println("Отсутствуют файлы пользователей");
        }
    }

    public static boolean isAuthenticate(String username){
        if (users.isEmpty()) return false;
        try {
            return users.containsKey(username);
        }catch (Exception e){
            return false;
        }
    }

    public static boolean login(String username, String password){
        if (users.isEmpty()) return false;
        try {
            return users.containsKey(username) && users.get(username).equals(password);
        }catch (Exception e){
            return false;
        }
    }

    public static Boolean register(String username, String password){
        if (isAuthenticate(username)) {
            return false;
        }

        users.put(username,password);

        StringBuilder builder = new StringBuilder(username + ":" + password + "\n" );
        try {
            Files.write(Path.of(USERS_FILE), builder.toString().getBytes(),StandardOpenOption.APPEND);
            return true;
        } catch (IOException ignored){ return false;}
    }

    public static void deleteFile(String username){

    }



    public static List<String> getAllUsers() {
        return new ArrayList<>(users.keySet()); // Все логины
    }
}
