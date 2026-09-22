package client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import common.*;

public class ClientApp {
    private String clientId = UUID.randomUUID().toString();
    private String masterHost ;
    private int masterPort;

    public ClientApp(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Χρήση: java ClientApp <ip> <port>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        ClientApp client = new ClientApp(ip, port);
        client.run();
    }

    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("------------Client Console------------");
                System.out.println("1. Search Store");
                System.out.println("0. Exit");

                int choice = getIntInput(scanner, "Choose an option: ");
                if (choice == 0) break;
                else if (choice == 1) handleStoreInteraction(scanner);
                else System.out.println("Invalid option. Try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStoreInteraction(Scanner scanner) {
        Store selectedStore = handleSearch(scanner);
        if (selectedStore == null) return;

        for(Product p : selectedStore.getProducts()) {
            System.out.println(p.getName() + "\tPrice: " + p.getPrice());
        }
        int buyChoice = getBinaryInput(scanner, "Do you want to buy products? (1=Yes, 0=No): ");
        while (buyChoice != 1 && buyChoice != 0) {
            System.out.println("Invalid option. Try again.");
            buyChoice = getBinaryInput(scanner, "Do you want to buy products? (1=Yes, 0=No): ");
        }
        boolean didBuy = false;

        if (buyChoice == 1) {
            didBuy = handleBuy(selectedStore, scanner);
        }

        if (didBuy) {
            int rateChoice = getBinaryInput(scanner, "Do you want to rate this store? (1=Yes, 0=No): ");
            while (rateChoice != 1 && rateChoice != 0) {
                System.out.println("Invalid option. Try again.");
                rateChoice = getBinaryInput(scanner, "Do you want to rate this store? (1=Yes, 0=No): ");
            }
            if (rateChoice == 1) {
                handleRate(selectedStore.getName(), scanner);
            }
        }
    }

