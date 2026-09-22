package master;

import java.net.*;
import java.io.*;
import java.util.*;

import common.*;


public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MasterServer master;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket clientSocket, MasterServer master) {
        this.socket = clientSocket;
        this.master = master;
    }

    @Override
    public void run() {

        try {
            // Initialize output stream and flush immediately
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            // Initialize input stream
            in = new ObjectInputStream(socket.getInputStream());
            socket.setKeepAlive(true);



            while (!socket.isClosed()) {
                Object obj = in.readObject();
                if (obj == null) {
                    System.out.println("[Master] Null input received, closing connection.");
                    break;
                }

                try {
                    processRequest(out, obj); // Process the request and send a response
                    out.flush();
                } catch (IOException e) {
                    System.err.println("[Master] Error processing request: " + e.getMessage());
                    e.printStackTrace();
                    break;  // Exit loop on IO error
                } catch (Exception e) {
                    System.err.println("[Master] Error processing request: " + e.getMessage());
                    e.printStackTrace();
                    sendErrorResponse(out, "Request processing error: " + e.getMessage());
                }
            }
        } catch (EOFException e) {
            System.out.println("[Master] Client disconnected normally.");
        } catch (Exception e) {
            System.err.println("[Master] Error in client handler:");
            e.printStackTrace();
        } finally {
            closeResources(in, out, socket);
        }
    }

    private void processRequest(ObjectOutputStream out, Object obj) throws IOException {
        if (obj instanceof Store store) {
            System.out.println("[Master] Processing store registration for: " + store.getName());
            master.registerStore(store);
            sendResponse(out, "Store " + store.getName() + " registered successfully");
        } else if (obj instanceof RegistrationRequest) {
            RegistrationRequest reg = (RegistrationRequest) obj;
            System.out.println("[Master] Worker registration from " + reg.getWorkerHost() + ":" + reg.getWorkerPort());

            // Αποθήκευση του worker
            WorkerInfo workerInfo = new WorkerInfo();
            workerInfo.setHost(reg.getWorkerHost());
            workerInfo.setPort(reg.getWorkerPort());
            workerInfo.setSocket(socket);
            workerInfo.setIn(in);
            workerInfo.setOut(out);

            master.registerWorker(workerInfo);
            out.writeObject("Registration successful");
            out.flush();
        } else if (obj instanceof WorkerInfo info) {
            info.setSocket(socket);
            info.setIn(in);
            info.setOut(out);
            System.out.println("[Master] Registered worker on port " + info.getPort());

            master.registerWorker(info); // προσθήκη στον χάρτη activeWorkerselse
        } else if (obj instanceof SearchRequest searchRequest) {
            handleSearchRequest(out, searchRequest);
        } else if (obj instanceof UpdateStockRequest updateRequest) {
            handleUpdateStockRequest(out, updateRequest);
        } else if (obj instanceof SalesQueryRequest salesQueryRequest) {
            handleSalesQueryRequest(out, salesQueryRequest);
        } else if (obj instanceof RegisterReducerRequest) {
            RegisterReducerRequest reg = (RegisterReducerRequest) obj;
            System.out.println("[Master] Worker registration from " + reg.getReducerHost() + ":" + reg.getReducerPort());

            ReducerInfo reducerInfo = new ReducerInfo();
            reducerInfo.setHost(reg.getReducerHost());
            reducerInfo.setPort(reg.getReducerPort());
            reducerInfo.setSocket(socket);
            reducerInfo.setIn(in);
            reducerInfo.setOut(out);

            master.setReducerInfo(reducerInfo);
            String response = "Reducer registered successfully at " + reducerInfo.getHost() + ":" + reducerInfo.getPort();
            System.out.println(response);
            sendResponse(out, response);
        }else if (obj instanceof ReducerInfo info) {
            info.setSocket(socket);
            info.setIn(in);
            info.setOut(out);
            System.out.println("[Master] Registered reducer on port " + info.getPort());

            master.setReducerInfo(info);
        }else if (obj instanceof BuyRequest buyRequest) {
            handleBuyRequest(out, buyRequest);
        } else if (obj instanceof AddProductsRequest addRequest) {
            handleAddProductsRequest(out, addRequest);
        } else if (obj instanceof HideProductRequest hideRequest) {
            handleHideProductRequest(out, hideRequest);
        } else if (obj instanceof RateStoreRequest rateRequest) {
            handleRateStoreRequest(out, rateRequest);
        } else if (obj instanceof GetStoreRequest getStoreRequest) {
            handleGetStoreRequest(out, getStoreRequest);
        }else if (obj instanceof GetAllStoresRequest) {
            List<Store> allStores = master.getAllStores();
            out.writeObject(allStores);
        } else {
            String errorResponse = "Unknown request type: " + obj.getClass().getName();
            System.err.println("[Master] " + errorResponse);
            sendResponse(out, errorResponse);
        }
    }

    private synchronized void sendResponse(ObjectOutputStream out, Object response) throws IOException {
        try {
            if (socket != null && !socket.isClosed()) {
                out.writeObject(response);
                out.flush();
            } else {
                throw new IOException("Socket is closed");
            }

        } catch (IOException e) {
            System.err.println("[Master] Error sending response: " + e.getMessage());
            throw e;
        }
    }


    private synchronized void sendErrorResponse(ObjectOutputStream out, String errorMessage) {
        try {
            out.writeObject("Error: " + errorMessage);  // Send a proper error message instead of leaving the stream empty
            out.flush();  // Flush to ensure the error message is sent
        } catch (IOException e) {
            System.err.println("[Master] Failed to send error response: " + e.getMessage());
        }
    }


    private void closeResources(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
        try {
            if (in != null) in.close();
            if (out != null) {
                out.flush();
                out.close();
            }
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Master] Error closing resources: " + e.getMessage());
        }
    }


    private void handleSearchRequest(ObjectOutputStream out, SearchRequest req) throws IOException {
        // 1. Verify reducer exists
        if (master.getReducerInfo() == null) {
            sendErrorResponse(out, "Reducer service unavailable");
            return;
        }

        // 2. Get active workers
        List<WorkerConnection> workers = master.getWorkers();
        if (workers.isEmpty()) {
            sendErrorResponse(out, "No healthy workers available");
            return;
        }

        // 3. Process workers sequentially with timeouts
        Map<String, List<Store>> workerResults = new HashMap<>();
        List<Thread> workerThreads = new ArrayList<>();
        Object lock = new Object(); // For synchronization

        for (WorkerConnection worker : workers) {
            Thread t = new Thread(() -> {
                try {
                    worker.getOut().writeObject(req);
                    worker.getOut().flush();

                    // Set read timeout
                    worker.getWorkerInfo().getSocket().setSoTimeout(5000);
                    Object response = worker.getIn().readObject();

                    synchronized(lock) {
                        if (response instanceof Map) {
                            workerResults.putAll((Map<String, List<Store>>) response);
                        }
                    }
                } catch (Exception e) {
                    System.err.printf("[Worker %s] Failed: %s\n",
                            worker.getWorkerInfo(), e.getMessage());
                }
            });

            workerThreads.add(t);
            t.start();
        }

        // 4. Wait for threads with timeout
        long endTime = System.currentTimeMillis() + 10000; // 10s total timeout
        for (Thread t : workerThreads) {
            try {
                long remaining = endTime - System.currentTimeMillis();
                if (remaining > 0) {
                    t.join(remaining);
                }
            } catch (InterruptedException e) {
                System.err.println("[Master] Thread interrupted");
            }
        }

        // 5. Process results
        if (workerResults.isEmpty()) {
            sendErrorResponse(out, "Search failed - no worker responses");
        } else {
            try {
                Object reduced = callReducerSearch(workerResults);
                out.writeObject(reduced);
            } catch (Exception e) {
                sendErrorResponse(out, "Reducer error: " + e.getMessage());
            }
        }
    }

    private Object callReducerSearch(Map<String, List<Store>> data) {
        ReducerInfo reducer = master.getReducerInfo();
        if (reducer == null) throw new IllegalStateException("No reducer");

        try (Socket reducerSocket = new Socket(reducer.getHost(), reducer.getPort())) {
            ObjectOutputStream out = new ObjectOutputStream(reducerSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(reducerSocket.getInputStream());

            out.writeObject(data);
            out.flush();
            System.out.println("[Master] Sent data to reducer");

            return in.readObject();
        } catch (SocketTimeoutException e) {
            System.err.println("[Master] Reducer timeout - returning partial results");
            return new ArrayList<>(data.values().iterator().next()); // Fallback
        } catch (Exception e) {
            throw new RuntimeException("Reducer error", e);
        }
    }

    private Map<String, Integer> callReducer(Map<String, List<Integer>> data) {
        ReducerInfo reducerInfo = master.getReducerInfo();
        if (reducerInfo == null) {
            System.err.println("[Master] No reducer registered!");
            return Collections.emptyMap();
        }

        try (
                Socket reducerSocket = new Socket(reducerInfo.getHost(), reducerInfo.getPort());
                ObjectOutputStream out = new ObjectOutputStream(reducerSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(reducerSocket.getInputStream())
        ) {
            out.writeObject(data);
            Object obj = in.readObject();
            if (obj instanceof SalesResult result) {
                return result.getStoreSales();
            } else {
                throw new IllegalArgumentException("Invalid object received from reducer");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    private void handleBuyRequest(ObjectOutputStream out, BuyRequest req) throws IOException {
        Store store = master.getStore(req.getStoreName().trim());
        if (store == null) {
            sendResponse(out, "ERROR: Store not found");
            return;
        }
        WorkerInfo worker = master.getOrCreateWorker(req.getStoreName());  // Get worker for specific store
        Object result = map(worker, req);  // Process the buy request
        sendResponse(out, result);  // Send result back to client
    }

    private void handleUpdateStockRequest(ObjectOutputStream out, UpdateStockRequest req) throws IOException {
        Store store = master.getStore(req.getStoreName().trim());
        if (store == null) {
            sendResponse(out, "ERROR: Store not found");
            return;
        }
        WorkerInfo worker = master.getOrCreateWorker(req.getStoreName());
        Object result = map(worker, req);
        Object updatedStore = map(worker, new GetStoreRequest(req.getStoreName(), true, req.getClientId()));  // Get updated store details
        sendResponse(out, List.of(result, updatedStore));  // Send both results back
    }

    private void handleAddProductsRequest(ObjectOutputStream out, AddProductsRequest req) throws IOException {
        Store store = master.getStore(req.getStoreName().trim());
        if (store == null) {
            sendResponse(out, "ERROR: Store not found");
            return;
        }
        WorkerInfo worker = master.getOrCreateWorker(req.getStoreName());
        Object result = map(worker, req);
        Object updatedStore = map(worker, new GetStoreRequest(req.getStoreName(), true, req.getClientId()));
        sendResponse(out, List.of(result, updatedStore));
    }

    private void handleHideProductRequest(ObjectOutputStream out, HideProductRequest req) throws IOException {
        Store store = master.getStore(req.getStoreName().trim());
        if (store == null) {
            sendResponse(out, "ERROR: Store not found");
            return;
        }
        WorkerInfo worker = master.getOrCreateWorker(req.getStoreName());
        Object result = map(worker, req);
        Object updatedStore = map(worker, new GetStoreRequest(req.getStoreName(), true, req.getClientId()));
        sendResponse(out, List.of(result, updatedStore));
    }

    private void handleRateStoreRequest(ObjectOutputStream out, RateStoreRequest req) throws IOException {
        Store store = master.getStore(req.getStoreName().trim());
        if (store == null) {
            sendResponse(out, "ERROR: Store not found");
            return;
        }
        WorkerInfo worker = master.getOrCreateWorker(req.getStoreName());
        Object result = map(worker, req);
        Object updatedStore = map(worker, new GetStoreRequest(req.getStoreName(), true, req.getClientId()));
        sendResponse(out, List.of(result, updatedStore));
    }

    private void handleGetStoreRequest(ObjectOutputStream out, GetStoreRequest req) throws IOException {
        String normalizedStoreName = req.getStoreName().trim();
        System.out.println("[MASTER] Searching for: " + normalizedStoreName);

        Store store = master.getStore(normalizedStoreName);
        if (store != null) {
            WorkerInfo worker = master.getOrCreateWorker(normalizedStoreName);
            if (worker == null) {
                sendErrorResponse(out, "No worker available for store");
            }
            Object response = map(worker, req); // Forward to worker
            if (response == null) {
                sendErrorResponse(out, "Failed to get store from worker");
            } else {
                sendResponse(out, response);
            }        }else{
            sendErrorResponse(out, "Store not found");
        }
    }

    private void handleSalesQueryRequest(ObjectOutputStream out, SalesQueryRequest query) throws IOException {

        Map<String, List<Integer>> partialSales = new HashMap<>();

        for (WorkerConnection worker : master.getWorkers()) {
            Object resp = map(worker.getWorkerInfo(), query);  // Get sales result from each worker
            if (resp instanceof SalesResult sr) {
                // Προσθέτουμε τα δεδομένα κάθε worker στη δομή partialSales
                for (Map.Entry<String, Integer> entry : sr.getStoreSales().entrySet()) {
                    partialSales.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                }
            }
        }

        // 2. Στέλνουμε τα μερικά αποτελέσματα στον reducer
        Map<String, Integer> reducedSales = callReducer(partialSales);

        // 3. Δημιουργούμε τελικό αντικείμενο SalesResult για να το επιστρέψουμε
        int total = reducedSales.values().stream().mapToInt(Integer::intValue).sum();
        SalesResult finalResult = new SalesResult(reducedSales, total);

        // 4. Στέλνουμε την απάντηση πίσω στον client
        sendResponse(out, finalResult);
    }


    private Object map(WorkerInfo worker, Object data) {
        if (worker == null) return null;

        synchronized (worker) {
            try {
                worker.getOut().writeObject(data);
                worker.getOut().flush();
                return worker.getIn().readObject();  // Send request and get response
            } catch (Exception e) {
                System.err.println("[Master] Map error: " + e.getMessage());
                return null;
            }
        }
    }

}