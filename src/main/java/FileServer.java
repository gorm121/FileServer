import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.logging.*;

public class FileServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;



    public void start() {
        try {
            int PORT = 12345;
            serverSocket = new ServerSocket(PORT);
            threadPool = Executors.newCachedThreadPool();

            System.out.println("Сервер запущен на порту " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("Новое подключение: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket));

            }

        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) serverSocket.close();
            if (threadPool != null) threadPool.shutdown();
        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DirectoriesInit.init();

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Настройка логирования ===");
        System.out.println("1. Без логирования");
        System.out.println("2. Только WARNING (критические ошибки)");
        System.out.println("3. WARNING + INFO (все события)");
        System.out.print("Выберите уровень (1-3): ");
        String logChoice = scanner.nextLine();
        ServerLogger.initialize(logChoice);
        scanner.close();

        Database.loadData();
        FileServer server = new FileServer();
        server.start();
    }
}