    private boolean handleBuy(Store store, Scanner scanner) {
        List<OrderItem> products = new ArrayList<>();
        scanner.nextLine();

        while (true) {
            store.getVisibleProducts().forEach(p -> System.out.println(p.getName() + "\tPrice: " + p.getPrice()));
            System.out.print("Enter product name (or 'done'): ");
            String productName = scanner.nextLine().trim();
            if (productName.equalsIgnoreCase("done")) break;

            Map<String, Product> productMap = store.getVisibleProducts().stream()
                    .collect(Collectors.toMap(p -> p.getName().toLowerCase(), p -> p));

            if (!productMap.containsKey(productName.toLowerCase())) {
                System.out.println("Invalid product. Available products:");
                productMap.values().forEach(p -> System.out.println(" - " + p.getName()));
                continue;
            }
            int quantity = getPositiveIntInput(scanner, "Enter quantity: ");
            if (quantity <= 0){
                System.out.println("Invalid quantity. Please enter a positive number.");
                continue;
            }else if (quantity > productMap.get(productName).getStock()){
                System.out.println("Invalid quantity. The store only has " + productMap.get(productName.toLowerCase()).getStock() + " " + productName + "s.");
                continue;
            }
            products.add(new OrderItem(productMap.get(productName.toLowerCase()).getName(), quantity));
        }

        if (products.isEmpty()) {
            System.out.println("No products selected.");
            return false;
        }

        boolean[] purchaseSuccessful = {false};

        Thread purchaseThread = new Thread(() -> {
            try (Socket socket = new Socket(masterHost, masterPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                BuyRequest buyRequest = new BuyRequest(store.getName(), products, clientId);
                out.writeObject(buyRequest);
                out.flush();

                Object response = in.readObject();
                if (response instanceof List<?> list && list.size() == 2) {
                    Object buyResult = list.get(0);
                    Object updatedStore = list.get(1);

                    purchaseSuccessful[0] = processPurchaseResults(buyResult);
                    if (purchaseSuccessful[0] && updatedStore instanceof Store) {
                        displayPurchaseConfirmation((Store) updatedStore, products);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error during purchase: " + e.getMessage());
            }
        });

        purchaseThread.start();

        try {
            purchaseThread.join();  // Wait for the purchase thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return purchaseSuccessful[0];  // Return the result of the purchase
    }

    private static int getPositiveIntInput(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int input = scanner.nextInt();
                scanner.nextLine(); // consume newline
                if (input > 0) {
                    return input;
                } else {
                    System.out.println("Please enter a positive number.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // clear invalid input
            }
        }
    }

    private static boolean processPurchaseResults(Object buyResult) {
        if (buyResult instanceof List<?> messages) {
            boolean success = true;
            System.out.println("Purchase results:");
            for (Object msg : messages) {
                System.out.println(msg);
                if (msg instanceof String s && s.startsWith("ERROR")) {
                    success = false;
                }
            }
            return success;
        }
        return false;
    }

    private static void displayPurchaseConfirmation(Store updated, List<OrderItem> products) {
        System.out.println("\n=== Purchase Confirmation ===");
        System.out.println("Store: " + updated.getName());

        for (OrderItem item : products) {
            updated.getProducts().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(item.getProductName()))
                    .findFirst()
                    .ifPresent(p -> System.out.printf(
                            " - %s: %d → %d (bought %d)\n",
                            p.getName(),
                            p.getStock() + item.getQuantity(),
                            p.getStock(),
                            item.getQuantity()
                    ));
        }
    }

    private static int getIntInput(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter an integer.");
                scanner.nextLine();
            }
        }
    }

    private static int getBinaryInput(Scanner scanner, String prompt) {
        while (true) {
            int input = getIntInput(scanner, prompt);
            if (input == 0 || input == 1) return input;
            System.out.println("Invalid input. Please enter 1 or 0.");
        }
    }

    private Store handleSearch(Scanner scanner) {
        try {
            PriceCategory priceCategory = getPriceCategoryInput(scanner);
            double minStars = getMinimumRatingInput(scanner);
            List<FoodCategory> foodCategories = getFoodCategoriesInput(scanner);

            SearchRequest request = new SearchRequest(
                    priceCategory,
                    foodCategories,
                    minStars,
                    37.997654114908705, // Default latitude
                    23.73533676959039,  // Default longitude
                    5.0,                // Search radius in km
                    clientId
            );

            // 1. First try direct Master connection
            try (Socket socket = new Socket(masterHost, masterPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(request);
                Object response = in.readObject();
                System.out.println("Server response: " + response.toString());

                // 2. Handle response from Master/Reducer
                if (response instanceof String && ((String)response).startsWith("Error")) {
                    System.out.println("Search failed: " + response);
                    return null;
                }

                if (response instanceof List<?> storeList) {
                    List<Store> stores = storeList.stream()
                            .filter(Store.class::isInstance)
                            .map(Store.class::cast)
                            .collect(Collectors.toList());

                    if (stores.isEmpty()) {
                        System.out.println("No stores match your criteria.");
                        return null;
                    }

                    // Display results
                    System.out.println("Found " + stores.size() + " stores:");
                    for (int i = 0; i < stores.size(); i++) {
                        Store s = stores.get(i);
                        System.out.printf("%d. %s (Rating: %.1f, Price: %s)\n",
                                i+1, s.getName(), s.getRating(), s.getPriceCategory());
                    }

                    // Store selection
                    int choice = getIntInput(scanner, "Select store (0 to cancel): ");
                    return (choice > 0 && choice <= stores.size())
                            ? stores.get(choice-1)
                            : null;
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to server. Please check:");
            System.out.println("- Server is running at " + masterHost + ":" + masterPort);
            System.out.println("- Your network connection");
        } catch (SocketTimeoutException e) {
            System.out.println("Search timeout. The system is busy.");
        } catch (Exception e) {
            System.out.println("Search failed: " + e.getMessage());
        }
        return null;
    }


    private void handleRate(String storeName, Scanner scanner) {
        while (true) {
            System.out.print("Enter rating (1.0 - 5.0) or 'skip': ");
            String input = scanner.nextLine().trim();
            scanner.nextLine();
            if (input.equalsIgnoreCase("skip")) {
                System.out.println("Rating skipped.");
                return;
            }

            try {
                double rating = Double.parseDouble(input);
                if (rating >= 1.0 && rating <= 5.0) {
                    sendRatingToServer(storeName, rating);
                    break;
                } else {
                    System.out.println("Rating must be between 1.0 and 5.0.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number like 4.5 or type 'skip'.");
            }

        }
    }

    private void sendRatingToServer(String storeName, double rating) {
        try (Socket socket = new Socket(masterHost, masterPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            RateStoreRequest request = new RateStoreRequest(storeName, rating, clientId);
            out.writeObject(request);
            out.flush();

            Object response = in.readObject();
            System.out.println("Server response: " + response);
        } catch (Exception e) {
            System.err.println("Error during rating: " + e.getMessage());
        }
    }

    private static PriceCategory getPriceCategoryInput(Scanner scanner) {
        PriceCategory priceCategory = null;
        EnumSet<PriceCategory> allowed = EnumSet.of(PriceCategory.$, PriceCategory.$$, PriceCategory.$$$);
        while (priceCategory == null) {
            System.out.print("Price Category ($, $$, $$$): ");
            scanner.nextLine();
            String priceCategoryStr = scanner.nextLine().trim();
            try {
                priceCategory = PriceCategory.valueOf(priceCategoryStr.toUpperCase());
                if (!allowed.contains(priceCategory)) {
                    System.out.println("Invalid price category. Please enter one of $, $$, $$$.");
                    priceCategory = null;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid category. Please enter one of $, $$, $$$.");
            }
        }
        return priceCategory;
    }

    private static double getMinimumRatingInput(Scanner scanner) {
        double minStars = 0.0;
        while (true) {
            System.out.print("Minimum rating (1.0-5.0): ");
            try {
                minStars = scanner.nextDouble();
                scanner.nextLine(); // consume newline
                if (minStars >= 1.0 && minStars <= 5.0) {
                    break;
                } else {
                    System.out.println("Rating must be between 1.0 and 5.0. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid rating.");
                scanner.nextLine(); // clear the buffer
            }
        }
        return minStars;
    }

    private static List<FoodCategory> getFoodCategoriesInput(Scanner scanner) {
        List<FoodCategory> foodCategories = new ArrayList<>();
        System.out.println("Food Categories (type 'done' to finish):");
        for (FoodCategory c : FoodCategory.values()) {
            System.out.println(c.toString());
        }

        while (true) {
            String category = scanner.nextLine().trim();
            if (category.equalsIgnoreCase("done")) break;
            try {
                FoodCategory foodCategory = FoodCategory.valueOf(category.toUpperCase());
                foodCategories.add(foodCategory);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid category. Please enter one of the listed food categories or 'done' to finish.");
            }
        }
        return foodCategories;
    }

}