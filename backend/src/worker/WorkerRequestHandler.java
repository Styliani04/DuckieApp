package worker;

import common.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerRequestHandler implements Runnable {
    private Socket socket;
    private static Map<String, Store> stores = new HashMap<>();


    public WorkerRequestHandler(Socket socket) {
        this.socket = socket;
    }

    public static void setStoreMap(Map<String, Store> map) {
        stores = map;
    }

    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            out = new ObjectOutputStream(this.socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(this.socket.getInputStream());

            while(true) {
                Object obj = in.readObject();
                System.out.println("[Worker] Received request: " + obj.getClass().getSimpleName());

                if (obj == null) {
                    break;
                }

                if (obj instanceof String && ((String)obj).equals("PING")) {
                    out.writeObject("PONG");
                    out.flush();
                    continue;
                }

                if (obj instanceof Store) {
                    Store store = (Store)obj;
                    String name = store.getName().trim();
                    synchronized(WorkerRequestHandler.class) {
                        // Initialize initialStock for all products when first received
                        for (Product p : store.getProducts()) {
                            if (p.getInitialStock() == 0) {
                                p.setInitialStock(p.getStock());
                            }
                        }
                        this.stores.put(name, store);
                        System.out.println("[WORKER] Stored: " + name + " | Current stores: " + stores.keySet());
                        out.writeObject(store);
                        out.flush();
                    }

                    System.out.println("[Worker] Current stores in memory:");
                    for (String key : stores.keySet()) {
                        System.out.println(" - " + key);
                    }
                } else if (obj instanceof SearchRequest) {
                    SearchRequest req = (SearchRequest)obj;
                    processSearchRequest(req, out);
                } else if (obj instanceof BuyRequest) {
                    BuyRequest buy = (BuyRequest)obj;
                    this.processBuyRequest(buy, out);
                } else if (obj instanceof UpdateStockRequest) {
                    UpdateStockRequest req = (UpdateStockRequest)obj;
                    this.processUpdateStockRequest(req, out);
                } else if (obj instanceof AddProductsRequest) {
                    AddProductsRequest req = (AddProductsRequest)obj;
                    this.processAddProductsRequest(req, out);
                } else if (obj instanceof HideProductRequest) {
                    HideProductRequest req = (HideProductRequest)obj;
                    this.processHideProductRequest(req, out);
                } else if (obj instanceof RateStoreRequest) {
                    RateStoreRequest req = (RateStoreRequest)obj;
                    this.processRateStoreRequest(req, out);
                } else if (obj instanceof GetStoreRequest) {
                    GetStoreRequest req = (GetStoreRequest)obj;
                    this.processGetStoreRequest(req, out);
                } else if (obj instanceof SalesQueryRequest) {
                    SalesQueryRequest query = (SalesQueryRequest)obj;
                    this.processSalesQueryRequest(query, out);
                } else {
                    out.writeObject("ERROR: Unknown request type");
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("[Worker] Error: " + e.getMessage());
        } finally {
            this.closeResources(in, out, this.socket);
        }

    }

    private void closeResources(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
        try {
            if (in != null) {
                in.close();
            }

            if (out != null) {
                out.close();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (IOException var5) {
        }

    }

    private void processSearchRequest(SearchRequest req, ObjectOutputStream out) {
        try {
            Map<String, List<Store>> results = new HashMap<>();
            List<Store> matchingStores = new ArrayList<>();

            synchronized(stores) {
                if (stores.isEmpty()) {
                    System.out.println("[Worker] Warning: No stores available for search");
                }

                for (Store s : stores.values()) {
                    if (s.matches(req)) {
                        matchingStores.add(s);
                    }
                }
            }

            if (!matchingStores.isEmpty()) {
                results.put(req.getSearchTerm(), matchingStores);
                out.writeObject(results);
                out.flush();
                System.out.println("[Worker] Sent " + matchingStores.size() + " search results to master");
            } else {
                // Send empty but valid response instead of nothing
                out.writeObject(results);
                out.flush();
                System.out.println("[Worker] No matching stores found for search criteria");
            }
        } catch (IOException e) {
            System.err.println("[Worker] Search processing failed: " + e.getMessage());
            try {
                out.writeObject(new HashMap<>()); // Send empty result instead of failing
                out.flush();
            } catch (IOException ignored) {}
        }
    }


    private void processBuyRequest(BuyRequest buy, ObjectOutputStream out) throws IOException {
        Store store = null;
        for(Store s : this.stores.values()) {
            if (s.getName().equalsIgnoreCase(buy.getStoreName())) {
                store = s;
            }
        }
        if (store != null) {
            List<String> results = new ArrayList();
            boolean allSuccessful = true;
            Store responseStore = new Store(store);
            synchronized(store) {
                for(OrderItem item : buy.getProducts()) {
                    boolean success = responseStore.buyProduct(item);
                    results.add(success ? "OK: Bought " + item.getQuantity() + " x " + item.getProductName() : "ERROR: Not enough stock for " + item.getProductName());
                    if (!success) {
                        allSuccessful = false;
                    }
                }

                if (allSuccessful) {
                    store.copyFrom(responseStore);
                    out.writeObject(List.of(results, responseStore));
                    System.out.println("[Worker] BuyRequest processed for store: " + store.getName());
                    System.out.println("Updated stock levels:");

                    for(Product p : store.getProducts()) {
                        PrintStream var10000 = System.out;
                        String var10001 = p.getName();
                        var10000.println(" - " + var10001 + ": " + p.getStock());
                    }
                } else {
                    out.writeObject(List.of(results, (Object)null));
                }
            }
        } else {
            out.writeObject(List.of(List.of("ERROR: Store not found"), (Object)null));
        }

        out.flush();
    }

    private void processUpdateStockRequest(UpdateStockRequest req, ObjectOutputStream out) throws IOException {
        Store store = null;
        for(Store s : this.stores.values()) {
            if (s.getName().equalsIgnoreCase(req.getStoreName())) {
                store = s;
            }
        }
        if (store != null) {
            synchronized (store) {
                Product product = store.findProduct(req.getProductName());
                if (product != null) {
                    // Update initialStock if it's not set
                    if (product.getInitialStock() == 0) {
                        product.setInitialStock(product.getStock());
                    }
                    // Perform the update
                    store.updateProductStock(req.getProductName(), req.getNewStock());
                }
                // Return confirmation + the ACTUAL updated store
                out.writeObject(List.of(
                        "OK: Stock updated to " + req.getNewStock(),
                        new Store(store) // Fresh copy
                ));
            }
        } else {
            out.writeObject(List.of("ERROR: Store not found", null));
        }
    }

    private void processAddProductsRequest(AddProductsRequest req, ObjectOutputStream out) throws IOException {
        Store store = null;
        for(Store s : this.stores.values()) {
            if (s.getName().equalsIgnoreCase(req.getStoreName())) {
                store = s;
            }
        }        if (store != null) {
            Store responseStore = new Store(store);
            synchronized(store) {
                for(Product p : req.getProducts()) {
                    p.setInitialStock(p.getStock());
                    responseStore.addProduct(p);
                }

                store.copyFrom(responseStore);
                out.writeObject(List.of("OK: Added " + req.getProducts().size() + " products", responseStore));
                System.out.println("[Worker] Added products to store " + req.getStoreName());
                responseStore.getProducts().forEach((px) -> {
                    PrintStream var10000 = System.out;
                    String var10001 = px.getName();
                    var10000.println(" - " + var10001 + ": " + px.getStock());
                });
            }
        } else {
            out.writeObject(List.of("ERROR: Store not found", (Object)null));
        }

        out.flush();
    }

    private void processHideProductRequest(HideProductRequest req, ObjectOutputStream out) throws IOException {
        Store store = null;
        for(Store s : this.stores.values()) {
            if (s.getName().equalsIgnoreCase(req.getStoreName())) {
                store = s;
            }
        }
        if (store != null) {
            Store responseStore = new Store(store);
            synchronized(store) {
                responseStore.setProductVisibility(req.getProductName(), false);
                store.copyFrom(responseStore);
                out.writeObject(List.of("OK: Product hidden", responseStore));
                PrintStream var10000 = System.out;
                String var10001 = req.getProductName();
                var10000.println("[Worker] Hid product " + var10001 + " in store " + req.getStoreName());
            }
        } else {
            out.writeObject(List.of("ERROR: Store not found", (Object)null));
        }

        out.flush();
    }

    private void processRateStoreRequest(RateStoreRequest req, ObjectOutputStream out) throws IOException {
        Store store = null;
        for(Store s : this.stores.values()) {
            if (s.getName().equalsIgnoreCase(req.getStoreName())) {
                store = s;
            }
        }
        if (store != null) {
            synchronized(store) {
                double newRating = (store.getRating() * (double)store.getReviews() + req.getRating()) / (double)(store.getReviews() + 1);
                store.setRating(newRating);
                store.setReviews(store.getReviews() + 1);
                out.writeObject(List.of("OK: New rating is " + newRating, store));
                System.out.printf("[Worker] Updated rating for %s to %.1f (%d reviews)\n", store.getName(), newRating, store.getReviews());
            }
        } else {
            out.writeObject(List.of("ERROR: Store not found", (Object)null));
        }

        out.flush();
    }

    private void processGetStoreRequest(GetStoreRequest req, ObjectOutputStream out) throws IOException {
        Store store = null;
        for(Store s : this.stores.values()) {
            if (s.getName().equalsIgnoreCase(req.getStoreName())) {
                store = s;
            }
        }
        if (store != null) {
            Store responseStore = req.isForceRefresh() ? new Store(store) : store;
            out.writeObject(responseStore);
            PrintStream var10000 = System.out;
            boolean var10001 = req.isForceRefresh();
            var10000.println("[Worker] Sent store data (forceRefresh=" + var10001 + "): " + String.valueOf(responseStore));
        } else {
            out.writeObject("ERROR: Store not found");
        }

        out.flush();
    }

    private void processSalesQueryRequest(SalesQueryRequest query, ObjectOutputStream out) throws IOException {
        Map<String, Integer> salesData = new HashMap<>();

        synchronized(stores) {
            stores.values().forEach(store -> {
                if (query.getType() == SalesQueryRequest.Type.BY_STORE_TYPE) {
                    // Πωλήσεις ανά τύπο καταστήματος
                    String key = store.getFoodCategory().toString();
                    int sales = store.getProducts().stream()
                            .mapToInt(p -> p.getInitialStock() - p.getStock())
                            .sum();
                    salesData.merge(key, sales, Integer::sum);
                }
                else if (query.getType() == SalesQueryRequest.Type.BY_PRODUCT_CATEGORY) {
                    // Πωλήσεις ανά κατηγορία προϊόντος
                    store.getProducts().forEach(p -> {
                        String key = p.getCategory().toString();
                        int itemSales = p.getInitialStock() - p.getStock();
                        salesData.merge(key, itemSales, Integer::sum);
                    });
                }
            });
        }

        out.writeObject(new SalesResult(salesData, salesData.values().stream().mapToInt(i->i).sum()));
    }
}
