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
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Настройка логирования ===");
        System.out.println("1. Без логирования");
        System.out.println("2. Только WARNING (критические ошибки)");
        System.out.println("3. WARNING + INFO (все события)");
        System.out.print("Выберите уровень (1-3): ");

        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                disableLogging();
                System.out.println("Логирование отключено");
                break;
            case 2:
                setupWarningLogging();
                System.out.println("Логирование WARNING включено");
                break;
            case 3:
                setupInfoLogging();
                System.out.println("Логирование INFO + WARNING включено");
                break;
            default:
                System.out.println("Неверный выбор, логирование отключено");
                disableLogging();
        }

        Database.loadData();
        FileServer server = new FileServer();
        server.start();
        scanner.close();
    }

    private static void disableLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.OFF);
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
    }

    private static void setupWarningLogging() {
        try {
            FileHandler fileHandler = new FileHandler("data/logs/server_%g.log", 1024 * 1024, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.WARNING); // Только WARNING и SEVERE

            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.WARNING);

            removeConsoleHandlers(rootLogger);
            rootLogger.addHandler(fileHandler);

        } catch (IOException e) {
            System.out.println("Ошибка настройки логирования: " + e.getMessage());
        }
    }

    private static void setupInfoLogging() {
        try {
            FileHandler fileHandler = new FileHandler("data/logs/server_%g.log", 1024 * 1024, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);

            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.INFO);

            removeConsoleHandlers(rootLogger);
            rootLogger.addHandler(fileHandler);

        } catch (IOException e) {
            System.out.println("Ошибка настройки логирования: " + e.getMessage());
        }
    }

    private static void removeConsoleHandlers(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                logger.removeHandler(handler);
            }
        }
    }
}