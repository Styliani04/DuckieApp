package reducer;

import common.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ReducerServer {
    private final String masterHost;
    private final int masterPort;
    private final int reducerPort;

    public ReducerServer(String masterHost, int masterPort, int reducerPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.reducerPort = reducerPort;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java ReducerServer <master_host> <master_port> <reducer_port>");
            return;
        }

        ReducerServer reducer = new ReducerServer(
                args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2])
        );
        reducer.start();
    }

    public void start() {
        try (Socket masterSocket = new Socket(masterHost, masterPort);
             ObjectOutputStream out = new ObjectOutputStream(masterSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(masterSocket.getInputStream())){


            ReducerInfo info = new ReducerInfo();
            info.setHost(masterSocket.getLocalAddress().getHostAddress()); // Explicit IP
            System.out.println("[Reducer] IP: " + info.getHost());
            info.setPort(this.reducerPort);

            out.writeObject(new RegisterReducerRequest(info.getHost(), info.getPort()));
            out.flush();

            // Wait for and read the response from Master
            Object response = in.readObject();
            System.out.println("[Reducer] Registration response: " + response);

            System.out.printf("[Reducer] Registered with Master at %s:%d (from %s:%d)%n",
                    masterHost, masterPort, info.getHost(), info.getPort());
            // Ξεκινάει server για να δέχεται reduce calls
            ServerSocket serverSocket = new ServerSocket(reducerPort);
            System.out.println("Reducer listening on port " + reducerPort);

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ReducerHandler(client)).start();
            }
        } catch (Exception e) {
            System.err.printf("[Reducer] Failed to register with Master at %s:%d: %s%n",
                    masterHost, masterPort, e.getMessage());
        }
    }
}