import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
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

        String[] parts = command.split(":");
        String action = parts[0].toUpperCase();

        switch (action) {

            case "LOGIN" -> handleLogin(parts);
            case "REGISTER" -> handleRegister(parts);

            case "LIST_USERS" -> handleListUsers();
            case "UPLOAD" -> handleUpload(parts);
            case "DOWNLOAD" -> handleDownload(parts);
            case "LIST_FILES" -> handleListFiles();
            case "DELETE" -> handleDelete(parts);
            case "SEND_TO" -> handleSendTo(parts);

            default -> out.println("ERROR: Неизвестная команда: " + action);
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

    private void handleUpload(String[] parts) {
        if (!checkAuth()) return;

    }

    private void handleDownload(String[] parts) {
        if (!checkAuth()) return;
        // TODO: логика скачивания файла
        out.println("DOWNLOAD_RECEIVED");
    }

    private void handleListFiles() {
        if (!checkAuth()) return;
        // TODO: список файлов пользователя
        out.println("LIST_FILES_RECEIVED");
    }

    private void handleDelete(String[] parts) {
        if (!checkAuth()) return;
        // TODO: удаление файла
        out.println("DELETE_RECEIVED");
    }

    private void handleSendTo(String[] parts) {
        if (!checkAuth()) return;
        String recipient = parts[1];
        String filename = parts[2];
        String from = parts[4];
        byte[] bytes = parts[3].getBytes();
        String str = ("FROM_" + from + "_TO_" + recipient + "_FILE_" +filename);
        Path path = Path.of("/data/received_files/" + str);
        System.out.println(recipient);
        System.out.println(parts[3]);

        try {
            Files.createFile(path);
            Files.write(path,bytes);
            out.println("SEND_TO_RECEIVED");
        } catch (IOException e) {
            System.out.println("Не удалось создать файл");
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
