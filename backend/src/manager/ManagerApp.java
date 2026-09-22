package manager;

import java.io.*;
import java.net.*;
import java.util.*;
import common.*;

public class ManagerApp {
    static Scanner s = new Scanner(System.in);
    private String masterHost;
    private int masterPort;
    private static String clientId = UUID.randomUUID().toString();

    public ManagerApp(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Χρήση: java ManagerApp <ip> <port>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        ManagerApp manager = new ManagerApp(ip, port);
        manager.run();
    }

    public void run() {
        try {
            int c;
            do {
                do {
                    System.out.println("------------Manager Console------------\n 1. Add Store \n 2. Change stock \n " +
                            "3. Add new products \n 4. Delete product \n 5. Show total sales per product \n 0. I want to leave");
                    try {
                        c = s.nextInt();
                        s.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println("Error: Please enter a valid integer option.");
                        s.nextLine();
                        c = -1;
                    }
                } while (c > 5 || c < 0);

                if (c == 1) {
                    handleAddStore();
                } else if (c == 2) {
                    handleChangeStock();
                } else if (c == 3) {
                    handleAddProducts();
                } else if (c == 4) {
                    handleDeleteProduct();
                } else if (c == 5) {
                    handleShowSales();
                }
            } while (c != 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddStore() {
        System.out.println("Give the store name:");
        String folder = s.nextLine().trim();

        if (folder.isEmpty()) {
            System.out.println("Error: Store name cannot be empty");
            return;
        }

        try {
            Store store = addStore(folder);
            System.out.println("Debug: Created store object: " + store);
            System.out.println("Debug: Store name: " + store.getName());
            System.out.println("Debug: Number of products: " + store.getProducts().size());

            Object response = sendRequestToMaster(store);
            System.out.println("Server response: " + response);
        } catch (IOException e) {
            System.err.println("Error creating store: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChangeStock() {
        try {
            String name = readStoreName();
            Store store = fetchStoreFromWorker(name);

            // Show current stock
            System.out.println("Current stock:");
            store.getProducts().forEach(p ->
                    System.out.println(p.getName() + " - " + p.getPrice() + " - " + p.getStock())
            );

            // Get update info
            System.out.println("Choose a product:");
            String productName = s.nextLine().trim();
            while(store.findProduct(productName) == null) {
                System.out.println("Product not found. Try again:");
                productName = s.nextLine().trim();
                if (productName.isEmpty()) {
                    return;
                }
            }
            System.out.print("Enter new stock: ");
            int newStock = s.nextInt();

            s.nextLine(); // Consume newline

            // Send single update request
            UpdateStockRequest request = new UpdateStockRequest(
                    store.getName(), productName, newStock, clientId
            );
            Object response = sendRequestToMaster(request);

            // Parse response
            if (response instanceof List) {
                List<?> responseList = (List<?>) response;
                if (!responseList.isEmpty()) {
                    System.out.println("Update result: " + responseList.get(0));
                }
                if (responseList.size() > 1 && responseList.get(1) instanceof Store) {
                    Store updatedStore = (Store) responseList.get(1);
                    System.out.println("Final stock levels:");
                    updatedStore.getProducts().forEach(p ->
                            System.out.println(p.getName() + ": " + p.getStock())
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleAddProducts() {
        try {
            String name = readStoreName();
            Store store = fetchStoreFromWorker(name);
            List<Product> newProducts = readProductsFromInput();

            AddProductsRequest request = new AddProductsRequest(
                    store.getName(),
                    newProducts,
                    clientId
            );

            Object response = sendRequestToMaster(request);
            System.out.println("Add result: " + response);

            // Verify update
            Store updated = fetchStoreFromWorker(store.getName());
            System.out.println("Updated product list:");
            updated.getProducts().forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleDeleteProduct() {
        try {
            String name = readStoreName();
            Store store = fetchStoreFromWorker(name);
            Product p = deleteProduct(store);

            HideProductRequest request = new HideProductRequest(
                    store.getName().trim(),
                    p.getName(),
                    clientId
            );

            Object response = sendRequestToMaster(request);
            System.out.println("Delete result: " + response);

            // Verify update
            Store updated = fetchStoreFromWorker(store.getName());
            System.out.println("Updated product list:");
            updated.getProducts().forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleShowSales() {
        try {
            showSales();
        } catch (Exception e) {
            System.out.println("Error showing sales: " + e.getMessage());
        }
    }

    private Product deleteProduct(Store store) {
        // First refresh the store data
        Store freshStore = fetchStoreFromWorker(store.getName());
        showProducts(freshStore);
        String ch = s.nextLine();
        for (Product p : freshStore.getProducts()) {
            if (p.getName().equalsIgnoreCase(ch)) {
                p.setVisibleToClient(false); //oxi pragmatiko delete apla den emfanizontai ston pelath
                Store updated = fetchStoreFromWorker(freshStore.getName());
                updated.getProducts().stream()
                        .filter(prod -> prod.getName().equals(p.getName()))
                        .findFirst()
                        .ifPresent(prod ->
                                System.out.println("Updated stock: " + prod.getStock())
                        );
                return p;
            }
        }
        throw new RuntimeException("Product not found");
    }


    public void showSales() {
        System.out.println("Sales report type:\n1. By Product Category\n2. By Store Type");
        int option = s.nextInt();
        s.nextLine();

        try {
            // Ανακτάμε όλα τα stores
            List<Store> allStores = getAllStores();

            if (allStores.isEmpty()) {
                System.out.println("No stores found!");
                return;
            }

            Map<String, Integer> salesMap = new HashMap<>();
            int grandTotal = 0;

            // Υπολογισμός πωλήσεων
            for (Store store : allStores) {
                if (option == 1) {
                    // Ανά κατηγορία προϊόντος
                    for (Product p : store.getProducts()) {
                        String category = p.getCategory().toString();
                        int sold = p.getInitialStock() - p.getStock();
                        salesMap.merge(category, sold, Integer::sum);
                        grandTotal += sold;
                    }
                } else {
                    // Ανά τύπο καταστήματος
                    String storeType = store.getFoodCategory().toString();
                    int storeSales = store.getProducts().stream()
                            .mapToInt(p -> p.getInitialStock() - p.getStock())
                            .sum();
                    salesMap.merge(storeType, storeSales, Integer::sum);
                    grandTotal += storeSales;
                }
            }

            // Εκτύπωση αποτελεσμάτων
            printSalesReport(salesMap, grandTotal, option);

        } catch (Exception e) {
            System.err.println("Error generating sales report: " + e.getMessage());
        }
    }

    private List<Store> getAllStores() throws Exception {
        try (Socket socket = new Socket(masterHost, masterPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new GetAllStoresRequest(clientId));
            Object response = in.readObject();

            if (response instanceof List<?>) {
                return (List<Store>) response;
            }
            return Collections.emptyList();
        }
    }

    private void printSalesReport(Map<String, Integer> sales, int total, int reportType) {
        String reportTitle = (reportType == 1) ? "PRODUCT CATEGORY" : "STORE TYPE";

        System.out.println("\n=== SALES REPORT ===");
        System.out.printf("%-20s | SALES\n", reportTitle);
        System.out.println("---------------------|-------");

        sales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    System.out.printf("%-20s | %5d\n", entry.getKey(), entry.getValue());
                });

        System.out.println("---------------------|-------");
        System.out.printf("%-20s | %5d\n", "TOTAL", total);
        System.out.println("=====================|=======");
    }


    public static void showProducts(Store store) {
        for (Product p : store.getProducts()) {
            System.out.println(p.getName() + " - " + p.getPrice() + " - " + p.getStock());
        }
        System.out.println("Choose a product:");
    }

    public static Store addStore(String folder) throws IOException {
        // diavazei to store.json xeirokinhta
        if (folder == null || folder.trim().isEmpty()) {
            throw new IllegalArgumentException("Store folder name cannot be empty");
        }

        String folderPath = "stores" + File.separator + folder;
        File jsonFile = new File(folderPath, "store.json");

        if (!jsonFile.exists()) {
            throw new FileNotFoundException("Store configuration file not found: " + jsonFile.getPath());
        }

        BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        reader.close();

        String json = jsonBuilder.toString();

        Store store = new Store();
        String name = extractValue(json, "name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Store name cannot be empty in JSON file");
        }
        store.setName(name.trim());

        store.setLocation(new Location(extractDoubleValue(json, "latitude"), extractDoubleValue(json, "longitude")));
        store.setRating(extractDoubleValue(json, "rating"));
        store.setReviews(extractIntValue(json, "reviews"));
        store.setLogoPath(extractValue(json, "logo"));
        store.setFoodCategory(FoodCategory.valueOf(extractValue(json, "category").toUpperCase()));
        store.setProducts(extractProducts(json));
        store.updatePriceCategory();
        return store;
    }


    public Object sendRequestToMaster(Object request) {
        int maxRetries = 3;
        int attempt = 0;
        long timeout = 5000; // 5 seconds

        while (attempt < maxRetries) {
            try (Socket socket = new Socket(masterHost, masterPort)) {
                socket.setSoTimeout((int) timeout);

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(request);
                out.flush();

                return in.readObject();
            } catch (SocketTimeoutException e) {
                System.err.println("[Manager] Timeout, retrying... (" + (attempt + 1) + "/" + maxRetries + ")");
                attempt++;
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Failed to communicate with Master: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Failed after " + maxRetries + " retries");
    }



    private static List<Product> readProductsFromInput() {
        List<Product> products = new ArrayList<>();

        System.out.println("How many products do you want to add?");
        int numProducts = s.nextInt();
        s.nextLine();

        for (int i = 0; i < numProducts; i++) {
            System.out.println("\nAdding product " + (i + 1) + ":");

            System.out.print("Product name: ");
            String productName = s.nextLine();

            System.out.print("Price (€): ");
            double price = s.nextDouble();

            System.out.print("Stock quantity: ");
            int stock = s.nextInt();
            s.nextLine();

            System.out.print("Category: ");
            for (ProductCategory c : ProductCategory.values()) {
                System.out.println(c.toString());
            }
            String categoryStr = s.nextLine().toUpperCase();

            ProductCategory category;
            try {
                category = ProductCategory.valueOf(categoryStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid category");
                continue;
            }

            products.add(new Product(productName, price, stock, category));
        }

        return products;
    }

    public Store fetchStoreFromWorker(String storeName) {
        try (Socket socket = new Socket(masterHost, masterPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new GetStoreRequest(storeName.trim(), true, clientId));
            out.flush();

            Object response = in.readObject();
            if (response instanceof Store) {
                return (Store) response;
            } else if (response instanceof String) {
                throw new RuntimeException("Error from server: " + response);
            }
            throw new RuntimeException("Invalid response type: " + (response != null ? response.getClass() : "null"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch store '" + storeName + "': " + e.getMessage(), e);
        }
    }

    private static String readStoreName() {
        System.out.print("Give the store name: ");
        return s.nextLine().trim();
    }

    // gia to parsing apo ta json arxeia
    private static String extractValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) {
            throw new IllegalStateException("Key '" + key + "' not found in JSON");
        }

        int start = json.indexOf(':', keyIndex) + 1;
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length()) {
            throw new IllegalStateException("Invalid JSON format for key '" + key + "'");
        }

        // Handle both quoted and unquoted values
        if (json.charAt(start) == '"') {
            start++; // Skip the opening quote
            int end = json.indexOf('"', start);
            if (end == -1) {
                throw new IllegalStateException("Missing closing quote for key '" + key + "'");
            }
            return json.substring(start, end).trim();
        } else {
            // Handle numeric or boolean values
            int end = start;
            while (end < json.length() && !Character.isWhitespace(json.charAt(end))
                    && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            String value = json.substring(start, end).trim();
            if (value.isEmpty()) {
                throw new IllegalStateException("Value for '" + key + "' cannot be empty");
            }
            return value;
        }
    }

    private static double extractDoubleValue(String json, String key) {
        String value = extractValue(json, key);
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid numeric value for key '" + key + "': " + value);
        }
    }


    private static int extractIntValue(String json, String key) {
        return Integer.parseInt(extractValue(json, key).replace(",", "").trim());
    }

    private static List<Product> extractProducts(String json) {
        List<Product> products = new ArrayList<>();

        int start = json.indexOf("\"products\": [") + "\"products\": [".length();
        int end = json.indexOf("]", start);
        String productsBlock = json.substring(start, end);

        String[] productEntries = productsBlock.split("\\},\\s*\\{");
        for (String entry : productEntries) {
            String clean = entry.replace("{", "").replace("}", "").trim();
            String[] fields = clean.split(",");

            String name = "";
            ProductCategory category = null;
            double price = 0;
            int stock = 0;

            for (String field : fields) {
                String[] keyVal = field.split(":");
                if (keyVal.length != 2) continue;
                String key = keyVal[0].replace("\"", "").trim();
                String value = keyVal[1].replace("\"", "").trim();

                switch (key) {
                    case "name": name = value; break;
                    case "price": price = Double.parseDouble(value); break;
                    case "stock": stock = Integer.parseInt(value); break;
                    case "category": category = ProductCategory.valueOf(value.toUpperCase()); break;
                }
            }

            products.add(new Product(name, price, stock, category));
        }

        return products;
    }
}
