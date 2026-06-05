package protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ProductService {
    private final ConcurrentHashMap<String, Product> storage = new ConcurrentHashMap<>();

    public Product create(String name, String category, int quantity, double price) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("назва товару не може бути порожньою");
        if (quantity < 0)
            throw new IllegalArgumentException("кількість не може бути від'ємною");
        if (price < 0)
            throw new IllegalArgumentException("ціна не може бути від'ємною");

        Product product = new Product(name, category, quantity, price);
        storage.put(product.getId(), product);
        return product;
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Product update(String id, String name, String category, Integer quantity, Double price) {
        Product[] result = {null};
        storage.compute(id, (k, product) -> {
            if (product == null)
                throw new IllegalArgumentException("товар не знайдено: " + id);
            if (name != null && !name.isBlank())     product.setName(name);
            if (category != null)                     product.setCategory(category);
            if (quantity != null) {
                if (quantity < 0) throw new IllegalArgumentException("кількість не може бути від'ємною");
                product.setQuantity(quantity);
            }
            if (price != null) {
                if (price < 0) throw new IllegalArgumentException("ціна не може бути від'ємною");
                product.setPrice(price);
            }
            result[0] = product;
            return product;
        });
        if (result[0] == null)
            throw new IllegalArgumentException("товар не знайдено: " + id);
        return result[0];
    }

    public boolean delete(String id) {
        return storage.remove(id) != null;
    }

    public PageResult<Product> search(ProductFilter filter) {
        List<Product> matched = new ArrayList<>();
        for (Product p : storage.values()) {
            if (filter.matches(p)) matched.add(p);
        }
        matched.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int total = matched.size();
        int from = filter.getPage() * filter.getPageSize();
        int to = Math.min(from + filter.getPageSize(), total);

        List<Product> page = (from >= total) ? List.of() : matched.subList(from, to);
        return new PageResult<>(new ArrayList<>(page), filter.getPage(), filter.getPageSize(), total);
    }

    public List<Product> getAll() {
        return new ArrayList<>(storage.values());
    }
}
