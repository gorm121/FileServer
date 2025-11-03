import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
                System.out.println("Произошла ошибка: " + e.getMessage());
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
                case "LOGOUT" -> {authenticated = false;}
                default -> out.println("ERROR: Неизвестная команда: " + action);
            }
        } catch (NullPointerException e) {
            running = false;
        }

    }
    private void handleLogin(String[] parts) {
        if (!Database.isAuthenticate(parts[1])) {
            out.println("ERROR: Произошла ошибка");
            return;
        }

        if (parts.length != 3) {
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
        } else {
            out.println("AUTH_FAILED");
        }
    }

    private void handleRegister(String[] parts) {
        if (Database.isAuthenticate(parts[1])) {
            out.println("ERROR: Произошла ошибка");
            return;
        }

        if (parts.length != 3) {
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
        } else {
            out.println("REGISTER_FAILED");
        }
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
        StringBuilder response = new StringBuilder("DATA:");
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
        out.println("LIST_FILES_RECEIVED:");
        out.println(response);
        try {
            String str = in.readLine();
            if (str.startsWith("DELETE")) deleteFiles(str);
        } catch (IOException e) {
            System.out.println("Что то пошло не так: " + e.getMessage());
        }

    }

    private void deleteFiles(String str){
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