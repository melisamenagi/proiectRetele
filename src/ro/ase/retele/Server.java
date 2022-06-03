package ro.ase.retele;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt("6000");

        ServerSocket serverSocket = new ServerSocket(port);
        try {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new Connection(socket);
            }
        } catch (IOException e) {
            System.out.println("Listen socket:" + e.getMessage());
        } finally {
            try {
                serverSocket.close();
                System.out.println("Server disconnected of " + port);
            } catch (IOException e) {
                System.out.println("Server error");
            }
        }

    }

    @Override
    public void run() {

    }
}

