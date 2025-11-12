import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    boolean running = true;
    private String currentUser;


    ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);


            while (running){
                handleCommand(readFromClient());
            }
        } catch (IOException e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println(currentUser + " отключился");
                ServerLogger.writeUserLog(currentUser + " отключился");
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
                default -> writeToClient("ERROR: Неизвестная команда: " + action);
            }
        } catch (NullPointerException e) {
            running = false;
        }

    }
    private void handleLogin(String[] parts) {
        if (!Database.isAuthenticate(parts[1]) || parts.length != 3) {
            writeToClient("AUTH_FAILED");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (Database.login(username, password)) {
            currentUser = username;

            writeToClient("AUTH_SUCCESS");

            System.out.println("Пользователь " + username + " авторизовался");
            ServerLogger.writeUserLog("Пользователь " + username + " авторизовался\n");
        } else {
            writeToClient("AUTH_FAILED");
        }
    }

    private void handleRegister(String[] parts) {
        if (Database.isAuthenticate(parts[1]) || parts.length != 3) {
            writeToClient("REGISTER_FAILED");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (Database.register(username, password)) {
            currentUser = username;
            writeToClient("REGISTER_SUCCESS");
            System.out.println("Зарегистрирован новый пользователь: " + username);
            ServerLogger.writeUserLog("Зарегистрирован новый пользователь: " + username + "\n");
        } else {
            writeToClient("REGISTER_FAILED");
        }
    }

    private void handleLogout(){
        System.out.println("Пользователь " + currentUser + " вышел из аккаунта");
        ServerLogger.writeUserLog("Пользователь " + currentUser + " вышел из аккаунта\n");
    }

    private void handleListUsers() {

        List<String> users = Database.getAllUsers();
        StringBuilder response = new StringBuilder("USERS_LIST:");
        for (int i = 0; i < users.size(); i++) {
            String marker = users.get(i).equals(currentUser) ? " (Вы)" : "";
            response.append(i + 1).append(".").append(users.get(i)).append(marker).append("|");
        }
        writeToClient(String.valueOf(response));
    }



    private void handleListFiles() {

        File dir = new File("data" + File.separator  + "received_files" + File.separator + currentUser);
        StringBuilder response = new StringBuilder("LIST_FILES_RECEIVED:");
        try {
            for ( File file : Objects.requireNonNull(dir.listFiles())){
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
            writeToClient("EMPTY");
            System.out.println("Файлов нет для " + currentUser);
            return;
        }

        writeToClient(String.valueOf(response));
        ServerLogger.writeFileLog(currentUser + " получил файлы\n");
        try {
            String str = readFromClient();
            if (str.startsWith("DELETE")) deleteFiles();
        } catch (IOException e) {
            System.out.println("Что то пошло не так: " + e.getMessage());
        }

    }

    private void deleteFiles(){
        File dir = new File("data" + File.separator  + "received_files" + File.separator + currentUser);
        for ( File file : Objects.requireNonNull(dir.listFiles())){
            if (file.delete()) ServerLogger.writeFileLog("Файлы для " + currentUser + " удалены");
        }
    }

    private void handleSendTo(String[] parts) {
        String recipient = parts[1];
        String filename = parts[2];
        String from = parts[4];
        if (!Database.getAllUsers().contains(recipient)){
            writeToClient("BAD_SEND_TO");
            return;
        }
        byte[] bytes = parts[3].getBytes();
        String str = ("FROM_" + from + "_TO_" + recipient + "_FILE_" + filename);

        Path directory = Path.of("data" + File.separator  + "received_files" + File.separator).resolve(recipient);
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
            writeToClient("SEND_TO_RECEIVED");
            ServerLogger.writeFileLog(" Отправлен файл " + filename + " от " + from + ", пользователю " + recipient +"\n");
        } catch (IOException e) {
            System.out.println("Не удалось создать/записать файл: " + e.getMessage());
        }
    }



    private void writeToClient(String message) {
        try {
            out.println(message);
        } catch (Exception e) {
            if (isConnectionError(e)) {
                System.out.println("Потеряно соединение с сервером");
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    System.out.println("Произошла ошибка при закрытии сокета: " + e.getMessage());
                }

            }
        }
    }


    private String readFromClient() throws IOException {
        try {
            return in.readLine();
        } catch (IOException e) {
            if (isConnectionError(e)) {
                System.out.println("Потеряно соединение с сервером");
                clientSocket.close();
            }
            throw e;
        }
    }

    private boolean isConnectionError(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("Connection reset") ||
                        message.contains("Connection refused") ||
                        message.contains("closed") ||
                        message.contains("broken pipe") ||
                        message.contains("reset by peer") ||
                        message.contains("Software caused connection abort")
        );
    }

}