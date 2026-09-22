package master;

import java.io.*;
import java.net.*;
import java.util.*;
import common.*;


public class MasterServer {

    private static final Map<String, WorkerInfo> activeWorkers = new HashMap<>();
    private int nextAvailablePort = 6000;
    private final String ip;
    private final int port;
    private ReducerInfo reducerInfo;
    private static final Map<String, WorkerInfo> storeToWorker = new HashMap<>();
    private static final Map<String, Store> registeredStores = new HashMap<>();
    private ServerSocket serverSocket;

    public MasterServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Χρήση: java MasterServer <ip> <port>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        MasterServer master = new MasterServer(ip, port);
        master.start();
    }

    public synchronized void registerStore(Store store) throws IOException {
        String key = store.getName().trim().toLowerCase();
        store.setName(key);
        registeredStores.put(key, store);

        WorkerInfo worker = getOrCreateWorker(key);
        // Ensure worker streams are initialized
        if (worker.getOut() == null || worker.getIn() == null) {
            throw new IOException("Worker streams not initialized");
        }

        try {
            System.out.println("[MASTER] Registering store: " + store + " (normalized: " + store + ")");
            worker.getOut().writeObject(store);
            worker.getOut().flush();
            // Wait for confirmation from worker
            Object response = worker.getIn().readObject();
            if (!(response instanceof Store)) {
                throw new IOException("Worker failed to confirm store registration");
            }
            System.out.println("[MASTER] Store successfully registered with worker: " + key);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[MASTER] Failed to sync store with worker: " + e.getMessage());
            throw new IOException("Store registration failed", e);
        }
    }

    public synchronized Store getStore(String storeName) {
        for (Store store : registeredStores.values()) {
            if (store.getName().equalsIgnoreCase(storeName.trim())) {
                System.out.println("[DEBUG] Registered stores: " + registeredStores.keySet());
                System.out.println("[Master] getStore: " + storeName + ") -> " + (store != null ? "FOUND" : "NOT FOUND"));
                return store;
            }
        }
        System.out.println("[Master] getStore: " + storeName + ") -> NOT FOUND");
        return null;
    }

    public synchronized void setReducerInfo(ReducerInfo reducerInfo) {
        this.reducerInfo = reducerInfo;
    }

    public synchronized ReducerInfo getReducerInfo() {
        return reducerInfo;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[Master] Listening on port " + port);

            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[Master] Shutdown hook triggered. Closing server...");
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept(); // Μπλοκάρει εδώ
                    new Thread(new ClientHandler(clientSocket, this)).start();
                } catch (SocketException e) {
                    System.out.println("[Master] Server socket closed. Exiting accept loop...");
                    break;
                }
            }
            System.out.println("---- Active Threads ----");
            Thread.getAllStackTraces().keySet().forEach(t ->
                    System.out.println(t.getName() + " (daemon=" + t.isDaemon() + ")"));

        } catch (IOException e) {
            System.err.println("[Master] Server error: " + e.getMessage());
        }
    }

    public synchronized WorkerInfo getOrCreateWorker(String storeName) throws IOException {
        String name = storeName.trim();
        System.out.println("[DEBUG] Assigning store '" + name + "' to worker: " + storeToWorker.get(name));
        boolean found = false;
        for(String key: storeToWorker.keySet()){
            if (key.equalsIgnoreCase(name)) {
                found = true;
                break;
            }
        }
        if (found) {
            WorkerInfo worker = storeToWorker.get(name);

            // Verify socket and streams are still active
            if (worker.getSocket() != null && !worker.getSocket().isClosed()
                    && worker.getOut() != null && worker.getIn() != null) {
                return worker;
            }        }

        if (activeWorkers.size() < 8) {
            int assignedPort = nextAvailablePort++;
            System.out.println("[Master] Starting new worker on port " + assignedPort);
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp",
                    "out/production/Duckie",
                    "worker.WorkerServer",
                    this.ip,
                    String.valueOf(port),  // master port
                    String.valueOf(assignedPort)    //worker port
            );
            pb.inheritIO();
            pb.start();

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            Socket workerSocket = new Socket(ip, assignedPort);
            ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream());

            WorkerInfo info = new WorkerInfo();
            info.setStoreName("__ANY__"); // Χρησιμοποιείται για worker pool
            info.setPort(assignedPort);
            info.setSocket(workerSocket);
            info.setHost(ip);
            info.setOut(out);
            info.setIn(in);

            // Χρησιμοποίησε consistent key για το activeWorkers
            String key = ip + ":" + assignedPort;
            activeWorkers.put(key, info);
        }

        // Επιλέγουμε worker (hash μπορεί να χρησιμοποιηθεί αρχικά)
        List<WorkerInfo> workerList = new ArrayList<>(activeWorkers.values());
        int index = hash(storeName, workerList.size());
        WorkerInfo chosenWorker = workerList.get(index);

        // Τον αντιστοιχούμε στο κατάστημα
        chosenWorker.setStoreName(storeName);
        storeToWorker.put(storeName, chosenWorker);
        return chosenWorker;
    }

    public synchronized List<WorkerConnection> getWorkers() {
        List<WorkerConnection> connections = new ArrayList<>();
        List<String> toRemove = new ArrayList<>(); // Track failed workers

        // If no workers are available, create at least one
        if (activeWorkers.isEmpty()) {
            try {
                int assignedPort = nextAvailablePort++;
                WorkerInfo newWorker = createNewWorker(assignedPort);
                String key = newWorker.getHost() + ":" + newWorker.getPort();
                activeWorkers.put(key, newWorker);
            } catch (IOException | InterruptedException e) {
                System.err.println("[Master] Failed to create initial worker: " + e.getMessage());
                return connections; // Return empty list instead of null
            }
        }

        for (Map.Entry<String, WorkerInfo> entry : activeWorkers.entrySet()) {
            WorkerInfo info = entry.getValue();
            try {
                // Check if worker is still alive
                if (info.getSocket() != null && !info.getSocket().isClosed()) {
                    try {
                        // Test connection with quick ping
                        info.getOut().writeObject("PING");
                        info.getOut().flush();
                        Object resp = info.getIn().readObject();
                        if ("PONG".equals(resp)) {
                            connections.add(new WorkerConnection(info));
                            continue; // Connection is good, move to next worker
                        }
                    } catch (Exception e) {
                        // Connection failed, will try to reconnect
                    }
                }

                // Need to establish new connection
                Socket workerSocket = new Socket();
                workerSocket.setSoTimeout(5000);
                workerSocket.connect(new InetSocketAddress(info.getHost(), info.getPort()), 3000);

                ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream());

                info.setSocket(workerSocket);
                info.setOut(out);
                info.setIn(in);

                // Verify connection
                out.writeObject("PING");
                out.flush();

                Object resp = in.readObject();
                if ("PONG".equals(resp)) {
                    connections.add(new WorkerConnection(info));
                    System.out.println("[Master] Worker " + info.getHost() + ":" + info.getPort() + " connected");
                } else {
                    throw new IOException("Invalid PONG response");
                }

            } catch (Exception e) {
                System.err.printf("[Master] Worker %s:%d failed - %s\n",
                        info.getHost(), info.getPort(), e.getMessage());
                toRemove.add(entry.getKey());
                try {
                    if (info.getSocket() != null) {
                        info.getSocket().close();
                    }
                } catch (IOException ignored) {}
            }
        }

        // Remove failed workers
        for (String key : toRemove) {
            WorkerInfo removed = activeWorkers.remove(key);
            if (removed != null) {
                // Clean up store assignments for removed worker
                storeToWorker.entrySet().removeIf(entry -> entry.getValue().equals(removed));
            }
        }

        return connections;
    }

    public static int hash(String storeName, int numberOfWorkers) {
        if (numberOfWorkers <= 0) {
            throw new IllegalArgumentException("Number of workers must be positive");
        }
        return Math.abs(storeName.hashCode()) % numberOfWorkers;
    }

    // Μέθοδος που καλείται από τον ClientHandler όταν ένας Worker κάνει register
    public synchronized void registerWorker(WorkerInfo workerInfo) {
        String key = workerInfo.getHost() + ":" + workerInfo.getPort();
        WorkerInfo existing = activeWorkers.get(key);
        if (existing != null) {
            // Αν υπάρχει ήδη, κράτα το storeName που έχει ήδη αντιστοιχιστεί
            workerInfo.setStoreName(existing.getStoreName());
        }
        activeWorkers.put(key, workerInfo);
        System.out.println("[Master] Registered worker: " + key);
    }

    private WorkerInfo createNewWorker(int assignedPort) throws IOException, InterruptedException {
        System.out.println("[Master] Starting new worker on port " + assignedPort);
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp",
                "out/production/Duckie",
                "worker.WorkerServer",
                this.ip,
                String.valueOf(port),
                String.valueOf(assignedPort)
        );
        pb.inheritIO();
        pb.start();

        // Wait for worker to start
        Thread.sleep(1000);

        Socket workerSocket = new Socket(ip, assignedPort);
        ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream());

        WorkerInfo info = new WorkerInfo();
        info.setStoreName("__ANY__");
        info.setPort(assignedPort);
        info.setSocket(workerSocket);
        info.setHost(ip);
        info.setOut(out);
        info.setIn(in);

        return info;
    }

    public List<Store> getAllStores() {
        return new ArrayList<>(registeredStores.values());
    }
}