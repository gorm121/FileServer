import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.*;

public class FileServer {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    public void start() {
        try {
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
        Database.loadData();
        FileServer server = new FileServer();
        server.start();


    }
}