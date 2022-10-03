package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private ServerSocket socket;
    private final int port;
    private final ThreadPool threadPool;

    public Server(int port, int poolSize) {
        this.port = port;
        this.threadPool = new ThreadPool(poolSize, poolSize);
    }

    @Override
    public void run() {
        try {
            this.socket = new ServerSocket(this.port);
        } catch (IOException e) {
            System.exit(-1);
        }

        socketLoop();
    }

    private void socketLoop() {
        while (true) {
            Socket clientSocket;
            try {
                clientSocket = this.socket.accept();
            } catch (IOException e) {
                throw new RuntimeException("Error on socket accept");
            }

            this.threadPool.add(new Http(clientSocket));
        }
    }
}
