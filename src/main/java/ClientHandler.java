import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    boolean running = true;
    private String currentUser;
    private boolean authenticated = false;

    ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);


            while (running){
                handleCommand(in.readLine());
            }
        } catch (IOException e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println(currentUser + " отключился");
                ServerLogger.writeUserLog(": "+ currentUser + " отключился");
            }
        }
    }



    private void handleCommand(String command) {

        try {
            String[] parts = command.split(":");
            String action = parts[0].toUpperCase();

            switch (action) {

                case "LOGIN" -> handleLogin(parts);
                case "REGISTER" -> handleRegister(parts);

                case "LIST_USERS" -> handleListUsers();
                case "LIST_FILES" -> handleListFiles();
                case "SEND_TO" -> handleSendTo(parts);
                case "LOGOUT" -> handleLogout();
                default -> out.println("ERROR: Неизвестная команда: " + action);
            }
        } catch (NullPointerException e) {
            running = false;
        }

    }
    private void handleLogin(String[] parts) {
        if (!Database.isAuthenticate(parts[1]) || parts.length != 3) {
            out.println("AUTH_FAILED");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (Database.login(username, password)) {
            currentUser = username;
            authenticated = true;
            out.println("AUTH_SUCCESS");
            System.out.println("Пользователь " + username + " авторизовался");
            ServerLogger.writeUserLog(" Пользователь " + username + " авторизовался\n");
        } else {
            out.println("AUTH_FAILED");
        }
    }

    private void handleRegister(String[] parts) {
        if (Database.isAuthenticate(parts[1]) || parts.length != 3) {
            out.println("REGISTER_FAILED");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (Database.register(username, password)) {
            currentUser = username;
            authenticated = true;
            out.println("REGISTER_SUCCESS");
            System.out.println("Зарегистрирован новый пользователь: " + username);
            ServerLogger.writeUserLog(" Зарегистрирован новый пользователь: " + username + "\n");
        } else {
            out.println("REGISTER_FAILED");
        }
    }

    private void handleLogout(){
        authenticated = false;
        System.out.println("Пользователь " + currentUser + " вышел из аккаунта");
        ServerLogger.writeUserLog(" Пользователь " + currentUser + " вышел из аккаунта\n");
    }

    private void handleListUsers() {
        if (!checkAuth()) return;

        List<String> users = Database.getAllUsers();
        StringBuilder response = new StringBuilder("USERS_LIST:");
        for (int i = 0; i < users.size(); i++) {
            String marker = users.get(i).equals(currentUser) ? " (Вы)" : "";
            response.append(i + 1).append(".").append(users.get(i)).append(marker).append("|");
        }
        out.println(response);
    }



    private void handleListFiles() {
        if (!checkAuth()) return;
        String path = "./data/received_files/" + currentUser;
        File dir = new File("./data/received_files/" + currentUser);
        StringBuilder response = new StringBuilder("LIST_FILES_RECEIVED:");
        try {
            for ( File file : dir.listFiles() ){
                if (file.isFile()) {
                    try {
                        String[] parts = file.getName().split("_");

                        response.append(parts[1]).append("-")
                                .append(parts[5]).append("-")
                                .append(Files.readAllLines(Path.of(file.getPath())))
                                .append(":");
                        System.out.println(response);
                    } catch (IOException ignored) {}
                }
            }
        }catch (Exception e) {
            out.println("EMPTY");
            System.out.println("Файлов нет для " + currentUser);
            return;
        }

        out.println(response);
        ServerLogger.writeFileLog(currentUser + " получил файлы\n");
        try {
            String str = in.readLine();
            if (str.startsWith("DELETE")) deleteFiles();
        } catch (IOException e) {
            System.out.println("Что то пошло не так: " + e.getMessage());
        }

    }

    private void deleteFiles(){
        File dir = new File("./data/received_files/" + currentUser);
        for ( File file : dir.listFiles() ){
            if (file.delete()) System.out.println("да");;
        }
    }

    private void handleSendTo(String[] parts) {
        if (!checkAuth()) return;
        String recipient = parts[1];
        String filename = parts[2];
        String from = parts[4];

        byte[] bytes = parts[3].getBytes();
        String str = ("FROM_" + from + "_TO_" + recipient + "_FILE_" + filename);

        Path directory = Path.of("./data/received_files/").resolve(recipient);
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                System.out.println("Не удалось создать директорию: " + e.getMessage());
                return;
            }
        }

        Path path = directory.resolve(str);

        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, bytes);
            out.println("SEND_TO_RECEIVED");
            ServerLogger.writeFileLog(" Отправлен файл " + filename + " от " + from + ", пользователю " + recipient +"\n");
        } catch (IOException e) {
            System.out.println("Не удалось создать/записать файл: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean checkAuth() {
        if (!authenticated) {
            out.println("ERROR: Требуется авторизация");
            return false;
        }
        return true;
    }
}