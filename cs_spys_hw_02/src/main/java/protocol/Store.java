package protocol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class Store {
    private final ConcurrentHashMap<String, AtomicInteger> quantities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> prices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();

    public int getQuantity(String product) {
        AtomicInteger q = quantities.get(product);
        return q == null ? 0 : q.get();
    }

    public void addQuantity(String product, int amount) {
        quantities.putIfAbsent(product, new AtomicInteger(0));
        quantities.get(product).addAndGet(amount);
    }

    public boolean deductQuantity(String product, int amount) {
        quantities.putIfAbsent(product, new AtomicInteger(0));
        AtomicInteger q = quantities.get(product);
        while (true) {
            int current = q.get();
            if (current < amount) return false;
            if (q.compareAndSet(current, current - amount)) return true;
        }
    }

    public void addGroup(String groupName) {
        groups.putIfAbsent(groupName, new CopyOnWriteArraySet<>());
    }

    public void addProductToGroup(String groupName, String product) {
        addGroup(groupName);
        groups.get(groupName).add(product);
    }

    public void setPrice(String product, double price) {
        prices.put(product, price);
    }
}
