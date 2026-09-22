package reducer;

import common.SalesResult;
import common.Store;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class ReducerHandler implements Runnable {
    private final Socket socket;

    public ReducerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ) {
            Object obj = in.readObject();

            if (obj instanceof Map<?, ?> rawMap) {
                if (isSalesData(rawMap)) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, List<Integer>> salesData = (Map<String, List<Integer>>) rawMap;
                        Map<String, Integer> reduced = new HashMap<>();
                        int total = 0;

                        for (var entry : salesData.entrySet()) {
                            int sum = entry.getValue().stream().mapToInt(i -> i).sum();
                            reduced.put(entry.getKey(), sum);
                            total += sum;
                        }

                        SalesResult result = new SalesResult(reduced, total);
                        out.writeObject(result);
                        out.flush();
                        System.out.println("[Reducer] Reduced sales data. Total sales = " + total);
                    } catch (IOException e) {
                        System.err.println("[Reducer] Failed to send response: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (isSearchData(rawMap)) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<Store>> searchMap = (Map<String, List<Store>>) rawMap;
                    List<Store> merged = new ArrayList<>();
                    for (List<Store> list : searchMap.values()) {
                        merged.addAll(list);
                    }
                    out.writeObject(merged);
                    out.flush();
                    System.out.println("[Reducer] Reduced search data. Found " + merged.size() + " stores.");
                } else {
                    out.writeObject("ERROR: Unknown map data format");
                    out.flush();
                }

            } else {
                out.writeObject("ERROR: Invalid data received");
                out.flush();
            }

        } catch (SocketTimeoutException e) {
            System.err.println("[Reducer] Worker timeout - check network connectivity");
        } catch (Exception e) {
            System.err.println("[Reducer] Error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static boolean isSalesData(Map<?, ?> map) {
        return map.keySet().stream().allMatch(k -> k instanceof String)
                && map.values().stream().allMatch(v -> v instanceof List<?> list && list.stream().allMatch(i -> i instanceof Integer));
    }

    private static boolean isSearchData(Map<?, ?> map) {
        return map.keySet().stream().allMatch(k -> k instanceof String)
                && map.values().stream().allMatch(v -> v instanceof List<?> list && list.stream().allMatch(i -> i instanceof Store));
    }
}