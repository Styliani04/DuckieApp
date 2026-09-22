package worker;

import java.io.*;
import java.net.*;
import java.util.*;
import common.*;

public class WorkerServer {
    private final String masterHost;
    private final int masterPort;
    private int port;

    public WorkerServer(String masterHost, int masterPort, int port) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.port = port;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Χρήση: java WorkerServer <master_ip> <master_port> <port>");
            return;
        }

        String masterHost = args[0];
        int masterPort = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        WorkerServer worker = new WorkerServer(masterHost, masterPort, port);
        try {
            worker.start();
        } catch (IOException | InterruptedException e) {
            System.err.println("[Worker] Σφάλμα κατά την εκκίνηση: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ServerSocket createServerSocket() throws IOException {
        // Try the specified port first
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            return serverSocket;
        } catch (BindException e) {
            // If the specified port is in use, try to find an available port
            try (ServerSocket socket = new ServerSocket(0)) {
                this.port = socket.getLocalPort();
                System.out.printf("[Worker] Original port %d was in use, using port %d instead\n",
                        port, this.port);
            }
            // Create and return the new server socket with the available port
            ServerSocket serverSocket = new ServerSocket(this.port);
            serverSocket.setReuseAddress(true);
            return serverSocket;
        }
    }

    public void start() throws IOException, InterruptedException {
        ServerSocket serverSocket = createServerSocket();

        // Register with master (with retries)
        int attempts = 0;
        while (attempts < 3) {
            try (Socket masterSocket = new Socket(masterHost, masterPort)) {
                masterSocket.setKeepAlive(true);
                ObjectOutputStream out = new ObjectOutputStream(masterSocket.getOutputStream());

                WorkerInfo info = new WorkerInfo();
                info.setHost(masterSocket.getLocalAddress().getHostAddress());
                info.setPort(this.port); // Using potentially updated port

                out.writeObject(info);
                System.out.printf("[Worker] Successfully registered with Master at %s:%d\n",
                        masterHost, masterPort);
                break;
            } catch (IOException e) {
                attempts++;
                System.err.printf("[Worker] Registration attempt %d/3 failed: %s\n",
                        attempts, e.getMessage());
                if (attempts == 3) throw e;
                Thread.sleep(3000); // Wait before retry
            }
        }

        // Start handling requests
        while (true) {
            Socket clientSocket = serverSocket.accept();
            clientSocket.setKeepAlive(true);
            new Thread(new WorkerRequestHandler(clientSocket)).start();
        }
    }
}
