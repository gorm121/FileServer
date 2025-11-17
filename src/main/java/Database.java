import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static final String USERS_FILE = "data"+ File.separator +"users.txt";
    private static final Map<String, String> users = new HashMap<>();

    public static void loadData(){
        createPath();
        loadUsersData();
    }

    private static void createPath(){
        try {Files.createDirectories(Path.of("data"+ File.separator +"logs"));}
        catch (IOException e) {System.out.println("Произошла ошибка при создании (возможно она уже создана): " + e.getMessage());}
        try {Files.createDirectories(Path.of("data"+ File.separator +"received_files"));}
        catch (IOException e) {System.out.println("Произошла ошибка при создании (возможно она уже создана): " + e.getMessage());}
        try {Files.createFile(Path.of(USERS_FILE));}
        catch (IOException e) {System.out.println("Произошла ошибка при создании (возможно она уже создана): " + e.getMessage());}
    }


    private static void loadUsersData(){
        try {
            users.clear();
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


    public static List<String> getAllUsers() {
        loadUsersData();
        return new ArrayList<>(users.keySet());
    }
}
